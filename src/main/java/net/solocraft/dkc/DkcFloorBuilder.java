package net.solocraft.dkc;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.DKCTowerAuraEntity;
import net.solocraft.entity.KaiselinEntity;
import net.solocraft.entity.VulcanEntity;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.init.SololevelingModBlocks;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.DKCPathTeleportProcedure;
import net.solocraft.util.SystemNotifications;

import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Staged, idempotent builder and transition runtime for the remade DKC.
 * The dimension is static; each player's floors are generated lazily in the
 * 5 x 4 cell grid belonging to their persistent spatial slot.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DkcFloorBuilder {
	private static final int SLICE_WIDTH = 16;
	private static final int TICKS_PER_SLICE = 1;
	private static final String OWNER_TAG = "dkc_owner";
	private static final String TRANSITION_COOLDOWN = "dkc_transition_cooldown";
	private static final String BUILD_RESUME_COOLDOWN = "dkc_build_resume_cooldown";
	private static final String RETURN_COOLDOWN = "dkc_return_sigil_cooldown";
	private static final String DEBUG_BOSS_RESET_PREFIX = "dkc_debug_reset_boss_";
	private static final String DEBUG_REQUESTED_FLOOR_TAG = "dkc_debug_requested_floor";
	private static final int COMBAT_SPAWN_ATTEMPTS = 40;
	private static final int COMBAT_PATH_ATTEMPTS = 5;
	private static final int COMBAT_MIN_SPAWN_DISTANCE = 18;
	private static final int COMBAT_MAX_SPAWN_DISTANCE = 30;
	private static final int COMBAT_ENTRY_SAFE_RADIUS = 24;
	private static final int[] COMBAT_Y_OFFSETS = {0, 1, -1, 2, -2, 3, -3, -4, -5};
	private static final Map<BuildKey, BuildContext> ACTIVE_BUILDS = new ConcurrentHashMap<>();
	private static final Map<OwnedSpawnKey, Long> OWNED_SPAWN_GUARDS = new ConcurrentHashMap<>();

	private static final Zone STREET_A = new Zone("dkc_lower_street_a", 32, 24, false);
	private static final Zone STREET_B = new Zone("dkc_lower_street_b", 32, 24, false);
	private static final Zone STREET_C = new Zone("dkc_lower_street_c", 32, 24, false);
	private static final Zone INTERSECTION = new Zone("dkc_lower_intersection_four_way", 32, 32, false);
	private static final Zone RUNES = new Zone("dkc_lower_rune_plaza_through", 32, 32, false);
	private static final Zone MARKET = new Zone("dkc_lower_patrol_market", 48, 48, false);
	private static final Zone COURTYARD = new Zone("dkc_lower_purification_courtyard", 48, 48, false);
	private static final Zone MAGMA = new Zone("dkc_lower_magma_sluice", 48, 48, false);
	private static final Zone VILLAGE = tiled("dkc_open_burnt_village");
	private static final Zone ASH_WASTES = tiled("dkc_open_ash_wastes");
	private static final Zone CATHEDRAL = tiled("dkc_open_ruined_cathedral");
	private static final Zone FORGE = tiled("dkc_open_forge_arena");
	private static final Zone DRAGON_COURT = tiled("dkc_open_dragon_court");
	private static final Zone THRONE = tiled("dkc_open_throne_court");
	public static final int RADIRU_FLOOR = 15;
	private static final int RADIRU_CASTLE_X = -56;
	private static final int RADIRU_CASTLE_Z = 24;
	private static final int RADIRU_CASTLE_WIDTH = 112;
	private static final int RADIRU_CASTLE_LENGTH = 96;
	private static final int RADIRU_FIELD_X = -56;
	private static final int RADIRU_FIELD_Z = 120;
	private static final int RADIRU_FIELD_WIDTH = 112;
	private static final int RADIRU_FIELD_LENGTH = 96;
	private static final int RADIRU_TOWER_Z = 216;
	private static final BlockPos RADIRU_ESIL = new BlockPos(0, 3, 49);
	private static final BlockPos RADIRU_GATE = new BlockPos(0, 3, 116);
	private static final BlockPos RADIRU_COURTYARD = new BlockPos(0, 3, 98);
	private static final List<BlockPos> RADIRU_RESIDENTS = List.of(
			new BlockPos(-21, 3, 91), new BlockPos(20, 3, 91),
			new BlockPos(-29, 3, 97), new BlockPos(28, 3, 97));
	private static final List<BlockPos> RADIRU_TRAINING_DUMMIES = List.of(
			new BlockPos(23, 3, 53), new BlockPos(30, 3, 53),
			new BlockPos(23, 3, 63), new BlockPos(30, 3, 63),
			new BlockPos(23, 3, 73), new BlockPos(30, 3, 73));
	private static final List<BlockPos> RADIRU_WAVE_ANCHORS = List.of(
			new BlockPos(0, 3, 142), new BlockPos(0, 3, 163), new BlockPos(0, 3, 184));
	private static final Layout[] LAYOUTS = createLayouts();

	private DkcFloorBuilder() {
	}

	/**
	 * Invalidates queued construction work and ready callbacks belonging to one
	 * player. Queued server tasks may still wake up, but their canceled context
	 * fails {@link #isContextCurrent(BuildContext)} before placing or finishing.
	 */
	public static void cancelPlayerBuilds(MinecraftServer server, UUID playerId) {
		if (server == null || playerId == null)
			return;
		ACTIVE_BUILDS.forEach((key, context) -> {
			if (key.server() == server && playerId.equals(key.player()))
				cancelBuild(key, context);
		});
		OWNED_SPAWN_GUARDS.keySet().removeIf(key ->
				key.server() == server && playerId.equals(key.player()));
	}

	public static void prepareFloor(ServerPlayer player, int floor) {
		prepareFloor(player, floor, null, null);
	}

	private static void prepareFloor(ServerPlayer player, int floor,
			@Nullable ReadyAction readyAction, @Nullable Runnable onReady) {
		if (player == null || player.server == null || floor < 1 || floor > DkcFloorRegistry.LAST_FLOOR)
			return;
		MinecraftServer server = player.server;
		DkcRunSavedData runs = DkcRunSavedData.get(server);
		if (!runs.isUnlocked(player, floor)) {
			player.displayClientMessage(Component.literal("\u00A74Floor " + floor + " is still sealed."), true);
			return;
		}
		ServerLevel level = server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
		if (level == null) {
			player.displayClientMessage(Component.literal("\u00A74The floor dimension failed to manifest."), true);
			return;
		}

		if (runs.isGenerated(player, floor)) {
			restoreRuntimeState(level, player, floor);
			if (onReady != null)
				onReady.run();
			return;
		}

		BuildKey key = new BuildKey(server, player.getUUID(), floor);
		BuildContext existing = ACTIVE_BUILDS.get(key);
		if (existing != null && !isContextCurrent(existing)) {
			cancelBuild(key, existing);
			existing = null;
		}
		if (existing != null) {
			if (readyAction != null && onReady != null)
				existing.callbacks.putIfAbsent(readyAction, onReady);
			player.displayClientMessage(Component.literal("\u00A75Floor " + floor + " is still taking shape..."), true);
			return;
		}

		Layout layout = layout(floor);
		BuildContext context = new BuildContext(key, server, player, floor, level, layout);
		if (readyAction != null && onReady != null)
			context.callbacks.put(readyAction, onReady);
		ACTIVE_BUILDS.put(key, context);

		List<BuildStep> steps;
		try {
			steps = createBuildSteps(context);
		} catch (RuntimeException exception) {
			context.failed = true;
			SololevelingMod.LOGGER.error("Failed to prepare DKC floor {} build steps for {}",
					floor, player.getGameProfile().getName(), exception);
			finishBuild(key, context);
			return;
		}
		for (int index = 0; index < steps.size(); index++) {
			BuildStep step = steps.get(index);
			SololevelingMod.queueServerWork(server, 1 + index * TICKS_PER_SLICE,
					() -> placeSafely(context, step));
		}
		SololevelingMod.queueServerWork(server, 1 + steps.size() * TICKS_PER_SLICE,
				() -> finishBuild(key, context));
	}

	private static List<BuildStep> createBuildSteps(BuildContext context) {
		List<BuildStep> steps = new ArrayList<>();
		BlockPos floorOrigin = origin(context.player, context.floor);
		if (context.floor == RADIRU_FLOOR
				&& DkcRunSavedData.get(context.server).needsCleanup(context.player, context.floor))
			addRadiruCleanupSteps(steps, floorOrigin);
		for (Placement placement : context.layout.placements) {
			StructureTemplate template = context.level.getStructureManager().getOrCreate(placement.template);
			Vec3i size = template.getSize();
			if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
				throw new IllegalStateException("Missing DKC structure " + placement.template);
			BlockPos desiredMin = floorOrigin.offset(placement.x, placement.y, placement.z);
			BlockPos templateOrigin = rotationOrigin(desiredMin, size, placement.rotation);
			BoundingBox bounds = template.getBoundingBox(placementSettings(placement, null), templateOrigin);
			for (int minX = bounds.minX(); minX <= bounds.maxX(); minX += SLICE_WIDTH) {
				int maxX = Math.min(bounds.maxX(), minX + SLICE_WIDTH - 1);
				for (int minZ = bounds.minZ(); minZ <= bounds.maxZ(); minZ += SLICE_WIDTH) {
					int maxZ = Math.min(bounds.maxZ(), minZ + SLICE_WIDTH - 1);
					steps.add(new BuildStep(desiredMin, placement,
							new BoundingBox(minX, bounds.minY(), minZ, maxX, bounds.maxY(), maxZ), false));
				}
			}
		}
		return List.copyOf(steps);
	}

	private static void addRadiruCleanupSteps(List<BuildStep> steps, BlockPos root) {
		// v5 and older used a different sparse modular floor here. Clear only authored
		// space above the permanent flat-world foundation, in 16x16 slices, before
		// laying the v6 castle so old walls can never survive inside its open routes.
		addCleanupRegion(steps, root, -56, 55, 0, RADIRU_TOWER_Z - 1);
		addCleanupRegion(steps, root, -32, 31, RADIRU_TOWER_Z, RADIRU_TOWER_Z + 63);
	}

	private static void addCleanupRegion(List<BuildStep> steps, BlockPos root,
			int localMinX, int localMaxX, int localMinZ, int localMaxZ) {
		int minX = root.getX() + localMinX;
		int maxX = root.getX() + localMaxX;
		int minZ = root.getZ() + localMinZ;
		int maxZ = root.getZ() + localMaxZ;
		for (int x = minX; x <= maxX; x += SLICE_WIDTH) {
			for (int z = minZ; z <= maxZ; z += SLICE_WIDTH) {
				steps.add(new BuildStep(null, null, new BoundingBox(x, root.getY() + 3, z,
						Math.min(maxX, x + SLICE_WIDTH - 1), root.getY() + 95,
						Math.min(maxZ, z + SLICE_WIDTH - 1)), true));
			}
		}
	}

	private static void placeSafely(BuildContext context, BuildStep step) {
		if (context.failed)
			return;
		if (!isContextCurrent(context)) {
			cancelBuild(context.key, context);
			return;
		}
		try {
			loadStepChunks(context.level, step.bounds);
			if (step.clear)
				clearAuthoredSpace(context.level, step.bounds);
			else
				place(context.level, step.desiredMin, step.placement, step.bounds);
		} catch (RuntimeException exception) {
			context.failed = true;
			String stepName = step.clear ? "Radiru migration cleanup" : step.placement.template.toString();
			SololevelingMod.LOGGER.error("Failed to place DKC floor {} step {} for {}",
					context.floor, stepName, context.player.getGameProfile().getName(), exception);
		}
	}

	private static void loadStepChunks(ServerLevel level, BoundingBox bounds) {
		int minChunkX = bounds.minX() >> 4;
		int maxChunkX = bounds.maxX() >> 4;
		int minChunkZ = bounds.minZ() >> 4;
		int maxChunkZ = bounds.maxZ() >> 4;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
				level.getChunk(chunkX, chunkZ);
	}

	private static void clearAuthoredSpace(ServerLevel level, BoundingBox bounds) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
			for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
				for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
					cursor.set(x, y, z);
					if (!level.isEmptyBlock(cursor))
						level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	private static void finishBuild(BuildKey key, BuildContext context) {
		if (!isContextCurrent(context) || !ACTIVE_BUILDS.remove(key, context)) {
			cancelBuild(key, context);
			return;
		}
		if (context.failed) {
			if (isOnlinePlayer(context))
				context.player.displayClientMessage(Component.literal("\u00A74The castle flickered. Use the key or pedestal to resume construction."), false);
			context.callbacks.clear();
			return;
		}
		restoreRuntimeState(context.level, context.player, context.floor);
		DkcRunSavedData.get(context.server).markGenerated(context.player, context.floor);
		if (context.floor == RADIRU_FLOOR
				&& DkcSpatialLayout.isPlayerInFloor(context.player, context.floor))
			DkcRadiruManager.onFloorEntered(context.player);
		if (!isOnlinePlayer(context)) {
			context.callbacks.clear();
			return;
		}
		for (Runnable callback : context.callbacks.values()) {
			try {
				callback.run();
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.error("DKC floor-ready callback failed", exception);
			}
		}
		context.callbacks.clear();
	}

	private static boolean isContextCurrent(BuildContext context) {
		return context != null && !context.cancelled
				&& ACTIVE_BUILDS.get(context.key) == context
				&& context.player.server == context.server
				&& context.level.getServer() == context.server
				&& isOnlinePlayer(context)
				&& ServerLifecycleHooks.getCurrentServer() == context.server;
	}

	private static boolean isOnlinePlayer(BuildContext context) {
		return !context.player.hasDisconnected()
				&& context.server.getPlayerList().getPlayer(context.player.getUUID()) == context.player;
	}

	private static void cancelBuild(BuildKey key, BuildContext context) {
		if (context == null)
			return;
		context.cancelled = true;
		context.callbacks.clear();
		ACTIVE_BUILDS.remove(key, context);
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		ACTIVE_BUILDS.forEach((key, context) -> {
			if (key.server() == event.getServer())
				cancelBuild(key, context);
		});
		OWNED_SPAWN_GUARDS.keySet().removeIf(key -> key.server() == event.getServer());
	}

	private static void place(ServerLevel level, BlockPos desiredMin, Placement placement) {
		place(level, desiredMin, placement, null);
	}

	private static void place(ServerLevel level, BlockPos desiredMin, Placement placement,
			@Nullable BoundingBox bounds) {
		StructureTemplate template = level.getStructureManager().getOrCreate(placement.template);
		Vec3i size = template.getSize();
		if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
			throw new IllegalStateException("Missing DKC structure " + placement.template);
		BlockPos templateOrigin = rotationOrigin(desiredMin, size, placement.rotation);
		StructurePlaceSettings settings = placementSettings(placement, bounds);
		// Client update without a neighbor-update storm; these authored districts
		// contain no redstone logic that needs per-block propagation while placing.
		if (!template.placeInWorld(level, templateOrigin, templateOrigin, settings,
				level.random, 2))
			throw new IllegalStateException("DKC structure placement wrote no blocks for "
					+ placement.template + (bounds == null ? ""
							: " in slice " + bounds));
		for (StructureTemplate.StructureBlockInfo marker : template.filterBlocks(templateOrigin, settings, Blocks.STRUCTURE_BLOCK))
			if (bounds == null || bounds.isInside(marker.pos())) {
				boolean removed = level.setBlock(marker.pos(),
						Blocks.AIR.defaultBlockState(), 2);
				if (!removed && !level.isEmptyBlock(marker.pos()))
					throw new IllegalStateException("Could not remove DKC structure marker at "
							+ marker.pos().toShortString());
			}
	}

	private static StructurePlaceSettings placementSettings(Placement placement,
			@Nullable BoundingBox bounds) {
		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setRotation(placement.rotation).setMirror(Mirror.NONE).setIgnoreEntities(true)
				.setKnownShape(true);
		return bounds == null ? settings : settings.setBoundingBox(bounds);
	}

	public static boolean handlePermit(ServerPlayer player, BlockPos clickedPos) {
		if (player == null || player.server == null)
			return false;
		int floor = DkcSpatialLayout.floor(player);
		if (floor == 0)
			return false;
		if (floor >= DkcFloorRegistry.LAST_FLOOR) {
			player.displayClientMessage(Component.literal("\u00A75There is no floor above the Demon King's throne."), true);
			return true;
		}
		BlockPos expected = pedestalPosition(player, floor);
		if (!clickedPos.closerThan(expected, 2.25D)) {
			player.displayClientMessage(Component.literal("\u00A74This seal is not bound to your castle path."), true);
			return true;
		}

		SololevelingModVariables.PlayerVariables vars = variables(player);
		if ((int) vars.dkc_cleared < floor) {
			player.displayClientMessage(Component.literal("\u00A74Defeat this floor's objective before presenting a permit."), true);
			return true;
		}

		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		if (runs.isTransitionArmed(player, floor)) {
			prepareFloor(player, floor + 1, ReadyAction.OPEN_TRANSITION,
					() -> openTransition(player, floor));
			player.displayClientMessage(Component.literal(runs.isGenerated(player, floor + 1)
					? "\u00A75The seal is already broken."
					: "\u00A75The claimed floor is taking shape again..."), true);
			return true;
		}

		InteractionHand permitHand = permitHand(player, floor);
		boolean recoveredRadiruClaim = permitHand == null && floor == RADIRU_FLOOR
				&& DkcRadiruManager.canRecoverTransitionWithoutPermit(player);
		if (permitHand == null && !recoveredRadiruClaim) {
			player.displayClientMessage(Component.literal("\u00A74Present an Entry Permit to the crimson pedestal."), true);
			return true;
		}

		// Persist the generation claim before consuming the permit. If the server
		// stops mid-build, another click resumes the same claimed floor for free.
		if (!runs.claimTransition(player, floor))
			return true;
		if (permitHand != null && !player.getAbilities().instabuild)
			player.getItemInHand(permitHand).shrink(1);
		if (recoveredRadiruClaim)
			player.displayClientMessage(Component.literal("\u00A75The System recognizes your resolved Radiru claim and restores the lost permit seal."), false);
		player.serverLevel().playSound(null, clickedPos, SoundEvents.RESPAWN_ANCHOR_CHARGE,
				SoundSource.PLAYERS, 1.0F, 0.65F);
		player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
				clickedPos.getX() + 0.5D, clickedPos.getY() + 1.0D, clickedPos.getZ() + 0.5D,
				36, 0.45D, 0.65D, 0.45D, 0.025D);
		prepareFloor(player, floor + 1, ReadyAction.OPEN_TRANSITION,
				() -> openTransition(player, floor));
		return true;
	}

	private static void restoreRuntimeState(ServerLevel level, ServerPlayer player, int floor) {
		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		ensureTowerAura(level, player, floor);
		if (floor >= DkcFloorRegistry.LAST_FLOOR)
			return;
		if (runs.isTransitionArmed(player, floor) && runs.isGenerated(player, floor + 1))
			openTransitionBlocks(level, player, floor);
		else
			placeClosedTransition(level, player, floor);
		if (floor == RADIRU_FLOOR)
			restoreRadiruPhysicalState(level, player);
	}

	private static void restoreRadiruPhysicalState(ServerLevel level, ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		boolean openGate = player.getPersistentData().getBoolean("radiru_floor_15_surrendered")
				|| vars.dkc_cleared >= RADIRU_FLOOR;
		Placement gate = piece(openGate ? "dkc_radiru_gate_open" : "dkc_radiru_gate_closed", -10, 0, 112);
		try {
			place(level, origin(player, RADIRU_FLOOR).offset(gate.x, gate.y, gate.z), gate);
			if (vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR && !vars.radiru_slaughtered)
				sealRadiruTowerBlocks(level, player);
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Failed to restore House Radiru physical state for {}",
					player.getGameProfile().getName(), exception);
		}
	}

	private static void placeClosedTransition(ServerLevel level, ServerPlayer player, int floor) {
		clearLegacyPedestal(level, player, floor);
		BlockPos pedestal = pedestalPosition(player, floor);
		level.setBlock(pedestal, SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get().defaultBlockState(), 3);
		int towerZ = layout(floor).towerZ;
		try {
			place(level, origin(player, floor).offset(-10, 0, towerZ),
					piece("dkc_tower_gate_closed", -10, 0, towerZ));
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Failed to close DKC floor {} tower gate", floor, exception);
		}
	}

	private static void openTransition(ServerPlayer player, int floor) {
		if (player.server == null)
			return;
		ServerLevel current = player.server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
		if (current == null)
			return;
		openTransitionBlocks(current, player, floor);
		BlockPos center = transitionPosition(player, floor);
		current.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8F, 0.8F);
		current.sendParticles(ParticleTypes.REVERSE_PORTAL, center.getX() + 0.5D, center.getY() + 1.0D,
				center.getZ() + 0.5D, 90, 1.8D, 1.2D, 1.8D, 0.08D);
		if (DkcSpatialLayout.isPlayerInFloor(player, floor))
			SystemNotifications.showTitleUnder(player, 0xFFB22CFF, 100,
					Component.literal("TOWER PATH OPEN").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
					Component.literal("Walk into the ascension seal to enter Floor " + (floor + 1) + ".").withStyle(ChatFormatting.GRAY));
	}

	private static void openTransitionBlocks(ServerLevel level, ServerPlayer player, int floor) {
		clearLegacyPedestal(level, player, floor);
		BlockPos pedestal = pedestalPosition(player, floor);
		if (level.getBlockState(pedestal).is(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get()))
			level.setBlock(pedestal, Blocks.AIR.defaultBlockState(), 3);
		int towerZ = layout(floor).towerZ;
		try {
			place(level, origin(player, floor).offset(-10, 0, towerZ),
					piece("dkc_tower_gate_open", -10, 0, towerZ));
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Failed to open DKC floor {} tower gate", floor, exception);
		}
	}

	private static void clearLegacyPedestal(ServerLevel level, ServerPlayer player, int floor) {
		if (floor <= 1)
			return;
		BlockPos legacy = origin(player, floor).offset(-5, 4, layout(floor).towerZ + 16);
		if (level.getBlockState(legacy).is(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get()))
			level.setBlock(legacy, Blocks.AIR.defaultBlockState(), 3);
	}

	/**
	 * Repairs the permit pedestal for a cleared floor without rebuilding its
	 * authored tower. This is intentionally safe to call for existing saves: an
	 * already-open transition remains open, while an interrupted next-floor build
	 * keeps a pedestal available so presenting the permit can resume it.
	 */
	public static boolean ensurePermitPedestal(ServerPlayer player, int floor) {
		if (player == null || player.server == null || floor <= 0
				|| floor >= DkcFloorRegistry.LAST_FLOOR)
			return false;
		ServerLevel level = player.server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
		if (level == null || player.serverLevel() != level
				|| !DkcSpatialLayout.isPlayerInFloor(player, floor))
			return false;

		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (vars.dkc_cleared < floor)
			return false;
		// A peaceful post-conquest Radiru visit deliberately has no ascension
		// pedestal; restoring one there would revive a retired Floor-16 route.
		if (floor == RADIRU_FLOOR
				&& vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR
				&& !vars.radiru_slaughtered)
			return false;

		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		if (!runs.isGenerated(player, floor))
			return false;
		if (runs.isTransitionArmed(player, floor)
				&& runs.isGenerated(player, floor + 1))
			return true;

		BlockPos pedestal = pedestalPosition(player, floor);
		level.getChunkAt(pedestal);
		clearLegacyPedestal(level, player, floor);
		if (!level.getBlockState(pedestal)
				.is(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get()))
			level.setBlock(pedestal,
					SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get()
							.defaultBlockState(), 3);
		return level.getBlockState(pedestal)
				.is(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get());
	}

	public static void tickPlayer(ServerPlayer player) {
		int floor = DkcSpatialLayout.floor(player);
		if (floor <= 0 || player.server == null)
			return;
		if (player.serverLevel().getGameTime() % 100L == Math.floorMod(player.getId(), 100))
			ensureTowerAura(player.serverLevel(), player, floor);
		if (player.serverLevel().getGameTime() % 10L == Math.floorMod(player.getId(), 10))
			enforceInstanceBounds(player, floor);
		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		if (!runs.isGenerated(player, floor)) {
			long now = player.serverLevel().getGameTime();
			if (now >= player.getPersistentData().getLong(BUILD_RESUME_COOLDOWN)) {
				player.getPersistentData().putLong(BUILD_RESUME_COOLDOWN, now + 100L);
				prepareFloor(player, floor);
			}
			return;
		}
		// A conquered DKC leaves Floor 15 as a peaceful Radiru destination. The
		// tower and its aura remain as scenery, but its old Floor-16 trigger is dead.
		if (floor == RADIRU_FLOOR && variables(player).dkc_cleared >= DkcFloorRegistry.LAST_FLOOR
				&& !variables(player).radiru_slaughtered)
			return;
		BlockPos pedestal = pedestalPosition(player, floor);
		if (variables(player).dkc_cleared >= floor
				&& player.serverLevel().getGameTime() % 100L == Math.floorMod(player.getId(), 100)
				&& player.blockPosition().closerThan(pedestal, 96.0D))
			ensurePermitPedestal(player, floor);
		if (floor >= DkcFloorRegistry.LAST_FLOOR) {
			tickReturnSigil(player, floor);
			return;
		}
		if (!runs.isTransitionArmed(player, floor))
			return;
		if (!runs.isGenerated(player, floor + 1)) {
			long now = player.serverLevel().getGameTime();
			if (now >= player.getPersistentData().getLong(BUILD_RESUME_COOLDOWN)) {
				player.getPersistentData().putLong(BUILD_RESUME_COOLDOWN, now + 100L);
				prepareFloor(player, floor + 1, ReadyAction.OPEN_TRANSITION,
						() -> openTransition(player, floor));
			}
			return;
		}
		BlockPos center = transitionPosition(player, floor);
		if (player.serverLevel().getGameTime() % 10L == 0L)
			player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL,
					center.getX() + 0.5D, center.getY() + 0.8D, center.getZ() + 0.5D,
					8, 1.4D, 0.7D, 1.4D, 0.035D);
		if (player.serverLevel().getGameTime() < player.getPersistentData().getLong(TRANSITION_COOLDOWN))
			return;
		AABB trigger = new AABB(center).inflate(3.0D, 2.5D, 3.0D);
		if (trigger.intersects(player.getBoundingBox())) {
			player.getPersistentData().putLong(TRANSITION_COOLDOWN, player.serverLevel().getGameTime() + 60L);
			teleportToFloor(player, floor + 1);
		}
	}

	private static void tickReturnSigil(ServerPlayer player, int floor) {
		BlockPos local = layout(floor).returnSigil;
		if (local == null)
			return;
		BlockPos center = origin(player, floor).offset(local);
		if (player.serverLevel().getGameTime() % 10L == 0L)
			player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL,
					center.getX() + 0.5D, center.getY() + 0.6D, center.getZ() + 0.5D,
					10, 1.0D, 0.9D, 0.6D, 0.035D);
		if (variables(player).dkc_cleared < DkcFloorRegistry.LAST_FLOOR)
			return;
		long now = player.serverLevel().getGameTime();
		if (now < player.getPersistentData().getLong(RETURN_COOLDOWN))
			return;
		if (new AABB(center).inflate(2.0D, 2.5D, 2.0D).intersects(player.getBoundingBox())) {
			player.getPersistentData().putLong(RETURN_COOLDOWN, now + 80L);
			DKCPathTeleportProcedure.returnToSavedOverworld(player);
		}
	}

	/**
	 * Flat floor dimensions are intentionally featureless outside the authored
	 * district. Keeping players near their instance prevents accidental long-range
	 * exploration from generating an unlimited carpet of otherwise-unused chunks.
	 */
	private static void enforceInstanceBounds(ServerPlayer player, int floor) {
		BlockPos root = origin(player, floor);
		Layout layout = layout(floor);
		double x = player.getX();
		double z = player.getZ();
		if (x >= root.getX() - 72.0D && x <= root.getX() + 72.0D
				&& z >= root.getZ() - 32.0D && z <= root.getZ() + layout.maxZ + 32.0D)
			return;

		BlockPos spawn = spawnPosition(player, floor);
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.fallDistance = 0.0F;
		player.teleportTo(player.serverLevel(), spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
	}

	public static void teleportToFloor(ServerPlayer player, int floor) {
		if (player == null || player.server == null)
			return;
		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		if (!runs.isUnlocked(player, floor)) {
			player.displayClientMessage(Component.literal("\u00A74That tower path is still sealed."), true);
			return;
		}
		prepareFloor(player, floor, ReadyAction.TELEPORT,
				() -> teleportPrepared(player, floor));
	}

	/** Debug-only path that runs cleanup after the destination is ready and occupied. */
	public static void debugTeleportToFloor(ServerPlayer player, int floor, @Nullable Runnable afterTeleport) {
		if (player == null || player.server == null)
			return;
		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		if (!runs.isUnlocked(player, floor))
			return;
		if (!DkcFloorRegistry.isDkc(player.level()))
			DKCPathTeleportProcedure.saveReturnPositionForDebug(player);
		player.getPersistentData().putInt(DEBUG_REQUESTED_FLOOR_TAG, floor);
		prepareFloor(player, floor, ReadyAction.DEBUG_TELEPORT, () -> {
			if (player.getPersistentData().getInt(DEBUG_REQUESTED_FLOOR_TAG) != floor)
				return;
			player.getPersistentData().remove(DEBUG_REQUESTED_FLOOR_TAG);
			teleportPrepared(player, floor);
			if (afterTeleport != null)
				afterTeleport.run();
		});
	}

	/** Invalidates loaded and unloaded owned encounter actors for a fresh command test. */
	public static void resetEncounterForDebug(ServerPlayer player, int floor) {
		if (player == null || player.server == null || floor < DkcFloorRegistry.FIRST_FLOOR
				|| floor > DkcFloorRegistry.LAST_FLOOR)
			return;
		if (floor == 1)
			player.getPersistentData().putBoolean(DEBUG_BOSS_RESET_PREFIX + floor + "_cerberus", true);
		else if (floor == 10)
			player.getPersistentData().putBoolean(DEBUG_BOSS_RESET_PREFIX + floor + "_vulcan", true);
		else if (floor == DkcFloorRegistry.LAST_FLOOR) {
			player.getPersistentData().putBoolean(DEBUG_BOSS_RESET_PREFIX + floor + "_baran", true);
			player.getPersistentData().putBoolean(DEBUG_BOSS_RESET_PREFIX + floor + "_kaiselin", true);
		}
		OWNED_SPAWN_GUARDS.keySet().removeIf(key -> key.server == player.server
				&& key.player.equals(player.getUUID()) && key.floor == floor);
		ServerLevel level = player.server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
		if (level == null)
			return;
		String owner = player.getStringUUID();
		level.getEntitiesOfClass(Entity.class, combatBounds(player, floor), entity -> {
			CompoundTag tag = entity.getPersistentData();
			return floor == (int) tag.getDouble("dkc_floor_number")
					&& owner.equals(tag.getString("dkc_spawned_by"));
		}).forEach(Entity::discard);
	}

	private static void teleportPrepared(ServerPlayer player, int floor) {
		if (player.server == null)
			return;
		ServerLevel target = player.server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
		if (target == null)
			return;
		BlockPos spawn = spawnPosition(player, floor);
		target.getChunk(spawn);
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.fallDistance = 0.0F;
		float arrivalYaw = floor == RADIRU_FLOOR ? 180.0F : 0.0F;
		player.teleportTo(target, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
				arrivalYaw, 0.0F);
		player.getPersistentData().putBoolean(DkcSpatialLayout.ACTIVE_RUN_TAG, true);
		player.getPersistentData().putDouble("dkc_previous_floor", player.getPersistentData().getDouble("dkc_current_floor"));
		player.getPersistentData().putDouble("dkc_current_floor", floor);
		player.getPersistentData().putBoolean("dkc_floor_just_changed", true);
		player.getPersistentData().putLong(TRANSITION_COOLDOWN, target.getGameTime() + 60L);
		if (floor == RADIRU_FLOOR && variables(player).dkc_cleared >= DkcFloorRegistry.LAST_FLOOR
				&& !variables(player).radiru_slaughtered
				&& sealRadiruTower(target, player)) {
			player.getPersistentData().putBoolean("radiru_floor_15_tower_sealed", true);
		}
		if (floor == RADIRU_FLOOR)
			DkcRadiruManager.onFloorEntered(player);
		target.playSound(null, spawn, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.55F, 0.75F);
		SololevelingMod.queueServerWork(player.server, 20, () -> {
			if (player.isAlive() && DkcSpatialLayout.isPlayerInFloor(player, floor))
				ensureBosses(player, floor);
		});
	}

	public static void ensureBosses(ServerPlayer player, int floor) {
		if (player == null || !(player.level() instanceof ServerLevel level))
			return;
		if (!DkcSpatialLayout.isPlayerInFloor(player, floor))
			return;
		if (variables(player).dkc_cleared >= floor)
			return;
		AABB area = combatBounds(player, floor);
		BlockPos bossPos = bossPosition(player, floor);
		if (floor == 1 && !player.getPersistentData().getBoolean("dkc_floor_1_boss_defeated")) {
			ensureSingleBoss(level, player, floor, area, bossPos, 0.0D, "cerberus",
					SololevelingModEntities.CERBERUS.get(), CerberusEntity.class);
		} else if (floor == 10 && player.getPersistentData().getBoolean("dkc_floor_10_complete")
				&& !player.getPersistentData().getBoolean("dkc_floor_10_boss_defeated")) {
			ensureSingleBoss(level, player, floor, area, bossPos, 0.0D, "vulcan",
					SololevelingModEntities.VULCAN.get(), VulcanEntity.class);
		} else if (floor == 20) {
			if (!player.getPersistentData().getBoolean("dkc_floor_20_baran_defeated")
					&& !player.getPersistentData().getBoolean("dkc_floor_20_boss_defeated"))
				ensureSingleBoss(level, player, floor, area, bossPos, 0.0D, "baran",
						SololevelingModEntities.BARAN.get(), BaranEntity.class);
			if (!player.getPersistentData().getBoolean("dkc_floor_20_kaiselin_defeated")) {
				BlockPos dragonPos = bossPos.offset(0, 0, -20);
				ensureSingleBoss(level, player, floor, area, dragonPos, 6.0D, "kaiselin",
						SololevelingModEntities.KAISELIN.get(), KaiselinEntity.class);
			}
		}
	}

	private static <T extends Mob> void ensureSingleBoss(ServerLevel level, ServerPlayer player, int floor,
			AABB floorArea, BlockPos position, double yOffset, String kind,
			EntityType<T> entityType, Class<T> entityClass) {
		if (!level.isPositionEntityTicking(position))
			return;
		String owner = player.getStringUUID();
		AABB legacyArea = new AABB(position).inflate(48.0D, 24.0D, 48.0D);
		List<T> candidates = level.getEntitiesOfClass(entityClass, floorArea, entity -> {
			if (entity.getType() != entityType)
				return false;
			String entityOwner = entity.getPersistentData().getString("dkc_spawned_by");
			return owner.equals(entityOwner) || entityOwner.isBlank() && legacyArea.intersects(entity.getBoundingBox());
		});
		String debugResetTag = DEBUG_BOSS_RESET_PREFIX + floor + "_" + kind;
		if (player.getPersistentData().getBoolean(debugResetTag)) {
			player.getPersistentData().remove(debugResetTag);
			candidates.forEach(Entity::discard);
			candidates = List.of();
		}
		if (!candidates.isEmpty()) {
			candidates.sort(Comparator
					.<T, Boolean>comparing(entity -> !owner.equals(entity.getPersistentData().getString("dkc_spawned_by")))
					.thenComparingDouble(entity -> entity.distanceToSqr(position.getX() + 0.5D,
							position.getY() + yOffset, position.getZ() + 0.5D)));
			T keeper = candidates.get(0);
			markBossOwnership(keeper, player, floor);
			keeper.setTarget(player);
			for (int index = 1; index < candidates.size(); index++)
				candidates.get(index).discard();
			OWNED_SPAWN_GUARDS.remove(new OwnedSpawnKey(player.server, player.getUUID(), floor, kind));
			return;
		}

		OwnedSpawnKey key = new OwnedSpawnKey(player.server, player.getUUID(), floor, kind);
		long now = level.getGameTime();
		Long guardedUntil = OWNED_SPAWN_GUARDS.get(key);
		if (guardedUntil != null && guardedUntil > now)
			return;
		OWNED_SPAWN_GUARDS.put(key, now + 20L);
		T boss = spawnOwnedBoss(level, entityType, player, floor, position, yOffset);
		if (boss == null)
			OWNED_SPAWN_GUARDS.remove(key);
	}

	@Nullable
	private static <T extends Mob> T spawnOwnedBoss(ServerLevel level, EntityType<T> entityType,
			ServerPlayer player, int floor, BlockPos position, double yOffset) {
		T boss = entityType.create(level);
		if (boss == null)
			return null;
		try {
			markBoss(boss, player, floor, position, yOffset);
			boss.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.MOB_SUMMONED, null);
			markBossOwnership(boss, player, floor);
			boss.setTarget(player);
			boss.setHealth(boss.getMaxHealth());
			if (!level.addFreshEntity(boss)) {
				boss.discard();
				return null;
			}
			return boss;
		} catch (RuntimeException exception) {
			boss.discard();
			SololevelingMod.LOGGER.error("Failed to spawn DKC floor {} boss {}", floor,
					entityType.getDescriptionId(), exception);
			return null;
		}
	}

	private static void markBoss(@Nullable Entity boss, ServerPlayer player, int floor, BlockPos position, double yOffset) {
		if (boss == null)
			return;
		boss.moveTo(position.getX() + 0.5D, position.getY() + yOffset, position.getZ() + 0.5D, 180.0F, 0.0F);
		markBossOwnership(boss, player, floor);
		if (boss instanceof Mob mob)
			mob.setTarget(player);
	}

	private static void markBossOwnership(Entity boss, ServerPlayer player, int floor) {
		boss.getPersistentData().putDouble("dkc_floor_number", floor);
		boss.getPersistentData().putString("dkc_spawned_by", player.getStringUUID());
		boss.getPersistentData().putString(DungeonMobLevelAdapter.ROLE_TAG,
				DungeonMobLevelAdapter.MobRole.BOSS.id());
	}

	private static void ensureTowerAura(ServerLevel level, ServerPlayer player, int floor) {
		BlockPos anchor = origin(player, floor).offset(0, 0, layout(floor).towerZ + 32);
		if (!level.isPositionEntityTicking(anchor))
			return;
		AABB area = new AABB(anchor).inflate(5.0D, 8.0D, 5.0D);
		List<DKCTowerAuraEntity> found = level.getEntitiesOfClass(DKCTowerAuraEntity.class, area,
				aura -> aura.getPersistentData().hasUUID(OWNER_TAG)
						&& player.getUUID().equals(aura.getPersistentData().getUUID(OWNER_TAG))
						&& (aura.getPersistentData().getInt("dkc_floor_number") == floor
								|| floor == 1 && !aura.getPersistentData().contains("dkc_floor_number")));
		DKCTowerAuraEntity keeper = null;
		for (DKCTowerAuraEntity aura : found) {
			if (keeper == null) {
				keeper = aura;
			} else {
				aura.discard();
			}
		}
		float radius = 32.0F;
		float height = towerAuraHeight(floor);
		float intensity = towerAuraIntensity(floor);
		OwnedSpawnKey auraKey = new OwnedSpawnKey(player.server, player.getUUID(), floor, "tower_aura");
		if (keeper == null) {
			long now = level.getGameTime();
			Long guardedUntil = OWNED_SPAWN_GUARDS.get(auraKey);
			if (guardedUntil != null && guardedUntil > now)
				return;
			OWNED_SPAWN_GUARDS.put(auraKey, now + 20L);
			keeper = DKCTowerAuraEntity.spawn(level, anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D,
					radius, height, intensity);
		} else {
			OWNED_SPAWN_GUARDS.remove(auraKey);
		}
		keeper.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
		keeper.getPersistentData().putInt("dkc_floor_number", floor);
		keeper.setAuraRadius(radius);
		keeper.setAuraHeight(height);
		keeper.setIntensity(intensity);
		keeper.setCrownLightning(floor == 1 || floor >= 16);
	}

	private static float towerAuraHeight(int floor) {
		if (floor == 1)
			return 320.0F;
		if (floor <= 5)
			return 96.0F;
		if (floor <= 10)
			return 112.0F;
		if (floor <= 15)
			return 128.0F;
		if (floor <= 18)
			return 144.0F;
		return 192.0F;
	}

	private static float towerAuraIntensity(int floor) {
		if (floor == 1)
			return 0.86F;
		if (floor <= 5)
			return 0.58F + (floor - 2) * 0.026F;
		if (floor <= 10)
			return 0.68F + (floor - 6) * 0.020F;
		if (floor <= 15)
			return 0.78F + (floor - 11) * 0.020F;
		if (floor <= 18)
			return 0.88F + (floor - 16) * 0.020F;
		return floor == 19 ? 0.96F : 1.0F;
	}

	public static BlockPos origin(ServerPlayer player, int floor) {
		if (player == null || player.server == null)
			throw new IllegalArgumentException("A connected server player is required for a DKC origin.");
		int slot = DkcRunSavedData.get(player.server).slot(player);
		return DkcSpatialLayout.floorOrigin(slot, floor);
	}

	public static BlockPos spawnPosition(ServerPlayer player, int floor) {
		return origin(player, floor).offset(layout(floor).spawn);
	}

	public static BlockPos transitionPosition(ServerPlayer player, int floor) {
		BlockPos transition = layout(floor).transition;
		return transition == null ? origin(player, floor) : origin(player, floor).offset(transition);
	}

	public static BlockPos pedestalPosition(ServerPlayer player, int floor) {
		BlockPos pedestal = layout(floor).pedestal;
		return pedestal == null ? origin(player, floor) : origin(player, floor).offset(pedestal);
	}

	public static BlockPos bossPosition(ServerPlayer player, int floor) {
		BlockPos boss = layout(floor).boss;
		return origin(player, floor).offset(boss == null ? new BlockPos(0, 3, 48) : boss);
	}

	/** Stable Floor-15 throne anchor used by the Esil/Radiru runtime. */
	public static BlockPos radiruEsilPosition(ServerPlayer player) {
		return origin(player, RADIRU_FLOOR).offset(RADIRU_ESIL);
	}

	/** Stable peaceful resident anchors in the open castle courtyard. */
	public static List<BlockPos> radiruResidentPositions(ServerPlayer player) {
		BlockPos root = origin(player, RADIRU_FLOOR);
		return RADIRU_RESIDENTS.stream().map(root::offset).toList();
	}

	/** Stable no-AI target positions inside the east-wing training hall. */
	public static List<BlockPos> radiruTrainingDummyPositions(ServerPlayer player) {
		BlockPos root = origin(player, RADIRU_FLOOR);
		return RADIRU_TRAINING_DUMMIES.stream().map(root::offset).toList();
	}

	/** Three broad battlefield centers available for the surrender wave. */
	public static List<BlockPos> radiruWavePositions(ServerPlayer player) {
		BlockPos root = origin(player, RADIRU_FLOOR);
		return RADIRU_WAVE_ANCHORS.stream().map(root::offset).toList();
	}

	public static BlockPos radiruGatePosition(ServerPlayer player) {
		return origin(player, RADIRU_FLOOR).offset(RADIRU_GATE);
	}

	public static BlockPos radiruCourtyardPosition(ServerPlayer player) {
		return origin(player, RADIRU_FLOOR).offset(RADIRU_COURTYARD);
	}

	public static AABB radiruGateBounds(ServerPlayer player) {
		BlockPos root = origin(player, RADIRU_FLOOR);
		return new AABB(root.getX() - 10.0D, root.getY() + 3.0D, root.getZ() + 115.0D,
				root.getX() + 10.0D, root.getY() + 18.0D, root.getZ() + 120.0D);
	}

	public static AABB radiruCastleBounds(ServerPlayer player) {
		BlockPos root = origin(player, RADIRU_FLOOR);
		return new AABB(root.getX() + RADIRU_CASTLE_X, root.getY(), root.getZ() + RADIRU_CASTLE_Z,
				root.getX() + RADIRU_CASTLE_X + RADIRU_CASTLE_WIDTH, root.getY() + 64.0D,
				root.getZ() + RADIRU_CASTLE_Z + RADIRU_CASTLE_LENGTH);
	}

	/**
	 * Replaces only the Floor-15 portcullis overlay. No castle chunks are rebuilt
	 * and no permanent ticket is installed; callers should invoke this while the
	 * player is physically inside their owned Floor-15 cell.
	 */
	public static boolean openRadiruGate(ServerLevel level, ServerPlayer player) {
		return setRadiruGate(level, player, true);
	}

	/** Restores the authored closed portcullis for a fresh/failed encounter. */
	public static boolean closeRadiruGate(ServerLevel level, ServerPlayer player) {
		return setRadiruGate(level, player, false);
	}

	/** Makes the old ascension chamber visibly inert after the DKC is conquered. */
	public static boolean sealRadiruTower(ServerLevel level, ServerPlayer player) {
		if (level == null || player == null || player.server == null
				|| level.dimension() != DkcFloorRegistry.SHARED_DIMENSION
				|| !DkcSpatialLayout.isPlayerInFloor(player, RADIRU_FLOOR))
			return false;
		try {
			sealRadiruTowerBlocks(level, player);
			return true;
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Failed to seal House Radiru's conquered ascension tower for {}",
					player.getGameProfile().getName(), exception);
			return false;
		}
	}

	private static void sealRadiruTowerBlocks(ServerLevel level, ServerPlayer player) {
		place(level, origin(player, RADIRU_FLOOR).offset(-10, 0, RADIRU_TOWER_Z),
				piece("dkc_tower_gate_closed", -10, 0, RADIRU_TOWER_Z));
		BlockPos pedestal = pedestalPosition(player, RADIRU_FLOOR);
		if (level.getBlockState(pedestal).is(SololevelingModBlocks.DEEPSLATE_KEYBLOCK_DKC.get()))
			level.setBlock(pedestal, Blocks.AIR.defaultBlockState(), 3);
	}

	private static boolean setRadiruGate(ServerLevel level, ServerPlayer player, boolean opened) {
		if (level == null || player == null || player.server == null
				|| !level.dimension().equals(DkcFloorRegistry.SHARED_DIMENSION)
				|| !DkcSpatialLayout.isPlayerInFloor(player, RADIRU_FLOOR))
			return false;
		String templateName = opened ? "dkc_radiru_gate_open" : "dkc_radiru_gate_closed";
		Placement gate = piece(templateName, -10, 0, 112);
		try {
			place(level, origin(player, RADIRU_FLOOR).offset(gate.x, gate.y, gate.z), gate);
			BlockPos center = radiruGatePosition(player);
			level.playSound(null, center, opened ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
					SoundSource.BLOCKS, 1.2F, opened ? 0.68F : 0.58F);
			level.sendParticles(opened ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
					center.getX() + 0.5D, center.getY() + 6.0D, center.getZ() + 0.5D,
					28, 4.5D, 4.5D, 1.2D, 0.025D);
			return true;
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Failed to {} House Radiru's Floor-15 gate for {}",
					opened ? "open" : "close", player.getGameProfile().getName(), exception);
			return false;
		}
	}

	public static AABB combatBounds(ServerPlayer player, int floor) {
		BlockPos origin = origin(player, floor);
		Layout layout = layout(floor);
		// This remains instance-bounded, but extends beyond the player's enforced
		// travel boundary so a knocked-back or briefly-lured persistent mob cannot
		// fall outside the existence scan and be duplicated by replenishment.
		return new AABB(origin.getX() - 96, 0, origin.getZ() - 64,
				origin.getX() + 96, 384, origin.getZ() + layout.maxZ + 64);
	}

	@Nullable
	public static BlockPos findCombatSpawn(ServerLevel level, ServerPlayer player, int floor, int salt, Mob mob) {
		if (level == null || player == null || mob == null)
			return null;
		Layout layout = layout(floor);
		BlockPos root = origin(player, floor);
		int minimumZ = root.getZ() + 26;
		int maximumZ = root.getZ() + layout.towerZ - 10;
		if (maximumZ < minimumZ)
			return null;

		int playerY = player.blockPosition().getY();
		BlockPos entry = spawnPosition(player, floor);
		double entrySafeRadiusSquared = (double) COMBAT_ENTRY_SAFE_RADIUS * COMBAT_ENTRY_SAFE_RADIUS;
		int pathAttempts = 0;
		double phase = Math.floorMod(salt, COMBAT_SPAWN_ATTEMPTS)
				* (Math.PI * 2.0D / COMBAT_SPAWN_ATTEMPTS);
		for (int attempt = 0; attempt < COMBAT_SPAWN_ATTEMPTS; attempt++) {
			double angle = phase + attempt * 2.399963229728653D + (level.random.nextDouble() - 0.5D) * 0.35D;
			int distance = COMBAT_MIN_SPAWN_DISTANCE + level.random.nextInt(
					COMBAT_MAX_SPAWN_DISTANCE - COMBAT_MIN_SPAWN_DISTANCE + 1);
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
			if (z < minimumZ || z > maximumZ)
				continue;
			double entryDx = x + 0.5D - (entry.getX() + 0.5D);
			double entryDz = z + 0.5D - (entry.getZ() + 0.5D);
			if (entryDx * entryDx + entryDz * entryDz < entrySafeRadiusSquared)
				continue;
			int localZ = z - root.getZ();
			int halfWidth = combatHalfWidth(floor, localZ);
			if (halfWidth <= 0 || Math.abs(x - root.getX()) > halfWidth)
				continue;

			for (int yOffset : COMBAT_Y_OFFSETS) {
				BlockPos feet = new BlockPos(x, playerY + yOffset, z);
				if (!level.hasChunkAt(feet) || !level.isPositionEntityTicking(feet)
						|| !isOpenCombatSpace(level, feet))
					continue;
				mob.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D,
						level.random.nextFloat() * 360.0F, 0.0F);
				mob.refreshDimensions();
				if (!level.noCollision(mob, mob.getBoundingBox()) || !mob.hasLineOfSight(player))
					continue;
				mob.setOnGround(true);
				Path path = mob.getNavigation().createPath(player.blockPosition(), 0);
				pathAttempts++;
				if (path != null && path.canReach())
					return feet;
				if (pathAttempts >= COMBAT_PATH_ATTEMPTS)
					return null;
			}
		}
		return null;
	}

	/** True only for the authored district footprint where floor-wave mobs belong. */
	public static boolean isInsideCombatArea(ServerPlayer player, int floor, BlockPos position) {
		if (player == null || position == null)
			return false;
		BlockPos root = origin(player, floor);
		Layout layout = layout(floor);
		int localZ = position.getZ() - root.getZ();
		int halfWidth = combatHalfWidth(floor, localZ);
		return localZ >= 24 && localZ <= layout.towerZ - 8
				&& halfWidth > 0 && Math.abs(position.getX() - root.getX()) <= halfWidth + 2
				&& position.getY() >= root.getY() - 2 && position.getY() <= root.getY() + 16;
	}

	/** Relocates a trapped live wave mob through the same safe-spawn contract. */
	public static boolean recoverWaveMob(ServerLevel level, ServerPlayer player, int floor, Mob mob) {
		if (level == null || player == null || mob == null)
			return false;
		Entity created = mob.getType().create(level);
		if (!(created instanceof Mob probe))
			return false;
		if (mob instanceof net.solocraft.entity.DemonEntity source
				&& probe instanceof net.solocraft.entity.DemonEntity target)
			target.setVisualScale(source.getVisualScale());
		BlockPos destination = findCombatSpawn(level, player, floor, mob.getId(), probe);
		probe.discard();
		if (destination == null)
			return false;
		mob.getNavigation().stop();
		mob.teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
		mob.setDeltaMovement(0.0D, 0.0D, 0.0D);
		mob.fallDistance = 0.0F;
		mob.setOnGround(true);
		mob.setTarget(player);
		return true;
	}

	private static boolean isOpenCombatSpace(ServerLevel level, BlockPos feet) {
		BlockPos groundPos = feet.below();
		BlockState ground = level.getBlockState(groundPos);
		if (!ground.getFluidState().isEmpty() || !ground.isFaceSturdy(level, groundPos, Direction.UP)
				|| ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CAMPFIRE)
				|| ground.is(Blocks.SOUL_CAMPFIRE))
			return false;
		for (int y = 0; y <= 2; y++) {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					BlockPos check = feet.offset(x, y, z);
					BlockState state = level.getBlockState(check);
					if (!state.getFluidState().isEmpty()
							|| !state.getCollisionShape(level, check).isEmpty()
							|| state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE))
						return false;
				}
			}
		}
		return true;
	}

	private static int combatHalfWidth(int floor, int localZ) {
		if (floor == RADIRU_FLOOR)
			return localZ >= 128 && localZ <= 206 ? 40 : 0;
		int cursor = 24;
		for (Zone zone : zones(floor)) {
			if (localZ >= cursor && localZ < cursor + zone.length)
				return Math.max(6, zone.width / 2 - 2);
			cursor += zone.length;
		}
		return 0;
	}

	private static InteractionHand permitHand(ServerPlayer player, int floor) {
		if (DkcRadiruManager.isPermitValidForFloor(player.getMainHandItem(), player, floor))
			return InteractionHand.MAIN_HAND;
		if (DkcRadiruManager.isPermitValidForFloor(player.getOffhandItem(), player, floor))
			return InteractionHand.OFF_HAND;
		return null;
	}

	private static SololevelingModVariables.PlayerVariables variables(ServerPlayer player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static Layout layout(int floor) {
		if (floor < DkcFloorRegistry.FIRST_FLOOR || floor > DkcFloorRegistry.LAST_FLOOR)
			throw new IllegalArgumentException("Invalid DKC floor layout: " + floor);
		return LAYOUTS[floor];
	}

	private static Layout[] createLayouts() {
		Layout[] layouts = new Layout[DkcFloorRegistry.LAST_FLOOR + 1];
		for (int floor = DkcFloorRegistry.FIRST_FLOOR; floor <= DkcFloorRegistry.LAST_FLOOR; floor++)
			layouts[floor] = createLayout(floor);
		return layouts;
	}

	private static Layout createLayout(int floor) {
		if (floor == 1)
			return floorOne();
		if (floor == RADIRU_FLOOR)
			return floorFifteen();
		List<Placement> placements = new ArrayList<>();
		placements.add(piece("dkc_lower_start", -12, 0, 0));
		int z = 24;
		for (Zone zone : zones(floor)) {
			addZone(placements, zone, z);
			z += zone.length;
		}
		BlockPos boss = new BlockPos(0, floor == 20 ? 6 : 3, floor == 20 ? 24 + 54 : bossZFor(floor, z));
		if (floor == 20) {
			addTowerStage(placements, towerStage(floor), -32, 0, z);
			placements.add(piece("dkc_return_shrine", -8, 0, z + 16));
			placements.add(piece("dkc_tower_gate_open", -10, 0, z));
			return new Layout(List.copyOf(placements), new BlockPos(0, 3, 9), null, null, boss,
					new BlockPos(0, 4, z + 27), z + 64, z);
		}
		addTowerStage(placements, towerStage(floor), -32, 0, z);
		placements.add(piece(floor % 5 == 0 ? "dkc_lower_rest_transition" : "dkc_lower_transition", -16, 0, z));
		placements.add(piece("dkc_tower_gate_closed", -10, 0, z));
		return new Layout(List.copyOf(placements), new BlockPos(0, 3, 9),
				new BlockPos(0, 4, z + 18), new BlockPos(-4, 4, z - 4), boss, null, z + 64, z);
	}

	private static Layout floorFifteen() {
		List<Placement> placements = new ArrayList<>();
		addTiledArea(placements, "dkc_radiru_castle", RADIRU_CASTLE_X, 0, RADIRU_CASTLE_Z,
				RADIRU_CASTLE_WIDTH, RADIRU_CASTLE_LENGTH, 32);
		addTiledArea(placements, "dkc_radiru_battlefield", RADIRU_FIELD_X, 0, RADIRU_FIELD_Z,
				RADIRU_FIELD_WIDTH, RADIRU_FIELD_LENGTH, 32);
		addTowerStage(placements, towerStage(RADIRU_FLOOR), -32, 0, RADIRU_TOWER_Z);
		placements.add(piece("dkc_lower_rest_transition", -16, 0, RADIRU_TOWER_Z));
		placements.add(piece("dkc_tower_gate_closed", -10, 0, RADIRU_TOWER_Z));
		// Placed last so the encounter portcullis overlays the castle's authored aperture.
		placements.add(piece("dkc_radiru_gate_closed", -10, 0, 112));
		return new Layout(List.copyOf(placements), new BlockPos(0, 3, 196),
				new BlockPos(0, 4, RADIRU_TOWER_Z + 18),
				new BlockPos(-4, 4, RADIRU_TOWER_Z - 4),
				new BlockPos(0, 3, 163), null, RADIRU_TOWER_Z + 64, RADIRU_TOWER_Z);
	}

	private static String towerStage(int floor) {
		if (floor <= 5)
			return "base";
		if (floor <= 9)
			return "mid_a";
		if (floor <= 12)
			return "mid_b";
		if (floor <= 15)
			return "mid_c";
		if (floor <= 18)
			return "mid_d";
		return "crown";
	}

	private static int bossZFor(int floor, int transitionZ) {
		if (floor == 10)
			return 24 + 40;
		return Math.max(42, transitionZ - 32);
	}

	private static Layout floorOne() {
		List<Placement> pieces = new ArrayList<>();
		pieces.add(piece("dkc_f1_arrival_plaza", -24, 0, 0));
		pieces.add(piece("dkc_f1_approach_a", -16, 0, 48));
		for (int tileZ = 0; tileZ < 2; tileZ++)
			for (int tileX = 0; tileX < 2; tileX++)
				pieces.add(piece("dkc_f1_cerberus_courtyard_x" + tileX + "_z" + tileZ,
						-40 + tileX * 40, 0, 72 + tileZ * 40));
		addTowerStage(pieces, "base", -32, 0, 152);
		addTowerStage(pieces, "mid_a", -32, 48, 152);
		addTowerStage(pieces, "mid_b", -32, 84, 152);
		addTowerStage(pieces, "mid_c", -32, 120, 152);
		addTowerStage(pieces, "mid_d", -32, 156, 152);
		addTowerStage(pieces, "crown", -32, 192, 152);
		pieces.add(piece("dkc_tower_gate_closed", -10, 0, 152));
		pieces.add(piece("dkc_tower_lobby", -18, 0, 166));
		pieces.add(piece("dkc_f1_ascension_chamber", -16, 0, 208));
		return new Layout(List.copyOf(pieces), new BlockPos(0, 3, 14),
				new BlockPos(0, 4, 224), new BlockPos(-4, 4, 148), new BlockPos(0, 3, 110), null, 240, 152);
	}

	private static List<Zone> zones(int floor) {
		return switch (floor) {
			case 2 -> List.of(VILLAGE);
			case 3 -> List.of(STREET_A, RUNES, STREET_B);
			case 4 -> List.of(ASH_WASTES);
			case 5 -> List.of(MARKET, STREET_C);
			case 6 -> List.of(CATHEDRAL);
			case 7 -> List.of(MAGMA, STREET_A);
			case 8 -> List.of(VILLAGE, INTERSECTION);
			case 9 -> List.of(ASH_WASTES, COURTYARD);
			case 10 -> List.of(FORGE);
			case 11 -> List.of(STREET_C, MARKET, STREET_B);
			case 12 -> List.of(CATHEDRAL, MAGMA);
			case 13 -> List.of(INTERSECTION, RUNES, STREET_C, COURTYARD);
			case 14 -> List.of(FORGE, STREET_A, INTERSECTION);
			case 15 -> List.of(); // Floor 15 has its bespoke Radiru castle/field layout.
			case 16 -> List.of(DRAGON_COURT, STREET_B);
			case 17 -> List.of(DRAGON_COURT, MAGMA);
			case 18 -> List.of(VILLAGE, DRAGON_COURT);
			case 19 -> List.of(CATHEDRAL, DRAGON_COURT);
			case 20 -> List.of(THRONE);
			default -> List.of(STREET_A, RUNES);
		};
	}

	private static Zone tiled(String name) {
		return new Zone(name, 80, 80, true);
	}

	private static void addZone(List<Placement> placements, Zone zone, int z) {
		if (!zone.tiled) {
			placements.add(piece(zone.name, -zone.width / 2, 0, z));
			return;
		}
		for (int tileZ = 0; tileZ < 2; tileZ++)
			for (int tileX = 0; tileX < 2; tileX++)
				placements.add(piece(zone.name + "_x" + tileX + "_z" + tileZ,
						-zone.width / 2 + tileX * 40, 0, z + tileZ * 40));
	}

	private static void addTowerStage(List<Placement> placements, String stage, int x, int y, int z) {
		for (int tileZ = 0; tileZ < 2; tileZ++)
			for (int tileX = 0; tileX < 2; tileX++)
				placements.add(piece("dkc_tower_" + stage + "_x" + tileX + "_z" + tileZ,
						x + tileX * 32, y, z + tileZ * 32));
	}

	private static void addTiledArea(List<Placement> placements, String name, int x, int y, int z,
			int width, int length, int tileSize) {
		for (int tileZ = 0; tileZ * tileSize < length; tileZ++)
			for (int tileX = 0; tileX * tileSize < width; tileX++)
				placements.add(piece(name + "_x" + tileX + "_z" + tileZ,
						x + tileX * tileSize, y, z + tileZ * tileSize));
	}

	private static Placement piece(String name, int x, int y, int z) {
		return new Placement(ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, name), x, y, z, Rotation.NONE);
	}

	private static BlockPos rotationOrigin(BlockPos desiredMin, Vec3i size, Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> desiredMin.offset(size.getZ() - 1, 0, 0);
			case CLOCKWISE_180 -> desiredMin.offset(size.getX() - 1, 0, size.getZ() - 1);
			case COUNTERCLOCKWISE_90 -> desiredMin.offset(0, 0, size.getX() - 1);
			default -> desiredMin;
		};
	}

	private record Placement(ResourceLocation template, int x, int y, int z, Rotation rotation) {
	}

	private record BuildStep(@Nullable BlockPos desiredMin, @Nullable Placement placement,
			BoundingBox bounds, boolean clear) {
	}

	private record Zone(String name, int width, int length, boolean tiled) {
	}

	private record Layout(List<Placement> placements, BlockPos spawn, @Nullable BlockPos transition,
			@Nullable BlockPos pedestal, @Nullable BlockPos boss, @Nullable BlockPos returnSigil,
			int maxZ, int towerZ) {
	}

	private record BuildKey(MinecraftServer server, UUID player, int floor) {
	}

	private record OwnedSpawnKey(MinecraftServer server, UUID player, int floor, String kind) {
	}

	private enum ReadyAction {
		OPEN_TRANSITION,
		TELEPORT,
		DEBUG_TELEPORT
	}

	private static final class BuildContext {
		private final BuildKey key;
		private final MinecraftServer server;
		private final ServerPlayer player;
		private final int floor;
		private final ServerLevel level;
		private final Layout layout;
		private final Map<ReadyAction, Runnable> callbacks = new EnumMap<>(ReadyAction.class);
		private volatile boolean failed;
		private volatile boolean cancelled;

		private BuildContext(BuildKey key, MinecraftServer server, ServerPlayer player, int floor,
				ServerLevel level, Layout layout) {
			this.key = key;
			this.server = server;
			this.player = player;
			this.floor = floor;
			this.level = level;
			this.layout = layout;
		}
	}
}
