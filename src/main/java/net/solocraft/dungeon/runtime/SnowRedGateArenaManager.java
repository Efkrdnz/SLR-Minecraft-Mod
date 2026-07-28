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
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	/** Shared realm used by every Monarch Red Gate territory. */
	public static final ResourceKey<Level> SNOW_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation("sololeveling", "dungeon_dimension_snow"));
	public static final String TERRITORY_TAG = "slr_red_gate_territory";
	/** Removed dimension keys are retained only for active-save recovery. */
	private static final Map<RiftTerritory, ResourceKey<Level>> LEGACY_TERRITORY_DIMENSIONS =
			createLegacyTerritoryDimensions();

	private static final ResourceLocation BEAR_POOL = new ResourceLocation("sololeveling", "red_gate_ice_bears");
	private static final ResourceLocation ELF_POOL = new ResourceLocation("sololeveling", "red_gate_ice_elves");
	private static final ResourceLocation BARUKA_POOL = new ResourceLocation("sololeveling", "red_gate_baruka");
	private static final List<ResourceLocation> REQUIRED_POOLS = List.of(BEAR_POOL, ELF_POOL, BARUKA_POOL);

	private static final String SEQUENCE_KEY = "red_gate_monarch";
	private static final String GATE_INSTANCE_TAG = "slr_red_gate_wave_instance";
	private static final String PROCEDURAL_DUNGEON_TAG = "slr_procedural_dungeon";
	private static final String PROCEDURAL_RED_TAG = "slr_procedural_red_gate";
	private static final String BOUNDARY_NOTICE_TAG = "slr_red_gate_boundary_notice";
	private static final String REVEAL_READY_TIME_TAG = "slr_red_gate_reveal_ready_time";
	private static final int FIRST_WAVE_DELAY = 100;
	private static final int INTERMISSION_TICKS = 160;
	private static final int BOSS_INTRO_TICKS = 200;
	private static final int ARENA_RADIUS = 80;
	private static final int MOB_LEASH_RADIUS = 76;
	private static final int MAX_ACTIVE_WAVE_MOBS = 18;
	private static final int LANDSCAPE_RADIUS = 112;
	private static final int LANDSCAPE_MIN_OFFSET = -4;
	private static final int LANDSCAPE_MAX_OFFSET = 16;
	private static final int LANDSCAPE_CLEAR_HEIGHT = 26;
	private static final int SAFE_CLEARING_RADIUS = 11;
	private static final int SCENERY_COUNT = 42;
	private static final int SHARD_COUNT = 18;
	/** Minimum time the synced red texture is visible before any entrant teleports. */
	private static final int MIN_GATE_REVEAL_TICKS = 10;
	/** Global horizontal-column budget shared by every arena being prepared. */
	private static final int PREPARATION_WORK_BUDGET = 256;
	private static final int MINIMUM_CELL_SEPARATION = ARENA_RADIUS * 2 + 64;
	private static final Map<UUID, ArenaPreparationJob> PREPARATIONS = new LinkedHashMap<>();
	private static int tickCounter;

	private SnowRedGateArenaManager() {
	}

	private static Map<RiftTerritory, ResourceKey<Level>> createLegacyTerritoryDimensions() {
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

	private static Optional<RiftTerritory> legacyTerritoryForDimension(ResourceKey<Level> dimension) {
		return LEGACY_TERRITORY_DIMENSIONS.entrySet().stream()
				.filter(entry -> entry.getValue().equals(dimension))
				.map(Map.Entry::getKey)
				.findFirst();
	}

	private static Optional<RiftTerritory> territoryForInstance(DungeonInstanceSavedData.Instance instance) {
		if (instance == null)
			return Optional.empty();
		Optional<RiftTerritory> legacy = legacyTerritoryForDimension(instance.dimension());
		if (legacy.isPresent())
			return legacy;
		if (!SNOW_DIMENSION.equals(instance.dimension()))
			return Optional.empty();
		Optional<RiftTerritory> encoded = instance.playerStart()
				.flatMap(RedGateRealmLayout::cellAt)
				.map(RedGateRealmLayout.Cell::territory);
		// The original snow arena predates encoded cells and was always Frost.
		return encoded.isPresent() ? encoded : Optional.of(RiftTerritory.FROST);
	}

	private static boolean isArenaDimension(ResourceKey<Level> dimension) {
		return SNOW_DIMENSION.equals(dimension) || legacyTerritoryForDimension(dimension).isPresent();
	}

	public static boolean isArenaInstance(DungeonInstanceSavedData.Instance instance) {
		return instance != null && (ARENA_ID.equals(instance.dungeonId())
				|| LEGACY_ARENA_ID.equals(instance.dungeonId()))
				&& isArenaDimension(instance.dimension());
	}

	/** True when an entity belongs to a live Monarch Red Gate arena instance. */
	public static boolean isArenaMob(Entity entity) {
		if (entity == null || entity.level().isClientSide()
				|| !(entity.level() instanceof ServerLevel level))
			return false;
		String instanceText = entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		try {
			return !instanceText.isBlank() && DungeonInstanceSavedData.get(level)
					.getInstance(UUID.fromString(instanceText))
					.filter(SnowRedGateArenaManager::isArenaInstance)
					.isPresent();
		} catch (IllegalArgumentException ignored) {
			return false;
		}
	}

	/**
	 * Resolves the exact territory of a live arena entity from its persisted
	 * instance binding. Dimension identity alone is intentionally insufficient
	 * now that every territory shares the snow realm.
	 */
	public static Optional<RiftTerritory> arenaTerritory(Entity entity) {
		if (entity == null || entity.level().isClientSide()
				|| !(entity.level() instanceof ServerLevel level))
			return Optional.empty();
		String instanceText = entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		try {
			return instanceText.isBlank() ? Optional.empty() : DungeonInstanceSavedData.get(level)
					.getInstance(UUID.fromString(instanceText))
					.filter(SnowRedGateArenaManager::isArenaInstance)
					.flatMap(SnowRedGateArenaManager::territoryForInstance);
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	/** True while at least one scoped Monarch red-gate run still exists. */
	public static boolean hasActiveArena(MinecraftServer server) {
		return server != null && DungeonInstanceSavedData.get(server).listInstances().stream()
				.anyMatch(SnowRedGateArenaManager::isArenaInstance);
	}

	/** Reconciles persistent gate state after a player leaves through the clear portal. */
	public static void onParticipantExited(MinecraftServer server,
			DungeonInstanceSavedData.Instance instance) {
		if (server == null || !isArenaInstance(instance) || !instance.participants().isEmpty())
			return;
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(server);
		if (instance.completed()) {
			ServerLevel level = server.getLevel(instance.dimension());
			if (level != null)
				discardReturnPortals(level, instance);
			registry.pruneCompletedEmptyInstances();
			recordArenaClosure(server, instance);
		} else {
			// A self-service emergency exit is an abandonment, never a clear.
			// Tear down the now-empty encounter so it cannot leave an orphaned
			// instance, tracked mobs, or a permanently active Red Gate flag.
			failAbandonedInstance(server, registry, instance);
		}
	}

	/** Handles the original dedicated red-gate entity. */
	public static boolean enterLegacy(LevelAccessor world, RedGateEntity gate, ServerPlayer initiator) {
		if (world == null || gate == null || initiator == null)
			return false;
		if (gate.getEntityData().get(RedGateEntity.DATA_usedbefore))
			return true;
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
		return SNOW_DIMENSION;
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
		MinecraftServer server = sourceLevel.getServer();
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(server);
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

		ResourceKey<Level> destination = dimensionFor(territory);
		ServerLevel arenaLevel = server.getLevel(destination);
		if (arenaLevel == null) {
			openingFailure(world, gate, initiator,
					"The " + territory.displayName() + " Monarch territory is unavailable.");
			return true;
		}

		UUID instanceId = gate.getUUID();
		registry.pruneCompletedEmptyInstances();
		if (registry.getInstance(instanceId).isPresent()) {
			initiator.sendSystemMessage(Component.literal("This red gate already has an active encounter.")
					.withStyle(ChatFormatting.RED));
			return true;
		}

		long seed = gate.getUUID().getMostSignificantBits() ^ gate.getUUID().getLeastSignificantBits()
				^ arenaLevel.getSeed();
		int effectiveLevel = effectiveLevelFor(territory, initiator, entrants);
		BlockPos center = allocateArenaCenter(arenaLevel, registry, territory).orElse(null);
		if (center == null) {
			openingFailure(world, gate, initiator,
					"No isolated Red Gate arena cell is currently available.");
			return true;
		}

		DungeonInstanceSavedData.MutationResult<DungeonInstanceSavedData.Instance> created = registry.create(
				instanceId, ARENA_ID, destination, seed, effectiveLevel, arenaLevel.getGameTime());
		if (!created.success() || created.value() == null) {
			openingFailure(world, gate, initiator,
					"Could not create the red gate encounter: " + created.message());
			return true;
		}

		DungeonInstanceSavedData.Instance instance = created.value();
		// Only a procedural gate that rolled red shows its return portal from the
		// beginning. DungeonDimensionPlayerLeavesDimensionProcedure keeps that
		// visible portal locked until this exact instance is complete. The original
		// dedicated RedGateEntity retains its legacy boss-created exit.
		instance.setReturnPortalDeferred(markLegacyUsed);
		for (ServerPlayer entrant : entrants) {
			if (!instance.addParticipant(entrant.getUUID())) {
				registry.remove(instanceId);
				openingFailure(world, gate, initiator, "The red gate party is too large for one encounter.");
				return true;
			}
		}
		// Persist the allocated cell before any world work begins. This reserves the
		// collision-safe slot across restarts; an instance with a center and no
		// encounters is the durable "preparing" state.
		if (!instance.setPlayerStart(center)) {
			registry.remove(instanceId);
			openingFailure(world, gate, initiator,
					"Could not reserve the red gate arena cell.");
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

		enqueuePreparation(instance, center, territory);
		SololevelingMod.LOGGER.info("Queued {} Monarch red-gate arena {} at {} for {} participant(s), effective level {}",
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
			for (int scanY = level.getMaxBuildHeight() - 3;
					scanY > level.getMinBuildHeight() + 1; scanY--) {
				BlockPos scanned = new BlockPos(x, scanY, z);
				if (safeStandingPosition(level, scanned))
					return scanned;
			}
		}
		return fallbackCenter != null && safeStandingPosition(level, fallbackCenter) ? fallbackCenter : null;
	}

	private static boolean safeStandingPosition(ServerLevel level, BlockPos position) {
		if (position.getY() <= level.getMinBuildHeight() + 1 || position.getY() >= level.getMaxBuildHeight() - 3)
			return false;
		BlockState floor = level.getBlockState(position.below());
		return floor.isFaceSturdy(level, position.below(), Direction.UP)
				&& !floor.is(Blocks.BEDROCK)
				&& !isUnsafeLandingBlock(floor)
				&& level.getFluidState(position).isEmpty()
				&& level.getFluidState(position.above()).isEmpty()
				&& level.getBlockState(position).getCollisionShape(level, position).isEmpty()
				&& level.getBlockState(position.above()).getCollisionShape(level, position.above()).isEmpty()
				&& level.getBlockState(position.above(2)).getCollisionShape(level, position.above(2)).isEmpty()
				&& !isUnsafeLandingBlock(level.getBlockState(position))
				&& !isUnsafeLandingBlock(level.getBlockState(position.above()));
	}

	private static Optional<BlockPos> allocateArenaCenter(ServerLevel level,
			DungeonInstanceSavedData registry, RiftTerritory territory) {
		Set<Integer> occupiedSlots = new HashSet<>();
		List<BlockPos> occupiedCenters = new ArrayList<>();
		for (DungeonInstanceSavedData.Instance existing : registry.listInstances()) {
			if (!SNOW_DIMENSION.equals(existing.dimension()))
				continue;
			BlockPos existingCenter = existing.playerStart().orElse(null);
			if (existingCenter == null)
				continue;
			occupiedCenters.add(existingCenter);
			RedGateRealmLayout.cellAt(existingCenter)
					.filter(cell -> cell.territory() == territory)
					.ifPresent(cell -> occupiedSlots.add(cell.slot()));
		}

		long minimumDistanceSquared = (long) MINIMUM_CELL_SEPARATION * MINIMUM_CELL_SEPARATION;
		for (int slot = 0; slot < RedGateRealmLayout.MAX_SLOTS_PER_TERRITORY; slot++) {
			if (occupiedSlots.contains(slot))
				continue;
			BlockPos horizontal = RedGateRealmLayout.center(territory, slot, 0);
			boolean overlaps = occupiedCenters.stream().anyMatch(existing -> {
				long dx = (long) existing.getX() - horizontal.getX();
				long dz = (long) existing.getZ() - horizontal.getZ();
				return dx * dx + dz * dz < minimumDistanceSquared;
			});
			if (overlaps)
				continue;

			int standingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					horizontal.getX(), horizontal.getZ());
			if (standingY <= level.getMinBuildHeight() + 1)
				standingY = Mth.clamp(64, level.getMinBuildHeight() + 8, level.getMaxBuildHeight() - 16);
			return Optional.of(new BlockPos(horizontal.getX(), standingY, horizontal.getZ()));
		}
		return Optional.empty();
	}

	private static BlockPos surface(ServerLevel level, int x, int z) {
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
	}

	private static void prepareLanding(ServerLevel level, BlockPos center, RiftTerritory territory) {
		Block support = paletteFor(territory).groundPrimary();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				BlockPos standing = center.offset(dx, 0, dz);
				BlockPos floor = standing.below();
				BlockState floorState = level.getBlockState(floor);
				if (!floorState.isFaceSturdy(level, floor, Direction.UP)
						|| isUnsafeLandingBlock(floorState)
						|| !level.getFluidState(floor).isEmpty()) {
					setBlockIfChanged(level, floor, support.defaultBlockState());
				}
				for (int y = 0; y <= 3; y++) {
					BlockPos clear = standing.above(y);
					BlockState clearState = level.getBlockState(clear);
					if (!clearState.getCollisionShape(level, clear).isEmpty()
							|| !level.getFluidState(clear).isEmpty()
							|| isUnsafeLandingBlock(clearState)) {
						setBlockIfChanged(level, clear, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
	}

	private static boolean setBlockIfChanged(ServerLevel level, BlockPos position, BlockState state) {
		return !state.equals(level.getBlockState(position)) && level.setBlock(position, state, 2);
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

	private static boolean isPreparingInstance(DungeonInstanceSavedData.Instance instance) {
		return isArenaInstance(instance) && !instance.completed()
				&& instance.playerStart().isPresent() && instance.encounters().isEmpty();
	}

	private static void enqueuePreparation(DungeonInstanceSavedData.Instance instance,
			BlockPos center, RiftTerritory territory) {
		PREPARATIONS.compute(instance.id(), (ignored, existing) ->
				existing != null && existing.matches(center, territory, instance.seed())
						? existing : new ArenaPreparationJob(instance.id(), center, territory, instance.seed()));
	}

	/** Reconstructs a lost in-memory job from the durable preparing state. */
	private static void ensurePreparationQueued(DungeonInstanceSavedData.Instance instance) {
		if (!isPreparingInstance(instance))
			return;
		BlockPos center = instance.playerStart().orElse(null);
		RiftTerritory territory = territoryForInstance(instance).orElse(null);
		if (center != null && territory != null)
			enqueuePreparation(instance, center, territory);
	}

	private static List<ServerPlayer> onlineBoundParticipants(MinecraftServer server,
			DungeonInstanceSavedData.Instance instance) {
		String instanceId = instance.id().toString();
		Set<UUID> participants = instance.participants();
		return server.getPlayerList().getPlayers().stream()
				.filter(player -> participants.contains(player.getUUID()))
				.filter(player -> instanceId.equals(player.getPersistentData()
						.getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				.toList();
	}

	private static void processPreparationJobs(MinecraftServer server) {
		if (PREPARATIONS.isEmpty())
			return;
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(server);
		List<ArenaPreparationJob> runnable = new ArrayList<>();
		for (ArenaPreparationJob job : new ArrayList<>(PREPARATIONS.values())) {
			DungeonInstanceSavedData.Instance instance = registry.getInstance(job.instanceId()).orElse(null);
			if (instance == null || !isPreparingInstance(instance)) {
				PREPARATIONS.remove(job.instanceId());
				continue;
			}
			if (!job.matches(instance.playerStart().orElse(null),
					territoryForInstance(instance).orElse(null), instance.seed())) {
				failPreparation(server, registry, instance,
						"the persisted arena allocation changed while it was preparing", null);
				continue;
			}
			if (instance.participants().isEmpty()) {
				failAbandonedInstance(server, registry, instance);
				continue;
			}
			if (server.getLevel(instance.dimension()) == null) {
				failPreparation(server, registry, instance,
						"the shared Red Gate realm is unavailable", null);
				continue;
			}
			// No bound player means no reason to touch or retain the arena chunks.
			if (!onlineBoundParticipants(server, instance).isEmpty())
				runnable.add(job);
		}
		if (runnable.isEmpty())
			return;

		int remaining = PREPARATION_WORK_BUDGET;
		for (int index = 0; index < runnable.size() && remaining > 0; index++) {
			ArenaPreparationJob job = runnable.get(index);
			int jobsLeft = runnable.size() - index;
			int allowance = Math.max(1, remaining / jobsLeft);
			int used = advancePreparation(server, registry, job, allowance);
			remaining -= Math.min(remaining, used);
		}
	}

	private static int advancePreparation(MinecraftServer server, DungeonInstanceSavedData registry,
			ArenaPreparationJob job, int allowance) {
		DungeonInstanceSavedData.Instance instance = registry.getInstance(job.instanceId())
				.filter(SnowRedGateArenaManager::isPreparingInstance)
				.orElse(null);
		if (instance == null) {
			PREPARATIONS.remove(job.instanceId());
			return 0;
		}
		ServerLevel level = server.getLevel(instance.dimension());
		if (level == null) {
			failPreparation(server, registry, instance,
					"the shared Red Gate realm became unavailable", null);
			return 0;
		}
		try {
			int used = job.advance(level, allowance);
			long revealTicks = Math.max(0L, level.getGameTime() - instance.createdGameTime());
			boolean entrantsSawRedGate = onlineBoundParticipants(server, instance).stream()
					.allMatch(player -> revealDelayRemaining(server, player) == 0);
			if (job.finished() && revealTicks >= MIN_GATE_REVEAL_TICKS
					&& entrantsSawRedGate)
				finishPreparation(server, registry, level, instance, job);
			return used;
		} catch (RuntimeException exception) {
			failPreparation(server, registry, instance,
					"an unexpected world-generation error occurred", exception);
			return 0;
		}
	}

	private static void finishPreparation(MinecraftServer server, DungeonInstanceSavedData registry,
			ServerLevel level, DungeonInstanceSavedData.Instance instance, ArenaPreparationJob job) {
		if (!isPreparingInstance(instance)) {
			PREPARATIONS.remove(job.instanceId());
			return;
		}
		prepareLanding(level, job.center(), job.territory());
		String setupProblem = configureInstance(level, instance, job.center(),
				Math.max(1, instance.participants().size()), instance.seed(), job.territory());
		if (setupProblem != null) {
			failPreparation(server, registry, instance,
					"encounter setup failed: " + setupProblem, null);
			return;
		}
		PREPARATIONS.remove(job.instanceId());
		List<ServerPlayer> entrants = onlineBoundParticipants(server, instance);
		for (ServerPlayer entrant : entrants)
			entrant.setNoGravity(true);
		if (!entrants.isEmpty())
			SololevelingMod.queueServerWork(server, 1,
					() -> teleportEntrants(level, job.center(), entrants, instance));
		SololevelingMod.LOGGER.info("Prepared {} Monarch red-gate arena {} at {} in staged server ticks",
				job.territory().id(), instance.id(), job.center());
	}

	private static void failPreparation(MinecraftServer server, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance, String reason, RuntimeException exception) {
		PREPARATIONS.remove(instance.id());
		DungeonEncounterRuntime.clearInstanceHighlights(server, instance);
		discardTrackedMobs(server, instance);
		registry.remove(instance.id());
		rollbackPreparingGate(server, instance.id());
		for (ServerPlayer participant : server.getPlayerList().getPlayers()) {
			if (!instance.participants().contains(participant.getUUID())
					|| !instance.id().toString().equals(participant.getPersistentData()
							.getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				continue;
			if (isArenaDimension(participant.level().dimension()))
				recoverEntrant(participant,
						"The Red Gate could not finish stabilizing, so you were returned safely.");
			else {
				clearEntrantState(participant);
				participant.sendSystemMessage(Component.literal(
						"The Red Gate could not finish stabilizing. You may try it again.")
						.withStyle(ChatFormatting.YELLOW));
			}
		}
		refreshArenaFlag(server);
		if (exception == null)
			SololevelingMod.LOGGER.warn("Cancelled preparing Monarch red-gate arena {}: {}",
					instance.id(), reason);
		else
			SololevelingMod.LOGGER.error("Cancelled preparing Monarch red-gate arena {}: {}",
					instance.id(), reason, exception);
	}

	private static void rollbackPreparingGate(MinecraftServer server, UUID gateId) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity gate = level.getEntity(gateId);
			if (gate == null)
				continue;
			gate.getPersistentData().remove(GATE_INSTANCE_TAG);
			gate.getPersistentData().putBoolean(PROCEDURAL_RED_TAG, false);
			gate.getPersistentData().putBoolean("slr_is_red_gate", false);
			if (gate instanceof Portal1Entity portal) {
				portal.getEntityData().set(Portal1Entity.DATA_usedbefore, false);
				portal.setTexture("portalgate2");
			} else if (gate instanceof RedGateEntity redGate) {
				redGate.getEntityData().set(RedGateEntity.DATA_usedbefore, false);
				redGate.setTexture("portalgate2");
			}
			return;
		}
	}

	private static ArenaPalette paletteFor(RiftTerritory territory) {
		return switch (territory) {
			case DESTRUCTION -> new ArenaPalette(Blocks.RED_TERRACOTTA, Blocks.NETHERRACK,
					Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
			case FROST -> new ArenaPalette(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE,
					Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
			case FANGS -> new ArenaPalette(Blocks.MOSS_BLOCK, Blocks.PODZOL,
					Blocks.COARSE_DIRT, Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK);
			case PLAGUES -> new ArenaPalette(Blocks.MUD, Blocks.GREEN_TERRACOTTA,
					Blocks.CLAY, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA);
			case IRON_BODY -> new ArenaPalette(Blocks.TUFF, Blocks.ANDESITE,
					Blocks.GRAY_TERRACOTTA, Blocks.SMOOTH_BASALT, Blocks.POLISHED_ANDESITE);
			case WHITE_FLAMES -> new ArenaPalette(Blocks.SOUL_SOIL, Blocks.WHITE_TERRACOTTA,
					Blocks.SOUL_SAND, Blocks.CALCITE, Blocks.SMOOTH_QUARTZ);
			case TRANSFIGURATION -> new ArenaPalette(Blocks.WARPED_NYLIUM, Blocks.PURPLE_TERRACOTTA,
					Blocks.CYAN_TERRACOTTA, Blocks.WARPED_WART_BLOCK, Blocks.CRYING_OBSIDIAN);
			case BEGINNING -> new ArenaPalette(Blocks.SCULK, Blocks.DEEPSLATE,
					Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE);
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
		RiftTerritory territory = territoryForInstance(activeInstance).orElse(RiftTerritory.FROST);
		boolean removedStaleBinding = false;
		for (int index = 0; index < entrants.size(); index++) {
			ServerPlayer entrant = entrants.get(index);
			if (entrant == null || entrant.hasDisconnected())
				continue;
			if (!activeInstance.participants().contains(entrant.getUUID())
					|| !activeInstance.id().toString().equals(entrant.getPersistentData()
							.getString(DungeonMobLevelAdapter.INSTANCE_TAG))) {
				DungeonEncounterRuntime.clearHighlightsFor(entrant, activeInstance);
				removedStaleBinding |= activeInstance.removeParticipant(entrant.getUUID());
				continue;
			}
			double angle = Math.PI * 2.0D * index / Math.max(1, entrants.size());
			// Keep the validated arena Y. Re-querying the heightmap here could return
			// the dimension's void-floor Y even though the center was already safe.
			BlockPos arrival = center.offset(Mth.floor(Math.cos(angle) * 3.0D), 0,
					Mth.floor(Math.sin(angle) * 3.0D));
			prepareLanding(level, arrival, territory);
			entrant.teleportTo(level, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D,
					entrant.getYRot(), entrant.getXRot());
			entrant.setNoGravity(false);
			entrant.fallDistance = 0.0F;
			entrant.getPersistentData().remove(REVEAL_READY_TIME_TAG);
			DungeonEncounterRuntime.restoreCompletionFor(entrant, activeInstance);
		}
		ensureReturnPortal(level, activeInstance);
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
		entrant.getPersistentData().putLong(REVEAL_READY_TIME_TAG,
				sourceLevel.getServer().overworld().getGameTime() + MIN_GATE_REVEAL_TICKS);
		UrgentQuestManager.markDungeonId(entrant, "red_gate");
		// The player remains in the source world while the cell is prepared. Freeze
		// gravity only for the final one-tick teleport handoff.
		entrant.setNoGravity(false);
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

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		MinecraftServer server = event.getServer();
		processPreparationJobs(server);
		if (++tickCounter % 20 != 0)
			return;
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(server);
		for (DungeonInstanceSavedData.Instance instance : registry.listInstances()) {
			if (!isArenaInstance(instance))
				continue;
			ServerLevel level = server.getLevel(instance.dimension());
			if (level == null) {
				failUnavailableInstance(server, registry, instance,
						"A legacy Red Gate realm was retired, so you were returned safely.");
				continue;
			}
			Optional<BlockPos> center = instance.playerStart();
			if (center.isEmpty()) {
				failUnavailableInstance(server, registry, instance,
						"Your Red Gate arena had no valid center, so you were returned safely.");
				continue;
			}
			if (isPreparingInstance(instance)) {
				ensurePreparationQueued(instance);
				continue;
			}
			enforceBoundary(level, instance, center.get());
			leashEncounterMobs(level, instance, center.get());
			ensureReturnPortal(level, instance);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREPARATIONS.clear();
		tickCounter = 0;
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
			if (!player.level().dimension().equals(authoritative.dimension()))
				player.getPersistentData().putLong(REVEAL_READY_TIME_TAG,
						player.server.overworld().getGameTime() + MIN_GATE_REVEAL_TICKS);
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
		territoryForInstance(instance).ifPresent(territory ->
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
		if (isPreparingInstance(instance)) {
			ensurePreparationQueued(instance);
			player.setNoGravity(false);
			player.fallDistance = 0.0F;
			return;
		}
		if (player.level() == level) {
			player.setNoGravity(false);
			player.fallDistance = 0.0F;
			DungeonEncounterRuntime.restoreCompletionFor(player, instance);
			return;
		}
		player.setNoGravity(false);
		int revealDelay = Math.max(1, revealDelayRemaining(player.server, player));
		SololevelingMod.queueServerWork(player.server, revealDelay, () -> {
			if (!player.hasDisconnected() && instance.id().toString().equals(
					player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				teleportEntrants(level, center, List.of(player), instance);
		});
	}

	private static int revealDelayRemaining(MinecraftServer server, ServerPlayer player) {
		if (server == null || player == null)
			return 0;
		long readyTime = player.getPersistentData().getLong(REVEAL_READY_TIME_TAG);
		long remaining = readyTime - server.overworld().getGameTime();
		return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
	}

	/** Death ejects the player while the sealed entrance remains until it breaks. */
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
			detachArenaParticipant(original.server, registry, instance, original.getUUID());
		clearEntrantState(clone);
		clone.sendSystemMessage(Component.literal(
				"You died inside the red gate. Its entrance remains sealed until the gate breaks.")
				.withStyle(ChatFormatting.YELLOW));
	}

	private static void detachArenaParticipant(MinecraftServer server, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance, UUID playerId) {
		DungeonEncounterRuntime.clearHighlightsFor(server.getPlayerList().getPlayer(playerId), instance);
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
		boolean inTerritory = isArenaDimension(player.level().dimension());
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
		boolean stranded = isArenaDimension(player.level().dimension());
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
		// A removed custom dimension can make Minecraft place the player in the
		// Overworld before this login event runs. The persisted dungeon return is
		// still authoritative in that case, so do not require the current level to
		// be one of the (now unavailable) arena dimensions.
		boolean shouldReturn = stranded || hasSavedReturn;
		if (!shouldReturn || overworld == null)
			return;
		SololevelingMod.queueServerWork(player.server, 1, () -> {
			if (player.hasDisconnected()
					|| (stranded && !isArenaDimension(player.level().dimension())))
				return;
			player.teleportTo(overworld, returnX, returnY, returnZ, player.getYRot(), player.getXRot());
			player.setNoGravity(false);
			player.fallDistance = 0.0F;
		});
	}

	private static void clearEntrantState(ServerPlayer player) {
		player.getPersistentData().remove(DungeonMobLevelAdapter.INSTANCE_TAG);
		player.getPersistentData().remove("dungeon_tag");
		player.getPersistentData().remove(BOUNDARY_NOTICE_TAG);
		player.getPersistentData().remove(REVEAL_READY_TIME_TAG);
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
		PREPARATIONS.remove(instance.id());
		DungeonEncounterRuntime.clearInstanceHighlights(server, instance);
		ServerLevel level = server.getLevel(instance.dimension());
		if (level != null)
			discardReturnPortals(level, instance);
		discardTrackedMobs(server, instance);
		registry.remove(instance.id());
		// Abandonment is not a clear. Keep the already-used Overworld gate locked
		// and leave the Red Gate flag set until PortalPerTick performs its normal
		// lifetime break. Recording a closure here would discard the gate at once.
		SololevelingModVariables.MapVariables variables =
				SololevelingModVariables.MapVariables.get(server.overworld());
		variables.RedGate = true;
		variables.syncData(server.overworld());
		SololevelingMod.LOGGER.info(
				"Closed empty Monarch red-gate arena {}; its entrance remains sealed until it breaks",
				instance.id());
	}

	private static void failUnavailableInstance(MinecraftServer server, DungeonInstanceSavedData registry,
			DungeonInstanceSavedData.Instance instance, String message) {
		PREPARATIONS.remove(instance.id());
		DungeonEncounterRuntime.clearInstanceHighlights(server, instance);
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
		if (instance.returnPortalSuppressed()) {
			discardReturnPortals(level, instance);
			return;
		}
		if (!instance.completed() && instance.returnPortalDeferred()) {
			// Dedicated legacy Red Gates still create their portal at completion.
			discardReturnPortals(level, instance);
			return;
		}
		BlockPos exit = instance.exit().orElseGet(() -> instance.playerStart().orElse(BlockPos.ZERO));
		// Never wake an offline arena just to recreate a portal. Procedural Red
		// Gates restore their locked portal as soon as a bound participant loads
		// the exit; deferred legacy exits follow the same rule after completion.
		boolean participantPresent = level.players().stream().anyMatch(player ->
				instance.participants().contains(player.getUUID())
						&& instance.id().toString().equals(player.getPersistentData()
								.getString(DungeonMobLevelAdapter.INSTANCE_TAG)));
		if (!participantPresent || !level.hasChunkAt(exit))
			return;
		AABB search = AABB.ofSize(Vec3.atCenterOf(exit), 16.0D, 10.0D, 16.0D);
		boolean exists = !level.getEntitiesOfClass(Entity.class, search, entity ->
				entity.getType() == SololevelingModEntities.PORTAL_12.get()
						&& instance.id().toString().equals(entity.getPersistentData()
								.getString(DungeonMobLevelAdapter.INSTANCE_TAG))).isEmpty();
		if (exists)
			return;
		prepareLanding(level, exit, territoryForInstance(instance).orElse(RiftTerritory.FROST));
		Entity portal = DungeonReturnPortalSpawner.spawn(level, exit, instance.exitFacing().orElse(Direction.SOUTH),
				instance.id(), instance.id().toString());
		if (portal != null)
			SololevelingMod.LOGGER.info("{} return portal for Monarch red-gate arena {}",
					instance.completed() ? "Opened" : "Created locked", instance.id());
	}

	private enum PreparationPhase {
		LANDSCAPE,
		SCENERY,
		SHARDS,
		COMPLETE
	}

	/**
	 * Mutable, server-thread-only cursor for one deterministic arena build. Each
	 * consumed unit represents one horizontal landscape column or one bounded
	 * scenery feature. The generated hills feather back into the cheap flat realm
	 * outside the playable cell, so the player sees terrain rather than a platform
	 * edge without paying for infinite terrain generation.
	 */
	private static final class ArenaPreparationJob {
		private static final long DECORATION_SALT = 0x51A7E0F12D34B678L;
		private static final long SURFACE_SALT = 0x6A09E667F3BCC909L;
		private final UUID instanceId;
		private final BlockPos center;
		private final RiftTerritory territory;
		private final long seed;
		private final ArenaPalette palette;
		private final RandomSource random;
		private PreparationPhase phase = PreparationPhase.LANDSCAPE;
		private int landscapeIndex;
		private int sceneryIndex;
		private int shardIndex;
		private boolean staleEntitiesDiscarded;

		private ArenaPreparationJob(UUID instanceId, BlockPos center,
				RiftTerritory territory, long seed) {
			this.instanceId = instanceId;
			this.center = center.immutable();
			this.territory = territory;
			this.seed = seed;
			this.palette = paletteFor(territory);
			this.random = RandomSource.create(seed ^ DECORATION_SALT);
		}

		private UUID instanceId() {
			return instanceId;
		}

		private BlockPos center() {
			return center;
		}

		private RiftTerritory territory() {
			return territory;
		}

		private boolean matches(BlockPos expectedCenter, RiftTerritory expectedTerritory,
				long expectedSeed) {
			return center.equals(expectedCenter) && territory == expectedTerritory && seed == expectedSeed;
		}

		private boolean finished() {
			return phase == PreparationPhase.COMPLETE;
		}

		private int advance(ServerLevel level, int budget) {
			if (budget <= 0 || finished())
				return 0;
			if (!staleEntitiesDiscarded) {
				AABB cleanupBounds = new AABB(center).inflate(ARENA_RADIUS, 64.0D, ARENA_RADIUS);
				for (Entity stale : level.getEntitiesOfClass(Entity.class, cleanupBounds,
						entity -> !(entity instanceof ServerPlayer)))
					stale.discard();
				staleEntitiesDiscarded = true;
			}

			int used = 0;
			int sceneryThisPass = 0;
			int shardsThisPass = 0;
			while (used < budget && !finished()) {
				if (phase == PreparationPhase.SCENERY && sceneryThisPass >= 4)
					break;
				if (phase == PreparationPhase.SHARDS && shardsThisPass >= 6)
					break;
				PreparationPhase consumedPhase = phase;
				boolean consumed = switch (phase) {
					case LANDSCAPE -> advanceLandscapeColumn(level);
					case SCENERY -> advanceScenery(level);
					case SHARDS -> advanceShard(level);
					case COMPLETE -> false;
				};
				if (consumed) {
					used++;
					if (consumedPhase == PreparationPhase.SCENERY)
						sceneryThisPass++;
					else if (consumedPhase == PreparationPhase.SHARDS)
						shardsThisPass++;
				}
			}
			return used;
		}

		private boolean advanceLandscapeColumn(ServerLevel level) {
			int diameter = LANDSCAPE_RADIUS * 2 + 1;
			int total = diameter * diameter;
			while (landscapeIndex < total) {
				int index = landscapeIndex++;
				int dx = index / diameter - LANDSCAPE_RADIUS;
				int dz = index % diameter - LANDSCAPE_RADIUS;
				if (dx * dx + dz * dz > LANDSCAPE_RADIUS * LANDSCAPE_RADIUS)
					continue;

				int baseSurfaceY = center.getY() - 1;
				int targetSurfaceY = Mth.clamp(baseSurfaceY + terrainOffset(dx, dz),
						level.getMinBuildHeight() + 2, level.getMaxBuildHeight() - 3);
				int minimumY = Math.max(level.getMinBuildHeight() + 1,
						baseSurfaceY + LANDSCAPE_MIN_OFFSET);
				int maximumY = Math.min(level.getMaxBuildHeight() - 2,
						baseSurfaceY + LANDSCAPE_CLEAR_HEIGHT);
				BlockState surface = surfaceState(dx, dz);
				BlockState fill = palette.groundPrimary().defaultBlockState();
				for (int y = minimumY; y <= maximumY; y++) {
					BlockPos position = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
					BlockState desired = y > targetSurfaceY
							? Blocks.AIR.defaultBlockState()
							: y == targetSurfaceY ? surface : fill;
					setBlockIfChanged(level, position, desired);
				}
				return true;
			}
			phase = PreparationPhase.SCENERY;
			return false;
		}

		private boolean advanceScenery(ServerLevel level) {
			if (sceneryIndex >= SCENERY_COUNT) {
				phase = PreparationPhase.SHARDS;
				return false;
			}
			int feature = sceneryIndex++;
			double angle = random.nextDouble() * Math.PI * 2.0D;
			int radius = 22 + random.nextInt(61);
			int dx = Mth.floor(Math.cos(angle) * radius);
			int dz = Mth.floor(Math.sin(angle) * radius);
			BlockPos base = standingPosition(dx, dz);
			placeSceneryFeature(level, base, feature);
			return true;
		}

		private boolean advanceShard(ServerLevel level) {
			if (shardIndex >= SHARD_COUNT) {
				phase = PreparationPhase.COMPLETE;
				return false;
			}
			int shard = shardIndex++;
			double angle = Math.PI * 2.0D * shard / SHARD_COUNT + random.nextDouble() * 0.18D;
			int radius = 67 + random.nextInt(18);
			int dx = Mth.floor(Math.cos(angle) * radius);
			int dz = Mth.floor(Math.sin(angle) * radius);
			BlockPos base = standingPosition(dx, dz);
			int height = 3 + random.nextInt(5);
			for (int y = 0; y < height; y++) {
				BlockPos shardPos = base.above(y);
				BlockState current = level.getBlockState(shardPos);
				if (!current.isAir() && !current.is(Blocks.SNOW))
					break;
				boolean accent = y == height - 1 || random.nextInt(5) == 0;
				setBlockIfChanged(level, shardPos, (accent
						? palette.shardAccent() : palette.shardPrimary()).defaultBlockState());
			}
			if (shardIndex >= SHARD_COUNT)
				phase = PreparationPhase.COMPLETE;
			return true;
		}

		private BlockPos standingPosition(int dx, int dz) {
			return center.offset(dx, terrainOffset(dx, dz), dz);
		}

		private int terrainOffset(int dx, int dz) {
			double radius = Math.sqrt((double) dx * dx + (double) dz * dz);
			if (radius <= SAFE_CLEARING_RADIUS || radius >= LANDSCAPE_RADIUS)
				return 0;

			double phaseA = unit(seed ^ 0x243F6A8885A308D3L) * Math.PI * 2.0D;
			double phaseB = unit(seed ^ 0x13198A2E03707344L) * Math.PI * 2.0D;
			double phaseC = unit(seed ^ 0xA4093822299F31D0L) * Math.PI * 2.0D;
			double broad = Math.sin(dx * 0.052D + phaseA)
					+ Math.cos(dz * 0.047D + phaseB)
					+ Math.sin((dx + dz) * 0.031D + phaseC) * 0.72D;
			double detail = Math.sin(dx * 0.137D - dz * 0.109D + phaseB) * 0.8D
					+ Math.cos((dx - dz) * 0.091D + phaseA) * 0.45D;
			double shape = switch (territory) {
				case DESTRUCTION -> 4.0D + broad * 3.6D + Math.abs(detail) * 2.8D;
				case FROST -> 5.0D + broad * 4.8D + Math.abs(detail) * 3.7D;
				case FANGS -> 3.0D + broad * 3.5D + detail * 1.8D;
				case PLAGUES -> broad * 1.8D - Math.abs(detail) * 1.2D;
				case IRON_BODY -> 5.0D + broad * 5.4D + Math.abs(detail) * 3.8D;
				case WHITE_FLAMES -> 3.0D + broad * 3.7D + Math.abs(detail) * 2.0D;
				case TRANSFIGURATION -> 4.0D + broad * 5.0D + detail * 3.2D;
				case BEGINNING -> 4.0D + broad * 4.4D + detail * 2.1D;
			};

			double clearingBlend = smoothStep((radius - SAFE_CLEARING_RADIUS) / 18.0D);
			/*
			 * The playable barrier ends at radius 80. Raise the last 24 blocks of
			 * generated terrain into a natural horizon ridge instead of feathering
			 * back down to the shared realm's cheap flat substrate. From anywhere
			 * the player can legally stand, the ridge and client fog conceal the
			 * finite terrain seam without generating an infinite world.
			 */
			double ridgeStart = ARENA_RADIUS + 6.0D;
			double ridge = smoothStep((radius - ridgeStart)
					/ Math.max(1.0D, LANDSCAPE_RADIUS - ridgeStart)) * 11.0D;
			return Mth.clamp((int) Math.round(shape * clearingBlend + ridge),
					LANDSCAPE_MIN_OFFSET, LANDSCAPE_MAX_OFFSET);
		}

		private BlockState surfaceState(int dx, int dz) {
			double radius = Math.sqrt((double) dx * dx + (double) dz * dz);
			if (radius <= SAFE_CLEARING_RADIUS + 4.0D)
				return palette.groundPrimary().defaultBlockState();
			double roll = unit(seed ^ SURFACE_SALT
					^ (long) dx * 0x9E3779B97F4A7C15L
					^ (long) dz * 0xC2B2AE3D27D4EB4FL);
			if (roll < 0.10D && !(radius < 24.0D
					&& isUnsafeLandingBlock(palette.groundAccent().defaultBlockState())))
				return palette.groundAccent().defaultBlockState();
			if (roll < 0.38D)
				return palette.groundSecondary().defaultBlockState();
			return palette.groundPrimary().defaultBlockState();
		}

		private void placeSceneryFeature(ServerLevel level, BlockPos base, int feature) {
			switch (territory) {
				case FROST -> {
					if (feature % 3 == 0)
						placeCanopyTree(level, base, Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES,
								5 + random.nextInt(3), true);
					else
						placeSpire(level, base, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
								3 + random.nextInt(5));
				}
				case FANGS -> {
					if (feature % 3 == 0)
						placeCanopyTree(level, base, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES,
								4 + random.nextInt(3), true);
					else
						placeBoulder(level, base, Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK);
				}
				case PLAGUES -> {
					if (feature % 4 == 0)
						placeDeadTree(level, base, Blocks.MANGROVE_LOG, 4 + random.nextInt(4));
					else
						placeBoulder(level, base, Blocks.MANGROVE_ROOTS, Blocks.MUD);
				}
				case DESTRUCTION -> {
					if (feature % 4 == 0)
						placeDeadTree(level, base, Blocks.STRIPPED_DARK_OAK_LOG, 4 + random.nextInt(4));
					else
						placeSpire(level, base, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK,
								3 + random.nextInt(5));
				}
				case IRON_BODY -> placeBoulder(level, base, Blocks.TUFF, Blocks.SMOOTH_BASALT);
				case WHITE_FLAMES -> placeSpire(level, base, Blocks.CALCITE, Blocks.SMOOTH_QUARTZ,
						4 + random.nextInt(5));
				case TRANSFIGURATION -> {
					if (feature % 3 == 0)
						placeCanopyTree(level, base, Blocks.WARPED_STEM, Blocks.WARPED_WART_BLOCK,
								4 + random.nextInt(4), false);
					else
						placeSpire(level, base, Blocks.CRYING_OBSIDIAN, Blocks.WARPED_WART_BLOCK,
								3 + random.nextInt(6));
				}
				case BEGINNING -> {
					if (feature % 3 == 0)
						placeMonolith(level, base, 5 + random.nextInt(5));
					else
						placeBoulder(level, base, Blocks.DEEPSLATE, Blocks.SCULK);
				}
			}
		}

		private void placeCanopyTree(ServerLevel level, BlockPos base, Block trunk, Block canopy,
				int height, boolean persistentLeaves) {
			for (int y = 0; y < height; y++)
				setSceneryBlock(level, base.above(y), trunk.defaultBlockState());
			BlockState canopyState = canopy.defaultBlockState();
			if (persistentLeaves && canopyState.hasProperty(LeavesBlock.PERSISTENT))
				canopyState = canopyState.setValue(LeavesBlock.PERSISTENT, true);
			for (int layer = -2; layer <= 1; layer++) {
				int radius = layer >= 1 ? 1 : layer == 0 ? 2 : 1;
				int y = height + layer;
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (Math.abs(dx) + Math.abs(dz) > radius + 1)
							continue;
						setSceneryBlock(level, base.offset(dx, y, dz), canopyState);
					}
				}
			}
		}

		private void placeDeadTree(ServerLevel level, BlockPos base, Block trunk, int height) {
			for (int y = 0; y < height; y++)
				setSceneryBlock(level, base.above(y), trunk.defaultBlockState());
			Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			Direction second = first.getClockWise();
			setSceneryBlock(level, base.above(height - 2).relative(first), trunk.defaultBlockState());
			setSceneryBlock(level, base.above(height - 1).relative(second), trunk.defaultBlockState());
		}

		private void placeSpire(ServerLevel level, BlockPos base, Block primary, Block accent, int height) {
			for (int y = 0; y < height; y++) {
				Block block = y == height - 1 || (y > 1 && random.nextInt(5) == 0) ? accent : primary;
				setSceneryBlock(level, base.above(y), block.defaultBlockState());
				if (y < 2) {
					setSceneryBlock(level, base.offset(1, y, 0), primary.defaultBlockState());
					setSceneryBlock(level, base.offset(0, y, 1), primary.defaultBlockState());
				}
			}
		}

		private void placeBoulder(ServerLevel level, BlockPos base, Block primary, Block accent) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					for (int y = 0; y <= 1; y++) {
						if (Math.abs(dx) + Math.abs(dz) + y > 3)
							continue;
						Block block = random.nextInt(5) == 0 ? accent : primary;
						setSceneryBlock(level, base.offset(dx, y, dz), block.defaultBlockState());
					}
				}
			}
		}

		private void placeMonolith(ServerLevel level, BlockPos base, int height) {
			for (int y = 0; y < height; y++) {
				Block block = y == height - 1 ? Blocks.CHISELED_DEEPSLATE
						: y % 3 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_BRICKS;
				setSceneryBlock(level, base.above(y), block.defaultBlockState());
			}
			setSceneryBlock(level, base.relative(Direction.EAST), Blocks.SCULK.defaultBlockState());
			setSceneryBlock(level, base.relative(Direction.WEST), Blocks.SCULK.defaultBlockState());
		}

		private void setSceneryBlock(ServerLevel level, BlockPos position, BlockState state) {
			BlockState current = level.getBlockState(position);
			if (current.isAir() || current.is(Blocks.SNOW))
				setBlockIfChanged(level, position, state);
		}

		private static double smoothStep(double value) {
			double clamped = Mth.clamp(value, 0.0D, 1.0D);
			return clamped * clamped * (3.0D - 2.0D * clamped);
		}

		private static double unit(long value) {
			value ^= value >>> 33;
			value *= 0xFF51AFD7ED558CCDL;
			value ^= value >>> 33;
			value *= 0xC4CEB9FE1A85EC53L;
			value ^= value >>> 33;
			return (value >>> 11) * 0x1.0p-53;
		}
	}

	private record ArenaPalette(Block groundPrimary, Block groundSecondary, Block groundAccent,
			Block shardPrimary, Block shardAccent) {
	}

	private record WaveSpec(String id, ResourceLocation pool, int count, boolean boss, boolean elite,
			int delayTicks) {
	}
}
