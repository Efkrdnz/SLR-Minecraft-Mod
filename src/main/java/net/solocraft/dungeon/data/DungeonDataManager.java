package net.solocraft.dungeon.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.ProceduralDungeonRank;

import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;

/**
 * Owns the immutable server datapack view for addon-defined dungeons.
 *
 * <p>Every reload is parsed and cross-validated off to the side. The live view
 * changes with one volatile assignment, so runtime code can never observe half
 * of a reload.</p>
 */
public final class DungeonDataManager {
	private static final int MAX_DUNGEON_LEVEL = 1_000;
	private static final int MAX_BASE_XP = 1_000_000;
	private static final int MAX_PROCEDURAL_ROOMS = 64;
	private static final int MAX_MARKERS_PER_WAVE = 256;
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	private static final String ROOM_DIRECTORY = "slr/rooms";
	private static final String DUNGEON_DIRECTORY = "slr/dungeons";
	private static final String POOL_DIRECTORY = "slr/mob_pools";
	private static final String MODIFIER_DIRECTORY = "slr/pool_modifiers";
	private static final String LEGACY_MODIFIER_DIRECTORY = "slr/mob_pool_modifiers";
	private static final ResourceLocation BEDROCK_ID = new ResourceLocation("minecraft", "bedrock");
	private static volatile DungeonDataSnapshot current = DungeonDataSnapshot.empty();

	private DungeonDataManager() {
	}

	public static DungeonDataSnapshot snapshot() {
		return current;
	}

	public static Optional<DungeonRoomDefinition> room(ResourceLocation id) {
		return current.room(id);
	}

	public static Optional<DungeonDefinition> dungeon(ResourceLocation id) {
		return current.dungeon(id);
	}

	public static Optional<MobPoolDefinition> mobPool(ResourceLocation id) {
		return current.mobPool(id);
	}

	public static List<ResourceLocation> roomIds() {
		return current.roomIds();
	}

	public static List<ResourceLocation> dungeonIds() {
		return current.dungeonIds();
	}

	public static List<ResourceLocation> mobPoolIds() {
		return current.mobPoolIds();
	}

	static PreparableReloadListener reloadListener(ICondition.IContext conditionContext) {
		return new Loader(conditionContext);
	}

	static void clear() {
		current = DungeonDataSnapshot.empty();
	}

	private static final class Loader extends SimplePreparableReloadListener<DungeonDataSnapshot> {
		private final ICondition.IContext conditionContext;

		private Loader(ICondition.IContext conditionContext) {
			this.conditionContext = conditionContext;
		}

		@Override
		protected DungeonDataSnapshot prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
			profiler.push("sololeveling_dungeon_data");
			List<ValidationIssue> issues = new ArrayList<>();
			try {
				Map<ResourceLocation, JsonElement> poolJson = scan(resourceManager, POOL_DIRECTORY);
				Map<ResourceLocation, JsonElement> roomJson = scan(resourceManager, ROOM_DIRECTORY);
				Map<ResourceLocation, JsonElement> dungeonJson = scan(resourceManager, DUNGEON_DIRECTORY);
				Map<ResourceLocation, JsonElement> modifierJson = modifiers(resourceManager, issues);

				Map<ResourceLocation, MobPoolDefinition> pools = parseResources(poolJson,
						(id, element) -> parsePool(id, element, conditionContext, issues), issues);
				List<MobPoolModifier> modifiers = parseResources(modifierJson,
						(id, element) -> parseModifier(id, element, conditionContext, issues), issues)
						.values().stream().sorted(Comparator.comparing(modifier -> modifier.id().toString())).toList();
				pools = applyModifiers(pools, modifiers, issues);

				Map<ResourceLocation, DungeonRoomDefinition> rooms = parseResources(roomJson,
						(id, element) -> parseRoom(id, element, conditionContext), issues);
				Map<ResourceLocation, DungeonDefinition> dungeons = parseResources(dungeonJson,
						(id, element) -> parseDungeon(id, element, conditionContext), issues);

				pools.values().forEach(pool -> {
					if (pool.entries().isEmpty())
						issues.add(warning(pool.id(), "Mob pool is empty."));
				});
				rooms = validatedRooms(rooms, pools, issues);
				dungeons = validatedDungeons(dungeons, rooms, pools, issues);
				return new DungeonDataSnapshot(0, rooms, dungeons, pools, issues);
			} finally {
				profiler.pop();
			}
		}

		@Override
		protected void apply(DungeonDataSnapshot prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
			DungeonDataSnapshot next = prepared.withRevision(current.revision() + 1L);
			current = next;
			long errors = next.issues().stream().filter(issue -> issue.severity() == Severity.ERROR).count();
			long warnings = next.issues().size() - errors;
			SololevelingMod.LOGGER.info("Loaded SLR dungeon data revision {}: {} rooms, {} dungeons, {} mob pools ({} errors, {} warnings)",
					next.revision(), next.rooms().size(), next.dungeons().size(), next.mobPools().size(), errors, warnings);
			for (ValidationIssue issue : next.issues()) {
				String message = "[SLR dungeon data] " + issue.resource() + ": " + issue.message();
				if (issue.severity() == Severity.ERROR)
					SololevelingMod.LOGGER.error(message);
				else
					SololevelingMod.LOGGER.warn(message);
			}
		}
	}

	private static Map<ResourceLocation, JsonElement> scan(ResourceManager manager, String directory) {
		Map<ResourceLocation, JsonElement> values = new HashMap<>();
		SimpleJsonResourceReloadListener.scanDirectory(manager, directory, GSON, values);
		return values;
	}

	private static Map<ResourceLocation, JsonElement> modifiers(ResourceManager manager,
			List<ValidationIssue> issues) {
		Map<ResourceLocation, JsonElement> legacy = scan(manager, LEGACY_MODIFIER_DIRECTORY);
		Map<ResourceLocation, JsonElement> canonical = scan(manager, MODIFIER_DIRECTORY);
		Map<ResourceLocation, JsonElement> combined = new HashMap<>(legacy);
		for (Map.Entry<ResourceLocation, JsonElement> entry : canonical.entrySet()) {
			if (combined.put(entry.getKey(), entry.getValue()) != null)
				issues.add(warning(entry.getKey(), "Modifier exists in both slr/pool_modifiers and the legacy slr/mob_pool_modifiers directory; the canonical file won."));
		}
		return combined;
	}

	@FunctionalInterface
	private interface ResourceParser<T> {
		Optional<T> parse(ResourceLocation id, JsonElement json);
	}

	private static <T> Map<ResourceLocation, T> parseResources(Map<ResourceLocation, JsonElement> resources,
			ResourceParser<T> parser, List<ValidationIssue> issues) {
		Map<ResourceLocation, T> parsed = new LinkedHashMap<>();
		resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
				.forEach(entry -> {
					try {
						parser.parse(entry.getKey(), entry.getValue()).ifPresent(value -> parsed.put(entry.getKey(), value));
					} catch (RuntimeException exception) {
						issues.add(error(entry.getKey(), cleanMessage(exception)));
					}
				});
		return parsed;
	}

	private static Optional<MobPoolDefinition> parsePool(ResourceLocation id, JsonElement element,
			ICondition.IContext conditions, List<ValidationIssue> issues) {
		JsonObject json = object(element, "mob pool");
		if (!conditionsApply(json, conditions))
			return Optional.empty();
		int format = formatVersion(json);
		JsonArray entriesJson = array(json, "entries", new JsonArray());
		List<MobPoolEntry> entries = new ArrayList<>();
		for (JsonElement entryElement : entriesJson) {
			JsonObject entryJson = object(entryElement, "mob pool entry");
			parsePoolEntry(id, entryJson, conditions, issues).ifPresent(entries::add);
		}
		return Optional.of(new MobPoolDefinition(id, format, entries));
	}

	private static Optional<MobPoolModifier> parseModifier(ResourceLocation id, JsonElement element,
			ICondition.IContext conditions, List<ValidationIssue> issues) {
		JsonObject json = object(element, "mob pool modifier");
		if (!conditionsApply(json, conditions))
			return Optional.empty();
		formatVersion(json);
		ResourceLocation target = internalId(requiredString(json, "target"), id.getNamespace());
		String operationName = string(json, "operation", "add").toLowerCase(Locale.ROOT);
		ModifierOperation operation = switch (operationName) {
			case "add", "append" -> ModifierOperation.ADD;
			case "remove" -> ModifierOperation.REMOVE;
			default -> throw new JsonParseException("operation must be add or remove");
		};

		List<MobPoolEntry> entries = new ArrayList<>();
		List<EntitySelector> selectors = new ArrayList<>();
		JsonArray values = array(json, operation == ModifierOperation.ADD ? "entries" : "selectors", null);
		if (values == null && operation == ModifierOperation.REMOVE)
			values = array(json, "remove", null);
		if (values == null && operation == ModifierOperation.REMOVE)
			values = array(json, "entries", new JsonArray());
		if (values == null)
			values = new JsonArray();

		for (JsonElement value : values) {
			if (operation == ModifierOperation.ADD) {
				JsonObject entryJson = object(value, "modifier entry");
				parsePoolEntry(id, entryJson, conditions, issues).ifPresent(entries::add);
			} else {
				selectors.add(parseSelector(value, id.getNamespace()));
			}
		}
		if (operation == ModifierOperation.ADD && entries.isEmpty())
			issues.add(warning(id, "Add modifier has no active entries."));
		if (operation == ModifierOperation.REMOVE && selectors.isEmpty())
			issues.add(warning(id, "Remove modifier has no selectors."));
		return Optional.of(new MobPoolModifier(id, target, operation, entries, selectors));
	}

	private static Optional<MobPoolEntry> parsePoolEntry(ResourceLocation owner, JsonObject json,
			ICondition.IContext conditions, List<ValidationIssue> issues) {
		if (!conditionsApply(json, conditions))
			return Optional.empty();
		Optional<String> requiredMod = optionalString(json, "required_mod");
		if (requiredMod.isPresent()) {
			String modId = requiredMod.get();
			if (!modId.matches("[a-z][a-z0-9_-]{1,63}"))
				throw new JsonParseException("required_mod is not a valid mod id: " + modId);
			if (!ModList.get().isLoaded(modId))
				return Optional.empty();
		}
		EntitySelector selector = parseSelector(json, owner.getNamespace());
		if (selector.kind() == SelectorKind.ENTITY && !ForgeRegistries.ENTITY_TYPES.containsKey(selector.id())) {
			issues.add(warning(owner, "Skipped missing entity " + selector.id()
					+ requiredMod.map(mod -> " although required_mod " + mod + " is loaded").orElse("") + "."));
			return Optional.empty();
		}
		if (selector.kind() == SelectorKind.ENTITY
				&& ForgeRegistries.ENTITY_TYPES.getValue(selector.id()) == EntityType.PLAYER) {
			issues.add(warning(owner, "Skipped minecraft:player because it cannot be spawned as a dungeon mob."));
			return Optional.empty();
		}
		int weight = boundedInt(json, "weight", 1, 1, 1_000_000);
		Optional<IntRange> eligible = optionalRange(json, "eligible_level", 0, MAX_DUNGEON_LEVEL);
		Optional<IntRange> spawn = optionalRange(json, "spawn_level", 1, MAX_DUNGEON_LEVEL);
		if (spawn.isEmpty())
			spawn = optionalRange(json, "level_range", 1, MAX_DUNGEON_LEVEL);
		if (json.has("xp") && json.has("base_xp"))
			throw new JsonParseException("Mob pool entries must use either xp or base_xp, not both");
		Optional<Integer> baseXp = json.has("xp")
				? Optional.of(boundedInt(json, "xp", 0, 0, MAX_BASE_XP))
				: json.has("base_xp")
						? Optional.of(boundedInt(json, "base_xp", 0, 0, MAX_BASE_XP))
						: Optional.empty();
		return Optional.of(new MobPoolEntry(selector, weight, requiredMod, eligible, spawn, baseXp));
	}

	private static Map<ResourceLocation, MobPoolDefinition> applyModifiers(
			Map<ResourceLocation, MobPoolDefinition> original, List<MobPoolModifier> modifiers,
			List<ValidationIssue> issues) {
		Map<ResourceLocation, MobPoolDefinition> result = new LinkedHashMap<>(original);
		for (MobPoolModifier modifier : modifiers) {
			MobPoolDefinition target = result.get(modifier.target());
			if (target == null) {
				issues.add(error(modifier.id(), "Target mob pool " + modifier.target() + " does not exist."));
				continue;
			}
			List<MobPoolEntry> entries = new ArrayList<>(target.entries());
			if (modifier.operation() == ModifierOperation.ADD) {
				entries.addAll(modifier.entries());
			} else {
				Set<String> removedKeys = modifier.selectors().stream().map(EntitySelector::key)
						.collect(java.util.stream.Collectors.toSet());
				entries.removeIf(entry -> removedKeys.contains(entry.selector().key()));
			}
			result.put(target.id(), new MobPoolDefinition(target.id(), target.formatVersion(), entries));
		}
		return result;
	}

	private static Optional<DungeonRoomDefinition> parseRoom(ResourceLocation id, JsonElement element,
			ICondition.IContext conditions) {
		JsonObject json = object(element, "room");
		if (!conditionsApply(json, conditions))
			return Optional.empty();
		int format = formatVersion(json);
		ResourceLocation structure = internalId(requiredString(json, "structure"), id.getNamespace());
		Int3 size = vector(required(json, "size"), "size");
		Int3 origin = json.has("origin") ? vector(json.get("origin"), "origin") : new Int3(0, 0, 0);
		Optional<ResourceLocation> defaultPool = optionalInternalId(json, "default_mob_pool", id.getNamespace());
		List<Region> regions = parseRegions(json);
		List<Socket> sockets = parseSockets(json);
		List<Marker> markers = parseMarkers(json);
		String roleName = string(json, "role", "");
		RoomRole role = roleName.isBlank() ? inferredRole(markers) : strictRoomRole(roleName, "room role");
		int weight = boundedInt(json, "weight", 1, 1, 1_000_000);
		Optional<ShellSettings> shell = json.has("shell")
				? Optional.of(parseShell(json.get("shell"), id)) : Optional.empty();
		List<Encounter> encounters = parseEncounters(json, id, defaultPool, Optional.empty());
		return Optional.of(new DungeonRoomDefinition(id, format, structure, role, weight, size, origin,
				defaultPool, shell, regions, sockets, markers, encounters));
	}

	private static Optional<DungeonDefinition> parseDungeon(ResourceLocation id, JsonElement element,
			ICondition.IContext conditions) {
		JsonObject json = object(element, "dungeon");
		if (!conditionsApply(json, conditions))
			return Optional.empty();
		int format = formatVersion(json);
		String type = string(json, "generation", string(json, "type", "preset"));
		DungeonKind kind = strictDungeonKind(type);
		DungeonTopology topology = parseDungeonTopology(json, kind);
		Set<ProceduralDungeonRank> allowedRanks = parseAllowedRanks(json);
		Optional<ResourceLocation> structure = optionalInternalId(json, "structure", id.getNamespace());
		Optional<Int3> size = json.has("size") ? Optional.of(vector(json.get("size"), "size")) : Optional.empty();
		Optional<Int3> origin = json.has("origin") ? Optional.of(vector(json.get("origin"), "origin")) : Optional.empty();
		Optional<ResourceLocation> defaultPool = optionalInternalId(json, "default_mob_pool", id.getNamespace());
		Optional<ResourceLocation> bossPool = optionalInternalId(json, "boss_mob_pool", id.getNamespace());
		Map<RoomRole, List<WeightedRoom>> roomPools = parseRoomPools(json, id);
		List<FixedRoomPlacement> fixedPlacements = parseFixedPlacements(json, id);
		List<FixedRoomConnection> fixedConnections = parseFixedConnections(json);
		IntRange roomCount = kind == DungeonKind.PRESET ? new IntRange(1, 1)
				: kind == DungeonKind.FIXED ? new IntRange(Math.max(1, fixedPlacements.size()), Math.max(1, fixedPlacements.size()))
				: parseRoomCount(json);
		int maxDepth = boundedInt(json, "max_depth", roomCount.max(), 1, MAX_PROCEDURAL_ROOMS);
		LevelRule level = parseLevelRule(json);
		ShellSettings shell = json.has("shell") ? parseShell(json.get("shell"), id)
				: new ShellSettings(true, BEDROCK_ID, 1, true, true);
		List<Region> regions = parseRegions(json);
		List<Socket> sockets = parseSockets(json);
		List<Marker> markers = parseMarkers(json);
		List<Encounter> encounters = parseEncounters(json, id, defaultPool, bossPool);
		return Optional.of(new DungeonDefinition(id, format, kind, allowedRanks, structure, size, origin,
				defaultPool, bossPool, roomPools, roomCount, maxDepth, level, shell,
				regions, sockets, markers, encounters, fixedPlacements, fixedConnections, topology));
	}

	/**
	 * Missing rank metadata intentionally means every rank so schema-v1/v2 addon
	 * packs made before rank routing was introduced continue to work.
	 */
	private static Set<ProceduralDungeonRank> parseAllowedRanks(JsonObject json) {
		if (json.has("rank") && json.has("ranks"))
			throw new JsonParseException("Use either rank or ranks, not both");
		if (!json.has("rank") && !json.has("ranks"))
			return EnumSet.allOf(ProceduralDungeonRank.class);

		JsonElement element = json.has("ranks") ? json.get("ranks") : json.get("rank");
		JsonArray values = element.isJsonArray() ? element.getAsJsonArray() : singletonArray(element);
		if (values.isEmpty())
			throw new JsonParseException("ranks must contain at least one of E, D, C, B, A, or S");
		EnumSet<ProceduralDungeonRank> result = EnumSet.noneOf(ProceduralDungeonRank.class);
		for (JsonElement value : values) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
				throw new JsonParseException("Dungeon ranks must be strings such as \"A\"");
			String name = value.getAsString();
			if (name.equalsIgnoreCase("all")) {
				if (values.size() != 1)
					throw new JsonParseException("The rank value all cannot be combined with individual ranks");
				return EnumSet.allOf(ProceduralDungeonRank.class);
			}
			ProceduralDungeonRank rank = ProceduralDungeonRank.tryParse(name)
					.orElseThrow(() -> new JsonParseException("Unknown dungeon rank " + name
							+ ". Expected E, D, C, B, A, or S"));
			if (!result.add(rank))
				throw new JsonParseException("Duplicate dungeon rank " + rank.name());
		}
		return result;
	}

	private static Map<RoomRole, List<WeightedRoom>> parseRoomPools(JsonObject json, ResourceLocation owner) {
		EnumMap<RoomRole, List<WeightedRoom>> result = new EnumMap<>(RoomRole.class);
		JsonObject pools = null;
		if (json.has("room_pools"))
			pools = object(json.get("room_pools"), "room_pools");
		else if (json.has("rooms") && json.get("rooms").isJsonObject())
			pools = json.getAsJsonObject("rooms");
		if (pools != null) {
			for (Map.Entry<String, JsonElement> entry : pools.entrySet()) {
				RoomRole role = strictRoomRole(entry.getKey(), "room pool role");
				result.computeIfAbsent(role, ignored -> new ArrayList<>())
						.addAll(parseWeightedRooms(entry.getValue(), owner));
			}
		}

		Map<RoomRole, List<String>> aliases = Map.of(
				RoomRole.START, List.of("start_room", "start_rooms"),
				RoomRole.NORMAL, List.of("room_pool", "normal_rooms"),
				RoomRole.BOSS, List.of("boss_room", "boss_rooms"),
				RoomRole.CAP, List.of("cap_rooms"),
				RoomRole.TREASURE, List.of("treasure_rooms"),
				RoomRole.JUNCTION, List.of("junction_rooms"),
				RoomRole.DEAD_END, List.of("dead_end_rooms"),
				RoomRole.CORRIDOR, List.of("corridor_rooms"),
				RoomRole.STAIR, List.of("stair_rooms"));
		aliases.forEach((role, keys) -> keys.forEach(key -> {
			if (json.has(key))
				result.computeIfAbsent(role, ignored -> new ArrayList<>())
						.addAll(parseWeightedRooms(json.get(key), owner));
		}));
		return result;
	}

	private static List<WeightedRoom> parseWeightedRooms(JsonElement element, ResourceLocation owner) {
		JsonArray values = element.isJsonArray() ? element.getAsJsonArray() : singletonArray(element);
		List<WeightedRoom> result = new ArrayList<>();
		for (JsonElement value : values) {
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				result.add(new WeightedRoom(internalId(value.getAsString(), owner.getNamespace()), 1));
				continue;
			}
			JsonObject room = object(value, "weighted room");
			String roomId = room.has("room") ? requiredString(room, "room") : requiredString(room, "id");
			result.add(new WeightedRoom(internalId(roomId, owner.getNamespace()),
					boundedInt(room, "weight", 1, 1, 1_000_000)));
		}
		return result;
	}

	private static List<FixedRoomPlacement> parseFixedPlacements(JsonObject owner, ResourceLocation resource) {
		JsonArray values = array(owner, "placements", new JsonArray());
		List<FixedRoomPlacement> result = new ArrayList<>();
		for (JsonElement value : values) {
			JsonObject json = object(value, "fixed placement");
			String id = localId(requiredString(json, "id"), "fixed placement id");
			ResourceLocation room = internalId(requiredString(json, "room"), resource.getNamespace());
			Int3 position = vector(required(json, "position"), "fixed placement position");
			Rotation rotation = fixedRotation(required(json, "rotation"));
			result.add(new FixedRoomPlacement(id, room, position, rotation));
		}
		return result;
	}

	private static List<FixedRoomConnection> parseFixedConnections(JsonObject owner) {
		JsonArray values = array(owner, "connections", new JsonArray());
		List<FixedRoomConnection> result = new ArrayList<>();
		for (JsonElement value : values) {
			JsonObject json = object(value, "fixed connection");
			JsonObject from = object(required(json, "from"), "fixed connection from");
			JsonObject to = object(required(json, "to"), "fixed connection to");
			result.add(new FixedRoomConnection(
					localId(requiredString(from, "room"), "fixed connection room id"),
					localId(requiredString(from, "socket"), "fixed connection socket id"),
					localId(requiredString(to, "room"), "fixed connection room id"),
					localId(requiredString(to, "socket"), "fixed connection socket id")));
		}
		return result;
	}

	private static Rotation fixedRotation(JsonElement element) {
		String value;
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber())
			value = Integer.toString(element.getAsInt());
		else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
			value = element.getAsString().toLowerCase(Locale.ROOT);
		else
			throw new JsonParseException("fixed placement rotation must be 0, 90, 180, or 270");
		return switch (value) {
			case "0", "none" -> Rotation.NONE;
			case "90", "clockwise_90" -> Rotation.CLOCKWISE_90;
			case "180", "clockwise_180" -> Rotation.CLOCKWISE_180;
			case "270", "-90", "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
			default -> throw new JsonParseException("fixed placement rotation must be 0, 90, 180, or 270");
		};
	}

	private static List<Region> parseRegions(JsonObject owner) {
		JsonArray values = array(owner, "regions", new JsonArray());
		List<Region> result = new ArrayList<>();
		int index = 0;
		for (JsonElement value : values) {
			JsonObject json = object(value, "region");
			String type = string(json, "type", "region");
			String id = string(json, "id", type + "_" + (++index));
			result.add(new Region(localId(id, "region id"), type,
					new Bounds3(vector(required(json, "min"), "region min"),
							vector(required(json, "max"), "region max"))));
		}
		return result;
	}

	private static List<Socket> parseSockets(JsonObject owner) {
		JsonArray values = array(owner, "sockets", new JsonArray());
		List<Socket> result = new ArrayList<>();
		int index = 0;
		for (JsonElement value : values) {
			JsonObject json = object(value, "socket");
			String type = string(json, "type", "corridor");
			String id = string(json, "id", type + "_socket_" + (++index));
			Direction facing = Direction.byName(requiredString(json, "facing").toLowerCase(Locale.ROOT));
			if (facing == null)
				throw new JsonParseException("Unknown socket facing in " + id);
			result.add(new Socket(localId(id, "socket id"), type,
					new Bounds3(vector(required(json, "min"), "socket min"),
							vector(required(json, "max"), "socket max")),
					facing, bool(json, "required", false),
					boundedInt(json, "carve_depth", 1, 1, 16)));
		}
		return result;
	}

	private static List<Marker> parseMarkers(JsonObject owner) {
		JsonArray values = array(owner, "markers", new JsonArray());
		List<Marker> result = new ArrayList<>();
		int index = 0;
		for (JsonElement value : values) {
			JsonObject json = object(value, "marker");
			String type = string(json, "type", "marker");
			String id = string(json, "id", type + "_" + (++index));
			String group = optionalString(json, "group").orElse("");
			if (!group.isBlank())
				group = localId(group, "marker group");
			result.add(new Marker(localId(id, "marker id"), type, group,
					vector(required(json, "position"), "marker position")));
		}
		return result;
	}

	private static List<Encounter> parseEncounters(JsonObject owner, ResourceLocation resource,
			Optional<ResourceLocation> defaultPool, Optional<ResourceLocation> bossPool) {
		JsonArray values = array(owner, "encounters", new JsonArray());
		List<Encounter> result = new ArrayList<>();
		int encounterIndex = 0;
		for (JsonElement value : values) {
			JsonObject json = object(value, "encounter");
			String id = localId(string(json, "id", "encounter_" + (++encounterIndex)), "encounter id");
			Optional<String> trigger = optionalString(json, "trigger_region");
			if (trigger.isEmpty())
				trigger = optionalString(json, "trigger");
			trigger = trigger.map(valueId -> localId(valueId, "trigger region id"));
			List<String> lockSockets = stringList(json, "lock_sockets").stream()
					.map(valueId -> localId(valueId, "locked socket id")).toList();
			List<EncounterWave> waves = new ArrayList<>();
			JsonArray waveValues = array(json, "waves", null);
			if (waveValues == null && (json.has("spawn_group") || json.has("marker_group")))
				waveValues = singletonArray(json);
			if (waveValues != null) {
				int waveIndex = 0;
				for (JsonElement waveValue : waveValues) {
					JsonObject wave = object(waveValue, "encounter wave");
					String waveId = localId(string(wave, "id", id + "_wave_" + (++waveIndex)), "wave id");
					boolean boss = bool(wave, "boss", false);
					String group = wave.has("marker_group") ? requiredString(wave, "marker_group")
							: requiredString(wave, "spawn_group");
					Optional<ResourceLocation> pool = optionalInternalId(wave, "pool", resource.getNamespace());
					if (pool.isEmpty())
						pool = optionalInternalId(wave, "mob_pool", resource.getNamespace());
					if (pool.isEmpty())
						pool = boss && bossPool.isPresent() ? bossPool : defaultPool;
					if (pool.isEmpty())
						throw new JsonParseException("Encounter wave " + waveId + " has no mob pool and no default_mob_pool");
					IntRange count = wave.has("count") ? range(wave.get("count"), "count", 0, MAX_MARKERS_PER_WAVE)
							: new IntRange(1, 1);
					int delay = wave.has("delay_ticks")
							? boundedInt(wave, "delay_ticks", 0, 0, 1_000_000)
							: boundedInt(wave, "delay", 0, 0, 1_000_000);
					Optional<IntRange> waveLevel = optionalRange(wave, "level", 1, MAX_DUNGEON_LEVEL);
					if (waveLevel.isEmpty())
						waveLevel = optionalRange(wave, "spawn_level", 1, MAX_DUNGEON_LEVEL);
					waves.add(new EncounterWave(waveId, localId(group, "marker group"), pool.get(), count,
							delay, boss, waveLevel));
				}
			}
			result.add(new Encounter(id, trigger, waves, lockSockets));
		}
		return result;
	}

	private static IntRange parseRoomCount(JsonObject json) {
		if (json.has("room_count"))
			return range(json.get("room_count"), "room_count", 1, MAX_PROCEDURAL_ROOMS);
		int min = boundedInt(json, "min_rooms", 5, 1, MAX_PROCEDURAL_ROOMS);
		int max = boundedInt(json, "max_rooms", Math.max(min, 12), 1, MAX_PROCEDURAL_ROOMS);
		if (min > max)
			throw new JsonParseException("min_rooms cannot exceed max_rooms");
		return new IntRange(min, max);
	}

	private static LevelRule parseLevelRule(JsonObject json) {
		if (!json.has("level"))
			return new LevelRule("fixed", new IntRange(1, 1), 0);
		JsonElement element = json.get("level");
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			int value = element.getAsInt();
			if (value < 1 || value > MAX_DUNGEON_LEVEL)
				throw new JsonParseException("level must be between 1 and " + MAX_DUNGEON_LEVEL);
			return new LevelRule("fixed", new IntRange(value, value), 0);
		}
		JsonObject level = object(element, "level");
		String source = string(level, "source", "fixed");
		if (!Set.of("fixed", "owner", "player", "party_average", "average", "party_highest", "highest").contains(source.toLowerCase(Locale.ROOT)))
			throw new JsonParseException("Unknown level source " + source + ". Expected fixed, owner, party_average, or party_highest.");
		IntRange range;
		if (level.has("range")) {
			range = range(level.get("range"), "level range", 1, MAX_DUNGEON_LEVEL);
		} else {
			int min = boundedInt(level, "min", 1, 1, MAX_DUNGEON_LEVEL);
			int max = boundedInt(level, "max", min, 1, MAX_DUNGEON_LEVEL);
			if (min > max)
				throw new JsonParseException("level min cannot exceed max");
			range = new IntRange(min, max);
		}
		return new LevelRule(source, range, boundedInt(level, "variance", 0, 0, MAX_DUNGEON_LEVEL));
	}

	private static ShellSettings parseShell(JsonElement element, ResourceLocation owner) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			boolean enabled = element.getAsBoolean();
			return new ShellSettings(enabled, BEDROCK_ID, enabled ? 1 : 0, true, true);
		}
		JsonObject json = object(element, "shell");
		boolean enabled = bool(json, "enabled", true);
		ResourceLocation block = minecraftId(string(json, "block", BEDROCK_ID.toString()));
		if (!ForgeRegistries.BLOCKS.containsKey(block) || ForgeRegistries.BLOCKS.getValue(block) == Blocks.AIR)
			throw new JsonParseException("Unknown or invalid shell block " + block + " in " + owner);
		int thickness = enabled ? boundedInt(json, "thickness", 1, 1, 4) : 0;
		return new ShellSettings(enabled, block, thickness,
				bool(json, "cover_floor", true), bool(json, "cover_ceiling", true));
	}

	private static Map<ResourceLocation, DungeonRoomDefinition> validatedRooms(
			Map<ResourceLocation, DungeonRoomDefinition> input,
			Map<ResourceLocation, MobPoolDefinition> pools, List<ValidationIssue> issues) {
		Map<ResourceLocation, DungeonRoomDefinition> valid = new LinkedHashMap<>();
		input.forEach((id, room) -> {
			int before = errorCount(issues, id);
			if (!room.origin().equals(new Int3(0, 0, 0)))
				issues.add(error(id, "Non-zero room origin is reserved for a future schema version. Structure coordinates must start at [0,0,0]."));
			if (room.shellOverride().isPresent())
				issues.add(error(id, "Room-level shell overrides are reserved for a future schema version. Configure shell on the dungeon definition."));
			validateGeometry(id, room.size(), room.origin(), room.regions(), room.sockets(), room.markers(), issues);
			room.defaultMobPool().ifPresent(pool -> requirePool(id, pool, pools, issues));
			validateEncounters(id, room.formatVersion(), room.regions(), room.sockets(), room.markers(),
					room.encounters(), pools, issues);
			long horizontalSockets = room.sockets().stream().filter(socket -> socket.facing().getAxis().isHorizontal()).count();
			if (room.role() == RoomRole.START && horizontalSockets < 1)
				issues.add(warning(id, "Start room should have at least one horizontal exit socket."));
			if (room.role() == RoomRole.START && room.markers().stream().noneMatch(marker -> marker.type().equals("player_start")))
				issues.add(error(id, "Start room requires a player_start marker."));
			if (room.role() == RoomRole.START && room.markers().stream()
					.noneMatch(marker -> marker.type().equals("exit") || marker.type().equals("return_portal")))
				issues.add(error(id, "Start room requires an exit or return_portal marker."));
			if (room.role() == RoomRole.BOSS && room.markers().stream().noneMatch(marker -> marker.type().equals("boss_spawn")))
				issues.add(error(id, "Boss room requires a boss_spawn marker."));
			if (room.role() == RoomRole.BOSS && room.encounters().stream().flatMap(encounter -> encounter.waves().stream())
					.noneMatch(EncounterWave::boss))
				issues.add(error(id, "Boss room requires at least one encounter wave with boss=true."));
			if ((room.role() == RoomRole.BOSS || room.role() == RoomRole.CAP || room.role() == RoomRole.DEAD_END)
					&& horizontalSockets != 1)
				issues.add(warning(id, room.role().name().toLowerCase(Locale.ROOT)
						+ " room should normally have exactly one horizontal connection socket."));
			if (errorCount(issues, id) == before)
				valid.put(id, room);
		});
		return valid;
	}

	private static Map<ResourceLocation, DungeonDefinition> validatedDungeons(
			Map<ResourceLocation, DungeonDefinition> input,
			Map<ResourceLocation, DungeonRoomDefinition> rooms,
			Map<ResourceLocation, MobPoolDefinition> pools, List<ValidationIssue> issues) {
		Map<ResourceLocation, DungeonDefinition> valid = new LinkedHashMap<>();
		input.forEach((id, dungeon) -> {
			int before = errorCount(issues, id);
			if (!dungeon.shell().coverFloor() || !dungeon.shell().coverCeiling())
				issues.add(error(id, "Schema v2 protective shells must cover both floor and ceiling."));
			dungeon.defaultMobPool().ifPresent(pool -> requirePool(id, pool, pools, issues));
			dungeon.bossMobPool().ifPresent(pool -> requirePool(id, pool, pools, issues));
			if (dungeon.kind() == DungeonKind.PRESET) {
				if (dungeon.structure().isEmpty())
					issues.add(error(id, "Preset dungeon requires structure."));
				if (dungeon.size().isEmpty())
					issues.add(error(id, "Preset dungeon requires size."));
				if (dungeon.size().isPresent()) {
					Int3 dungeonOrigin = dungeon.origin().orElse(new Int3(0, 0, 0));
					if (!dungeonOrigin.equals(new Int3(0, 0, 0)))
						issues.add(error(id, "Non-zero preset origin is reserved for a future schema version. Structure coordinates must start at [0,0,0]."));
					validateGeometry(id, dungeon.size().get(), dungeonOrigin, dungeon.regions(),
							dungeon.sockets(), dungeon.markers(), issues);
				}
				validateEncounters(id, dungeon.formatVersion(), dungeon.regions(), dungeon.sockets(), dungeon.markers(),
						dungeon.encounters(), pools, issues);
				if (dungeon.markers().stream().noneMatch(marker -> marker.type().equals("player_start")))
					issues.add(error(id, "Preset dungeon requires a player_start marker."));
				if (dungeon.markers().stream().noneMatch(marker -> marker.type().equals("exit") || marker.type().equals("return_portal")))
					issues.add(error(id, "Preset dungeon requires an exit or return_portal marker."));
				if (dungeon.markers().stream().noneMatch(marker -> marker.type().equals("boss_spawn")))
					issues.add(error(id, "Preset dungeon requires a boss_spawn marker so the run can complete."));
				if (dungeon.encounters().stream().flatMap(encounter -> encounter.waves().stream())
						.noneMatch(EncounterWave::boss))
					issues.add(error(id, "Preset dungeon requires at least one encounter wave with boss=true."));
			} else if (dungeon.kind() == DungeonKind.PROCEDURAL) {
				if (dungeon.topology() == DungeonTopology.BRANCHING && dungeon.roomCount().min() < 4)
					issues.add(error(id, "Branching topology requires room_count minimum 4 or greater."));
				if (!dungeon.rooms(RoomRole.CAP).isEmpty())
					issues.add(error(id, "CAP room pools are reserved for a future schema version. Leave unused sockets as solid room walls."));
				if (dungeon.rooms(RoomRole.START).isEmpty())
					issues.add(error(id, "Procedural dungeon requires at least one start room."));
				if (dungeon.rooms(RoomRole.BOSS).isEmpty())
					issues.add(error(id, "Procedural dungeon requires at least one boss room."));
				boolean hasMiddle = dungeon.roomPools().entrySet().stream()
						.filter(entry -> entry.getKey() != RoomRole.START && entry.getKey() != RoomRole.BOSS
								&& entry.getKey() != RoomRole.CAP)
						.anyMatch(entry -> !entry.getValue().isEmpty());
				if (!hasMiddle)
					issues.add(error(id, "Procedural dungeon requires at least one normal, corridor, junction, treasure, dead-end, or stair room."));
				for (Map.Entry<RoomRole, List<WeightedRoom>> pool : dungeon.roomPools().entrySet()) {
					for (WeightedRoom choice : pool.getValue()) {
						DungeonRoomDefinition room = rooms.get(choice.room());
						if (room == null) {
							issues.add(error(id, "Referenced room " + choice.room() + " does not exist or failed validation."));
						} else if (room.role() != pool.getKey()) {
							issues.add(error(id, "Room " + choice.room() + " has role " + room.role().name().toLowerCase(Locale.ROOT)
									+ " but is listed in the " + pool.getKey().name().toLowerCase(Locale.ROOT) + " pool."));
						}
					}
				}
			} else {
				validateFixedDungeon(id, dungeon, rooms, issues);
			}
			if (errorCount(issues, id) == before)
				valid.put(id, dungeon);
		});
		return valid;
	}

	private static void validateFixedDungeon(ResourceLocation id, DungeonDefinition dungeon,
			Map<ResourceLocation, DungeonRoomDefinition> rooms, List<ValidationIssue> issues) {
		if (!dungeon.roomPools().isEmpty())
			issues.add(error(id, "Fixed dungeon uses placements/connections, not room_pools."));
		if (dungeon.fixedPlacements().isEmpty() || dungeon.fixedPlacements().size() > MAX_PROCEDURAL_ROOMS) {
			issues.add(error(id, "Fixed dungeon requires 1-" + MAX_PROCEDURAL_ROOMS + " placements."));
			return;
		}
		if (dungeon.fixedConnections().size() > 256)
			issues.add(error(id, "Fixed dungeon supports at most 256 connections."));
		validateUniqueIds(id, "fixed placement", dungeon.fixedPlacements().stream()
				.map(FixedRoomPlacement::id).toList(), issues);
		Map<String, DungeonRoomDefinition> placedRooms = new LinkedHashMap<>();
		int starts = 0;
		int bosses = 0;
		for (FixedRoomPlacement placement : dungeon.fixedPlacements()) {
			if (Math.abs((long) placement.position().x()) > 1_000_000L
					|| Math.abs((long) placement.position().y()) > 1_000_000L
					|| Math.abs((long) placement.position().z()) > 1_000_000L)
				issues.add(error(id, "Fixed placement " + placement.id() + " position exceeds the +/-1,000,000 safety range."));
			DungeonRoomDefinition room = rooms.get(placement.room());
			if (room == null) {
				issues.add(error(id, "Fixed placement " + placement.id() + " references missing room "
						+ placement.room() + "."));
				continue;
			}
			placedRooms.put(placement.id(), room);
			if (room.role() == RoomRole.START)
				starts++;
			if (room.role() == RoomRole.BOSS)
				bosses++;
		}
		if (starts != 1 || bosses != 1)
			issues.add(error(id, "Fixed dungeon requires exactly one start placement and exactly one boss placement."));

		Set<String> usedSockets = new HashSet<>();
		Map<String, Set<String>> adjacency = new HashMap<>();
		placedRooms.keySet().forEach(key -> adjacency.put(key, new HashSet<>()));
		for (FixedRoomConnection connection : dungeon.fixedConnections()) {
			DungeonRoomDefinition from = placedRooms.get(connection.fromRoom());
			DungeonRoomDefinition to = placedRooms.get(connection.toRoom());
			if (from == null || to == null || connection.fromRoom().equals(connection.toRoom())) {
				issues.add(error(id, "Fixed connection references a missing placement or connects a room to itself."));
				continue;
			}
			Socket fromSocket = from.sockets().stream().filter(socket -> socket.id().equals(connection.fromSocket()))
					.findFirst().orElse(null);
			Socket toSocket = to.sockets().stream().filter(socket -> socket.id().equals(connection.toSocket()))
					.findFirst().orElse(null);
			if (fromSocket == null || toSocket == null) {
				issues.add(error(id, "Fixed connection " + connection.fromRoom() + ":" + connection.fromSocket()
						+ " -> " + connection.toRoom() + ":" + connection.toSocket() + " references a missing socket."));
				continue;
			}
			if (!fromSocket.type().equals(toSocket.type()))
				issues.add(error(id, "Fixed connection sockets " + connection.fromRoom() + ":" + connection.fromSocket()
						+ " and " + connection.toRoom() + ":" + connection.toSocket() + " use different types."));
			String fromKey = connection.fromRoom() + "\u0000" + connection.fromSocket();
			String toKey = connection.toRoom() + "\u0000" + connection.toSocket();
			if (!usedSockets.add(fromKey) || !usedSockets.add(toKey))
				issues.add(error(id, "Each socket may appear in only one fixed connection."));
			adjacency.get(connection.fromRoom()).add(connection.toRoom());
			adjacency.get(connection.toRoom()).add(connection.fromRoom());
		}
		for (Map.Entry<String, DungeonRoomDefinition> placement : placedRooms.entrySet()) {
			for (Socket socket : placement.getValue().sockets())
				if (socket.required() && !usedSockets.contains(placement.getKey() + "\u0000" + socket.id()))
					issues.add(error(id, "Required socket " + placement.getKey() + ":" + socket.id()
							+ " is not used by a fixed connection."));
		}
		String start = placedRooms.entrySet().stream()
				.filter(entry -> entry.getValue().role() == RoomRole.START).map(Map.Entry::getKey)
				.findFirst().orElse(null);
		if (start != null) {
			Set<String> visited = new HashSet<>();
			java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
			visited.add(start);
			queue.add(start);
			while (!queue.isEmpty())
				for (String next : adjacency.getOrDefault(queue.removeFirst(), Set.of()))
					if (visited.add(next))
						queue.addLast(next);
			if (visited.size() != placedRooms.size())
				issues.add(error(id, "Every fixed placement must be connected to the start-room graph."));
		}
	}

	private static void validateGeometry(ResourceLocation id, Int3 size, Int3 origin,
			List<Region> regions, List<Socket> sockets, List<Marker> markers,
			List<ValidationIssue> issues) {
		if (!size.isPositive() || size.x() > 48 || size.y() > 48 || size.z() > 48)
			issues.add(error(id, "Structure size must be positive and no larger than 48 blocks on each axis."));
		if (!size.contains(origin))
			issues.add(error(id, "Origin lies outside structure size."));
		validateUniqueIds(id, "region", regions.stream().map(Region::id).toList(), issues);
		validateUniqueIds(id, "socket", sockets.stream().map(Socket::id).toList(), issues);
		validateUniqueIds(id, "marker", markers.stream().map(Marker::id).toList(), issues);
		if (sockets.stream().filter(Socket::required).count() > 2)
			issues.add(error(id, "Schema v2 supports at most two required sockets per room. Extra junction branches must be optional."));
		for (Region region : regions)
			if (!region.bounds().inside(size))
				issues.add(error(id, "Region " + region.id() + " lies outside structure size."));
		for (Marker marker : markers)
			if (!size.contains(marker.position()))
				issues.add(error(id, "Marker " + marker.id() + " lies outside structure size."));
			else if (marker.type().equals("loot") || marker.type().equals("checkpoint"))
				issues.add(warning(id, "Marker " + marker.id() + " is addon metadata only; schema v2 has no built-in "
						+ marker.type() + " behavior."));
		for (Socket socket : sockets) {
			if (!socket.opening().inside(size)) {
				issues.add(error(id, "Socket " + socket.id() + " lies outside structure size."));
				continue;
			}
			boolean flatPlane = switch (socket.facing().getAxis()) {
				case X -> socket.opening().min().x() == socket.opening().max().x();
				case Y -> socket.opening().min().y() == socket.opening().max().y();
				case Z -> socket.opening().min().z() == socket.opening().max().z();
			};
			if (!flatPlane)
				issues.add(error(id, "Socket " + socket.id() + " must be a flat opening plane perpendicular to its facing."));
			int inset = socket.insetFromBoundary(size);
			if (inset < 0 || inset > 1)
				issues.add(error(id, "Socket " + socket.id() + " must be on the wall plane or exactly one block inside the "
						+ socket.facing().getName() + " wall."));
			if (socket.type().equals("corridor") && socket.facing().getAxis().isVertical())
				issues.add(error(id, "Corridor socket " + socket.id() + " must face north, south, east, or west."));
			if (socket.type().equals("stair") && socket.facing().getAxis().isHorizontal())
				issues.add(error(id, "Stair socket " + socket.id() + " must face up or down."));
			if (socket.carveDepth() != 1)
				issues.add(error(id, "Socket " + socket.id() + " uses carve_depth, which is reserved for a future schema version. Omit it or use 1."));
		}
	}

	private static void validateEncounters(ResourceLocation id, int formatVersion, List<Region> regions,
			List<Socket> sockets, List<Marker> markers, List<Encounter> encounters,
			Map<ResourceLocation, MobPoolDefinition> pools, List<ValidationIssue> issues) {
		Set<String> regionIds = regions.stream().map(Region::id).collect(java.util.stream.Collectors.toSet());
		Set<String> socketIds = sockets.stream().map(Socket::id).collect(java.util.stream.Collectors.toSet());
		validateUniqueIds(id, "encounter", encounters.stream().map(Encounter::id).toList(), issues);
		for (Encounter encounter : encounters) {
			encounter.triggerRegion().ifPresent(trigger -> {
				if (!regionIds.contains(trigger))
					issues.add(error(id, "Encounter " + encounter.id() + " references missing trigger region " + trigger + "."));
			});
			for (String socket : encounter.lockSockets())
				if (!socketIds.contains(socket))
					issues.add(error(id, "Encounter " + encounter.id() + " references missing lock socket " + socket + "."));
			if (!encounter.lockSockets().isEmpty())
				issues.add(error(id, "Encounter " + encounter.id() + " uses lock_sockets, which is reserved for a later schema version and is not active yet."));
			if (formatVersion < 3 && encounter.waves().size() != 1)
				issues.add(error(id, "Encounter " + encounter.id() + " must contain exactly one wave before schema v3."));
			if (formatVersion >= 3 && encounter.waves().isEmpty())
				issues.add(error(id, "Encounter " + encounter.id() + " must contain at least one wave."));
			validateUniqueIds(id, "wave in encounter " + encounter.id(),
					encounter.waves().stream().map(EncounterWave::id).toList(), issues);
			for (int waveIndex = 0; waveIndex < encounter.waves().size(); waveIndex++) {
				EncounterWave wave = encounter.waves().get(waveIndex);
				List<Marker> groupedSpawnMarkers = markers.stream()
						.filter(marker -> isSpawnMarker(marker) && marker.belongsTo(wave.markerGroup())).toList();
				if (groupedSpawnMarkers.isEmpty())
					issues.add(error(id, "Wave " + wave.id() + " references missing marker/group " + wave.markerGroup() + "."));
				requirePool(id, wave.mobPool(), pools, issues);
				if (formatVersion < 3 && wave.delayTicks() != 0)
					issues.add(error(id, "Wave " + wave.id() + " uses delay_ticks, which requires format_version 3."));
				if (formatVersion >= 3 && wave.boss() && waveIndex != encounter.waves().size() - 1)
					issues.add(error(id, "Boss wave " + wave.id()
							+ " must be the terminal wave of its schema-v3 encounter."));
				if (wave.count().min() < 1)
					issues.add(error(id, "Wave " + wave.id() + " must spawn at least one mob."));
				List<Marker> eligibleMarkers = groupedSpawnMarkers.stream()
						.filter(marker -> wave.boss() == marker.type().equals("boss_spawn")).toList();
				if (wave.boss() && eligibleMarkers.isEmpty())
					issues.add(error(id, "Boss wave " + wave.id() + " must target a boss_spawn marker."));
				if (!wave.boss() && groupedSpawnMarkers.stream().anyMatch(marker -> marker.type().equals("boss_spawn")))
					issues.add(error(id, "Non-boss wave " + wave.id() + " cannot target a boss_spawn marker."));
				if (wave.count().max() > eligibleMarkers.size())
					issues.add(error(id, "Wave " + wave.id() + " can spawn " + wave.count().max()
							+ " mobs but has only " + eligibleMarkers.size() + " compatible spawn marker(s)."));
			}
		}
	}

	private static boolean isSpawnMarker(Marker marker) {
		return marker.type().equals("mob_spawn") || marker.type().equals("elite_spawn")
				|| marker.type().equals("boss_spawn");
	}

	private static void validateUniqueIds(ResourceLocation resource, String kind, List<String> ids,
			List<ValidationIssue> issues) {
		Set<String> unique = new HashSet<>();
		for (String id : ids)
			if (!unique.add(id))
				issues.add(error(resource, "Duplicate " + kind + " id " + id + "."));
	}

	private static void requirePool(ResourceLocation owner, ResourceLocation pool,
			Map<ResourceLocation, MobPoolDefinition> pools, List<ValidationIssue> issues) {
		MobPoolDefinition definition = pools.get(pool);
		if (definition == null)
			issues.add(error(owner, "Referenced mob pool " + pool + " does not exist."));
		else if (definition.entries().isEmpty())
			issues.add(error(owner, "Referenced mob pool " + pool
					+ " has no active entries. Add a fallback entry when optional mods are absent."));
	}

	private static int errorCount(List<ValidationIssue> issues, ResourceLocation resource) {
		return (int) issues.stream().filter(issue -> issue.severity() == Severity.ERROR
				&& issue.resource().equals(resource)).count();
	}

	private static RoomRole inferredRole(List<Marker> markers) {
		if (markers.stream().anyMatch(marker -> marker.type().equals("boss_spawn")))
			return RoomRole.BOSS;
		if (markers.stream().anyMatch(marker -> marker.type().equals("player_start")))
			return RoomRole.START;
		return RoomRole.NORMAL;
	}

	private static DungeonKind strictDungeonKind(String value) {
		String normalized = value == null ? "preset" : value.toLowerCase(Locale.ROOT).replace('-', '_');
		return switch (normalized) {
			case "preset", "structure", "single" -> DungeonKind.PRESET;
			case "procedural", "module_pool" -> DungeonKind.PROCEDURAL;
			case "fixed", "fixed_layout" -> DungeonKind.FIXED;
			default -> throw new JsonParseException("Unknown dungeon generation type " + value
					+ ". Expected preset, procedural, or fixed.");
		};
	}

	private static DungeonTopology parseDungeonTopology(JsonObject json, DungeonKind kind) {
		if (!json.has("topology"))
			return DungeonTopology.LINEAR;
		if (kind != DungeonKind.PROCEDURAL)
			throw new JsonParseException("topology is supported only by procedural dungeon definitions");
		String normalized = string(json, "topology", "linear").toLowerCase(Locale.ROOT).replace('-', '_');
		return switch (normalized) {
			case "linear", "path" -> DungeonTopology.LINEAR;
			case "branching", "branched", "branches" -> DungeonTopology.BRANCHING;
			default -> throw new JsonParseException("Unknown dungeon topology " + normalized
					+ ". Expected linear or branching.");
		};
	}

	private static RoomRole strictRoomRole(String value, String field) {
		String normalized = value == null ? "normal" : value.toLowerCase(Locale.ROOT).replace('-', '_');
		return switch (normalized) {
			case "start", "entry", "entrance" -> RoomRole.START;
			case "normal" -> RoomRole.NORMAL;
			case "junction", "branch" -> RoomRole.JUNCTION;
			case "dead_end", "deadend" -> RoomRole.DEAD_END;
			case "treasure", "reward" -> RoomRole.TREASURE;
			case "boss", "boss_room" -> RoomRole.BOSS;
			case "cap", "wall_cap" -> RoomRole.CAP;
			case "corridor", "hall" -> RoomRole.CORRIDOR;
			case "stair", "stairs" -> RoomRole.STAIR;
			default -> throw new JsonParseException("Unknown " + field + " " + value + ".");
		};
	}

	private static boolean conditionsApply(JsonObject json, ICondition.IContext context) {
		return CraftingHelper.processConditions(json, "conditions", context);
	}

	private static int formatVersion(JsonObject json) {
		int version = boundedInt(json, "format_version", 1, 1, 3);
		if (version != 1 && version != 2 && version != 3)
			throw new JsonParseException("Unsupported format_version " + version + "; supported versions are 1, 2, and 3");
		return version;
	}

	private static EntitySelector parseSelector(JsonElement element, String ownerNamespace) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
			String value = element.getAsString();
			return value.startsWith("#")
					? new EntitySelector(SelectorKind.TAG, tagId(value.substring(1), ownerNamespace))
					: new EntitySelector(SelectorKind.ENTITY, minecraftId(value));
		}
		return parseSelector(object(element, "entity selector"), ownerNamespace);
	}

	private static EntitySelector parseSelector(JsonObject json, String ownerNamespace) {
		boolean entity = json.has("entity");
		boolean tag = json.has("tag");
		if (entity == tag)
			throw new JsonParseException("Mob pool entry must contain exactly one of entity or tag");
		if (tag)
			return new EntitySelector(SelectorKind.TAG, tagId(requiredString(json, "tag"), ownerNamespace));
		String value = requiredString(json, "entity");
		if (value.startsWith("#"))
			return new EntitySelector(SelectorKind.TAG, tagId(value.substring(1), ownerNamespace));
		return new EntitySelector(SelectorKind.ENTITY, minecraftId(value));
	}

	private static ResourceLocation tagId(String value, String ownerNamespace) {
		return id(value, ownerNamespace, "entity tag");
	}

	private static ResourceLocation internalId(String value, String ownerNamespace) {
		return id(value, ownerNamespace, "resource id");
	}

	private static ResourceLocation minecraftId(String value) {
		return id(value, "minecraft", "registry id");
	}

	private static ResourceLocation id(String value, String defaultNamespace, String label) {
		String qualified = value.contains(":") ? value : defaultNamespace + ":" + value;
		ResourceLocation result = ResourceLocation.tryParse(qualified);
		if (result == null)
			throw new JsonParseException("Invalid " + label + " " + value);
		return result;
	}

	private static String localId(String value, String label) {
		if (value == null || !value.matches("[a-z0-9][a-z0-9_.-]{0,63}"))
			throw new JsonParseException("Invalid " + label + " " + value
					+ "; use lowercase letters, numbers, _, - or .");
		return value;
	}

	private static Optional<ResourceLocation> optionalInternalId(JsonObject json, String key,
			String ownerNamespace) {
		return optionalString(json, key).map(value -> internalId(value, ownerNamespace));
	}

	private static Optional<IntRange> optionalRange(JsonObject json, String key, int minimum, int maximum) {
		return json.has(key) ? Optional.of(range(json.get(key), key, minimum, maximum)) : Optional.empty();
	}

	private static IntRange range(JsonElement element, String label, int minimum, int maximum) {
		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			int value = element.getAsInt();
			if (value < minimum || value > maximum)
				throw new JsonParseException(label + " must be between " + minimum + " and " + maximum);
			return new IntRange(value, value);
		}
		if (element.isJsonArray()) {
			JsonArray values = element.getAsJsonArray();
			if (values.size() != 2)
				throw new JsonParseException(label + " array must contain [min, max]");
			int min = values.get(0).getAsInt();
			int max = values.get(1).getAsInt();
			if (min < minimum || max > maximum || min > max)
				throw new JsonParseException(label + " must stay between " + minimum + " and " + maximum + " with min <= max");
			return new IntRange(min, max);
		}
		JsonObject json = object(element, label);
		int min = boundedInt(json, "min", minimum, minimum, maximum);
		int max = boundedInt(json, "max", min, minimum, maximum);
		if (min > max)
			throw new JsonParseException(label + " min cannot exceed max");
		return new IntRange(min, max);
	}

	private static Int3 vector(JsonElement element, String label) {
		JsonArray array = element.isJsonArray() ? element.getAsJsonArray() : null;
		if (array == null || array.size() != 3)
			throw new JsonParseException(label + " must be an array of exactly three integers");
		try {
			return new Int3(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
		} catch (RuntimeException exception) {
			throw new JsonParseException(label + " must contain integers", exception);
		}
	}

	private static List<String> stringList(JsonObject json, String key) {
		if (!json.has(key))
			return List.of();
		JsonArray array = array(json, key, null);
		if (array == null)
			return List.of();
		List<String> result = new ArrayList<>();
		for (JsonElement value : array) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
				throw new JsonParseException(key + " must contain only strings");
			result.add(value.getAsString());
		}
		return result;
	}

	private static JsonArray singletonArray(JsonElement element) {
		JsonArray array = new JsonArray();
		array.add(element);
		return array;
	}

	private static JsonElement required(JsonObject json, String key) {
		if (!json.has(key) || json.get(key).isJsonNull())
			throw new JsonParseException("Missing required field " + key);
		return json.get(key);
	}

	private static String requiredString(JsonObject json, String key) {
		JsonElement value = required(json, key);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
			throw new JsonParseException(key + " must be a string");
		return value.getAsString();
	}

	private static Optional<String> optionalString(JsonObject json, String key) {
		if (!json.has(key) || json.get(key).isJsonNull())
			return Optional.empty();
		return Optional.of(requiredString(json, key));
	}

	private static String string(JsonObject json, String key, String fallback) {
		return optionalString(json, key).orElse(fallback);
	}

	private static boolean bool(JsonObject json, String key, boolean fallback) {
		if (!json.has(key))
			return fallback;
		JsonElement value = json.get(key);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
			throw new JsonParseException(key + " must be true or false");
		return value.getAsBoolean();
	}

	private static int boundedInt(JsonObject json, String key, int fallback, int minimum, int maximum) {
		if (!json.has(key))
			return fallback;
		JsonElement value = json.get(key);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
			throw new JsonParseException(key + " must be an integer");
		int result;
		try {
			result = value.getAsInt();
		} catch (RuntimeException exception) {
			throw new JsonParseException(key + " must be an integer", exception);
		}
		if (result < minimum || result > maximum)
			throw new JsonParseException(key + " must be between " + minimum + " and " + maximum);
		return result;
	}

	private static JsonObject object(JsonElement element, String label) {
		if (element == null || !element.isJsonObject())
			throw new JsonParseException(label + " must be a JSON object");
		return element.getAsJsonObject();
	}

	private static JsonArray array(JsonObject json, String key, JsonArray fallback) {
		if (!json.has(key))
			return fallback;
		JsonElement value = json.get(key);
		if (!value.isJsonArray())
			throw new JsonParseException(key + " must be an array");
		return value.getAsJsonArray();
	}

	private static ValidationIssue warning(ResourceLocation id, String message) {
		return new ValidationIssue(Severity.WARNING, id, message);
	}

	private static ValidationIssue error(ResourceLocation id, String message) {
		return new ValidationIssue(Severity.ERROR, id, message);
	}

	private static String cleanMessage(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}
}
