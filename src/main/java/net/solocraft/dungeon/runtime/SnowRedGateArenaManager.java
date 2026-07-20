package net.solocraft.dungeon.runtime;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.entity.RedGateEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.UrgentQuestManager;
import net.solocraft.world.dimension.rift.RiftTerritory;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates an instance-scoped Monarch-territory gauntlet for both legacy and
 * procedural red gates. The historical class name is retained so generated
 * procedures and existing integrations do not need a migration.
 */
@Mod.EventBusSubscriber
public final class SnowRedGateArenaManager {
	public static final ResourceLocation ARENA_ID = new ResourceLocation("sololeveling", "red_gate_monarch_arena");
	private static final ResourceLocation LEGACY_ARENA_ID = new ResourceLocation("sololeveling", "red_gate_snow_arena");
	/** Retained only so runs created by the first Frost implementation can finish. */
	public static final ResourceKey<Level> SNOW_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation("sololeveling", "dungeon_dimension_snow"));
	public static final String TERRITORY_TAG = "slr_red_gate_territory";
	private static final Map<RiftTerritory, ResourceKey<Level>> TERRITORY_DIMENSIONS = createTerritoryDimensions();

	private static final ResourceLocation BEAR_POOL = new ResourceLocation("sololeveling", "red_gate_ice_bears");
	private static final ResourceLocation ELF_POOL = new ResourceLocation("sololeveling", "red_gate_ice_elves");
	private static final ResourceLocation BARUKA_POOL = new ResourceLocation("sololeveling", "red_gate_baruka");
	private static final List<ResourceLocation> REQUIRED_POOLS = List.of(BEAR_POOL, ELF_POOL, BARUKA_POOL);

	private static final String SEQUENCE_KEY = "red_gate_monarch";
	private static final String GATE_INSTANCE_TAG = "slr_red_gate_wave_instance";
	private static final String PROCEDURAL_DUNGEON_TAG = "slr_procedural_dungeon";
	private static final String PROCEDURAL_RED_TAG = "slr_procedural_red_gate";
	private static final String BOUNDARY_NOTICE_TAG = "slr_red_gate_boundary_notice";
	private static final String WAVE_NOTICE_TAG = "slr_red_gate_wave_notice";
	private static final int FIRST_WAVE_DELAY = 100;
	private static final int INTERMISSION_TICKS = 160;
	private static final int BOSS_INTRO_TICKS = 200;
	private static final int ARENA_RADIUS = 112;
	private static final int MOB_LEASH_RADIUS = 104;
	private static final int MAX_ACTIVE_WAVE_MOBS = 18;
	private static final int SAFE_ARENA_SEARCH_RADIUS = 96;
	private static int tickCounter;

	private SnowRedGateArenaManager() {
	}

	private static Map<RiftTerritory, ResourceKey<Level>> createTerritoryDimensions() {
		Map<RiftTerritory, ResourceKey<Level>> dimensions = new EnumMap<>(RiftTerritory.class);
		for (RiftTerritory territory : RiftTerritory.values())
			dimensions.put(territory, ResourceKey.create(Registries.DIMENSION,
					new ResourceLocation("sololeveling", "monarch_territory_" + territory.id())));
		return Map.copyOf(dimensions);
	}

	private static Optional<RiftTerritory> storedTerritory(Entity gate) {
		if (gate == null)
			return Optional.empty();
		return Optional.ofNullable(RiftTerritory.fromName(gate.getPersistentData().getString(TERRITORY_TAG)));
	}

	private static RiftTerritory territoryFor(Entity gate) {
		return storedTerritory(gate).orElse(RiftTerritory.FROST);
	}

	private static Optional<RiftTerritory> territoryForDimension(ResourceKey<Level> dimension) {
		if (SNOW_DIMENSION.equals(dimension))
			return Optional.of(RiftTerritory.FROST);
		return TERRITORY_DIMENSIONS.entrySet().stream()
				.filter(entry -> entry.getValue().equals(dimension))
				.map(Map.Entry::getKey)
				.findFirst();
	}

	private static boolean isArenaInstance(DungeonInstanceSavedData.Instance instance) {
		return instance != null && (ARENA_ID.equals(instance.dungeonId())
				|| LEGACY_ARENA_ID.equals(instance.dungeonId()))
				&& territoryForDimension(instance.dimension()).isPresent();
	}

	/** True while at least one scoped Monarch red-gate run still exists. */
	public static boolean hasActiveArena(MinecraftServer server) {
		return server != null && DungeonInstanceSavedData.get(server).listInstances().stream()
				.anyMatch(SnowRedGateArenaManager::isArenaInstance);
	}

	/** Reconciles persistent gate state after a player leaves through the clear portal. */
	public static void onParticipantExited(MinecraftServer server,
			DungeonInstanceSavedData.Instance instance) {
		if (server == null || !isArenaInstance(instance) || !instance.completed()
				|| !instance.participants().isEmpty())
			return;
		DungeonInstanceSavedData.get(server).pruneCompletedEmptyInstances();
		recordArenaClosure(server, instance);
	}

	/** Handles the original dedicated red-gate entity. */
	public static boolean enterLegacy(LevelAccessor world, RedGateEntity gate, ServerPlayer initiator) {
		if (world == null || gate == null || initiator == null)
			return false;
		if (gate.getEntityData().get(RedGateEntity.DATA_usedbefore)) {
			initiator.displayClientMessage(Component.literal("The red gate has already sealed.")
					.withStyle(ChatFormatting.DARK_RED), true);
			return true;
		}
		RiftTerritory territory = territoryFor(gate);
		gate.getPersistentData().putString(TERRITORY_TAG, territory.id());
		return open(world, gate, initiator, nearbyPartyMembers(world, gate, initiator), true,
				territory);
	}

	/** Handles a normal procedural gate immediately after it transforms red. */
	public static boolean enterProcedural(LevelAccessor world, Entity gate, ServerPlayer initiator,
			List<ServerPlayer> entrants) {
		return open(world, gate, initiator, entrants, false, territoryFor(gate));
	}

	/** Assigns a stable territory to a newly created or newly transformed gate. */
	public static RiftTerritory assignTerritoryIfMissing(Entity gate) {
		RiftTerritory existing = storedTerritory(gate).orElse(null);
		if (existing != null)
			return existing;
		RiftTerritory[] territories = RiftTerritory.values();
		RiftTerritory selected = territories[Math.floorMod(gate.getUUID().hashCode(), territories.length)];
		gate.getPersistentData().putString(TERRITORY_TAG, selected.id());
		return selected;
	}

	public static ResourceKey<Level> dimensionFor(RiftTerritory territory) {
		return TERRITORY_DIMENSIONS.getOrDefault(territory, TERRITORY_DIMENSIONS.get(RiftTerritory.FROST));
	}

	private static boolean open(LevelAccessor world, Entity gate, ServerPlayer initiator,
			List<ServerPlayer> requestedEntrants, boolean markLegacyUsed, RiftTerritory territory) {
		if (world.isClientSide() || !(gate.level() instanceof ServerLevel sourceLevel))
			return true;
		if (isDungeonBound(initiator)) {
			initiator.displayClientMessage(Component.literal("You are already bound to a dungeon.")
					.withStyle(ChatFormatting.RED), true);
			return true;
		}
		List<ServerPlayer> entrants = sanitizeEntrants(sourceLevel, gate, initiator, requestedEntrants);
		if (entrants.isEmpty())
			return true;

		for (ResourceLocation pool : REQUIRED_POOLS) {
			if (DungeonDataManager.mobPool(pool).isEmpty()) {
				openingFailure(world, gate, initiator,
						"The red gate encounter is unavailable: missing mob pool " + pool);
				return true;
			}
		}

		MinecraftServer server = sourceLevel.getServer();
		ResourceKey<Level> destination = dimensionFor(territory);
		ServerLevel arenaLevel = server.getLevel(destination);
		if (arenaLevel == null) {
			openingFailure(world, gate, initiator,
					"The " + territory.displayName() + " Monarch territory is unavailable.");
			return true;
		}

		UUID instanceId = gate.getUUID();
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(server);
		if (registry.getInstance(instanceId).isPresent()) {
			initiator.sendSystemMessage(Component.literal("This red gate already has an active encounter.")
					.withStyle(ChatFormatting.RED));
			return true;
		}

		long seed = gate.getUUID().getMostSignificantBits() ^ gate.getUUID().getLeastSignificantBits()
				^ arenaLevel.getSeed();
		int effectiveLevel = effectiveLevelFor(territory, initiator, entrants);
		BlockPos center = findArenaCenter(arenaLevel, targetCoordinate(gate, "tpx", gate.getX()),
				targetCoordinate(gate, "tpz", gate.getZ()));

		DungeonInstanceSavedData.MutationResult<DungeonInstanceSavedData.Instance> created = registry.create(
				instanceId, ARENA_ID, destination, seed, effectiveLevel, arenaLevel.getGameTime());
		if (!created.success() || created.value() == null) {
			openingFailure(world, gate, initiator,
					"Could not create the red gate encounter: " + created.message());
			return true;
		}

		DungeonInstanceSavedData.Instance instance = created.value();
		for (ServerPlayer entrant : entrants) {
			if (!instance.addParticipant(entrant.getUUID())) {
				registry.remove(instanceId);
				openingFailure(world, gate, initiator, "The red gate party is too large for one encounter.");
				return true;
			}
		}
		decorateArena(arenaLevel, center, seed, territory);
		String setupProblem = configureInstance(arenaLevel, instance, center, entrants.size(), seed, territory);
		if (setupProblem != null) {
			registry.remove(instanceId);
			openingFailure(world, gate, initiator,
					"Could not prepare the red gate encounter: " + setupProblem);
			return true;
		}

		for (ServerPlayer entrant : entrants)
			prepareEntrant(sourceLevel, gate, entrant, instance, territory);
		gate.getPersistentData().putString(GATE_INSTANCE_TAG, instanceId.toString());
		gate.getPersistentData().putString(TERRITORY_TAG, territory.id());
		gate.getPersistentData().putBoolean("slr_is_red_gate", true);
		if (markLegacyUsed && gate instanceof RedGateEntity redGate) {
			redGate.getEntityData().set(RedGateEntity.DATA_usedbefore, true);
			redGate.setTexture("21");
		}
		SololevelingModVariables.MapVariables.get(world).RedGate = true;
		SololevelingModVariables.MapVariables.get(world).syncData(world);

		for (ServerPlayer entrant : entrants) {
			entrant.sendSystemMessage(Component.literal("RED GATE — " + territory.displayName().toUpperCase())
					.withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED));
			entrant.displayClientMessage(Component.literal("Survive the Monarch's assault. The exit will remain sealed.")
					.withStyle(ChatFormatting.AQUA), true);
		}
		SololevelingMod.queueServerWork(10, () -> teleportEntrants(arenaLevel, center, entrants, instance));
		SololevelingMod.LOGGER.info("Created {} Monarch red-gate arena {} at {} for {} participant(s), effective level {}",
				territory.id(), instanceId, center, entrants.size(), effectiveLevel);
		return true;
	}

	private static void openingFailure(LevelAccessor world, Entity gate, ServerPlayer initiator, String message) {
		initiator.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
		// A procedural gate is marked used by turnRed before this service runs. Roll
		// that mutation back so a transient datapack/worldgen failure cannot leave a
		// permanently locked gate.
		if (gate instanceof Portal1Entity portal && gate.getPersistentData().getBoolean(PROCEDURAL_RED_TAG)) {
			portal.getEntityData().set(Portal1Entity.DATA_usedbefore, false);
			portal.setTexture("portalgate2");
			gate.getPersistentData().putBoolean(PROCEDURAL_RED_TAG, false);
			gate.getPersistentData().putBoolean("slr_is_red_gate", false);
			SololevelingModVariables.MapVariables.get(world).RedGate = world.getServer() != null
					&& hasActiveArena(world.getServer());
			SololevelingModVariables.MapVariables.get(world).syncData(world);
		}
	}

	private static int effectiveLevelFor(RiftTerritory territory, ServerPlayer initiator, List<ServerPlayer> entrants) {
		if (territory == RiftTerritory.FROST)
			return DungeonLevelHelper.resolveEffectiveLevel(
					DungeonLevelHelper.EffectiveLevelSource.PARTY_AVERAGE, initiator, entrants,
					0, 0, 10);
		return DungeonLevelHelper.resolveEffectiveLevel(
				DungeonLevelHelper.EffectiveLevelSource.PARTY_AVERAGE, initiator, entrants,
				35, 1, DungeonInstanceSavedData.MAX_EFFECTIVE_LEVEL);
	}

	private static String configureInstance(ServerLevel level, DungeonInstanceSavedData.Instance instance,
			BlockPos center, int partySize, long seed, RiftTerritory territory) {
		instance.setPlayerStart(center);
		instance.setExit(center.offset(4, 0, 0));
		instance.setExitFacing(Direction.SOUTH);

		List<WaveSpec> waves = new ArrayList<>();
		if (territory == RiftTerritory.FROST)
			waves.add(new WaveSpec("ice_bear_rush", BEAR_POOL, scaledCount(3, partySize), false, false,
					FIRST_WAVE_DELAY));
		waves.add(new WaveSpec("ice_elf_vanguard", ELF_POOL, scaledCount(4, partySize), false, false,
				territory == RiftTerritory.FROST ? INTERMISSION_TICKS : FIRST_WAVE_DELAY));
		waves.add(new WaveSpec("ice_elf_ambush", ELF_POOL, scaledCount(6, partySize), false, false,
				INTERMISSION_TICKS));
		waves.add(new WaveSpec("ice_elf_encirclement", ELF_POOL, scaledCount(8, partySize), false, false,
				INTERMISSION_TICKS));
		waves.add(new WaveSpec("ice_elf_guard", ELF_POOL, scaledCount(6, partySize), false, true,
				INTERMISSION_TICKS));
		waves.add(new WaveSpec("baruka", BARUKA_POOL, 1, true, false, BOSS_INTRO_TICKS));

		for (int order = 0; order < waves.size(); order++) {
			WaveSpec wave = waves.get(order);
			DungeonInstanceSavedData.MutationResult<DungeonInstanceSavedData.EncounterState> created =
					instance.createEncounter(wave.id(), wave.pool(), wave.boss(), false,
							instance.effectiveLevel(), instance.effectiveLevel(), SEQUENCE_KEY, order, wave.delayTicks());
			if (!created.success() || created.value() == null)
				return created.message();
			DungeonInstanceSavedData.EncounterState encounter = created.value();
			if (order == 0 && !encounter.setTriggerBounds(new DungeonInstanceSavedData.Bounds(
					center.offset(-8, -4, -8), center.offset(8, 8, 8))))
				return "could not persist the arena entry trigger";
			List<BlockPos> positions = wavePositions(level, center, wave.count(), seed, order, wave.boss());
			if (positions.size() != wave.count())
				return "could only find " + positions.size() + " of " + wave.count() + " safe spawn positions for " + wave.id();
			String role = wave.boss() ? "boss" : wave.elite() ? "elite" : "normal";
			for (int marker = 0; marker < positions.size(); marker++) {
				if (!encounter.addMarker(wave.id() + "_" + marker, role, positions.get(marker)))
					return "could not persist spawn marker " + marker + " for " + wave.id();
			}
		}
		return null;
	}

	private static int scaledCount(int base, int partySize) {
		int extraPlayers = Math.max(0, partySize - 1);
		int perExtraPlayer = Math.max(1, (base + 1) / 2);
		return Math.min(MAX_ACTIVE_WAVE_MOBS, base + extraPlayers * perExtraPlayer);
	}

	private static List<BlockPos> wavePositions(ServerLevel level, BlockPos center, int count,
			long seed, int wave, boolean boss) {
		RandomSource random = RandomSource.create(seed ^ (0x9E3779B97F4A7C15L * (wave + 1L)));
		List<BlockPos> positions = new ArrayList<>();
		if (boss) {
			BlockPos position = findUniqueSafeSurface(level, center.getX() + 14, center.getZ(), center, positions);
			if (position != null)
				positions.add(position);
			return positions;
		}
		double offset = random.nextDouble() * Math.PI * 2.0D;
		for (int index = 0; index < count; index++) {
			double angle = offset + Math.PI * 2.0D * index / Math.max(1, count);
			int radius = 12 + random.nextInt(7);
			int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
			int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
			BlockPos position = findUniqueSafeSurface(level, x, z, center, positions);
			if (position != null)
				positions.add(position);
		}
		return positions;
	}

	private static BlockPos findUniqueSafeSurface(ServerLevel level, int requestedX, int requestedZ,
			BlockPos fallbackCenter, List<BlockPos> occupied) {
		for (int attempt = 0; attempt < 32; attempt++) {
			double angle = attempt * 2.399963229728653D;
			int radius = attempt / 4;
			BlockPos candidate = findSafeSurface(level,
					requestedX + Mth.floor(Math.cos(angle) * radius),
					requestedZ + Mth.floor(Math.sin(angle) * radius), fallbackCenter);
			if (candidate != null && !occupied.contains(candidate))
				return candidate;
		}
		return null;
	}

	private static BlockPos findSafeSurface(ServerLevel level, int requestedX, int requestedZ, BlockPos fallbackCenter) {
		for (int attempt = 0; attempt < 16; attempt++) {
			int ring = attempt / 4;
			int x = requestedX + switch (attempt & 3) {
				case 1 -> ring;
				case 2 -> -ring;
				default -> 0;
			};
			int z = requestedZ + switch (attempt & 3) {
				case 0 -> ring;
				case 3 -> -ring;
				default -> 0;
			};
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos position = new BlockPos(x, y, z);
			if (safeStandingPosition(level, position))
				return position;
		}
		return fallbackCenter != null && safeStandingPosition(level, fallbackCenter) ? fallbackCenter : null;
	}

	private static boolean safeStandingPosition(ServerLevel level, BlockPos position) {
		if (position.getY() <= level.getMinBuildHeight() + 1 || position.getY() >= level.getMaxBuildHeight() - 3)
			return false;
		BlockState floor = level.getBlockState(position.below());
		return floor.isFaceSturdy(level, position.below(), Direction.UP)
				&& !isUnsafeLandingBlock(floor)
				&& level.getFluidState(position).isEmpty()
				&& level.getFluidState(position.above()).isEmpty()
				&& level.getBlockState(position).getCollisionShape(level, position).isEmpty()
				&& level.getBlockState(position.above()).getCollisionShape(level, position.above()).isEmpty()
				&& level.getBlockState(position.above(2)).getCollisionShape(level, position.above(2)).isEmpty()
				&& !isUnsafeLandingBlock(level.getBlockState(position))
				&& !isUnsafeLandingBlock(level.getBlockState(position.above()));
	}

	private static BlockPos findArenaCenter(ServerLevel level, int targetX, int targetZ) {
		BlockPos best = null;
		int bestScore = Integer.MAX_VALUE;
		for (int offsetX = -SAFE_ARENA_SEARCH_RADIUS; offsetX <= SAFE_ARENA_SEARCH_RADIUS; offsetX += 16) {
			for (int offsetZ = -SAFE_ARENA_SEARCH_RADIUS; offsetZ <= SAFE_ARENA_SEARCH_RADIUS; offsetZ += 16) {
				BlockPos candidate = findSafeSurface(level, targetX + offsetX, targetZ + offsetZ, null);
				if (candidate == null)
					continue;
				int distancePenalty = (Math.abs(offsetX) + Math.abs(offsetZ)) / 16;
				int score = terrainScore(level, candidate) * 4 + distancePenalty;
				if (score < bestScore) {
					best = candidate;
					bestScore = score;
				}
			}
		}
		if (best == null)
			best = surface(level, targetX, targetZ);
		prepareLanding(level, best);
		return best;
	}

	private static int terrainScore(ServerLevel level, BlockPos center) {
		int minimum = Integer.MAX_VALUE;
		int maximum = Integer.MIN_VALUE;
		for (int[] sample : new int[][]{{0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}}) {
			int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					center.getX() + sample[0], center.getZ() + sample[1]);
			minimum = Math.min(minimum, height);
			maximum = Math.max(maximum, height);
		}
		return maximum - minimum;
	}

	private static BlockPos surface(ServerLevel level, int x, int z) {
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}

	private static void prepareLanding(ServerLevel level, BlockPos center) {
		Block support = territoryForDimension(level.dimension())
				.map(SnowRedGateArenaManager::paletteFor)
				.map(ArenaPalette::groundPrimary)
				.orElse(Blocks.STONE);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				BlockPos standing = center.offset(dx, 0, dz);
				BlockPos floor = standing.below();
				BlockState floorState = level.getBlockState(floor);
				if (!floorState.isFaceSturdy(level, floor, Direction.UP)
						|| isUnsafeLandingBlock(floorState)
						|| !level.getFluidState(floor).isEmpty()) {
					level.setBlockAndUpdate(floor, support.defaultBlockState());
				}
				for (int y = 0; y <= 3; y++) {
					BlockPos clear = standing.above(y);
					if (!level.getBlockState(clear).getCollisionShape(level, clear).isEmpty()
							|| !level.getFluidState(clear).isEmpty()
							|| isUnsafeLandingBlock(level.getBlockState(clear))) {
						level.setBlockAndUpdate(clear, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	private static boolean isUnsafeLandingBlock(BlockState state) {
		return state.is(Blocks.LAVA)
				|| state.is(Blocks.FIRE)
				|| state.is(Blocks.SOUL_FIRE)
				|| state.is(Blocks.MAGMA_BLOCK)
				|| state.is(Blocks.CACTUS)
				|| state.is(Blocks.SWEET_BERRY_BUSH)
				|| state.is(Blocks.POWDER_SNOW)
				|| state.is(Blocks.CAMPFIRE)
				|| state.is(Blocks.SOUL_CAMPFIRE);
	}

	/**
	 * Gives each run a recognizable territory-themed clearing without relying on
	 * reusable structure files. Deterministic palette seams and perimeter shards
	 * make the combat space readable from the spawn point.
	 */
	private static void decorateArena(ServerLevel level, BlockPos center, long seed, RiftTerritory territory) {
		ArenaPalette palette = paletteFor(territory);
		RandomSource random = RandomSource.create(seed ^ 0x51A7E0F12D34B678L);
		int clearingRadius = 27;
		for (int dx = -clearingRadius; dx <= clearingRadius; dx++) {
			for (int dz = -clearingRadius; dz <= clearingRadius; dz++) {
				int distanceSquared = dx * dx + dz * dz;
				if (distanceSquared > clearingRadius * clearingRadius)
					continue;
				BlockPos top = surface(level, center.getX() + dx, center.getZ() + dz);
				BlockPos ground = top.below();
				if (level.getBlockState(ground).is(Blocks.SNOW))
					ground = ground.below();
				if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
						|| level.getBlockState(ground).is(Blocks.BEDROCK))
					continue;
				double radial = Math.sqrt(distanceSquared) / clearingRadius;
				double iceChance = 0.12D + radial * 0.24D;
				double roll = random.nextDouble();
				if (roll < iceChance * 0.16D)
					level.setBlockAndUpdate(ground, palette.groundAccent().defaultBlockState());
				else if (roll < iceChance)
					level.setBlockAndUpdate(ground, palette.groundSecondary().defaultBlockState());
				else if (roll < 0.58D)
					level.setBlockAndUpdate(ground, palette.groundPrimary().defaultBlockState());
			}
		}

		for (int shard = 0; shard < 12; shard++) {
			double angle = Math.PI * 2.0D * shard / 12.0D + random.nextDouble() * 0.18D;
			int radius = 29 + random.nextInt(6);
			BlockPos base = surface(level,
					center.getX() + Mth.floor(Math.cos(angle) * radius),
					center.getZ() + Mth.floor(Math.sin(angle) * radius));
			int height = 3 + random.nextInt(5);
			for (int y = 0; y < height; y++) {
				BlockPos shardPos = base.above(y);
				if (!level.getBlockState(shardPos).isAir() && !level.getBlockState(shardPos).is(Blocks.SNOW))
					break;
				level.setBlockAndUpdate(shardPos,
						y == height - 1 || random.nextInt(5) == 0
								? palette.shardAccent().defaultBlockState() : palette.shardPrimary().defaultBlockState());
			}
		}
		if (palette.precipitation())
			level.setWeatherParameters(0, 12_000, true, false);
		else
			level.setWeatherParameters(12_000, 0, false, false);
	}

	private static ArenaPalette paletteFor(RiftTerritory territory) {
		return switch (territory) {
			case DESTRUCTION -> new ArenaPalette(Blocks.RED_TERRACOTTA, Blocks.NETHERRACK,
					Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, false);
			case FROST -> new ArenaPalette(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE,
					Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, true);
			case FANGS -> new ArenaPalette(Blocks.MOSS_BLOCK, Blocks.PODZOL,
					Blocks.COARSE_DIRT, Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK, true);
			case PLAGUES -> new ArenaPalette(Blocks.MUD, Blocks.GREEN_TERRACOTTA,
					Blocks.CLAY, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, true);
			case IRON_BODY -> new ArenaPalette(Blocks.TUFF, Blocks.ANDESITE,
					Blocks.GRAY_TERRACOTTA, Blocks.SMOOTH_BASALT, Blocks.POLISHED_ANDESITE, false);
			case WHITE_FLAMES -> new ArenaPalette(Blocks.SOUL_SOIL, Blocks.WHITE_TERRACOTTA,
					Blocks.SOUL_SAND, Blocks.CALCITE, Blocks.SMOOTH_QUARTZ, false);
			case TRANSFIGURATION -> new ArenaPalette(Blocks.WARPED_NYLIUM, Blocks.PURPLE_TERRACOTTA,
					Blocks.CYAN_TERRACOTTA, Blocks.WARPED_WART_BLOCK, Blocks.CRYING_OBSIDIAN, false);
			case BEGINNING -> new ArenaPalette(Blocks.SCULK, Blocks.DEEPSLATE,
					Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, true);
		};
	}

	private static void teleportEntrants(ServerLevel level, BlockPos center, List<ServerPlayer> entrants,
			DungeonInstanceSavedData.Instance instance) {
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(level);
		Optional<DungeonInstanceSavedData.Instance> current = registry.getInstance(instance.id())
				.filter(SnowRedGateArenaManager::isArenaInstance);
		if (current.isEmpty())
			return;
		DungeonInstanceSavedData.Instance activeInstance = current.get();
		boolean removedStaleBinding = false;
		for (int index = 0; index < entrants.size(); index++) {
			ServerPlayer entrant = entrants.get(index);
			if (entrant == null || entrant.hasDisconnected())
				continue;
			if (!activeInstance.participants().contains(entrant.getUUID())
					|| !activeInstance.id().toString().equals(entrant.getPersistentData()
							.getString(DungeonMobLevelAdapter.INSTANCE_TAG))) {
				removedStaleBinding |= activeInstance.removeParticipant(entrant.getUUID());
				continue;
			}
			double angle = Math.PI * 2.0D * index / Math.max(1, entrants.size());
			BlockPos arrival = surface(level, center.getX() + Mth.floor(Math.cos(angle) * 3.0D),
					center.getZ() + Mth.floor(Math.sin(angle) * 3.0D));
			prepareLanding(level, arrival);
			entrant.teleportTo(level, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D,
					entrant.getYRot(), entrant.getXRot());
			entrant.setNoGravity(false);
			entrant.fallDistance = 0.0F;
			DungeonEncounterRuntime.restoreCompletionFor(entrant, activeInstance);
		}
		if (removedStaleBinding && activeInstance.participants().isEmpty()) {
			if (activeInstance.completed()) {
				discardReturnPortals(level, activeInstance);
				registry.pruneCompletedEmptyInstances();
				recordArenaClosure(level.getServer(), activeInstance);
			} else {
				failAbandonedInstance(level.getServer(), registry, activeInstance);
			}
		}
	}

	private static void prepareEntrant(ServerLevel sourceLevel, Entity gate, ServerPlayer entrant,
			DungeonInstanceSavedData.Instance instance, RiftTerritory territory) {
		discardOwnedShadows(sourceLevel, gate, entrant);
		entrant.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = entrant.getX();
			capability.DunY = entrant.getY();
			capability.DunZ = entrant.getZ();
			capability.BossKilled = false;
			capability.dungeoning = true;
			capability.syncPlayerVariables(entrant);
		});
		entrant.getPersistentData().putString("dungeon_tag", gate.getStringUUID());
		entrant.getPersistentData().putBoolean(PROCEDURAL_DUNGEON_TAG, true);
		entrant.getPersistentData().putBoolean(PROCEDURAL_RED_TAG, true);
		entrant.getPersistentData().putString(TERRITORY_TAG, territory.id());
		entrant.getPersistentData().putString(DungeonMobLevelAdapter.INSTANCE_TAG, instance.id().toString());
		entrant.getPersistentData().remove(WAVE_NOTICE_TAG);
		UrgentQuestManager.markDungeonId(entrant, "red_gate");
		entrant.setNoGravity(true);
	}

	private static void discardOwnedShadows(ServerLevel level, Entity gate, ServerPlayer owner) {
		TagKey<net.minecraft.world.entity.EntityType<?>> shadows = TagKey.create(Registries.ENTITY_TYPE,
				new ResourceLocation("shadows"));
		for (Entity candidate : level.getEntitiesOfClass(Entity.class, gate.getBoundingBox().inflate(250.0D),
				entity -> entity.getType().is(shadows))) {
			if (candidate instanceof TamableAnimal tame && tame.isOwnedBy(owner))
				candidate.discard();
		}
	}

	private static List<ServerPlayer> nearbyPartyMembers(LevelAccessor world, Entity gate, ServerPlayer initiator) {
		String party = initiator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).party;
		if (party.isBlank())
			return List.of(initiator);
		List<ServerPlayer> result = new ArrayList<>();
		result.add(initiator);
		for (Entity candidate : new ArrayList<>(world.players())) {
			if (!(candidate instanceof ServerPlayer player) || player.getUUID().equals(initiator.getUUID()))
				continue;
			String candidateParty = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.orElse(new SololevelingModVariables.PlayerVariables()).party;
			if (party.equals(candidateParty) && player.distanceTo(gate) <= 10.0F)
				result.add(player);
		}
		return result;
	}

	private static List<ServerPlayer> sanitizeEntrants(ServerLevel sourceLevel, Entity gate,
			ServerPlayer initiator, List<ServerPlayer> requested) {
		List<ServerPlayer> result = new ArrayList<>();
		if (requested != null)
			for (ServerPlayer player : requested) {
				if (player != null && player.server == sourceLevel.getServer()
						&& player.level() == sourceLevel && player.distanceTo(gate) <= 10.0F
						&& !isDungeonBound(player)
						&& result.stream().noneMatch(existing -> existing.getUUID().equals(player.getUUID())))
					result.add(player);
			}
		if (result.stream().noneMatch(player -> player.getUUID().equals(initiator.getUUID())))
			result.add(0, initiator);
		return result;
	}

	private static boolean isDungeonBound(ServerPlayer player) {
		if (player == null)
			return false;
		if (!player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank())
			return true;
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.dungeoning)
				.orElse(false);
	}

	private static int targetCoordinate(Entity gate, String key, double fallback) {
		double value = gate.getPersistentData().contains(key, Tag.TAG_ANY_NUMERIC)
				? gate.getPersistentData().getDouble(key) : fallback;
		return Mth.clamp(Mth.floor(value), -29_999_000, 29_999_000);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ++tickCounter % 20 != 0)
			return;
		MinecraftServer server = event.getServer();
		for (DungeonInstanceSavedData.Instance instance : DungeonInstanceSavedData.get(server).listInstances()) {
			if (!isArenaInstance(instance))
				continue;
			ServerLevel level = server.getLevel(instance.dimension());
			if (level == null)
				continue;
			Optional<BlockPos> center = instance.playerStart();
			if (center.isEmpty())
				continue;
			enforceBoundary(level, instance, center.get());
			leashEncounterMobs(level, instance, center.get());
			if (instance.completed()) {
				ensureReturnPortal(level, instance);
				updatePlayerFeedback(level, instance, null);
			} else {
				DungeonInstanceSavedData.EncounterState activeWave = instance.encounters().stream()
						.filter(encounter -> encounter.activated() && !encounter.completed())
						.min(Comparator.comparingInt(DungeonInstanceSavedData.EncounterState::sequenceOrder))
						.orElse(null);
				if (activeWave != null)
					updatePlayerFeedback(level, instance, activeWave);
			}
		}
	}

	/** Restores a participant if the server stopped or they disconnected during entry. */
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(player.server);
		String instanceText = player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		Optional<UUID> declaredId = parseUuid(instanceText);
		Optional<DungeonInstanceSavedData.Instance> candidate = declaredId.flatMap(registry::getInstance);
		List<DungeonInstanceSavedData.Instance> memberships = registry.listInstances().stream()
				.filter(SnowRedGateArenaManager::isArenaInstance)
				.filter(instance -> instance.participants().contains(player.getUUID()))
				.toList();

		// A valid non-red instance tag belongs to the generic dungeon runtime. Clean
		// any stale red-gate memberships without taking ownership of that binding.
		if (candidate.isPresent() && !isArenaInstance(candidate.get())) {
			for (DungeonInstanceSavedData.Instance stale : memberships)
				detachArenaParticipant(player.server, registry, stale, player.getUUID());
			return;
		}

		DungeonInstanceSavedData.Instance selected = candidate
				.filter(SnowRedGateArenaManager::isArenaInstance)
				.filter(instance -> instance.participants().contains(player.getUUID()))
				.orElse(null);
		if (selected == null && memberships.size() == 1)
			selected = memberships.get(0);
		if (selected == null && memberships.size() > 1) {
			List<DungeonInstanceSavedData.Instance> dimensionMatches = memberships.stream()
					.filter(instance -> instance.dimension().equals(player.level().dimension()))
					.toList();
			if (dimensionMatches.size() == 1)
				selected = dimensionMatches.get(0);
		}

		if (selected != null) {
			DungeonInstanceSavedData.Instance authoritative = selected;
			candidate.filter(SnowRedGateArenaManager::isArenaInstance)
					.filter(instance -> instance != authoritative && instance.participants().isEmpty())
					.ifPresent(instance -> detachArenaParticipant(
							player.server, registry, instance, player.getUUID()));
			for (DungeonInstanceSavedData.Instance stale : memberships)
				if (stale != authoritative)
					detachArenaParticipant(player.server, registry, stale, player.getUUID());
			restoreArenaBinding(player, authoritative);
			resumeArenaParticipant(player, registry, authoritative);
			return;
		}

		if (memberships.size() > 1) {
			for (DungeonInstanceSavedData.Instance stale : memberships)
				detachArenaParticipant(player.server, registry, stale, player.getUUID());
		}
		candidate.filter(SnowRedGateArenaManager::isArenaInstance)
				.filter(instance -> !instance.participants().contains(player.getUUID()))
				.ifPresent(instance -> detachArenaParticipant(
						player.server, registry, instance, player.getUUID()));
		boolean arenaBinding = candidate.map(SnowRedGateArenaManager::isArenaInstance).orElse(false)
				|| needsArenaRecovery(player);
		if (!arenaBinding)
			return;
		UUID missingGateId = declaredId.orElseGet(() -> parseUuid(
				player.getPersistentData().getString("dungeon_tag")).orElse(null));
		if (missingGateId != null && registry.getInstance(missingGateId).isEmpty())
			recordArenaClosure(player.server, missingGateId);
		else if (missingGateId == null)
			refreshArenaFlag(player.server);
		recoverEntrant(player, instanceText.isBlank()
				? "Your red-gate encounter could not be restored, so you were returned safely."
				: "That red-gate encounter no longer exists, so you were returned safely.");
	}

	private static Optional<UUID> parseUuid(String value) {
		if (value == null || value.isBlank())
			return Optional.empty();
		try {
			return Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	private static void restoreArenaBinding(ServerPlayer player,
			DungeonInstanceSavedData.Instance instance) {
		player.getPersistentData().putString(DungeonMobLevelAdapter.INSTANCE_TAG, instance.id().toString());
		player.getPersistentData().putString("dungeon_tag", instance.id().toString());
		player.getPersistentData().putBoolean(PROCEDURAL_DUNGEON_TAG, true);
		player.getPersistentData().putBoolean(PROCEDURAL_RED_TAG, true);
		territoryForDimension(instance.dimension()).ifPresent(territory ->
				player.getPersistentData().putString(TERRITORY_TAG, territory.id()));
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dungeoning = true;
			capability.syncPlayerVariables(player);
		});
		UrgentQuestManager.markDungeonId(player, "red_gate");
	}

	private static void resumeArenaParticipant(ServerPlayer player, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance) {
		ServerLevel level = player.server.getLevel(instance.dimension());
		BlockPos center = instance.playerStart().orElse(null);
		if (level == null || center == null) {
			failUnavailableInstance(player.server, registry, instance,
					"That Monarch territory is unavailable, so you were returned safely.");
			return;
		}
		if (player.level() == level) {
			player.setNoGravity(false);
			player.fallDistance = 0.0F;
			DungeonEncounterRuntime.restoreCompletionFor(player, instance);
			return;
		}
		player.setNoGravity(true);
		SololevelingMod.queueServerWork(1, () -> {
			if (!player.hasDisconnected() && instance.id().toString().equals(
					player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				teleportEntrants(level, center, List.of(player), instance);
		});
	}

	/** A red gate is a no-respawn encounter: death ejects that participant. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!event.isWasDeath() || !(event.getOriginal() instanceof ServerPlayer original)
				|| !(event.getEntity() instanceof ServerPlayer clone))
			return;
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(original.serverLevel());
		List<DungeonInstanceSavedData.Instance> memberships = registry.listInstances().stream()
				.filter(SnowRedGateArenaManager::isArenaInstance)
				.filter(instance -> instance.participants().contains(original.getUUID()))
				.toList();
		if (memberships.isEmpty())
			return;
		for (DungeonInstanceSavedData.Instance instance : memberships)
			detachArenaParticipant(original.getServer(), registry, instance, original.getUUID());
		clearEntrantState(clone);
		clone.sendSystemMessage(Component.literal("You were expelled from the red gate.")
				.withStyle(ChatFormatting.DARK_RED));
	}

	private static void detachArenaParticipant(MinecraftServer server, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance, UUID playerId) {
		instance.removeParticipant(playerId);
		if (!instance.participants().isEmpty())
			return;
		if (instance.completed()) {
			ServerLevel level = server.getLevel(instance.dimension());
			if (level != null)
				discardReturnPortals(level, instance);
			registry.pruneCompletedEmptyInstances();
			recordArenaClosure(server, instance);
		} else {
			failAbandonedInstance(server, registry, instance);
		}
	}

	private static boolean needsArenaRecovery(ServerPlayer player) {
		boolean inTerritory = territoryForDimension(player.level().dimension()).isPresent();
		boolean dungeonCapability = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.dungeoning)
				.orElse(false);
		return player.getPersistentData().getBoolean(PROCEDURAL_RED_TAG)
				|| RiftTerritory.fromName(player.getPersistentData().getString(TERRITORY_TAG)) != null
				|| (inTerritory && (dungeonCapability
						|| !player.getPersistentData().getString("dungeon_tag").isBlank()));
	}

	private static void recoverEntrant(ServerPlayer player, String message) {
		boolean stranded = territoryForDimension(player.level().dimension()).isPresent();
		ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
		SololevelingModVariables.PlayerVariables variables = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
		boolean hasSavedReturn = player.getPersistentData().getBoolean(PROCEDURAL_RED_TAG)
				|| !player.getPersistentData().getString("dungeon_tag").isBlank()
				|| (variables != null && variables.dungeoning);
		double returnX;
		double returnY;
		double returnZ;
		if (variables != null && hasSavedReturn) {
			returnX = variables.DunX + 3.0D;
			returnY = variables.DunY;
			returnZ = variables.DunZ;
		} else if (overworld != null) {
			BlockPos spawn = overworld.getSharedSpawnPos();
			returnX = spawn.getX() + 0.5D;
			returnY = spawn.getY() + 1.0D;
			returnZ = spawn.getZ() + 0.5D;
		} else {
			returnX = player.getX();
			returnY = player.getY();
			returnZ = player.getZ();
		}
		clearEntrantState(player);
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
		if (!stranded || overworld == null)
			return;
		SololevelingMod.queueServerWork(1, () -> {
			if (player.hasDisconnected() || territoryForDimension(player.level().dimension()).isEmpty())
				return;
			player.teleportTo(overworld, returnX, returnY, returnZ, player.getYRot(), player.getXRot());
			player.setNoGravity(false);
			player.fallDistance = 0.0F;
		});
	}

	private static void clearEntrantState(ServerPlayer player) {
		player.getPersistentData().remove(DungeonMobLevelAdapter.INSTANCE_TAG);
		player.getPersistentData().remove("dungeon_tag");
		player.getPersistentData().remove(WAVE_NOTICE_TAG);
		player.getPersistentData().remove(BOUNDARY_NOTICE_TAG);
		player.getPersistentData().putBoolean(PROCEDURAL_DUNGEON_TAG, false);
		player.getPersistentData().putBoolean(PROCEDURAL_RED_TAG, false);
		player.getPersistentData().remove(TERRITORY_TAG);
		player.setNoGravity(false);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.BossKilled = false;
			capability.dungeoning = false;
			capability.syncPlayerVariables(player);
		});
	}

	private static void failAbandonedInstance(MinecraftServer server, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance) {
		discardTrackedMobs(server, instance);
		registry.remove(instance.id());
		recordArenaClosure(server, instance);
		SololevelingMod.LOGGER.info("Failed empty Monarch red-gate arena {} after its last participant died",
				instance.id());
	}

	private static void failUnavailableInstance(MinecraftServer server, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance, String message) {
		discardTrackedMobs(server, instance);
		registry.remove(instance.id());
		recordArenaClosure(server, instance);
		for (ServerPlayer participant : server.getPlayerList().getPlayers()) {
			if (instance.participants().contains(participant.getUUID()))
				recoverEntrant(participant, message);
		}
		SololevelingMod.LOGGER.warn("Closed unavailable Monarch red-gate arena {} in {}",
				instance.id(), instance.dimension().location());
	}

	private static void discardTrackedMobs(MinecraftServer server,
			DungeonInstanceSavedData.Instance instance) {
		ServerLevel level = server.getLevel(instance.dimension());
		if (level == null)
			return;
		for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters())
			for (UUID mobId : encounter.trackedMobs()) {
				Entity mob = level.getEntity(mobId);
				if (mob != null)
					mob.discard();
			}
	}

	private static void recordArenaClosure(MinecraftServer server, DungeonInstanceSavedData.Instance instance) {
		recordArenaClosure(server, instance.id());
	}

	private static void recordArenaClosure(MinecraftServer server, UUID gateId) {
		String gateToken = gateId + ",";
		SololevelingModVariables.MapVariables variables = SololevelingModVariables.MapVariables.get(server.overworld());
		if (!variables.GatesCleared.contains(gateToken))
			variables.GatesCleared += gateToken;
		variables.RedGate = hasActiveArena(server);
		variables.syncData(server.overworld());
	}

	private static void refreshArenaFlag(MinecraftServer server) {
		SololevelingModVariables.MapVariables variables = SololevelingModVariables.MapVariables.get(server.overworld());
		variables.RedGate = hasActiveArena(server);
		variables.syncData(server.overworld());
	}

	private static void discardReturnPortals(ServerLevel level, DungeonInstanceSavedData.Instance instance) {
		BlockPos exit = instance.exit().orElseGet(() -> instance.playerStart().orElse(BlockPos.ZERO));
		AABB search = AABB.ofSize(Vec3.atCenterOf(exit), 16.0D, 10.0D, 16.0D);
		for (Entity portal : level.getEntitiesOfClass(Entity.class, search, entity ->
				entity.getType() == SololevelingModEntities.PORTAL_12.get()
						&& instance.id().toString().equals(entity.getPersistentData()
								.getString(DungeonMobLevelAdapter.INSTANCE_TAG))))
			portal.discard();
	}

	private static void enforceBoundary(ServerLevel level, DungeonInstanceSavedData.Instance instance, BlockPos center) {
		for (ServerPlayer player : level.players()) {
			if (!instance.participants().contains(player.getUUID())
					|| !instance.id().toString().equals(player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				continue;
			double dx = player.getX() - (center.getX() + 0.5D);
			double dz = player.getZ() - (center.getZ() + 0.5D);
			if (dx * dx + dz * dz <= (double) ARENA_RADIUS * ARENA_RADIUS)
				continue;
			player.teleportTo(level, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D,
					player.getYRot(), player.getXRot());
			long lastNotice = player.getPersistentData().getLong(BOUNDARY_NOTICE_TAG);
			if (level.getGameTime() - lastNotice >= 100L) {
				player.displayClientMessage(Component.literal("The Monarch territory's barrier forces you back.")
						.withStyle(ChatFormatting.AQUA), true);
				player.getPersistentData().putLong(BOUNDARY_NOTICE_TAG, level.getGameTime());
			}
		}
	}

	private static void leashEncounterMobs(ServerLevel level, DungeonInstanceSavedData.Instance instance,
			BlockPos center) {
		for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters()) {
			if (!encounter.activated() || encounter.completed())
				continue;
			for (UUID mobId : encounter.trackedMobs()) {
				Entity mob = level.getEntity(mobId);
				if (mob == null)
					continue;
				double dx = mob.getX() - (center.getX() + 0.5D);
				double dz = mob.getZ() - (center.getZ() + 0.5D);
				if (dx * dx + dz * dz <= (double) MOB_LEASH_RADIUS * MOB_LEASH_RADIUS)
					continue;
				BlockPos returnPosition = findSafeSurface(level, center.getX() + level.random.nextInt(17) - 8,
						center.getZ() + level.random.nextInt(17) - 8, center);
				if (returnPosition != null)
					mob.teleportTo(returnPosition.getX() + 0.5D, returnPosition.getY(), returnPosition.getZ() + 0.5D);
			}
		}
	}

	private static void ensureReturnPortal(ServerLevel level, DungeonInstanceSavedData.Instance instance) {
		BlockPos exit = instance.exit().orElseGet(() -> instance.playerStart().orElse(BlockPos.ZERO));
		AABB search = AABB.ofSize(Vec3.atCenterOf(exit), 16.0D, 10.0D, 16.0D);
		boolean exists = !level.getEntitiesOfClass(Entity.class, search, entity ->
				entity.getType() == SololevelingModEntities.PORTAL_12.get()
						&& instance.id().toString().equals(entity.getPersistentData()
								.getString(DungeonMobLevelAdapter.INSTANCE_TAG))).isEmpty();
		if (exists)
			return;
		prepareLanding(level, exit);
		Entity portal = DungeonReturnPortalSpawner.spawn(level, exit, instance.exitFacing().orElse(Direction.SOUTH),
				instance.id(), instance.id().toString());
		if (portal != null)
			SololevelingMod.LOGGER.info("Opened return portal for completed Monarch red-gate arena {}", instance.id());
	}

	private static void updatePlayerFeedback(ServerLevel level, DungeonInstanceSavedData.Instance instance,
			DungeonInstanceSavedData.EncounterState wave) {
		String state = wave == null ? "complete" : wave.key();
		String receipt = instance.id() + ":" + state;
		for (ServerPlayer player : level.players()) {
			if (!instance.participants().contains(player.getUUID())
					|| !instance.id().toString().equals(player.getPersistentData()
							.getString(DungeonMobLevelAdapter.INSTANCE_TAG))
					|| receipt.equals(player.getPersistentData().getString(WAVE_NOTICE_TAG)))
				continue;
			player.getPersistentData().putString(WAVE_NOTICE_TAG, receipt);
			if (wave == null) {
				player.sendSystemMessage(Component.literal("Baruka has fallen. The red gate's exit is open.")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				player.displayClientMessage(Component.literal("DUNGEON CLEARED — EXIT UNSEALED")
						.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
			} else {
				Component announcement = Component.literal(waveTitle(wave))
						.withStyle(wave.boss() ? ChatFormatting.DARK_RED : ChatFormatting.AQUA,
								ChatFormatting.BOLD);
				player.sendSystemMessage(announcement);
				player.displayClientMessage(announcement, true);
			}
		}
	}

	private static String waveTitle(DungeonInstanceSavedData.EncounterState wave) {
		String label = switch (wave.key()) {
			case "ice_bear_rush" -> "ICE BEAR RUSH";
			case "ice_elf_vanguard" -> "ICE ELF VANGUARD";
			case "ice_elf_ambush" -> "ICE ELF AMBUSH";
			case "ice_elf_encirclement" -> "ICE ELF ENCIRCLEMENT";
			case "ice_elf_guard" -> "WARLORD'S GUARD";
			case "baruka" -> "BARUKA";
			default -> wave.key().toUpperCase();
		};
		if (wave.boss())
			return "FINAL WAVE - " + label;
		return "WAVE " + romanNumeral(wave.sequenceOrder() + 1) + " - " + label;
	}

	private static String romanNumeral(int number) {
		return switch (number) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			default -> Integer.toString(number);
		};
	}

	private record ArenaPalette(Block groundPrimary, Block groundSecondary, Block groundAccent,
			Block shardPrimary, Block shardAccent, boolean precipitation) {
	}

	private record WaveSpec(String id, ResourceLocation pool, int count, boolean boss, boolean elite,
			int delayTicks) {
	}
}
