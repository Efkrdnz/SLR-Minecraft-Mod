package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dungeon.runtime.DungeonInstanceSavedData;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative, gamerule-gated block destruction for player abilities.
 *
 * <p>Every request is converted to a bounded list of already-loaded block
 * positions. Mutations are then spread across server ticks, do not create item
 * drops, and pass normal Forge/player protection checks. This keeps the large
 * monarch effects dramatic without using vanilla explosions or loading chunks.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class AbilityDestructionManager {
	public static final TagKey<Block> IMMUNE_BLOCKS = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "ability_destruction_immune"));

	private static final int MAX_MUTATIONS_PER_TICK = 96;
	private static final int MAX_INSPECTIONS_PER_TICK = 512;
	private static final int MAX_MUTATIONS_PER_JOB_PER_TICK = 48;
	private static final int MAX_QUEUED_POSITIONS = 24576;
	private static final int MAX_QUEUED_JOBS = 128;
	private static final int MAX_QUEUED_POSITIONS_PER_OWNER = 6144;
	private static final int MAX_QUEUED_JOBS_PER_OWNER = 24;
	private static final int MAX_REQUESTS_PER_OWNER_PER_TICK = 4;
	private static final int MAX_REQUESTS_PER_TICK = 24;
	private static final ArrayDeque<DestructionJob> JOBS = new ArrayDeque<>();
	private static final Set<QueuedBlock> QUEUED_BLOCKS = new HashSet<>();
	private static final Map<UUID, Integer> OWNER_REQUESTS_THIS_TICK =
			new HashMap<>();
	private static final ThreadLocal<Boolean> POSTING_BREAK_EVENT =
			ThreadLocal.withInitial(() -> false);
	private static int queuedPositions;
	private static int requestTick = Integer.MIN_VALUE;
	private static int requestsThisTick;

	private AbilityDestructionManager() {
	}

	/** Profiles are intentionally finite; attributes interpolate within these bounds. */
	public enum Profile {
		FROST_SPEAR(5, 14, 0.7D, 1.25D, 1.5D, 5.0D),
		ARCANE_IMPACT(14, 48, 1.3D, 2.8D, 3.0D, 14.0D),
		ARCANE_CONVERGENCE(360, 1100, 6.0D, 14.0D, 18.0D, 55.0D),
		BARRIER_COLLAPSE(40, 140, 2.5D, 4.5D, 4.0D, 18.0D),
		BARRIER_CATASTROPHE(360, 1000, 6.0D, 11.0D, 18.0D, 55.0D),
		FIRE_ORB(24, 90, 2.0D, 4.2D, 4.0D, 22.0D),
		FIRE_DOMINION(220, 720, 5.0D, 10.0D, 12.0D, 40.0D),
		FIRE_HEAVENFALL(500, 1600, 8.0D, 18.0D, 20.0D, 65.0D),
		FIRE_BEAM_CHARGED(90, 320, 2.5D, 5.0D, 10.0D, 40.0D),
		FIRE_BEAM_OVERCHARGED(180, 600, 3.5D, 7.0D, 15.0D, 60.0D),
		STORM_THUNDERCLAP(60, 220, 2.5D, 5.0D, 8.0D, 28.0D),
		STORM_SKYBREAKER(240, 780, 5.0D, 10.0D, 15.0D, 50.0D),
		WHITE_FLAME_BREATH(70, 240, 2.0D, 4.5D, 10.0D, 45.0D),
		WHITE_FLAME_SPEAR(18, 64, 1.2D, 2.6D, 5.0D, 24.0D),
		WHITE_FLAME_HELLSTORM(70, 200, 2.5D, 5.0D, 10.0D, 38.0D),
		RANKER_IMPACT(8, 28, 1.0D, 2.0D, 2.0D, 8.0D),
		FIGHTER_SLAM(36, 120, 2.8D, 5.8D, 4.0D, 20.0D),
		TANKER_SLAM(32, 110, 2.6D, 5.2D, 4.0D, 20.0D),
		LIU_SWORD_CUT(18, 64, 1.0D, 2.2D, 4.0D, 20.0D),
		GOLIATH_SMASH(160, 520, 3.5D, 8.5D, 12.0D, 45.0D),
		GOLIATH_COLLAPSE(450, 1400, 8.0D, 14.0D, 20.0D, 65.0D),
		GOLIATH_PURSUIT_PATH(12, 40, 0.75D, 1.35D, 3.0D, 14.0D),
		GOLIATH_PURSUIT_IMPACT(180, 600, 5.0D, 9.5D, 12.0D, 45.0D),
		BEAST_CLAW_RIFT(60, 220, 1.5D, 3.5D, 10.0D, 35.0D),
		BEAST_RUBBLE_JAW(180, 600, 4.5D, 9.0D, 12.0D, 45.0D),
		GRAND_MARSHAL_GRAVITY(90, 300, 3.5D, 6.5D, 8.0D, 32.0D),
		GRAND_MARSHAL_DREAD(50, 180, 1.8D, 3.8D, 8.0D, 32.0D),
		GRAND_MARSHAL_SKY_REND(80, 280, 3.5D, 6.5D, 8.0D, 32.0D),
		ANTARES_CLAW(80, 280, 2.0D, 4.5D, 15.0D, 55.0D),
		ANTARES_CLAW_FINISH(300, 900, 5.0D, 10.0D, 25.0D, 75.0D),
		ANTARES_BREATH(100, 360, 2.5D, 5.5D, 20.0D, 70.0D),
		ANTARES_DESCENT(500, 1800, 8.0D, 16.0D, 35.0D, 90.0D),
		ANTARES_ROAR(320, 1000, 8.0D, 14.0D, 30.0D, 80.0D),
		ANTARES_EXTINCTION(260, 700, 3.5D, 7.0D, 40.0D, 100.0D),
		ANTARES_EXTINCTION_FINISH(500, 1400, 7.0D, 14.0D, 45.0D, 100.0D);

		private final int baseBudget;
		private final int maximumBudget;
		private final double baseRadius;
		private final double maximumRadius;
		private final double baseHardness;
		private final double maximumHardness;

		Profile(int baseBudget, int maximumBudget, double baseRadius,
				double maximumRadius, double baseHardness, double maximumHardness) {
			this.baseBudget = baseBudget;
			this.maximumBudget = maximumBudget;
			this.baseRadius = baseRadius;
			this.maximumRadius = maximumRadius;
			this.baseHardness = baseHardness;
			this.maximumHardness = maximumHardness;
		}
	}

	/** True only while the compatibility BreakEvent for an ability block is posted. */
	public static boolean isPostingAbilityBreakEvent() {
		return POSTING_BREAK_EVENT.get();
	}

	public static boolean enabled(ServerLevel level) {
		return level != null
				&& SololevelingModGameRules.abilityDestructionMode(
						level.getGameRules()) != SololevelingModGameRules.AbilityDestructionMode.FALSE
				&& !DkcFloorRegistry.isDkc(level)
				&& !SnowRedGateArenaManager.isRedGateDimension(level.dimension());
	}

	/** Applies the partial-mode dungeon restriction using the actual caster context. */
	private static boolean enabled(ServerPlayer player) {
		if (player == null || !enabled(player.serverLevel()))
			return false;
		return SololevelingModGameRules.abilityDestructionMode(
				player.serverLevel().getGameRules())
				!= SololevelingModGameRules.AbilityDestructionMode.PARTIAL
				|| !isDungeonContext(player);
	}

	/**
	 * Covers every shipped dungeon dimension and procedural-instance binding.
	 * The instance check also protects data-pack dungeon dimensions that do not
	 * use the built-in naming convention.
	 */
	private static boolean isDungeonContext(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		ResourceLocation dimensionId = level.dimension().location();
		if (SololevelingMod.MODID.equals(dimensionId.getNamespace())
				&& (dimensionId.getPath().startsWith("dungeon_dimension_")
						|| dimensionId.getPath().equals("cartenon_temple")))
			return true;

		String instanceTag = player.getPersistentData()
				.getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		String legacyTag = player.getPersistentData().getString("dungeon_tag");
		return DungeonInstanceSavedData.get(level).listInstances().stream().anyMatch(instance ->
				instance.dimension().equals(level.dimension())
						&& (instance.participants().contains(player.getUUID())
								|| instance.id().toString().equals(instanceTag)
								|| instance.id().toString().equals(legacyTag)))
				|| player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.map(variables -> variables.dungeoning
								&& SololevelingMod.MODID.equals(dimensionId.getNamespace()))
						.orElse(false);
	}

	/** Queues a crater/impact ellipsoid centred on the supplied point. */
	public static void impact(ServerPlayer player, Profile profile, Vec3 center,
			double drivingAttribute, boolean empowered) {
		if (!canSchedule(player, profile, center))
			return;
		ScaledProfile scaled = scale(profile, drivingAttribute, empowered);
		double horizontal = scaled.radius;
		double vertical = Math.max(1.0D, horizontal * impactDepthFactor(profile));
		BlockPos min = BlockPos.containing(center.x - horizontal, center.y - vertical,
				center.z - horizontal);
		BlockPos max = BlockPos.containing(center.x + horizontal, center.y + vertical * 0.38D,
				center.z + horizontal);
		List<ScoredPos> candidates = new ArrayList<>();
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			BlockPos pos = cursor.immutable();
			double dx = (pos.getX() + 0.5D - center.x) / horizontal;
			double dz = (pos.getZ() + 0.5D - center.z) / horizontal;
			double horizontalDistance = Math.sqrt(dx * dx + dz * dz) * horizontal;
			if (preservesCasterFooting(profile)
					&& pos.getY() + 0.5D < center.y
					&& horizontalDistance < 1.35D)
				continue;
			double dyScale = pos.getY() + 0.5D <= center.y ? vertical
					: Math.max(0.75D, vertical * 0.38D);
			double dy = (pos.getY() + 0.5D - center.y) / dyScale;
			double radial = dx * dx + dz * dz;
			double distance = radial + dy * dy;
			if (distance <= 1.0D) {
				double depth = Math.max(0.0D,
						(center.y - (pos.getY() + 0.5D)) / vertical);
				candidates.add(new ScoredPos(pos, depth * 0.68D + radial * 0.32D
						+ deterministicJitter(pos, player.getUUID()) * 0.12D));
			}
		}
		schedule(player, profile, scaled, candidates);
	}

	/** Queues a bounded cylindrical cut between two points. */
	public static void line(ServerPlayer player, Profile profile, Vec3 start, Vec3 end,
			double drivingAttribute, boolean empowered) {
		if (!canSchedule(player, profile, start) || !finite(end))
			return;
		Vec3 segment = end.subtract(start);
		double maximumLength = maximumLineLength(profile);
		if (segment.lengthSqr() > maximumLength * maximumLength)
			end = start.add(segment.normalize().scale(maximumLength));
		ScaledProfile scaled = scale(profile, drivingAttribute, empowered);
		double radius = scaled.radius;
		BlockPos min = BlockPos.containing(Math.min(start.x, end.x) - radius,
				Math.min(start.y, end.y) - radius, Math.min(start.z, end.z) - radius);
		BlockPos max = BlockPos.containing(Math.max(start.x, end.x) + radius,
				Math.max(start.y, end.y) + radius, Math.max(start.z, end.z) + radius);
		List<ScoredPos> candidates = new ArrayList<>();
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			BlockPos pos = cursor.immutable();
			Vec3 point = Vec3.atCenterOf(pos);
			double distance = distanceToSegment(point, start, end);
			if (distance <= radius) {
				double along = alongSegment(point, start, end);
				candidates.add(new ScoredPos(pos,
						distance / Math.max(0.25D, radius) * 0.55D
						+ deterministicJitter(pos, player.getUUID()) * 0.35D
						+ Math.abs(along - 0.5D) * 0.02D));
			}
		}
		schedule(player, profile, scaled, candidates);
	}

	/** Queues a thin ground fissure following a horizontal attack direction. */
	public static void fissure(ServerPlayer player, Profile profile, Vec3 origin,
			Vec3 direction, double length, double drivingAttribute, boolean empowered) {
		if (!finite(direction) || !Double.isFinite(length)
				|| direction.lengthSqr() < 1.0E-6D)
			return;
		if (!canSchedule(player, profile, origin))
			return;
		length = Mth.clamp(length, 0.5D, maximumLineLength(profile));
		Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
		if (horizontal.lengthSqr() < 1.0E-6D)
			return;
		horizontal = horizontal.normalize();
		ScaledProfile scaled = scale(profile, drivingAttribute, empowered);
		double halfWidth = Math.max(0.55D, scaled.radius * 0.42D);
		double depth = Mth.clamp(0.85D + scaled.radius * 0.25D, 1.0D, 3.0D);
		Vec3 start = origin;
		Vec3 end = start.add(horizontal.scale(length));
		BlockPos min = BlockPos.containing(Math.min(start.x, end.x) - halfWidth,
				origin.y - depth, Math.min(start.z, end.z) - halfWidth);
		BlockPos max = BlockPos.containing(Math.max(start.x, end.x) + halfWidth,
				origin.y + 0.15D, Math.max(start.z, end.z) + halfWidth);
		List<ScoredPos> candidates = new ArrayList<>();
		double segmentLengthSqr = length * length;
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			BlockPos pos = cursor.immutable();
			double px = pos.getX() + 0.5D - start.x;
			double pz = pos.getZ() + 0.5D - start.z;
			double along = Mth.clamp((px * (end.x - start.x)
					+ pz * (end.z - start.z)) / segmentLengthSqr, 0.0D, 1.0D);
			double closestX = start.x + (end.x - start.x) * along;
			double closestZ = start.z + (end.z - start.z) * along;
			double lateral = Math.hypot(pos.getX() + 0.5D - closestX,
					pos.getZ() + 0.5D - closestZ);
			if (lateral > halfWidth)
				continue;
			double belowSurface = Math.max(0.0D,
					(origin.y - (pos.getY() + 0.5D)) / depth);
			candidates.add(new ScoredPos(pos, belowSurface * 0.58D
					+ lateral / halfWidth * 0.24D
					+ Math.abs(along - 0.5D) * 0.02D
					+ deterministicJitter(pos, player.getUUID()) * 0.16D));
		}
		schedule(player, profile, scaled, candidates);
	}

	/** Queues a fractured annulus, useful for shockwaves without clearing a full disk. */
	public static void ring(ServerPlayer player, Profile profile, Vec3 center,
			double visualRadius, double drivingAttribute, boolean empowered) {
		if (!canSchedule(player, profile, center) || !Double.isFinite(visualRadius))
			return;
		visualRadius = Mth.clamp(visualRadius, 0.5D, 24.0D);
		ScaledProfile scaled = scale(profile, drivingAttribute, empowered);
		double ringRadius = Math.min(24.0D, Math.max(scaled.radius, visualRadius));
		double thickness = Mth.clamp(0.75D + scaled.radius * 0.15D,
				1.0D, 3.2D);
		BlockPos min = BlockPos.containing(center.x - ringRadius - thickness,
				center.y - 2.5D, center.z - ringRadius - thickness);
		BlockPos max = BlockPos.containing(center.x + ringRadius + thickness,
				center.y + 0.65D, center.z + ringRadius + thickness);
		List<ScoredPos> candidates = new ArrayList<>();
		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			BlockPos pos = cursor.immutable();
			double dx = pos.getX() + 0.5D - center.x;
			double dz = pos.getZ() + 0.5D - center.z;
			double radialDistance = Math.sqrt(dx * dx + dz * dz);
			double radialError = Math.abs(radialDistance - ringRadius);
			double spokeError = Math.abs(Math.sin(Math.atan2(dz, dx) * 4.0D));
			boolean ringBlock = radialError <= thickness;
			boolean crackBlock = radialDistance >= Math.max(1.5D, ringRadius * 0.18D)
					&& radialDistance <= ringRadius && spokeError <= 0.12D;
			if (!ringBlock && !crackBlock)
				continue;
			double belowSurface = Math.max(0.0D, center.y - (pos.getY() + 0.5D));
			double aboveSurface = Math.max(0.0D, pos.getY() + 0.5D - center.y);
			double shapeScore = ringBlock ? radialError / thickness * 0.18D
					: 0.20D + spokeError * 0.55D;
			candidates.add(new ScoredPos(pos, belowSurface * 0.34D
					+ aboveSurface * 0.62D + shapeScore
					+ deterministicJitter(pos, player.getUUID()) * 0.22D));
		}
		schedule(player, profile, scaled, candidates);
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (false || JOBS.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		int mutationBudget = MAX_MUTATIONS_PER_TICK;
		int inspectionBudget = MAX_INSPECTIONS_PER_TICK;
		int jobsThisTick = JOBS.size();
		while (jobsThisTick-- > 0 && mutationBudget > 0 && inspectionBudget > 0
				&& !JOBS.isEmpty()) {
			DestructionJob job = JOBS.removeFirst();
			ServerPlayer player = server.getPlayerList().getPlayer(job.owner);
			if (player == null || !player.isAlive()
					|| player.getId() != job.ownerEntityId
					|| !player.serverLevel().dimension().equals(job.dimension)
					|| player.serverLevel().getGameTime() > job.expiresAt
					|| !enabled(player)) {
				discard(job);
				continue;
			}
			int jobMutations = 0;
			while (job.index < job.positions.size() && job.remaining > 0
					&& mutationBudget > 0 && inspectionBudget > 0
					&& jobMutations < MAX_MUTATIONS_PER_JOB_PER_TICK) {
				BlockPos pos = job.positions.get(job.index++);
				QUEUED_BLOCKS.remove(new QueuedBlock(job.dimension, pos.asLong()));
				queuedPositions--;
				inspectionBudget--;
				if (destroyOne(player.serverLevel(), player, pos, job.maximumHardness)) {
					job.remaining--;
					jobMutations++;
					mutationBudget--;
				}
			}
			if (job.index < job.positions.size() && job.remaining > 0)
				JOBS.addLast(job);
			else
				discard(job);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		JOBS.clear();
		QUEUED_BLOCKS.clear();
		queuedPositions = 0;
		OWNER_REQUESTS_THIS_TICK.clear();
		requestTick = Integer.MIN_VALUE;
		requestsThisTick = 0;
		POSTING_BREAK_EVENT.remove();
	}

	private static void schedule(ServerPlayer player, Profile profile,
			ScaledProfile scaled, List<ScoredPos> scored) {
		UUID owner = player.getUUID();
		int ownerPositions = queuedPositionsFor(owner);
		if (scored.isEmpty() || JOBS.size() >= MAX_QUEUED_JOBS
				|| queuedJobsFor(owner) >= MAX_QUEUED_JOBS_PER_OWNER
				|| queuedPositions >= MAX_QUEUED_POSITIONS)
			return;
		scored.sort(Comparator.comparingDouble(ScoredPos::score));
		int reserve = Mth.clamp(scaled.budget / 3, 12, 256);
		int candidateLimit = Math.min(scored.size(), Math.min(scaled.budget + reserve,
				Math.min(MAX_QUEUED_POSITIONS - queuedPositions,
						MAX_QUEUED_POSITIONS_PER_OWNER - ownerPositions)));
		if (candidateLimit <= 0)
			return;
		ServerLevel level = player.serverLevel();
		List<BlockPos> positions = new ArrayList<>(candidateLimit);
		Set<Long> unique = new HashSet<>(candidateLimit * 2);
		for (ScoredPos candidate : scored) {
			if (positions.size() >= candidateLimit)
				break;
			BlockPos pos = candidate.pos;
			QueuedBlock queued = new QueuedBlock(level.dimension(), pos.asLong());
			if (!unique.add(pos.asLong()) || QUEUED_BLOCKS.contains(queued)
					|| !cheapCandidate(level, pos, scaled.maximumHardness))
				continue;
			positions.add(pos);
			QUEUED_BLOCKS.add(queued);
		}
		if (positions.isEmpty())
			return;
		JOBS.addLast(new DestructionJob(owner, player.getId(), level.dimension(), profile,
				positions, scaled.budget, scaled.maximumHardness,
				level.getGameTime() + 200L));
		queuedPositions += positions.size();
	}

	private static boolean destroyOne(ServerLevel level, ServerPlayer player,
			BlockPos pos, double maximumHardness) {
		if (!enabled(player) || !cheapCandidate(level, pos, maximumHardness)
				|| !level.mayInteract(player, pos)
				|| !player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY)
				|| !level.getBlockState(pos).canEntityDestroy(level, pos, player))
			return false;
		BlockState state = level.getBlockState(pos);
		BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
		boolean canceled;
		POSTING_BREAK_EVENT.set(true);
		try {
			canceled = NeoForge.EVENT_BUS.post(event).isCanceled();
		} finally {
			POSTING_BREAK_EVENT.set(false);
		}
		if (canceled || !level.getBlockState(pos).equals(state))
			return false;
		if (!EventHooks.onEntityDestroyBlock(player, pos, state))
			return false;
		if (!level.getBlockState(pos).equals(state)
				|| !cheapCandidate(level, pos, maximumHardness))
			return false;
		if (!state.onDestroyedByPlayer(level, pos, player, false,
				level.getFluidState(pos)))
			return false;
		state.getBlock().destroy(level, pos, state);
		return true;
	}

	private static boolean cheapCandidate(ServerLevel level, BlockPos pos,
			double maximumHardness) {
		if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
				|| !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos))
			return false;
		BlockState state = level.getBlockState(pos);
		if (state.hasBlockEntity() || level.getBlockEntity(pos) != null)
			return false;
		if (state.isAir() || !state.getFluidState().isEmpty() || state.is(IMMUNE_BLOCKS)
				|| state.is(BlockTags.WITHER_IMMUNE))
			return false;
		if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
				|| state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.END_GATEWAY)
				|| state.is(Blocks.BARRIER) || state.is(Blocks.STRUCTURE_BLOCK)
				|| state.is(Blocks.JIGSAW) || state.is(Blocks.COMMAND_BLOCK)
				|| state.is(Blocks.CHAIN_COMMAND_BLOCK)
				|| state.is(Blocks.REPEATING_COMMAND_BLOCK)
				|| state.is(Blocks.SPAWNER))
			return false;
		float hardness = state.getDestroySpeed(level, pos);
		return hardness >= 0.0F && hardness <= maximumHardness;
	}

	private static boolean canSchedule(ServerPlayer player, Profile profile, Vec3 point) {
		if (player == null || profile == null || !finite(point) || !player.isAlive()
				|| !enabled(player)
				|| JOBS.size() >= MAX_QUEUED_JOBS
				|| queuedPositions >= MAX_QUEUED_POSITIONS
				|| queuedJobsFor(player.getUUID()) >= MAX_QUEUED_JOBS_PER_OWNER
				|| queuedPositionsFor(player.getUUID()) >= MAX_QUEUED_POSITIONS_PER_OWNER
				|| !player.serverLevel().hasChunkAt(BlockPos.containing(point)))
			return false;
		return reserveRequest(player);
	}

	private static boolean reserveRequest(ServerPlayer player) {
		int tick = player.server.getTickCount();
		if (requestTick != tick) {
			requestTick = tick;
			requestsThisTick = 0;
			OWNER_REQUESTS_THIS_TICK.clear();
		}
		UUID owner = player.getUUID();
		int ownerRequests = OWNER_REQUESTS_THIS_TICK.getOrDefault(owner, 0);
		if (requestsThisTick >= MAX_REQUESTS_PER_TICK
				|| ownerRequests >= MAX_REQUESTS_PER_OWNER_PER_TICK)
			return false;
		requestsThisTick++;
		OWNER_REQUESTS_THIS_TICK.put(owner, ownerRequests + 1);
		return true;
	}

	private static int queuedJobsFor(UUID owner) {
		int count = 0;
		for (DestructionJob job : JOBS)
			if (job.owner.equals(owner))
				count++;
		return count;
	}

	private static int queuedPositionsFor(UUID owner) {
		int count = 0;
		for (DestructionJob job : JOBS)
			if (job.owner.equals(owner))
				count += Math.max(0, job.positions.size() - job.index);
		return count;
	}

	private static ScaledProfile scale(Profile profile, double attribute, boolean empowered) {
		if (!Double.isFinite(attribute))
			attribute = 0.0D;
		double normalized = Mth.clamp(Math.log1p(Math.max(0.0D, attribute))
				/ Math.log1p(attributeCeiling(profile)), 0.0D, 1.0D);
		if (empowered)
			normalized = Math.min(1.0D, 0.20D + normalized * 1.02D);
		int budget = Mth.clamp((int) Math.round(Mth.lerp(normalized,
				profile.baseBudget, profile.maximumBudget)), profile.baseBudget,
				profile.maximumBudget);
		double radius = Mth.lerp(normalized, profile.baseRadius, profile.maximumRadius);
		double hardness = Mth.lerp(normalized, profile.baseHardness,
				profile.maximumHardness);
		return new ScaledProfile(budget, radius, hardness);
	}

	private static double maximumLineLength(Profile profile) {
		return switch (profile) {
			case ANTARES_BREATH -> 64.0D;
			case ANTARES_EXTINCTION -> 160.0D;
			case ANTARES_CLAW -> 12.0D;
			case GOLIATH_PURSUIT_PATH -> 5.0D;
			case BEAST_CLAW_RIFT, LIU_SWORD_CUT -> 24.0D;
			default -> 32.0D;
		};
	}

	private static double impactDepthFactor(Profile profile) {
		return switch (profile) {
			case ANTARES_DESCENT -> 0.40D;
			case ANTARES_EXTINCTION_FINISH -> 0.43D;
			case FIRE_HEAVENFALL -> 0.35D;
			case GOLIATH_COLLAPSE -> 0.34D;
			case ARCANE_CONVERGENCE, BARRIER_CATASTROPHE -> 0.32D;
			default -> 0.30D;
		};
	}

	private static boolean preservesCasterFooting(Profile profile) {
		return profile == Profile.GOLIATH_COLLAPSE
				|| profile == Profile.ANTARES_DESCENT;
	}

	private static double attributeCeiling(Profile profile) {
		return switch (profile) {
			case ANTARES_CLAW, ANTARES_CLAW_FINISH, ANTARES_BREATH,
					ANTARES_DESCENT, ANTARES_ROAR, ANTARES_EXTINCTION,
					ANTARES_EXTINCTION_FINISH -> 1200.0D;
			case GOLIATH_SMASH, GOLIATH_COLLAPSE, GOLIATH_PURSUIT_PATH,
					GOLIATH_PURSUIT_IMPACT, BEAST_CLAW_RIFT,
					BEAST_RUBBLE_JAW -> 600.0D;
			case ARCANE_CONVERGENCE, BARRIER_CATASTROPHE, FIRE_DOMINION,
					FIRE_HEAVENFALL, FIRE_BEAM_OVERCHARGED,
					STORM_SKYBREAKER, WHITE_FLAME_HELLSTORM,
					GRAND_MARSHAL_GRAVITY, GRAND_MARSHAL_DREAD,
					GRAND_MARSHAL_SKY_REND -> 500.0D;
			default -> 350.0D;
		};
	}

	private static boolean finite(Vec3 point) {
		return point != null && Double.isFinite(point.x) && Double.isFinite(point.y)
				&& Double.isFinite(point.z);
	}

	private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		if (lengthSqr < 1.0E-8D)
			return point.distanceTo(start);
		double t = Mth.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
		return point.distanceTo(start.add(segment.scale(t)));
	}

	private static double alongSegment(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		return lengthSqr < 1.0E-8D ? 0.0D
				: Mth.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
	}

	private static double deterministicJitter(BlockPos pos, UUID owner) {
		long value = pos.asLong() ^ owner.getMostSignificantBits()
				^ Long.rotateLeft(owner.getLeastSignificantBits(), 23);
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdl;
		value ^= value >>> 33;
		return (value & 0xffffL) / 65535.0D;
	}

	private static void discard(DestructionJob job) {
		for (int i = job.index; i < job.positions.size(); i++)
			QUEUED_BLOCKS.remove(new QueuedBlock(job.dimension,
					job.positions.get(i).asLong()));
		int remainingPositions = Math.max(0, job.positions.size() - job.index);
		queuedPositions = Math.max(0, queuedPositions - remainingPositions);
		job.index = job.positions.size();
	}

	private record ScaledProfile(int budget, double radius, double maximumHardness) {
	}

	private record ScoredPos(BlockPos pos, double score) {
	}

	private record QueuedBlock(
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
			long position) {
	}

	private static final class DestructionJob {
		private final UUID owner;
		private final int ownerEntityId;
		private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
		@SuppressWarnings("unused")
		private final Profile profile;
		private final List<BlockPos> positions;
		private final double maximumHardness;
		private final long expiresAt;
		private int remaining;
		private int index;

		private DestructionJob(UUID owner, int ownerEntityId,
				net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
				Profile profile, List<BlockPos> positions, int remaining,
				double maximumHardness, long expiresAt) {
			this.owner = owner;
			this.ownerEntityId = ownerEntityId;
			this.dimension = dimension;
			this.profile = profile;
			this.positions = positions;
			this.remaining = remaining;
			this.maximumHardness = maximumHardness;
			this.expiresAt = expiresAt;
		}
	}
}
