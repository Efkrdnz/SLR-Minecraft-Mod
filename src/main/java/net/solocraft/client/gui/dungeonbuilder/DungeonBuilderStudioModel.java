package net.solocraft.client.gui.dungeonbuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Immutable, packet-friendly client view of the Dungeon Builder workspace.
 *
 * <p>The server remains authoritative. Every collection and string is bounded so
 * a malformed or unexpectedly large snapshot cannot turn the Studio into an
 * unbounded client allocation. The model deliberately contains no SavedData or
 * world references, which keeps network codecs and preview tests independent of
 * the live builder implementation.</p>
 */
public record DungeonBuilderStudioModel(
		long revision,
		String selectedProjectId,
		String selectedPoolId,
		String dungeonId,
		boolean loading,
		List<Project> projects,
		List<MobPool> pools,
		LayoutDraft layout,
		Simulation simulation,
		ValidationSummary validation,
		Notice notice,
		List<DraftSummary> dungeonDrafts) {

	public static final int MAX_PROJECTS = 128;
	public static final int MAX_POOLS = 128;
	public static final int MAX_SOCKETS = 64;
	public static final int MAX_ANCHORS = 320;
	public static final int MAX_POOL_ENTRIES = 256;
	public static final int MAX_FOOTPRINT_CELLS = 48 * 48;
	public static final int MAX_LAYOUT_ROOMS = 64;
	public static final int MAX_CONNECTIONS = 128;
	public static final int MAX_ISSUES = 128;
	public static final int MAX_DUNGEON_DRAFTS = 64;

	private static final int ID_LENGTH = 128;
	private static final int RESOURCE_ID_LENGTH = 192;
	private static final int LABEL_LENGTH = 96;
	private static final int MESSAGE_LENGTH = 320;

	public DungeonBuilderStudioModel {
		revision = Math.max(0L, revision);
		selectedProjectId = text(selectedProjectId, ID_LENGTH);
		selectedPoolId = text(selectedPoolId, RESOURCE_ID_LENGTH);
		dungeonId = defaultText(dungeonId, "my_dungeon", 192);
		projects = bounded(projects, MAX_PROJECTS, Function.identity());
		pools = bounded(pools, MAX_POOLS, Function.identity());
		layout = layout == null ? LayoutDraft.empty() : layout;
		simulation = simulation == null ? Simulation.empty() : simulation;
		validation = validation == null ? ValidationSummary.notRun() : validation;
		notice = notice == null ? Notice.none() : notice;
		dungeonDrafts = bounded(dungeonDrafts, MAX_DUNGEON_DRAFTS, Function.identity());
	}

	/** Compatibility constructor for snapshots created before the saved-dungeon catalog. */
	public DungeonBuilderStudioModel(long revision, String selectedProjectId, String selectedPoolId,
			String dungeonId, boolean loading, List<Project> projects, List<MobPool> pools,
			LayoutDraft layout, Simulation simulation, ValidationSummary validation, Notice notice) {
		this(revision, selectedProjectId, selectedPoolId, dungeonId, loading, projects, pools,
				layout, simulation, validation, notice, List.of());
	}

	public static DungeonBuilderStudioModel empty() {
		return new DungeonBuilderStudioModel(0L, "", "", "my_dungeon", false,
				List.of(), List.of(), LayoutDraft.empty(), Simulation.empty(),
				ValidationSummary.notRun(), Notice.none());
	}

	public static DungeonBuilderStudioModel loadingState() {
		return new DungeonBuilderStudioModel(0L, "", "", "my_dungeon", true,
				List.of(), List.of(), LayoutDraft.empty(), Simulation.empty(),
				ValidationSummary.notRun(), new Notice(Severity.INFO, "Loading builder workspace..."));
	}

	public Optional<Project> selectedProject() {
		return project(selectedProjectId);
	}

	public Optional<Project> project(String id) {
		String target = text(id, ID_LENGTH);
		return projects.stream().filter(project -> project.id().equals(target)).findFirst();
	}

	public Optional<MobPool> selectedPool() {
		return pool(selectedPoolId);
	}

	public Optional<MobPool> pool(String id) {
		String target = text(id, RESOURCE_ID_LENGTH);
		return pools.stream().filter(pool -> pool.id().equals(target)).findFirst();
	}

	public Optional<DraftSummary> draft(String id) {
		String target = text(id, 192);
		return dungeonDrafts.stream().filter(draft -> draft.id().equals(target)).findFirst();
	}

	public enum ProjectKind {
		PRESET, MODULE
	}

	public enum RoomRole {
		START, NORMAL, CORRIDOR, JUNCTION, DEAD_END, TREASURE, STAIR, BOSS
	}

	public enum SocketType {
		CORRIDOR, STAIR
	}

	public enum Facing {
		NORTH(0, 0, -1), EAST(1, 0, 0), SOUTH(0, 0, 1), WEST(-1, 0, 0),
		UP(0, 1, 0), DOWN(0, -1, 0);

		private final int stepX;
		private final int stepY;
		private final int stepZ;

		Facing(int stepX, int stepY, int stepZ) {
			this.stepX = stepX;
			this.stepY = stepY;
			this.stepZ = stepZ;
		}

		public int stepX() {
			return stepX;
		}

		public int stepZ() {
			return stepZ;
		}

		public int stepY() {
			return stepY;
		}
	}

	public enum AnchorKind {
		UNASSIGNED("?"), SPAWN_POINT("S"), MOB_SPAWN("M"), ELITE_SPAWN("E"), BOSS_SPAWN("B"), PLAYER_START("P"),
		RETURN_PORTAL("R"), TRIGGER("T"), LOOT("L"), CUSTOM("?");

		private final String symbol;

		AnchorKind(String symbol) {
			this.symbol = symbol;
		}

		public String symbol() {
			return symbol;
		}
	}

	public enum SpawnRole {
		NONE, NORMAL, ELITE, BOSS
	}

	public enum SelectorKind {
		ENTITY, TAG
	}

	public enum LayoutMode {
		PROCEDURAL, FIXED
	}

	public enum Topology {
		LINEAR, BRANCHING
	}

	public enum Severity {
		INFO, PASS, TODO, WARNING, ERROR
	}

	public enum SimulationStatus {
		IDLE, RUNNING, SUCCESS, FAILED
	}

	/** Lightweight catalog row; the active draft's full layout remains in {@link #layout()}. */
	public record DraftSummary(String id, LayoutMode mode, Topology topology,
			int roomCount, int placementCount) {
		public DraftSummary {
			id = defaultText(id, "builder:my_dungeon", 192);
			mode = mode == null ? LayoutMode.PROCEDURAL : mode;
			topology = topology == null ? Topology.LINEAR : topology;
			roomCount = clamp(roomCount, 0, MAX_PROJECTS);
			placementCount = clamp(placementCount, 0, MAX_LAYOUT_ROOMS);
		}
	}

	public record Point(int x, int y, int z) {
	}

	public record Bounds(Point min, Point max) {
		public Bounds {
			Point a = min == null ? new Point(0, 0, 0) : min;
			Point b = max == null ? a : max;
			min = new Point(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()));
			max = new Point(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
		}

		public int width() {
			return saturatedSize(min.x(), max.x());
		}

		public int height() {
			return saturatedSize(min.y(), max.y());
		}

		public int depth() {
			return saturatedSize(min.z(), max.z());
		}

		public boolean contains(Point point) {
			return point != null && point.x() >= min.x() && point.x() <= max.x()
					&& point.y() >= min.y() && point.y() <= max.y()
					&& point.z() >= min.z() && point.z() <= max.z();
		}
	}

	public record Project(
			String id,
			String name,
			ProjectKind kind,
			RoomRole role,
			int weight,
			Set<String> ranks,
			String shellBlock,
			int shellThickness,
			Bounds bounds,
			boolean snapshotCaptured,
			boolean snapshotOutdated,
			long snapshotRevision,
			List<FootprintCell> footprint,
			List<Socket> sockets,
			List<Anchor> anchors,
			int errors,
			int warnings) {
		public Project {
			id = defaultText(id, "unknown:room", ID_LENGTH);
			name = defaultText(name, id, LABEL_LENGTH);
			kind = kind == null ? ProjectKind.MODULE : kind;
			role = role == null ? RoomRole.NORMAL : role;
			weight = clamp(weight, 1, 10_000);
			Set<String> normalizedRanks = boundedSet(ranks, 8, 16);
			ranks = normalizedRanks.size() == 6
					&& normalizedRanks.containsAll(Set.of("E", "D", "C", "B", "A", "S"))
							? Set.of() : normalizedRanks;
			shellBlock = defaultText(shellBlock, "minecraft:bedrock", ID_LENGTH);
			shellThickness = clamp(shellThickness, 0, 4);
			snapshotRevision = Math.max(0L, snapshotRevision);
			footprint = bounded(footprint, MAX_FOOTPRINT_CELLS, Function.identity());
			sockets = bounded(sockets, MAX_SOCKETS, Function.identity());
			anchors = bounded(anchors, MAX_ANCHORS, Function.identity());
			errors = clamp(errors, 0, 10_000);
			warnings = clamp(warnings, 0, 10_000);
		}

		/** Compatibility constructor for snapshots created before per-preset shell settings. */
		public Project(String id, String name, ProjectKind kind, RoomRole role, int weight, Set<String> ranks,
				Bounds bounds, boolean snapshotCaptured, boolean snapshotOutdated, long snapshotRevision,
				List<FootprintCell> footprint, List<Socket> sockets, List<Anchor> anchors, int errors, int warnings) {
			this(id, name, kind, role, weight, ranks, "minecraft:bedrock", 1, bounds, snapshotCaptured,
					snapshotOutdated, snapshotRevision, footprint, sockets, anchors, errors, warnings);
		}

		public String snapshotLabel() {
			if (!snapshotCaptured)
				return "NOT CAPTURED";
			return snapshotOutdated ? "UPDATE NEEDED" : "CAPTURED";
		}
	}

	/** Local X/Z cell sampled from the topmost non-air block in the captured structure. */
	public record FootprintCell(int x, int z, int argb) {
		public FootprintCell {
			x = clamp(x, 0, 47);
			z = clamp(z, 0, 47);
			if ((argb >>> 24) == 0)
				argb |= 0xFF000000;
		}
	}

	public record Socket(
			String id,
			Point position,
			Facing facing,
			SocketType type,
			boolean required,
			int openingWidth,
			int openingHeight) {
		public Socket {
			id = defaultText(id, "socket", ID_LENGTH);
			position = position == null ? new Point(0, 0, 0) : position;
			facing = facing == null ? Facing.NORTH : facing;
			type = type == null ? SocketType.CORRIDOR : type;
			openingWidth = clamp(openingWidth, 1, 32);
			openingHeight = clamp(openingHeight, 1, 32);
		}
	}

	public record Anchor(
			String id,
			AnchorKind kind,
			SpawnRole spawnRole,
			Point position,
			Bounds triggerBounds,
			String encounterId,
			String poolId,
			boolean levelOverride,
			int minLevel,
			int maxLevel,
			boolean delayed) {
		public Anchor {
			id = defaultText(id, "anchor", ID_LENGTH);
			kind = kind == null ? AnchorKind.UNASSIGNED : kind;
			spawnRole = spawnRole == null ? SpawnRole.NONE : spawnRole;
			position = position == null ? new Point(0, 0, 0) : position;
			encounterId = defaultText(encounterId, "default", 64);
			poolId = text(poolId, RESOURCE_ID_LENGTH);
			minLevel = clamp(minLevel, 1, 1_000);
			maxLevel = clamp(maxLevel, minLevel, 1_000);
		}

		/** Compatibility constructor for snapshots created before level inheritance was explicit. */
		public Anchor(String id, AnchorKind kind, SpawnRole spawnRole, Point position, Bounds triggerBounds,
				String encounterId, String poolId, int minLevel, int maxLevel, boolean delayed) {
			this(id, kind, spawnRole, position, triggerBounds, encounterId, poolId, true, minLevel, maxLevel, delayed);
		}
	}

	public record MobPool(String id, boolean draft, List<PoolEntry> entries) {
		public MobPool {
			id = defaultText(id, "builder:new_pool", RESOURCE_ID_LENGTH);
			entries = bounded(entries, MAX_POOL_ENTRIES, Function.identity());
		}

		public int totalWeight() {
			long total = 0L;
			for (PoolEntry entry : entries)
				total += entry.weight();
			return (int) Math.min(Integer.MAX_VALUE, total);
		}
	}

	public record LevelRange(boolean present, int min, int max) {
		public LevelRange {
			min = clamp(min, 1, 1_000);
			max = clamp(max, min, 1_000);
		}

		public static LevelRange unset() {
			return new LevelRange(false, 1, 1);
		}

		public static LevelRange of(int min, int max) {
			return new LevelRange(true, min, max);
		}
	}

	public record OptionalXp(boolean present, int value) {
		public OptionalXp {
			value = clamp(value, 0, 1_000_000);
		}

		public static OptionalXp automatic() {
			return new OptionalXp(false, 0);
		}
	}

	public record PoolEntry(
			SelectorKind selectorKind,
			String selectorId,
			int weight,
			String requiredMod,
			LevelRange eligibleLevel,
			LevelRange spawnLevel,
			OptionalXp baseXp) {
		public PoolEntry {
			selectorKind = selectorKind == null ? SelectorKind.ENTITY : selectorKind;
			selectorId = defaultText(selectorId, "minecraft:zombie", RESOURCE_ID_LENGTH);
			weight = clamp(weight, 1, 1_000_000);
			requiredMod = text(requiredMod, 64);
			eligibleLevel = eligibleLevel == null ? LevelRange.unset() : eligibleLevel;
			spawnLevel = spawnLevel == null ? LevelRange.unset() : spawnLevel;
			baseXp = baseXp == null ? OptionalXp.automatic() : baseXp;
		}

		/** Convenience for a simple exact-entity entry with an explicit spawn range and XP. */
		public PoolEntry(String entityId, int weight, int xp, int minLevel, int maxLevel) {
			this(SelectorKind.ENTITY, entityId, weight, "", LevelRange.unset(),
					LevelRange.of(minLevel, maxLevel), new OptionalXp(true, xp));
		}

		public String selectorLabel() {
			return selectorKind == SelectorKind.TAG ? "#" + selectorId : selectorId;
		}
	}

	public record LayoutDraft(
			LayoutMode mode,
			Topology topology,
			int minRooms,
			int maxRooms,
			int maxDepth,
			Set<String> ranks,
			String shellBlock,
			int shellThickness,
			List<String> enabledProjectIds,
			List<RoomWeight> roomWeights,
			List<LayoutNode> nodes,
			List<LayoutConnection> connections) {
		public LayoutDraft {
			mode = mode == null ? LayoutMode.PROCEDURAL : mode;
			topology = topology == null ? Topology.LINEAR : topology;
			int minimumRooms = mode == LayoutMode.PROCEDURAL
					? (topology == Topology.BRANCHING ? 4 : 3) : 1;
			minRooms = clamp(minRooms, minimumRooms, 64);
			maxRooms = clamp(maxRooms, minRooms, 64);
			maxDepth = clamp(maxDepth, 1, 64);
			ranks = boundedSet(ranks, 8, 16);
			shellBlock = defaultText(shellBlock, "minecraft:bedrock", ID_LENGTH);
			shellThickness = clamp(shellThickness, 0, 4);
			enabledProjectIds = bounded(enabledProjectIds, MAX_PROJECTS,
					value -> text(value, ID_LENGTH));
			roomWeights = bounded(roomWeights, MAX_PROJECTS, Function.identity());
			nodes = bounded(nodes, MAX_LAYOUT_ROOMS, Function.identity());
			connections = bounded(connections, MAX_CONNECTIONS, Function.identity());
		}

		public static LayoutDraft empty() {
			return new LayoutDraft(LayoutMode.PROCEDURAL, Topology.LINEAR, 3, 8, 16,
					Set.of(), "minecraft:bedrock", 1, List.of(), List.of(), List.of(), List.of());
		}
	}

	public record RoomWeight(String projectId, int weight) {
		public RoomWeight {
			projectId = text(projectId, ID_LENGTH);
			weight = clamp(weight, 1, 1_000_000);
		}
	}

	public record LayoutNode(
			String id,
			String projectId,
			RoomRole role,
			int x,
			int y,
			int z,
			int width,
			int depth,
			int rotation,
			boolean locked) {
		public LayoutNode {
			id = defaultText(id, "node", ID_LENGTH);
			projectId = defaultText(projectId, "unknown:room", ID_LENGTH);
			role = role == null ? RoomRole.NORMAL : role;
			width = clamp(width, 1, 512);
			depth = clamp(depth, 1, 512);
			rotation = Math.floorMod(rotation, 360);
		}
	}

	public record LayoutConnection(String fromNodeId, String fromSocketId, String toNodeId, String toSocketId) {
		public LayoutConnection {
			fromNodeId = text(fromNodeId, ID_LENGTH);
			fromSocketId = text(fromSocketId, ID_LENGTH);
			toNodeId = text(toNodeId, ID_LENGTH);
			toSocketId = text(toSocketId, ID_LENGTH);
		}
	}

	public record Simulation(
			long seed,
			SimulationStatus status,
			String message,
			int attempts,
			List<SimRoom> rooms,
			List<SimConnection> connections) {
		public Simulation {
			status = status == null ? SimulationStatus.IDLE : status;
			message = text(message, MESSAGE_LENGTH);
			attempts = clamp(attempts, 0, 1_000_000);
			rooms = bounded(rooms, MAX_LAYOUT_ROOMS, Function.identity());
			connections = bounded(connections, MAX_CONNECTIONS, Function.identity());
		}

		public static Simulation empty() {
			return new Simulation(12345L, SimulationStatus.IDLE, "", 0, List.of(), List.of());
		}
	}

	public record SimRoom(
			String id,
			String projectId,
			RoomRole role,
			int x,
			int z,
			int width,
			int depth,
			int rotation) {
		public SimRoom {
			id = defaultText(id, "room", ID_LENGTH);
			projectId = defaultText(projectId, "unknown:room", ID_LENGTH);
			role = role == null ? RoomRole.NORMAL : role;
			width = clamp(width, 1, 512);
			depth = clamp(depth, 1, 512);
			rotation = Math.floorMod(rotation, 360);
		}
	}

	public record SimConnection(String fromRoomId, String toRoomId) {
		public SimConnection {
			fromRoomId = text(fromRoomId, ID_LENGTH);
			toRoomId = text(toRoomId, ID_LENGTH);
		}
	}

	public record ValidationSummary(boolean hasRun, int errors, int warnings, List<ValidationIssue> issues) {
		public ValidationSummary {
			errors = clamp(errors, 0, 10_000);
			warnings = clamp(warnings, 0, 10_000);
			issues = bounded(issues, MAX_ISSUES, Function.identity());
		}

		public static ValidationSummary notRun() {
			return new ValidationSummary(false, 0, 0, List.of());
		}

		public Severity severity() {
			if (!hasRun)
				return Severity.TODO;
			if (errors > 0)
				return Severity.ERROR;
			if (warnings > 0)
				return Severity.WARNING;
			return Severity.PASS;
		}
	}

	public record ValidationIssue(
			Severity severity,
			String code,
			String message,
			String projectId,
			String elementId) {
		public ValidationIssue {
			severity = severity == null ? Severity.INFO : severity;
			code = text(code, 48);
			message = text(message, MESSAGE_LENGTH);
			projectId = text(projectId, ID_LENGTH);
			elementId = text(elementId, ID_LENGTH);
		}
	}

	public record Notice(Severity severity, String message) {
		public Notice {
			severity = severity == null ? Severity.INFO : severity;
			message = text(message, MESSAGE_LENGTH);
		}

		public static Notice none() {
			return new Notice(Severity.INFO, "");
		}
	}

	private static int saturatedSize(int min, int max) {
		long value = (long) max - min + 1L;
		return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, value));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String defaultText(String value, String fallback, int maximum) {
		String clean = text(value, maximum);
		return clean.isBlank() ? text(fallback, maximum) : clean;
	}

	private static String text(String value, int maximum) {
		if (value == null)
			return "";
		String clean = value.replace('\u0000', ' ').trim();
		return clean.length() <= maximum ? clean : clean.substring(0, maximum);
	}

	private static Set<String> boundedSet(Set<String> source, int maximum, int stringLength) {
		if (source == null || source.isEmpty())
			return Set.of();
		LinkedHashSet<String> result = new LinkedHashSet<>();
		for (String value : source) {
			if (result.size() >= maximum)
				break;
			String clean = text(value, stringLength).toUpperCase(Locale.ROOT);
			if (!clean.isBlank())
				result.add(clean);
		}
		return Collections.unmodifiableSet(result);
	}

	private static <T, R> List<R> bounded(List<T> source, int maximum, Function<T, R> mapper) {
		if (source == null || source.isEmpty())
			return List.of();
		int size = Math.min(maximum, source.size());
		List<R> result = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			T value = source.get(index);
			if (value == null)
				continue;
			R mapped = mapper.apply(value);
			if (mapped != null)
				result.add(mapped);
		}
		return List.copyOf(result);
	}
}
