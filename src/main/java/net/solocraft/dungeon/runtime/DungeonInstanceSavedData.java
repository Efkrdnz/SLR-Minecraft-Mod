package net.solocraft.dungeon.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Durable, server-authoritative state for generated dungeon instances.
 *
 * <p>The data is always stored in the overworld so instances remain discoverable
 * even when their dungeon dimension is unloaded. All collection getters return
 * immutable copies; mutations pass through methods that mark the SavedData dirty.</p>
 */
public final class DungeonInstanceSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_dungeon_instances";
	private static final int SCHEMA_VERSION = 4;

	public static final int MAX_INSTANCES = 256;
	public static final int MAX_PARTICIPANTS = 64;
	public static final int MAX_ROOMS = 512;
	public static final int MAX_CONNECTIONS = 1_024;
	public static final int MAX_OCCUPIED_VOLUMES = MAX_ROOMS + MAX_CONNECTIONS;
	public static final int MAX_ENCOUNTERS = 256;
	public static final int MAX_MARKERS_PER_ENCOUNTER = 256;
	public static final int MAX_TRACKED_MOBS_PER_ENCOUNTER = 2_048;
	public static final int MAX_EFFECTIVE_LEVEL = 1_000;
	public static final int MAX_WAVE_DELAY_TICKS = 1_000_000;

	private static final int MAX_TOTAL_PARTICIPANTS_ON_LOAD = 8_192;
	private static final int MAX_TOTAL_ROOMS_ON_LOAD = 16_384;
	private static final int MAX_TOTAL_CONNECTIONS_ON_LOAD = 32_768;
	private static final int MAX_TOTAL_OCCUPIED_VOLUMES_ON_LOAD = 65_536;
	private static final int MAX_TOTAL_ENCOUNTERS_ON_LOAD = 8_192;
	private static final int MAX_TOTAL_MARKERS_ON_LOAD = 65_536;
	private static final int MAX_TOTAL_TRACKED_MOBS_ON_LOAD = 131_072;
	private static final int MAX_RESOURCE_ID_LENGTH = 256;
	private static final int MAX_KEY_LENGTH = 128;
	private static final int MAX_MARKER_TEXT_LENGTH = 128;
	private static final int MAX_HORIZONTAL_COORDINATE = 30_000_000;
	private static final int MAX_ABSOLUTE_VERTICAL_COORDINATE = 4_096;
	private static final int MAX_BOUNDS_AXIS = 1_024;
	private static final long MAX_BOUNDS_VOLUME = 16_777_216L;

	private final Map<UUID, Instance> instances = new LinkedHashMap<>();

	/** Returns the overworld-backed registry for any server level. */
	public static DungeonInstanceSavedData get(ServerLevel level) {
		if (level == null)
			throw new IllegalArgumentException("A server level is required.");
		return get(level.getServer());
	}

	/** Returns the overworld-backed registry for a server. */
	public static DungeonInstanceSavedData get(MinecraftServer server) {
		if (server == null)
			throw new IllegalArgumentException("A Minecraft server is required.");
		ServerLevel overworld = server.overworld();
		return overworld.getDataStorage().computeIfAbsent(
				DungeonInstanceSavedData::load, DungeonInstanceSavedData::new, DATA_NAME);
	}

	/** Creates an instance with a generated UUID. */
	public MutationResult<Instance> create(ResourceLocation dungeonId, ResourceKey<Level> dimension,
			long seed, int effectiveLevel, long createdGameTime) {
		UUID id;
		do {
			id = UUID.randomUUID();
		} while (instances.containsKey(id));
		return create(id, dungeonId, dimension, seed, effectiveLevel, createdGameTime);
	}

	/** Creates an instance with a caller-supplied stable UUID, such as a gate UUID. */
	public MutationResult<Instance> create(UUID instanceId, ResourceLocation dungeonId,
			ResourceKey<Level> dimension, long seed, int effectiveLevel, long createdGameTime) {
		String problem = validateInstanceIdentity(instanceId, dungeonId, dimension);
		if (problem != null)
			return MutationResult.failure(problem);
		pruneCompletedEmptyInstances();
		if (instances.size() >= MAX_INSTANCES)
			return MutationResult.failure("The server already has the maximum number of dungeon instances.");
		if (instances.containsKey(instanceId))
			return MutationResult.failure("Dungeon instance " + instanceId + " already exists.");

		Instance instance = new Instance(instanceId, dungeonId, dimension, seed,
				clampLevel(effectiveLevel), Math.max(0L, createdGameTime), this::setDirty);
		instances.put(instanceId, instance);
		setDirty();
		return MutationResult.success(instance, "Created dungeon instance " + instanceId + ".");
	}

	public Optional<Instance> getInstance(UUID instanceId) {
		return Optional.ofNullable(instanceId == null ? null : instances.get(instanceId));
	}

	/** Immutable copy of the registry's mutable instance handles. */
	public List<Instance> listInstances() {
		return List.copyOf(instances.values());
	}

	/** Deep immutable snapshots suitable for diagnostics, commands, or networking. */
	public List<InstanceView> views() {
		return instances.values().stream().map(Instance::view).toList();
	}

	/** Removes registry state only; world cleanup belongs to the instance runtime. */
	public Optional<InstanceView> remove(UUID instanceId) {
		if (instanceId == null)
			return Optional.empty();
		Instance removed = instances.remove(instanceId);
		if (removed == null)
			return Optional.empty();
		setDirty();
		return Optional.of(removed.view());
	}

	public int size() {
		return instances.size();
	}

	/** Reclaims completed records after every participant has safely exited. */
	public int pruneCompletedEmptyInstances() {
		int before = instances.size();
		instances.entrySet().removeIf(entry -> entry.getValue().completed()
				&& entry.getValue().participants().isEmpty());
		int removed = before - instances.size();
		if (removed > 0)
			setDirty();
		return removed;
	}

	@Override
	@Nonnull
	public CompoundTag save(@Nonnull CompoundTag tag) {
		tag.putInt("SchemaVersion", SCHEMA_VERSION);
		ListTag list = new ListTag();
		for (Instance instance : instances.values())
			list.add(instance.save());
		tag.put("Instances", list);
		return tag;
	}

	private static DungeonInstanceSavedData load(CompoundTag root) {
		DungeonInstanceSavedData data = new DungeonInstanceSavedData();
		LoadBudget budget = new LoadBudget();
		if (!root.contains("Instances", Tag.TAG_LIST))
			return data;

		ListTag list = root.getList("Instances", Tag.TAG_COMPOUND);
		int limit = Math.min(list.size(), MAX_INSTANCES);
		if (list.size() > limit)
			budget.sanitized = true;
		for (int index = 0; index < limit; index++) {
			Instance instance = Instance.load(list.getCompound(index), data::setDirty, budget);
			if (instance == null || data.instances.putIfAbsent(instance.id, instance) != null)
				budget.sanitized = true;
		}
		if (budget.sanitized)
			data.setDirty();
		return data;
	}

	private static String validateInstanceIdentity(@Nullable UUID id, @Nullable ResourceLocation dungeonId,
			@Nullable ResourceKey<Level> dimension) {
		if (id == null)
			return "Instance UUID is required.";
		if (!safeResourceId(dungeonId))
			return "Dungeon id is missing or too long.";
		if (dimension == null || !safeResourceId(dimension.location()))
			return "Dungeon dimension is missing or invalid.";
		return null;
	}

	private static int clampLevel(int level) {
		return Math.max(0, Math.min(MAX_EFFECTIVE_LEVEL, level));
	}

	private static boolean safeResourceId(@Nullable ResourceLocation id) {
		return id != null && id.toString().length() <= MAX_RESOURCE_ID_LENGTH;
	}

	@Nullable
	private static ResourceLocation parseResourceId(String value) {
		if (value == null || value.isBlank() || value.length() > MAX_RESOURCE_ID_LENGTH)
			return null;
		return ResourceLocation.tryParse(value);
	}

	private static boolean safeKey(String value, int maxLength) {
		if (value == null || value.isBlank() || value.length() > maxLength)
			return false;
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (!(character >= 'a' && character <= 'z')
					&& !(character >= '0' && character <= '9')
					&& character != '_' && character != '-' && character != '.'
					&& character != '/' && character != ':')
				return false;
		}
		return true;
	}

	private static boolean safePosition(@Nullable BlockPos position) {
		return position != null
				&& Math.abs((long) position.getX()) <= MAX_HORIZONTAL_COORDINATE
				&& Math.abs((long) position.getZ()) <= MAX_HORIZONTAL_COORDINATE
				&& Math.abs((long) position.getY()) <= MAX_ABSOLUTE_VERTICAL_COORDINATE;
	}

	@Nullable
	private static BlockPos readPosition(CompoundTag owner, String key, LoadBudget budget) {
		if (!owner.contains(key, Tag.TAG_COMPOUND))
			return null;
		BlockPos position = NbtUtils.readBlockPos(owner.getCompound(key));
		if (!safePosition(position)) {
			budget.sanitized = true;
			return null;
		}
		return position;
	}

	private static Rotation parseRotation(String value, LoadBudget budget) {
		try {
			return Rotation.valueOf(value);
		} catch (IllegalArgumentException | NullPointerException ignored) {
			budget.sanitized = true;
			return Rotation.NONE;
		}
	}

	public record MutationResult<T>(boolean success, String message, @Nullable T value) {
		private static <T> MutationResult<T> success(T value, String message) {
			return new MutationResult<>(true, message, value);
		}

		private static <T> MutationResult<T> failure(String message) {
			return new MutationResult<>(false, message, null);
		}
	}

	/** Inclusive and normalized axis-aligned world bounds. */
	public record Bounds(BlockPos min, BlockPos max) {
		public Bounds {
			if (min == null || max == null)
				throw new IllegalArgumentException("Bounds require two positions.");
			BlockPos first = min;
			BlockPos second = max;
			min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
					Math.min(first.getZ(), second.getZ()));
			max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
					Math.max(first.getZ(), second.getZ()));
		}

		public long sizeX() {
			return (long) max.getX() - min.getX() + 1L;
		}

		public long sizeY() {
			return (long) max.getY() - min.getY() + 1L;
		}

		public long sizeZ() {
			return (long) max.getZ() - min.getZ() + 1L;
		}

		public boolean contains(BlockPos position) {
			return position != null
					&& position.getX() >= min.getX() && position.getX() <= max.getX()
					&& position.getY() >= min.getY() && position.getY() <= max.getY()
					&& position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
		}

		public boolean intersects(Bounds other) {
			return other != null
					&& max.getX() >= other.min.getX() && min.getX() <= other.max.getX()
					&& max.getY() >= other.min.getY() && min.getY() <= other.max.getY()
					&& max.getZ() >= other.min.getZ() && min.getZ() <= other.max.getZ();
		}

		private boolean isSafe() {
			long x = sizeX();
			long y = sizeY();
			long z = sizeZ();
			return safePosition(min) && safePosition(max)
					&& x <= MAX_BOUNDS_AXIS && y <= MAX_BOUNDS_AXIS && z <= MAX_BOUNDS_AXIS
					&& x * y * z <= MAX_BOUNDS_VOLUME;
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.put("Min", NbtUtils.writeBlockPos(min));
			tag.put("Max", NbtUtils.writeBlockPos(max));
			return tag;
		}

		@Nullable
		private static Bounds load(CompoundTag tag, LoadBudget budget) {
			BlockPos min = readPosition(tag, "Min", budget);
			BlockPos max = readPosition(tag, "Max", budget);
			if (min == null || max == null) {
				budget.sanitized = true;
				return null;
			}
			Bounds bounds = new Bounds(min, max);
			if (!bounds.isSafe()) {
				budget.sanitized = true;
				return null;
			}
			return bounds;
		}
	}

	public record PlacedRoom(String roomId, ResourceLocation structureId, Rotation rotation, Bounds bounds) {
		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("RoomId", roomId);
			tag.putString("Structure", structureId.toString());
			tag.putString("Rotation", rotation.name());
			tag.put("Bounds", bounds.save());
			return tag;
		}

		@Nullable
		private static PlacedRoom load(CompoundTag tag, LoadBudget budget) {
			String roomId = tag.getString("RoomId");
			ResourceLocation structureId = parseResourceId(tag.getString("Structure"));
			Bounds bounds = tag.contains("Bounds", Tag.TAG_COMPOUND)
					? Bounds.load(tag.getCompound("Bounds"), budget) : null;
			if (!safeKey(roomId, MAX_KEY_LENGTH) || structureId == null || bounds == null) {
				budget.sanitized = true;
				return null;
			}
			return new PlacedRoom(roomId, structureId, parseRotation(tag.getString("Rotation"), budget), bounds);
		}
	}

	public record EncounterMarker(String id, String role, BlockPos position) {
		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Id", id);
			tag.putString("Role", role);
			tag.put("Position", NbtUtils.writeBlockPos(position));
			return tag;
		}

		@Nullable
		private static EncounterMarker load(CompoundTag tag, LoadBudget budget) {
			String id = tag.getString("Id");
			String role = tag.getString("Role");
			BlockPos position = readPosition(tag, "Position", budget);
			if (!safeKey(id, MAX_MARKER_TEXT_LENGTH) || !safeKey(role, MAX_MARKER_TEXT_LENGTH)
					|| position == null) {
				budget.sanitized = true;
				return null;
			}
			return new EncounterMarker(id, role, position);
		}
	}

	public record EncounterView(String key, ResourceLocation poolId, boolean boss, boolean levelOverride, int minSpawnLevel,
			int maxSpawnLevel, String sequenceKey, int sequenceOrder, int delayTicks,
			long scheduledGameTime, List<EncounterMarker> markers, @Nullable Bounds triggerBounds,
			boolean activated, boolean completed, Set<UUID> trackedMobs) {
	}

	public record InstanceView(UUID id, ResourceLocation dungeonId, ResourceKey<Level> dimension,
			long seed, int effectiveLevel, long createdGameTime, boolean completed, Set<UUID> participants,
			@Nullable BlockPos playerStart, @Nullable BlockPos exit, List<PlacedRoom> rooms,
			List<Bounds> carvedConnections, List<Bounds> occupiedVolumes, List<EncounterView> encounters) {
	}

	/** Mutable instance handle. All public mutations mark the owning SavedData dirty. */
	public static final class Instance {
		private final UUID id;
		private final ResourceLocation dungeonId;
		private final ResourceKey<Level> dimension;
		private final long seed;
		private final int effectiveLevel;
		private final long createdGameTime;
		private final Runnable dirty;
		private boolean completed;
		private final Set<UUID> participants = new LinkedHashSet<>();
		@Nullable
		private BlockPos playerStart;
		@Nullable
		private BlockPos exit;
		@Nullable
		private Direction exitFacing;
		private final List<PlacedRoom> rooms = new ArrayList<>();
		private final List<Bounds> carvedConnections = new ArrayList<>();
		private final List<Bounds> occupiedVolumes = new ArrayList<>();
		private final Map<String, EncounterState> encounters = new LinkedHashMap<>();

		private Instance(UUID id, ResourceLocation dungeonId, ResourceKey<Level> dimension, long seed,
				int effectiveLevel, long createdGameTime, Runnable dirty) {
			this.id = id;
			this.dungeonId = dungeonId;
			this.dimension = dimension;
			this.seed = seed;
			this.effectiveLevel = effectiveLevel;
			this.createdGameTime = createdGameTime;
			this.dirty = dirty;
		}

		public UUID id() {
			return id;
		}

		public ResourceLocation dungeonId() {
			return dungeonId;
		}

		public ResourceKey<Level> dimension() {
			return dimension;
		}

		public long seed() {
			return seed;
		}

		public int effectiveLevel() {
			return effectiveLevel;
		}

		public long createdGameTime() {
			return createdGameTime;
		}

		public boolean completed() {
			return completed;
		}

		public boolean setCompleted(boolean completed) {
			if (this.completed == completed)
				return false;
			this.completed = completed;
			dirty.run();
			return true;
		}

		public Set<UUID> participants() {
			return Set.copyOf(participants);
		}

		public boolean addParticipant(UUID playerId) {
			if (playerId == null || participants.size() >= MAX_PARTICIPANTS || !participants.add(playerId))
				return false;
			dirty.run();
			return true;
		}

		public boolean removeParticipant(UUID playerId) {
			if (playerId == null || !participants.remove(playerId))
				return false;
			dirty.run();
			return true;
		}

		public Optional<BlockPos> playerStart() {
			return Optional.ofNullable(playerStart);
		}

		public boolean setPlayerStart(@Nullable BlockPos position) {
			if (position != null && !safePosition(position))
				return false;
			if (java.util.Objects.equals(playerStart, position))
				return false;
			playerStart = position;
			dirty.run();
			return true;
		}

		public Optional<BlockPos> exit() {
			return Optional.ofNullable(exit);
		}

		public boolean setExit(@Nullable BlockPos position) {
			if (position != null && !safePosition(position))
				return false;
			if (java.util.Objects.equals(exit, position))
				return false;
			exit = position;
			dirty.run();
			return true;
		}

		/** Direction from the return-portal marker toward the start room's exit. */
		public Optional<Direction> exitFacing() {
			return Optional.ofNullable(exitFacing);
		}

		public boolean setExitFacing(@Nullable Direction facing) {
			if (facing != null && !facing.getAxis().isHorizontal())
				return false;
			if (exitFacing == facing)
				return false;
			exitFacing = facing;
			dirty.run();
			return true;
		}

		public List<PlacedRoom> rooms() {
			return List.copyOf(rooms);
		}

		public boolean addRoom(PlacedRoom room) {
			if (!validRoom(room) || rooms.size() >= MAX_ROOMS)
				return false;
			rooms.add(room);
			dirty.run();
			return true;
		}

		public boolean addRoom(String roomId, ResourceLocation structureId, Rotation rotation, Bounds bounds) {
			if (roomId == null || structureId == null || rotation == null || bounds == null)
				return false;
			return addRoom(new PlacedRoom(roomId, structureId, rotation, bounds));
		}

		public boolean removeRoom(String roomId) {
			boolean removed = rooms.removeIf(room -> room.roomId.equals(roomId));
			if (removed)
				dirty.run();
			return removed;
		}

		public List<Bounds> carvedConnections() {
			return List.copyOf(carvedConnections);
		}

		public boolean addCarvedConnection(Bounds bounds) {
			if (bounds == null || !bounds.isSafe() || carvedConnections.size() >= MAX_CONNECTIONS
					|| carvedConnections.contains(bounds))
				return false;
			carvedConnections.add(bounds);
			dirty.run();
			return true;
		}

		/**
		 * Conservative world volumes owned by this instance, including room and
		 * corridor interiors plus any protective-shell thickness. Schema-v2 instances
		 * legitimately return an empty list and are handled by a runtime fallback.
		 */
		public List<Bounds> occupiedVolumes() {
			return List.copyOf(occupiedVolumes);
		}

		public boolean addOccupiedVolume(Bounds bounds) {
			if (bounds == null || !bounds.isSafe() || occupiedVolumes.size() >= MAX_OCCUPIED_VOLUMES
					|| occupiedVolumes.contains(bounds))
				return false;
			occupiedVolumes.add(bounds);
			dirty.run();
			return true;
		}

		public List<EncounterState> encounters() {
			return List.copyOf(encounters.values());
		}

		public Optional<EncounterState> encounter(String key) {
			return Optional.ofNullable(key == null ? null : encounters.get(key));
		}

		public MutationResult<EncounterState> createEncounter(String key, ResourceLocation poolId,
				boolean boss, int minSpawnLevel, int maxSpawnLevel) {
			return createEncounter(key, poolId, boss, true, minSpawnLevel, maxSpawnLevel);
		}

		public MutationResult<EncounterState> createEncounter(String key, ResourceLocation poolId,
				boolean boss, boolean levelOverride, int minSpawnLevel, int maxSpawnLevel) {
			return createEncounter(key, poolId, boss, levelOverride, minSpawnLevel, maxSpawnLevel,
					"", -1, 0);
		}

		/**
		 * Creates one ordered schema-v3 wave. Waves sharing sequenceKey activate in
		 * ascending sequenceOrder; delayTicks is counted before this wave becomes
		 * eligible after its predecessor (or initial trigger) completes.
		 */
		public MutationResult<EncounterState> createEncounter(String key, ResourceLocation poolId,
				boolean boss, boolean levelOverride, int minSpawnLevel, int maxSpawnLevel,
				String sequenceKey, int sequenceOrder, int delayTicks) {
			if (!safeKey(key, MAX_KEY_LENGTH))
				return MutationResult.failure("Encounter key is missing or invalid.");
			if (!safeResourceId(poolId))
				return MutationResult.failure("Encounter pool id is missing or invalid.");
			boolean sequenced = sequenceKey != null && !sequenceKey.isBlank();
			if (sequenced && !safeKey(sequenceKey, MAX_KEY_LENGTH))
				return MutationResult.failure("Encounter sequence key is invalid.");
			if (sequenced && (sequenceOrder < 0 || sequenceOrder >= MAX_ENCOUNTERS))
				return MutationResult.failure("Encounter sequence order is outside the runtime safety range.");
			if (sequenced && (delayTicks < 0 || delayTicks > MAX_WAVE_DELAY_TICKS))
				return MutationResult.failure("Encounter wave delay is outside the runtime safety range.");
			if (encounters.size() >= MAX_ENCOUNTERS)
				return MutationResult.failure("This instance has the maximum number of encounters.");
			if (encounters.containsKey(key))
				return MutationResult.failure("Encounter " + key + " already exists.");
			if (sequenced && encounters.values().stream().anyMatch(existing -> existing.sequenced()
					&& existing.sequenceKey().equals(sequenceKey) && existing.sequenceOrder() == sequenceOrder))
				return MutationResult.failure("Encounter sequence " + sequenceKey
						+ " already contains wave order " + sequenceOrder + ".");
			int min = clampLevel(Math.min(minSpawnLevel, maxSpawnLevel));
			int max = clampLevel(Math.max(minSpawnLevel, maxSpawnLevel));
			EncounterState encounter = new EncounterState(key, poolId, boss, levelOverride, min, max,
					sequenced ? sequenceKey : "", sequenced ? sequenceOrder : -1,
					sequenced ? delayTicks : 0, dirty);
			encounters.put(key, encounter);
			dirty.run();
			return MutationResult.success(encounter, "Created encounter " + key + ".");
		}

		public boolean removeEncounter(String key) {
			if (key == null || encounters.remove(key) == null)
				return false;
			dirty.run();
			return true;
		}

		public InstanceView view() {
			return new InstanceView(id, dungeonId, dimension, seed, effectiveLevel, createdGameTime,
					completed, Set.copyOf(participants), playerStart, exit, List.copyOf(rooms),
					List.copyOf(carvedConnections), List.copyOf(occupiedVolumes),
					encounters.values().stream().map(EncounterState::view).toList());
		}

		private static boolean validRoom(@Nullable PlacedRoom room) {
			return room != null && safeKey(room.roomId, MAX_KEY_LENGTH)
					&& safeResourceId(room.structureId) && room.rotation != null
					&& room.bounds != null && room.bounds.isSafe();
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", id);
			tag.putString("DungeonId", dungeonId.toString());
			tag.putString("Dimension", dimension.location().toString());
			tag.putLong("Seed", seed);
			tag.putInt("EffectiveLevel", effectiveLevel);
			tag.putLong("CreatedGameTime", createdGameTime);
			tag.putBoolean("Completed", completed);

			ListTag participantList = new ListTag();
			for (UUID participant : participants) {
				CompoundTag entry = new CompoundTag();
				entry.putUUID("Id", participant);
				participantList.add(entry);
			}
			tag.put("Participants", participantList);
			if (playerStart != null)
				tag.put("PlayerStart", NbtUtils.writeBlockPos(playerStart));
			if (exit != null)
				tag.put("Exit", NbtUtils.writeBlockPos(exit));
			if (exitFacing != null)
				tag.putString("ExitFacing", exitFacing.getName());

			ListTag roomList = new ListTag();
			rooms.forEach(room -> roomList.add(room.save()));
			tag.put("Rooms", roomList);
			ListTag connectionList = new ListTag();
			carvedConnections.forEach(bounds -> connectionList.add(bounds.save()));
			tag.put("Connections", connectionList);
			ListTag occupiedList = new ListTag();
			occupiedVolumes.forEach(bounds -> occupiedList.add(bounds.save()));
			tag.put("OccupiedVolumes", occupiedList);
			ListTag encounterList = new ListTag();
			encounters.values().forEach(encounter -> encounterList.add(encounter.save()));
			tag.put("Encounters", encounterList);
			return tag;
		}

		@Nullable
		private static Instance load(CompoundTag tag, Runnable dirty, LoadBudget budget) {
			if (!tag.hasUUID("Id")) {
				budget.sanitized = true;
				return null;
			}
			UUID id = tag.getUUID("Id");
			ResourceLocation dungeonId = parseResourceId(tag.getString("DungeonId"));
			ResourceLocation dimensionId = parseResourceId(tag.getString("Dimension"));
			if (dungeonId == null || dimensionId == null) {
				budget.sanitized = true;
				return null;
			}
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
			Instance instance = new Instance(id, dungeonId, dimension, tag.getLong("Seed"),
					clampLevel(tag.getInt("EffectiveLevel")), Math.max(0L, tag.getLong("CreatedGameTime")), dirty);
			instance.completed = tag.getBoolean("Completed");

			ListTag participantList = tag.getList("Participants", Tag.TAG_COMPOUND);
			int participantLimit = Math.min(participantList.size(), MAX_PARTICIPANTS);
			if (participantList.size() > participantLimit)
				budget.sanitized = true;
			for (int index = 0; index < participantLimit && budget.takeParticipant(); index++) {
				CompoundTag entry = participantList.getCompound(index);
				if (entry.hasUUID("Id"))
					instance.participants.add(entry.getUUID("Id"));
				else
					budget.sanitized = true;
			}
			instance.playerStart = readPosition(tag, "PlayerStart", budget);
			instance.exit = readPosition(tag, "Exit", budget);
			Direction savedExitFacing = Direction.byName(tag.getString("ExitFacing"));
			if (savedExitFacing != null && savedExitFacing.getAxis().isHorizontal())
				instance.exitFacing = savedExitFacing;

			ListTag roomList = tag.getList("Rooms", Tag.TAG_COMPOUND);
			int roomLimit = Math.min(roomList.size(), MAX_ROOMS);
			if (roomList.size() > roomLimit)
				budget.sanitized = true;
			for (int index = 0; index < roomLimit && budget.takeRoom(); index++) {
				PlacedRoom room = PlacedRoom.load(roomList.getCompound(index), budget);
				if (room != null)
					instance.rooms.add(room);
				else
					budget.sanitized = true;
			}

			ListTag connectionList = tag.getList("Connections", Tag.TAG_COMPOUND);
			int connectionLimit = Math.min(connectionList.size(), MAX_CONNECTIONS);
			if (connectionList.size() > connectionLimit)
				budget.sanitized = true;
			for (int index = 0; index < connectionLimit && budget.takeConnection(); index++) {
				Bounds bounds = Bounds.load(connectionList.getCompound(index), budget);
				if (bounds != null && !instance.carvedConnections.contains(bounds))
					instance.carvedConnections.add(bounds);
				else
					budget.sanitized = true;
			}

			ListTag occupiedList = tag.getList("OccupiedVolumes", Tag.TAG_COMPOUND);
			int occupiedLimit = Math.min(occupiedList.size(), MAX_OCCUPIED_VOLUMES);
			if (occupiedList.size() > occupiedLimit)
				budget.sanitized = true;
			for (int index = 0; index < occupiedLimit && budget.takeOccupiedVolume(); index++) {
				Bounds bounds = Bounds.load(occupiedList.getCompound(index), budget);
				if (bounds != null && !instance.occupiedVolumes.contains(bounds))
					instance.occupiedVolumes.add(bounds);
				else
					budget.sanitized = true;
			}

			ListTag encounterList = tag.getList("Encounters", Tag.TAG_COMPOUND);
			int encounterLimit = Math.min(encounterList.size(), MAX_ENCOUNTERS);
			if (encounterList.size() > encounterLimit)
				budget.sanitized = true;
			for (int index = 0; index < encounterLimit && budget.takeEncounter(); index++) {
				EncounterState encounter = EncounterState.load(encounterList.getCompound(index), dirty, budget);
				boolean duplicateSequenceOrder = encounter != null && encounter.sequenced()
						&& instance.encounters.values().stream().anyMatch(existing -> existing.sequenced()
								&& existing.sequenceKey().equals(encounter.sequenceKey())
								&& existing.sequenceOrder() == encounter.sequenceOrder());
				if (encounter != null && !duplicateSequenceOrder
						&& instance.encounters.putIfAbsent(encounter.key, encounter) == null)
					continue;
				budget.sanitized = true;
			}
			return instance;
		}
	}

	/** Mutable encounter lifecycle state owned by one instance. */
	public static final class EncounterState {
		private final String key;
		private final ResourceLocation poolId;
		private final boolean boss;
		private final boolean levelOverride;
		private final int minSpawnLevel;
		private final int maxSpawnLevel;
		private final String sequenceKey;
		private final int sequenceOrder;
		private final int delayTicks;
		private final Runnable dirty;
		private final List<EncounterMarker> markers = new ArrayList<>();
		@Nullable
		private Bounds triggerBounds;
		private boolean activated;
		private boolean completed;
		private long scheduledGameTime = -1L;
		private final Set<UUID> trackedMobs = new LinkedHashSet<>();

		private EncounterState(String key, ResourceLocation poolId, boolean boss, boolean levelOverride, int minSpawnLevel,
				int maxSpawnLevel, String sequenceKey, int sequenceOrder, int delayTicks, Runnable dirty) {
			this.key = key;
			this.poolId = poolId;
			this.boss = boss;
			this.levelOverride = levelOverride;
			this.minSpawnLevel = minSpawnLevel;
			this.maxSpawnLevel = maxSpawnLevel;
			this.sequenceKey = sequenceKey;
			this.sequenceOrder = sequenceOrder;
			this.delayTicks = delayTicks;
			this.dirty = dirty;
		}

		public String key() {
			return key;
		}

		public ResourceLocation poolId() {
			return poolId;
		}

		public boolean boss() {
			return boss;
		}

		public boolean levelOverride() {
			return levelOverride;
		}

		public int minSpawnLevel() {
			return minSpawnLevel;
		}

		public int maxSpawnLevel() {
			return maxSpawnLevel;
		}

		public boolean sequenced() {
			return !sequenceKey.isBlank();
		}

		/** Empty for schema-v1/v2 encounters, which remain independently activated. */
		public String sequenceKey() {
			return sequenceKey;
		}

		/** Zero-based order within sequenceKey, or -1 for an unsequenced legacy encounter. */
		public int sequenceOrder() {
			return sequenceOrder;
		}

		public int delayTicks() {
			return delayTicks;
		}

		/** Persisted activation deadline, or -1 until this wave's trigger/prerequisite is latched. */
		public long scheduledGameTime() {
			return scheduledGameTime;
		}

		public boolean scheduleActivation(long gameTime) {
			if (!sequenced() || activated || completed || scheduledGameTime >= 0L)
				return false;
			scheduledGameTime = Math.max(0L, gameTime);
			dirty.run();
			return true;
		}

		public List<EncounterMarker> markers() {
			return List.copyOf(markers);
		}

		public boolean addMarker(EncounterMarker marker) {
			if (!validMarker(marker) || markers.size() >= MAX_MARKERS_PER_ENCOUNTER
					|| markers.stream().anyMatch(existing -> existing.id.equals(marker.id)))
				return false;
			markers.add(marker);
			dirty.run();
			return true;
		}

		public boolean addMarker(String id, String role, BlockPos position) {
			if (id == null || role == null || position == null)
				return false;
			return addMarker(new EncounterMarker(id, role, position));
		}

		public boolean removeMarker(String id) {
			boolean removed = markers.removeIf(marker -> marker.id.equals(id));
			if (removed)
				dirty.run();
			return removed;
		}

		public Optional<Bounds> triggerBounds() {
			return Optional.ofNullable(triggerBounds);
		}

		public boolean setTriggerBounds(@Nullable Bounds bounds) {
			if (bounds != null && !bounds.isSafe())
				return false;
			if (java.util.Objects.equals(triggerBounds, bounds))
				return false;
			triggerBounds = bounds;
			dirty.run();
			return true;
		}

		public boolean activated() {
			return activated;
		}

		public boolean activate() {
			if (activated)
				return false;
			activated = true;
			dirty.run();
			return true;
		}

		public boolean completed() {
			return completed;
		}

		public boolean markCompleted() {
			if (completed)
				return false;
			activated = true;
			completed = true;
			dirty.run();
			return true;
		}

		public boolean resetProgress() {
			if (!activated && !completed && trackedMobs.isEmpty())
				return false;
			activated = false;
			completed = false;
			trackedMobs.clear();
			dirty.run();
			return true;
		}

		public Set<UUID> trackedMobs() {
			return Set.copyOf(trackedMobs);
		}

		public boolean trackMob(UUID mobId) {
			if (mobId == null || trackedMobs.size() >= MAX_TRACKED_MOBS_PER_ENCOUNTER
					|| !trackedMobs.add(mobId))
				return false;
			dirty.run();
			return true;
		}

		public boolean untrackMob(UUID mobId) {
			if (mobId == null || !trackedMobs.remove(mobId))
				return false;
			dirty.run();
			return true;
		}

		public boolean clearTrackedMobs() {
			if (trackedMobs.isEmpty())
				return false;
			trackedMobs.clear();
			dirty.run();
			return true;
		}

		public EncounterView view() {
			return new EncounterView(key, poolId, boss, levelOverride, minSpawnLevel, maxSpawnLevel,
					sequenceKey, sequenceOrder, delayTicks, scheduledGameTime, List.copyOf(markers),
					triggerBounds, activated, completed, Set.copyOf(trackedMobs));
		}

		private static boolean validMarker(@Nullable EncounterMarker marker) {
			return marker != null && safeKey(marker.id, MAX_MARKER_TEXT_LENGTH)
					&& safeKey(marker.role, MAX_MARKER_TEXT_LENGTH) && safePosition(marker.position);
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Key", key);
			tag.putString("Pool", poolId.toString());
			tag.putBoolean("Boss", boss);
			tag.putBoolean("LevelOverride", levelOverride);
			tag.putInt("MinSpawnLevel", minSpawnLevel);
			tag.putInt("MaxSpawnLevel", maxSpawnLevel);
			if (sequenced()) {
				tag.putString("SequenceKey", sequenceKey);
				tag.putInt("SequenceOrder", sequenceOrder);
				tag.putInt("DelayTicks", delayTicks);
				if (scheduledGameTime >= 0L)
					tag.putLong("ScheduledGameTime", scheduledGameTime);
			}
			tag.putBoolean("Activated", activated);
			tag.putBoolean("Completed", completed);
			if (triggerBounds != null)
				tag.put("TriggerBounds", triggerBounds.save());
			ListTag markerList = new ListTag();
			markers.forEach(marker -> markerList.add(marker.save()));
			tag.put("Markers", markerList);
			ListTag mobList = new ListTag();
			for (UUID mobId : trackedMobs) {
				CompoundTag entry = new CompoundTag();
				entry.putUUID("Id", mobId);
				mobList.add(entry);
			}
			tag.put("TrackedMobs", mobList);
			return tag;
		}

		@Nullable
		private static EncounterState load(CompoundTag tag, Runnable dirty, LoadBudget budget) {
			String key = tag.getString("Key");
			ResourceLocation poolId = parseResourceId(tag.getString("Pool"));
			if (!safeKey(key, MAX_KEY_LENGTH) || poolId == null) {
				budget.sanitized = true;
				return null;
			}
			int firstLevel = clampLevel(tag.getInt("MinSpawnLevel"));
			int secondLevel = clampLevel(tag.getInt("MaxSpawnLevel"));
			boolean levelOverride = !tag.contains("LevelOverride", Tag.TAG_BYTE) || tag.getBoolean("LevelOverride");
			String sequenceKey = tag.getString("SequenceKey");
			int sequenceOrder = -1;
			int delayTicks = 0;
			if (!sequenceKey.isBlank()) {
				if (!safeKey(sequenceKey, MAX_KEY_LENGTH)) {
					budget.sanitized = true;
					return null;
				}
				sequenceOrder = tag.getInt("SequenceOrder");
				delayTicks = tag.getInt("DelayTicks");
				if (sequenceOrder < 0 || sequenceOrder >= MAX_ENCOUNTERS
						|| delayTicks < 0 || delayTicks > MAX_WAVE_DELAY_TICKS) {
					budget.sanitized = true;
					return null;
				}
			}
			EncounterState encounter = new EncounterState(key, poolId, tag.getBoolean("Boss"), levelOverride,
					Math.min(firstLevel, secondLevel), Math.max(firstLevel, secondLevel),
					sequenceKey, sequenceOrder, delayTicks, dirty);
			encounter.activated = tag.getBoolean("Activated");
			encounter.completed = tag.getBoolean("Completed");
			if (!sequenceKey.isBlank() && tag.contains("ScheduledGameTime", Tag.TAG_LONG))
				encounter.scheduledGameTime = Math.max(0L, tag.getLong("ScheduledGameTime"));
			if (encounter.completed)
				encounter.activated = true;
			if (tag.contains("TriggerBounds", Tag.TAG_COMPOUND))
				encounter.triggerBounds = Bounds.load(tag.getCompound("TriggerBounds"), budget);

			ListTag markerList = tag.getList("Markers", Tag.TAG_COMPOUND);
			int markerLimit = Math.min(markerList.size(), MAX_MARKERS_PER_ENCOUNTER);
			if (markerList.size() > markerLimit)
				budget.sanitized = true;
			for (int index = 0; index < markerLimit && budget.takeMarker(); index++) {
				EncounterMarker marker = EncounterMarker.load(markerList.getCompound(index), budget);
				if (marker != null && encounter.markers.stream().noneMatch(existing -> existing.id.equals(marker.id)))
					encounter.markers.add(marker);
				else
					budget.sanitized = true;
			}

			ListTag mobList = tag.getList("TrackedMobs", Tag.TAG_COMPOUND);
			int mobLimit = Math.min(mobList.size(), MAX_TRACKED_MOBS_PER_ENCOUNTER);
			if (mobList.size() > mobLimit)
				budget.sanitized = true;
			for (int index = 0; index < mobLimit && budget.takeTrackedMob(); index++) {
				CompoundTag entry = mobList.getCompound(index);
				if (entry.hasUUID("Id"))
					encounter.trackedMobs.add(entry.getUUID("Id"));
				else
					budget.sanitized = true;
			}
			return encounter;
		}
	}

	/** Global deserialization budget in addition to per-instance caps. */
	private static final class LoadBudget {
		private int participants;
		private int rooms;
		private int connections;
		private int occupiedVolumes;
		private int encounters;
		private int markers;
		private int trackedMobs;
		private boolean sanitized;

		private boolean takeParticipant() {
			return take(++participants, MAX_TOTAL_PARTICIPANTS_ON_LOAD);
		}

		private boolean takeRoom() {
			return take(++rooms, MAX_TOTAL_ROOMS_ON_LOAD);
		}

		private boolean takeConnection() {
			return take(++connections, MAX_TOTAL_CONNECTIONS_ON_LOAD);
		}

		private boolean takeOccupiedVolume() {
			return take(++occupiedVolumes, MAX_TOTAL_OCCUPIED_VOLUMES_ON_LOAD);
		}

		private boolean takeEncounter() {
			return take(++encounters, MAX_TOTAL_ENCOUNTERS_ON_LOAD);
		}

		private boolean takeMarker() {
			return take(++markers, MAX_TOTAL_MARKERS_ON_LOAD);
		}

		private boolean takeTrackedMob() {
			return take(++trackedMobs, MAX_TOTAL_TRACKED_MOBS_ON_LOAD);
		}

		private boolean take(int used, int maximum) {
			if (used <= maximum)
				return true;
			sanitized = true;
			return false;
		}
	}
}
