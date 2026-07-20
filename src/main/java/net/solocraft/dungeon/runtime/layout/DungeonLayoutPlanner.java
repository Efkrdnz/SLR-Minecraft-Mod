package net.solocraft.dungeon.runtime.layout;

import net.solocraft.dungeon.data.DungeonDataSnapshot;
import net.solocraft.dungeon.data.DungeonDefinition;
import net.solocraft.dungeon.data.DungeonRoomDefinition;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.PreparedTemplate;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.PreparationResult;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.RelativeBounds;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.TransformedSocket;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.WorldBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;
import static net.solocraft.dungeon.runtime.layout.DungeonLayoutPlan.*;

/**
 * Canonical, deterministic dungeon geometry planner shared by runtime
 * generation and client previews. Planning loads structure templates but never
 * writes blocks, creates instances, changes SavedData, or spawns entities.
 */
public final class DungeonLayoutPlanner {
	private static final List<Rotation> HORIZONTAL_ROTATIONS = List.of(Rotation.NONE,
			Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90);
	private static final long MAX_TOTAL_TEMPLATE_VOLUME = 500_000L;
	private static final int MAX_SEARCH_NODES = 20_000;
	private static final int MAX_COMBINATIONS_PER_STATE = 2_048;
	private static final int MAX_ATTACHMENTS_PER_STATE = 256;
	private static final int MAX_DIAGNOSTIC_SAMPLES = 16;
	private static final long TARGET_ROOMS_SALT = 0x47B1_29CE_85D3_A60FL;
	private static final long START_ORDER_SALT = 0x79E4_061B_C2A8_5D3FL;
	private static final long PATH_ORDER_SALT = 0xB48D_2307_6FC1_9AE5L;
	private static final long BOSS_ORDER_SALT = 0xD210_7B59_43AE_F86CL;
	private static final long BRANCH_ORDER_SALT = 0x1F6A_C804_B9D2_735EL;

	private DungeonLayoutPlanner() {
	}

	public enum PlanFailure {
		NONE,
		INVALID_ARGUMENT,
		INVALID_DEFINITION,
		MISSING_TEMPLATE,
		NO_LAYOUT,
		REQUIRED_SOCKET_UNUSED,
		OUT_OF_WORLD,
		SAFETY_LIMIT,
		SEARCH_BUDGET_EXHAUSTED
	}

	public enum DiagnosticCode {
		MISSING_TEMPLATE,
		SIZE_MISMATCH,
		NO_SOURCE_SOCKET,
		DEPTH_LIMIT,
		SOCKET_TYPE_MISMATCH,
		SOCKET_FACING_MISMATCH,
		OPENING_SHAPE_MISMATCH,
		OUT_OF_BUILD_HEIGHT,
		ROOM_COLLISION,
		DEAD_END_BEFORE_TARGET,
		REQUIRED_SOCKET_UNUSED,
		TOTAL_VOLUME_LIMIT,
		SEARCH_BUDGET_EXHAUSTED,
		CANDIDATE_LIMIT_REACHED,
		BRANCH_SOCKET_UNAVAILABLE,
		OPTIONAL_SOCKET_CAPPED,
		FIXED_DUPLICATE_PLACEMENT,
		FIXED_INVALID_CONNECTION,
		FIXED_SOCKET_REUSED,
		FIXED_PASSAGE_MISALIGNED,
		FIXED_PASSAGE_COLLISION,
		FIXED_GRAPH_DISCONNECTED
	}

	/**
	 * Zero overrides mean: roll room count from the definition and choose an
	 * automatic critical-path length. criticalPathRooms includes start and boss.
	 */
	public record PlanOptions(DungeonLayoutTopology topology, int targetRoomCount,
			int criticalPathRooms) {
		public PlanOptions {
			topology = topology == null ? DungeonLayoutTopology.LINEAR : topology;
		}

		public static PlanOptions linear() {
			return new PlanOptions(DungeonLayoutTopology.LINEAR, 0, 0);
		}

		public static PlanOptions branching() {
			return new PlanOptions(DungeonLayoutTopology.BRANCHING, 0, 0);
		}
	}

	public record PlanResult(boolean success, PlanFailure failure, String message,
			@Nullable DungeonLayoutPlan layout, DungeonLayoutPreview preview) {
	}

	/** Immutable room lookup used by datapacks and unexported builder workspaces. */
	public interface RoomCatalog {
		@Nullable DungeonRoomDefinition find(ResourceLocation id);

		long revision();

		static RoomCatalog snapshot(DungeonDataSnapshot snapshot) {
			return new RoomCatalog() {
				@Override
				public DungeonRoomDefinition find(ResourceLocation id) {
					return snapshot.room(id).orElse(null);
				}

				@Override
				public long revision() {
					return snapshot.revision();
				}
			};
		}
	}

	/** Resolves either a loaded NBT template or metadata-only builder geometry. */
	@FunctionalInterface
	public interface RoomTemplateResolver {
		GeometryPreparation prepare(ServerLevel level, DungeonRoomDefinition room,
				BlockPos desiredWorldMinimum, Rotation rotation);

		static RoomTemplateResolver loadedTemplates() {
			return (level, room, minimum, rotation) -> {
				PreparationResult prepared = DungeonTemplatePlacer.prepare(level, room.structure(), minimum, rotation);
				return prepared.success() && prepared.template() != null
						? GeometryPreparation.success(DungeonRoomGeometry.loaded(prepared.template()))
						: GeometryPreparation.failure(prepared.message());
			};
		}

		static RoomTemplateResolver declaredGeometry() {
			return (level, room, minimum, rotation) -> {
				try {
					return GeometryPreparation.success(DungeonRoomGeometry.declared(
							room.structure(), room.size(), minimum, rotation));
				} catch (RuntimeException exception) {
					String message = exception.getMessage();
					return GeometryPreparation.failure(message == null ? exception.getClass().getSimpleName() : message);
				}
			};
		}
	}

	public record GeometryPreparation(boolean success, String message,
			@Nullable DungeonRoomGeometry geometry) {
		private static GeometryPreparation success(DungeonRoomGeometry geometry) {
			return new GeometryPreparation(true, "", geometry);
		}

		private static GeometryPreparation failure(String message) {
			return new GeometryPreparation(false, message == null ? "Room geometry could not be prepared." : message, null);
		}
	}

	/** Exact editor-authored placement graph; room minima are relative to the supplied world origin. */
	public record FixedLayoutSpec(ResourceLocation layoutId, List<FixedRoomSpec> rooms,
			List<FixedConnectionSpec> connections) {
		public FixedLayoutSpec {
			rooms = rooms == null ? List.of() : List.copyOf(rooms);
			connections = connections == null ? List.of() : List.copyOf(connections);
		}
	}

	/** A module definition placed at an exact relative minimum and rotation. */
	public record FixedRoomSpec(String placementKey, DungeonRoomDefinition room,
			BlockPos relativeMinimum, Rotation rotation) {
		public FixedRoomSpec {
			placementKey = placementKey == null ? "" : placementKey;
			rotation = rotation == null ? Rotation.NONE : rotation;
		}
	}

	/** One exact socket edge in the fixed editor graph. */
	public record FixedConnectionSpec(String sourcePlacement, String sourceSocket,
			String targetPlacement, String targetSocket) {
	}

	public static PlanResult plan(ServerLevel level, DungeonDataSnapshot snapshot,
			DungeonDefinition dungeon, BlockPos minimum, long seed, PlanOptions options) {
		if (snapshot == null)
			return invalidResult(dungeon == null ? new ResourceLocation("minecraft", "empty") : dungeon.id(),
					0L, seed,
					options == null ? DungeonLayoutTopology.LINEAR : options.topology(),
					PlanFailure.INVALID_ARGUMENT, "Dungeon data snapshot is required.");
		return plan(level, RoomCatalog.snapshot(snapshot), dungeon, minimum, seed, options,
				RoomTemplateResolver.loadedTemplates());
	}

	/** Plans against a server-owned custom room catalog, including unexported builder snapshots. */
	public static PlanResult plan(ServerLevel level, RoomCatalog catalog,
			DungeonDefinition dungeon, BlockPos minimum, long seed, PlanOptions options,
			RoomTemplateResolver resolver) {
		if (level == null || catalog == null || dungeon == null || minimum == null || resolver == null)
			return invalidResult(dungeon == null ? new ResourceLocation("minecraft", "empty") : dungeon.id(),
					catalog == null ? 0L : catalog.revision(), seed,
					options == null ? DungeonLayoutTopology.LINEAR : options.topology(),
					PlanFailure.INVALID_ARGUMENT, "Level, room catalog, dungeon, origin, and template resolver are required.");
		PlanOptions safeOptions = options == null ? PlanOptions.linear() : options;
		if (dungeon.kind() == DungeonKind.FIXED)
			return planFixedDefinition(level, catalog, resolver, dungeon, minimum, seed);
		if (safeOptions.topology() == DungeonLayoutTopology.FIXED)
			return invalidResult(dungeon.id(), catalog.revision(), seed, safeOptions.topology(),
					PlanFailure.INVALID_ARGUMENT, "FIXED topology requires planFixed and an explicit placement graph.");
		if (safeOptions.targetRoomCount() < 0 || safeOptions.criticalPathRooms() < 0)
			return invalidResult(dungeon.id(), catalog.revision(), seed, safeOptions.topology(),
					PlanFailure.INVALID_ARGUMENT, "Room-count overrides cannot be negative.");
		return dungeon.kind() == DungeonKind.PRESET
				? planPreset(level, catalog.revision(), dungeon, minimum, seed, safeOptions)
				: planProcedural(level, catalog, resolver, dungeon, minimum, seed, safeOptions);
	}

	/** Converts a validated fixed-layout datapack definition into the exact planner graph. */
	private static PlanResult planFixedDefinition(ServerLevel level, RoomCatalog catalog,
			RoomTemplateResolver resolver, DungeonDefinition dungeon, BlockPos minimum, long seed) {
		List<FixedRoomSpec> rooms = new ArrayList<>(dungeon.fixedPlacements().size());
		for (FixedRoomPlacement placement : dungeon.fixedPlacements()) {
			DungeonRoomDefinition room = catalog.find(placement.room());
			if (room == null)
				return invalidResult(dungeon.id(), catalog.revision(), seed, DungeonLayoutTopology.FIXED,
						PlanFailure.INVALID_DEFINITION,
						"Fixed placement " + placement.id() + " references unknown room " + placement.room() + ".");
			Int3 position = placement.position();
			rooms.add(new FixedRoomSpec(placement.id(), room,
					new BlockPos(position.x(), position.y(), position.z()), placement.rotation()));
		}
		List<FixedConnectionSpec> connections = dungeon.fixedConnections().stream()
				.map(connection -> new FixedConnectionSpec(connection.fromRoom(), connection.fromSocket(),
						connection.toRoom(), connection.toSocket()))
				.toList();
		return planFixed(level, catalog.revision(), minimum, seed,
				new FixedLayoutSpec(dungeon.id(), rooms, connections), resolver);
	}

	/**
	 * Validates and prepares an exact editor-authored placement/socket graph.
	 * The supplied room definitions may come from datapacks or a server-owned
	 * builder snapshot; their structure IDs must be available to StructureManager.
	 */
	public static PlanResult planFixed(ServerLevel level, DungeonDataSnapshot snapshot,
			BlockPos minimum, long seed, FixedLayoutSpec spec) {
		if (snapshot == null) {
			ResourceLocation layoutId = spec == null || spec.layoutId() == null
					? new ResourceLocation("minecraft", "empty") : spec.layoutId();
			return invalidResult(layoutId, 0L, seed, DungeonLayoutTopology.FIXED,
					PlanFailure.INVALID_ARGUMENT, "Dungeon data snapshot is required.");
		}
		return planFixed(level, snapshot.revision(), minimum, seed, spec,
				RoomTemplateResolver.loadedTemplates());
	}

	/** Fixed-layout entry point for a builder-owned revision and custom template resolver. */
	public static PlanResult planFixed(ServerLevel level, long revision,
			BlockPos minimum, long seed, FixedLayoutSpec spec, RoomTemplateResolver resolver) {
		ResourceLocation layoutId = spec == null || spec.layoutId() == null
				? new ResourceLocation("minecraft", "empty") : spec.layoutId();
		if (level == null || minimum == null || spec == null || spec.layoutId() == null || resolver == null)
			return invalidResult(layoutId, revision, seed, DungeonLayoutTopology.FIXED,
					PlanFailure.INVALID_ARGUMENT, "Level, origin, fixed layout spec, and template resolver are required.");
		if (spec.rooms().isEmpty() || spec.rooms().size() > 64)
			return invalidResult(layoutId, revision, seed, DungeonLayoutTopology.FIXED,
					PlanFailure.INVALID_DEFINITION, "A fixed layout requires 1-64 room placements.");

		FixedLog log = new FixedLog();
		List<FixedPreparedRoom> preparedRooms = new ArrayList<>();
		Map<String, FixedPreparedRoom> byKey = new LinkedHashMap<>();
		long totalVolume = 0L;
		for (int index = 0; index < spec.rooms().size(); index++) {
			FixedRoomSpec roomSpec = spec.rooms().get(index);
			if (roomSpec == null || roomSpec.room() == null || roomSpec.relativeMinimum() == null
					|| roomSpec.placementKey().isBlank() || roomSpec.placementKey().length() > 96) {
				log.reject(DiagnosticCode.FIXED_DUPLICATE_PLACEMENT,
						"Every fixed room needs a unique 1-96 character placement key, definition, and relative minimum.",
						roomSpec == null || roomSpec.room() == null ? null : roomSpec.room().id(),
						roomSpec == null ? null : roomSpec.rotation(), null, -1);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.INVALID_ARGUMENT, "Invalid fixed room placement.");
			}
			if (byKey.containsKey(roomSpec.placementKey())) {
				log.reject(DiagnosticCode.FIXED_DUPLICATE_PLACEMENT,
						"Duplicate fixed placement key " + roomSpec.placementKey() + ".",
						roomSpec.room().id(), roomSpec.rotation(), null, index);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.INVALID_DEFINITION, "Fixed placement keys must be unique.");
			}
			BlockPos worldMinimum = minimum.offset(roomSpec.relativeMinimum());
			GeometryPreparation preparation = resolver.prepare(level, roomSpec.room(), worldMinimum, roomSpec.rotation());
			if (!preparation.success() || preparation.geometry() == null) {
				log.reject(DiagnosticCode.MISSING_TEMPLATE, preparation.message(), roomSpec.room().id(),
						roomSpec.rotation(), null, index);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.MISSING_TEMPLATE, preparation.message());
			}
			DungeonRoomGeometry prepared = preparation.geometry();
			if (!matchesSize(prepared, roomSpec.room().size())) {
				log.reject(DiagnosticCode.SIZE_MISMATCH,
						"Declared and template sizes differ for " + roomSpec.room().id() + ".",
						roomSpec.room().id(), roomSpec.rotation(), prepared.worldBounds(), index);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.INVALID_DEFINITION,
						"Fixed room " + roomSpec.placementKey() + " has a template-size mismatch.");
			}
			if (!insideBuildHeight(level, prepared.worldBounds())) {
				log.reject(DiagnosticCode.OUT_OF_BUILD_HEIGHT,
						"Fixed room extends outside build height.", roomSpec.room().id(), roomSpec.rotation(),
						prepared.worldBounds(), index);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.OUT_OF_WORLD,
						"Fixed room " + roomSpec.placementKey() + " extends outside build height.");
			}
			FixedPreparedRoom collision = preparedRooms.stream()
					.filter(existing -> existing.geometry().worldBounds().intersects(prepared.worldBounds()))
					.findFirst().orElse(null);
			if (collision != null) {
				log.reject(DiagnosticCode.ROOM_COLLISION,
						"Fixed room " + roomSpec.placementKey() + " overlaps " + collision.spec().placementKey() + ".",
						roomSpec.room().id(), roomSpec.rotation(), prepared.worldBounds(), collision.index());
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.NO_LAYOUT, "Fixed room placements overlap.");
			}
			totalVolume += volume(prepared.worldBounds());
			if (totalVolume > MAX_TOTAL_TEMPLATE_VOLUME) {
				log.reject(DiagnosticCode.TOTAL_VOLUME_LIMIT,
						"Fixed layout exceeds the total template-volume limit.", roomSpec.room().id(),
						roomSpec.rotation(), prepared.worldBounds(), index);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						List.of(), log, PlanFailure.SAFETY_LIMIT,
						"Fixed layout exceeds the safe total template volume of " + MAX_TOTAL_TEMPLATE_VOLUME + " blocks.");
			}
			FixedPreparedRoom fixed = new FixedPreparedRoom(index, roomSpec, prepared, new HashSet<>());
			preparedRooms.add(fixed);
			byKey.put(roomSpec.placementKey(), fixed);
		}

		List<FixedPreparedRoom> starts = preparedRooms.stream()
				.filter(room -> room.spec().room().role() == RoomRole.START).toList();
		long bosses = preparedRooms.stream().filter(room -> room.spec().room().role() == RoomRole.BOSS).count();
		if (starts.size() != 1 || bosses != 1L)
			return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
					List.of(), log, PlanFailure.INVALID_DEFINITION,
					"A fixed dungeon layout requires exactly one start room and exactly one boss room.");

		List<PlannedConnection> connections = new ArrayList<>();
		Set<String> usedSocketKeys = new HashSet<>();
		List<List<Integer>> adjacency = new ArrayList<>(preparedRooms.size());
		for (int index = 0; index < preparedRooms.size(); index++)
			adjacency.add(new ArrayList<>());
		for (FixedConnectionSpec edge : spec.connections()) {
			if (edge == null) {
				log.reject(DiagnosticCode.FIXED_INVALID_CONNECTION, "Null fixed connection.", null, null, null, -1);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.INVALID_DEFINITION, "Fixed connection is invalid.");
			}
			FixedPreparedRoom source = byKey.get(edge.sourcePlacement());
			FixedPreparedRoom target = byKey.get(edge.targetPlacement());
			if (source == null || target == null || source == target) {
				log.reject(DiagnosticCode.FIXED_INVALID_CONNECTION,
						"Connection references a missing placement or connects a room to itself.", null, null, null, -1);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.INVALID_DEFINITION,
						"Fixed connection references a missing placement or the same room twice.");
			}
			Socket sourceSocket = socket(source.spec().room(), edge.sourceSocket());
			Socket targetSocket = socket(target.spec().room(), edge.targetSocket());
			if (sourceSocket == null || targetSocket == null) {
				log.reject(DiagnosticCode.FIXED_INVALID_CONNECTION,
						"Connection references a missing socket.", null, null, null, -1);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.INVALID_DEFINITION,
						"Fixed connection references a missing socket.");
			}
			String sourceUse = source.spec().placementKey() + "\u0000" + sourceSocket.id();
			String targetUse = target.spec().placementKey() + "\u0000" + targetSocket.id();
			if (!usedSocketKeys.add(sourceUse) || !usedSocketKeys.add(targetUse)) {
				log.reject(DiagnosticCode.FIXED_SOCKET_REUSED,
						"A fixed socket may participate in only one connection.", null, null, null, -1);
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.INVALID_DEFINITION,
						"A fixed socket is connected more than once.");
			}
			TransformedSocket sourceWorld = source.geometry().transformSocket(relative(sourceSocket.opening()),
					sourceSocket.facing());
			TransformedSocket targetWorld = target.geometry().transformSocket(relative(targetSocket.opening()),
					targetSocket.facing());
			if (!sourceSocket.type().equals(targetSocket.type())
					|| sourceWorld.facing() != targetWorld.facing().getOpposite()
					|| !sameOpeningShape(sourceWorld.opening(), targetWorld.opening(), sourceWorld.facing())) {
				log.reject(DiagnosticCode.FIXED_INVALID_CONNECTION,
						"Fixed sockets have incompatible type, facing, width, or height.", target.spec().room().id(),
						target.spec().rotation(), target.geometry().worldBounds(), target.index());
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.NO_LAYOUT,
						"Fixed connection sockets are incompatible.");
			}
			WorldBounds carve = union(sourceWorld.opening(), targetWorld.opening());
			if (!fixedPassageAligned(sourceWorld, targetWorld) || axisLength(carve, sourceWorld.facing()) > 64) {
				log.reject(DiagnosticCode.FIXED_PASSAGE_MISALIGNED,
						"Connected openings are not aligned outward or their passage is longer than 64 blocks.",
						target.spec().room().id(), target.spec().rotation(), carve, target.index());
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.NO_LAYOUT,
						"Fixed connection openings must align on one axis and stay within 64 blocks.");
			}
			FixedPreparedRoom intersectedRoom = preparedRooms.stream()
					.filter(room -> room != source && room != target)
					.filter(room -> room.geometry().worldBounds().intersects(carve))
					.findFirst().orElse(null);
			if (intersectedRoom != null) {
				log.reject(DiagnosticCode.FIXED_PASSAGE_COLLISION,
						"Passage intersects unrelated room " + intersectedRoom.spec().placementKey() + ".",
						target.spec().room().id(), target.spec().rotation(), carve, intersectedRoom.index());
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.NO_LAYOUT,
						"A fixed passage intersects an unrelated room.");
			}
			PlannedConnection intersectedPassage = connections.stream()
					.filter(connection -> connection.bounds().intersects(carve))
					.findFirst().orElse(null);
			if (intersectedPassage != null) {
				log.reject(DiagnosticCode.FIXED_PASSAGE_COLLISION,
						"Passage intersects an existing passage.", target.spec().room().id(),
						target.spec().rotation(), carve, intersectedPassage.targetRoom());
				return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
						connections, log, PlanFailure.NO_LAYOUT,
						"Fixed passages may not cross or overlap.");
			}
			source.usedSockets().add(sourceSocket.id());
			target.usedSockets().add(targetSocket.id());
			connections.add(new PlannedConnection(source.index(), sourceSocket.id(), target.index(),
					targetSocket.id(), carve));
			adjacency.get(source.index()).add(target.index());
			adjacency.get(target.index()).add(source.index());
		}

		int[] depths = new int[preparedRooms.size()];
		Arrays.fill(depths, -1);
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		depths[starts.get(0).index()] = 0;
		queue.add(starts.get(0).index());
		while (!queue.isEmpty()) {
			int current = queue.removeFirst();
			for (int neighbor : adjacency.get(current)) {
				if (depths[neighbor] >= 0)
					continue;
				depths[neighbor] = depths[current] + 1;
				queue.addLast(neighbor);
			}
		}
		if (Arrays.stream(depths).anyMatch(depth -> depth < 0)) {
			log.reject(DiagnosticCode.FIXED_GRAPH_DISCONNECTED,
					"Every fixed room must be reachable from the start room.", null, null, null, -1);
			return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
					connections, log, PlanFailure.NO_LAYOUT, "Fixed room graph is disconnected.");
		}

		for (FixedPreparedRoom room : preparedRooms) {
			for (Socket socket : room.spec().room().sockets()) {
				if (socket.required() && !room.usedSockets().contains(socket.id())) {
					log.reject(DiagnosticCode.REQUIRED_SOCKET_UNUSED,
							"Required socket " + socket.id() + " in " + room.spec().placementKey() + " is unused.",
							room.spec().room().id(), room.spec().rotation(), room.geometry().worldBounds(), room.index());
					return fixedFailure(layoutId, revision, seed, minimum, spec.rooms().size(), preparedRooms,
							connections, log, PlanFailure.REQUIRED_SOCKET_UNUSED,
							"Every required socket in a fixed layout must be connected.");
				}
			}
		}

		List<PlannedRoom> rooms = fixedPlannedRooms(preparedRooms, depths);
		int capped = 0;
		for (FixedPreparedRoom room : preparedRooms)
			for (Socket socket : room.spec().room().sockets())
				if (!socket.required() && !room.usedSockets().contains(socket.id()))
					capped++;
		if (capped > 0)
			log.add(DiagnosticCode.OPTIONAL_SOCKET_CAPPED, capped);
		DungeonLayoutPlan layout = new DungeonLayoutPlan(rooms, connections);
		DungeonLayoutPreview preview = preview(true, PlanFailure.NONE, "", layoutId, null, revision, seed,
				DungeonLayoutTopology.FIXED, rooms.size(), rooms, connections, log.diagnostics(minimum), minimum);
		return new PlanResult(true, PlanFailure.NONE, "", layout, preview);
	}

	private static PlanResult planPreset(ServerLevel level, long revision,
			DungeonDefinition dungeon, BlockPos minimum, long seed, PlanOptions options) {
		if (dungeon.structure().isEmpty())
			return invalidResult(dungeon.id(), revision, seed, options.topology(),
					PlanFailure.INVALID_DEFINITION, "Preset dungeon has no structure.");
		PreparationResult prepared = DungeonTemplatePlacer.prepare(level, dungeon.structure().get(), minimum, Rotation.NONE);
		if (!prepared.success() || prepared.template() == null)
			return invalidResult(dungeon.id(), revision, seed, options.topology(),
					PlanFailure.MISSING_TEMPLATE, prepared.message());
		if (dungeon.size().isPresent() && !matchesSize(prepared.template(), dungeon.size().get()))
			return invalidResult(dungeon.id(), revision, seed, options.topology(),
					PlanFailure.INVALID_DEFINITION,
					"Definition size " + displaySize(dungeon.size().get())
							+ " does not match structure template size " + displaySize(prepared.template())
							+ ". Re-export the dungeon or correct its JSON size.");
		PlannedRoom room = PlannedRoom.preset(dungeon.id(), prepared.template());
		if (!insideBuildHeight(level, room.prepared().worldBounds()))
			return invalidResult(dungeon.id(), revision, seed, options.topology(),
					PlanFailure.OUT_OF_WORLD, "Preset structure would extend outside build height.");
		DungeonLayoutPlan layout = new DungeonLayoutPlan(List.of(room), List.of());
		DungeonLayoutPreview preview = preview(true, PlanFailure.NONE, "", dungeon.id(), dungeon, revision,
				seed, options.topology(), 1, layout.rooms(), layout.connections(),
				DungeonLayoutPreview.Diagnostics.empty(), minimum);
		return new PlanResult(true, PlanFailure.NONE, "", layout, preview);
	}

	private static PlanResult planProcedural(ServerLevel level, RoomCatalog catalog,
			RoomTemplateResolver resolver, DungeonDefinition dungeon, BlockPos minimum, long seed, PlanOptions options) {
		long layoutSeed = DungeonRandomStreams.seed(seed, DungeonRandomStreams.Stream.LAYOUT);
		int targetRooms = options.targetRoomCount() > 0 ? options.targetRoomCount()
				: dungeon.roomCount().random(RandomSource.create(DungeonRandomStreams.mix(layoutSeed ^ TARGET_ROOMS_SALT)));
		if (targetRooms < 3 || !dungeon.roomCount().contains(targetRooms))
			return invalidResult(dungeon.id(), catalog.revision(), seed, options.topology(),
					PlanFailure.INVALID_DEFINITION,
					"Procedural room count " + targetRooms + " must be inside the definition range and leave room for start, middle, and boss rooms.");

		List<RoomChoice> starts = resolveChoices(dungeon.rooms(RoomRole.START), catalog);
		List<RoomChoice> bosses = resolveChoices(dungeon.rooms(RoomRole.BOSS), catalog);
		List<WeightedRoom> middlePool = new ArrayList<>();
		for (RoomRole role : RoomRole.values()) {
			if (role != RoomRole.START && role != RoomRole.BOSS && role != RoomRole.CAP)
				middlePool.addAll(dungeon.rooms(role));
		}
		List<RoomChoice> middle = resolveChoices(middlePool, catalog);
		if (starts.isEmpty() || bosses.isEmpty() || middle.isEmpty())
			return invalidResult(dungeon.id(), catalog.revision(), seed, options.topology(),
					PlanFailure.INVALID_DEFINITION,
					"Procedural dungeon requires resolvable start, middle, and boss room pools.");

		int totalMiddle = targetRooms - 2;
		int branchRooms = 0;
		int criticalMiddle = totalMiddle;
		if (options.topology() == DungeonLayoutTopology.BRANCHING) {
			int requestedCritical = options.criticalPathRooms();
			if (requestedCritical > 0) {
				if (requestedCritical < 3 || requestedCritical >= targetRooms)
					return invalidResult(dungeon.id(), catalog.revision(), seed, options.topology(),
							PlanFailure.INVALID_ARGUMENT,
							"Branching criticalPathRooms must be at least 3 and smaller than the total room count.");
				criticalMiddle = requestedCritical - 2;
				branchRooms = targetRooms - requestedCritical;
			} else {
				branchRooms = Math.max(1, totalMiddle / 3);
				criticalMiddle = totalMiddle - branchRooms;
			}
			if (criticalMiddle < 1 || branchRooms < 1)
				return invalidResult(dungeon.id(), catalog.revision(), seed, options.topology(),
						PlanFailure.NO_LAYOUT,
						"Branching topology needs room budget for at least one critical-path middle room and one side room.");
		}

		SearchContext context = new SearchContext(level, catalog, resolver, dungeon, minimum, seed,
				layoutSeed, options.topology(), targetRooms);
		SearchState solved = null;
		List<RoomChoice> orderedStarts = weightedOrder(starts,
				DungeonRandomStreams.mix(layoutSeed ^ START_ORDER_SALT));
		List<Rotation> startRotations = shuffled(HORIZONTAL_ROTATIONS,
				DungeonRandomStreams.mix(layoutSeed ^ START_ORDER_SALT ^ 0x3141_5926L));
		for (RoomChoice choice : orderedStarts) {
			for (Rotation rotation : startRotations) {
				if (context.budgetExhausted())
					break;
				GeometryPreparation prepared = context.prepare(choice.definition(), minimum, rotation);
				if (!prepared.success() || prepared.geometry() == null) {
					context.reject(DiagnosticCode.MISSING_TEMPLATE, prepared.message(),
							choice.definition().id(), rotation, null, -1);
					continue;
				}
				DungeonRoomGeometry geometry = prepared.geometry();
				if (!matchesSize(geometry, choice.definition().size())) {
					context.reject(DiagnosticCode.SIZE_MISMATCH,
							"Declared and template sizes differ for start room " + choice.definition().id() + ".",
							choice.definition().id(), rotation, geometry.worldBounds(), -1);
					continue;
				}
				if (!insideBuildHeight(level, geometry.worldBounds())) {
					context.reject(DiagnosticCode.OUT_OF_BUILD_HEIGHT,
							"Start room extends outside build height.", choice.definition().id(), rotation,
							geometry.worldBounds(), -1);
					continue;
				}
				PlannedRoom start = new PlannedRoom(0, 0, choice.definition(), geometry, Set.of());
				SearchState initial = new SearchState(List.of(start), List.of(),
						volume(geometry.worldBounds()));
				context.observe(initial);
				solved = searchCriticalPath(context, initial, 0, criticalMiddle,
						branchRooms, middle, bosses);
				if (solved != null)
					break;
			}
			if (solved != null || context.budgetExhausted())
				break;
		}

		if (solved != null) {
			context.countCappedOptionalSockets(solved);
			DungeonLayoutPlan layout = solved.toPlan();
			DungeonLayoutPreview preview = preview(true, PlanFailure.NONE, "", dungeon.id(), dungeon,
					catalog.revision(), seed, options.topology(), targetRooms,
					layout.rooms(), layout.connections(), context.diagnostics(minimum), minimum);
			return new PlanResult(true, PlanFailure.NONE, "", layout, preview);
		}

		SearchState partial = context.bestState();
		List<PlannedRoom> partialRooms = partial == null ? List.of() : partial.rooms();
		List<PlannedConnection> partialConnections = partial == null ? List.of() : partial.connections();
		PlanFailure failure = context.failure();
		String message = context.failureMessage(options.topology());
		DungeonLayoutPreview preview = preview(false, failure, message, dungeon.id(), dungeon, catalog.revision(),
				seed, options.topology(), targetRooms, partialRooms, partialConnections,
				context.diagnostics(minimum), minimum);
		return new PlanResult(false, failure, message, null, preview);
	}

	@Nullable
	private static SearchState searchCriticalPath(SearchContext context, SearchState state,
			int endpointRoom, int middleRemaining, int branchRooms,
			List<RoomChoice> middleChoices, List<RoomChoice> bossChoices) {
		if (!context.enterNode())
			return null;
		context.observe(state);
		if (middleRemaining == 0) {
			List<Attachment> bosses = attachments(context, state, List.of(endpointRoom), bossChoices,
					BOSS_ORDER_SALT, true, false);
			if (bosses.isEmpty())
				context.reject(DiagnosticCode.DEAD_END_BEFORE_TARGET,
						"The critical path cannot attach a boss room.", null, null, null, -1);
			for (Attachment boss : bosses) {
				SearchState withBoss = state.apply(boss);
				context.observe(withBoss);
				if (branchRooms == 0) {
					if (requiredSocketsUsed(context, withBoss))
						return withBoss;
				} else {
					if (!requiredSocketsUsed(context, withBoss))
						continue;
					SearchState branched = searchBranches(context, withBoss, branchRooms, middleChoices);
					if (branched != null)
						return branched;
				}
			}
			return null;
		}

		List<Attachment> attachments = attachments(context, state, List.of(endpointRoom), middleChoices,
				PATH_ORDER_SALT ^ middleRemaining, false, false);
		if (attachments.isEmpty())
			context.reject(DiagnosticCode.DEAD_END_BEFORE_TARGET,
					"A critical-path room has no compatible onward attachment.", null, null, null, -1);
		for (Attachment attachment : attachments) {
			SearchState next = state.apply(attachment);
			SearchState solved = searchCriticalPath(context, next, attachment.room().index(),
					middleRemaining - 1, branchRooms, middleChoices, bossChoices);
			if (solved != null)
				return solved;
			if (context.budgetExhausted())
				return null;
		}
		return null;
	}

	@Nullable
	private static SearchState searchBranches(SearchContext context, SearchState state,
			int roomsRemaining, List<RoomChoice> middleChoices) {
		if (!context.enterNode())
			return null;
		context.observe(state);
		if (roomsRemaining == 0)
			return requiredSocketsUsed(context, state) ? state : null;

		List<Integer> sourceRooms = new ArrayList<>();
		for (PlannedRoom room : state.rooms()) {
			if (!room.preset() && room.definition().role() != RoomRole.BOSS)
				sourceRooms.add(room.index());
		}
		List<Attachment> attachments = attachments(context, state, sourceRooms, middleChoices,
				BRANCH_ORDER_SALT ^ roomsRemaining, false, true);
		if (attachments.isEmpty())
			context.reject(DiagnosticCode.BRANCH_SOCKET_UNAVAILABLE,
					"No compatible optional branch socket can place the remaining " + roomsRemaining + " room(s).",
					null, null, null, -1);
		for (Attachment attachment : attachments) {
			SearchState next = state.apply(attachment);
			SearchState solved = searchBranches(context, next, roomsRemaining - 1, middleChoices);
			if (solved != null)
				return solved;
			if (context.budgetExhausted())
				return null;
		}
		return null;
	}

	private static List<Attachment> attachments(SearchContext context, SearchState state,
			List<Integer> sourceRoomIndexes, List<RoomChoice> choices, long orderSalt,
			boolean boss, boolean branchPhase) {
		long stateSeed = DungeonRandomStreams.mix(context.layoutSeed() ^ orderSalt ^ fingerprint(state));
		List<SourceSocket> sources = sourceSockets(state, sourceRoomIndexes, branchPhase);
		if (sources.isEmpty()) {
			context.reject(DiagnosticCode.NO_SOURCE_SOCKET,
					branchPhase ? "No unused branch socket is available." : "No unused path socket is available.",
					null, null, null, -1);
			return List.of();
		}
		sources = shuffled(sources, DungeonRandomStreams.mix(stateSeed ^ 0x511E_9D23L));
		List<RoomChoice> orderedChoices = weightedOrder(choices,
				DungeonRandomStreams.mix(stateSeed ^ 0xA47C_182FL));
		List<Attachment> result = new ArrayList<>();
		int combinations = 0;

		outer:
		for (SourceSocket source : sources) {
			if (source.room().depth() >= context.dungeon().maxDepth()) {
				context.reject(DiagnosticCode.DEPTH_LIMIT,
						"Socket " + source.socket().id() + " is at max depth.",
						source.room().definitionId(), source.room().geometry().rotation(),
						source.room().geometry().worldBounds(), source.room().index());
				continue;
			}
			for (RoomChoice choice : orderedChoices) {
				DungeonRoomDefinition candidate = choice.definition();
				List<Socket> candidateSockets = candidate.sockets().stream().filter(Socket::required).toList();
				if (candidateSockets.isEmpty())
					candidateSockets = candidate.sockets();
				candidateSockets = shuffled(candidateSockets,
						DungeonRandomStreams.mix(stateSeed ^ candidate.id().hashCode() ^ source.socket().id().hashCode()));
				List<Rotation> rotations = shuffled(HORIZONTAL_ROTATIONS,
						DungeonRandomStreams.mix(stateSeed ^ candidate.structure().hashCode() ^ 0x792B_A31DL));
				for (Socket candidateSocket : candidateSockets) {
					if (!source.socket().type().equals(candidateSocket.type())) {
						context.increment(DiagnosticCode.SOCKET_TYPE_MISMATCH);
						continue;
					}
					for (Rotation rotation : rotations) {
						if (++combinations > MAX_COMBINATIONS_PER_STATE) {
							context.reject(DiagnosticCode.CANDIDATE_LIMIT_REACHED,
									"Candidate enumeration reached the per-state safety limit.",
									candidate.id(), rotation, null, -1);
							break outer;
						}
						GeometryPreparation zeroResult = context.zeroVariant(candidate, rotation);
						if (!zeroResult.success() || zeroResult.geometry() == null) {
							context.reject(DiagnosticCode.MISSING_TEMPLATE, zeroResult.message(),
									candidate.id(), rotation, null, -1);
							continue;
						}
						DungeonRoomGeometry zero = zeroResult.geometry();
						if (!matchesSize(zero, candidate.size())) {
							context.reject(DiagnosticCode.SIZE_MISMATCH,
									"Declared and template sizes differ for " + candidate.id() + ".",
									candidate.id(), rotation, zero.worldBounds(), -1);
							continue;
						}
						TransformedSocket zeroSocket = zero.transformSocket(relative(candidateSocket.opening()),
								candidateSocket.facing());
						if (zeroSocket.facing() != source.world().facing().getOpposite()) {
							context.increment(DiagnosticCode.SOCKET_FACING_MISMATCH);
							continue;
						}
						if (!sameOpeningShape(source.world().opening(), zeroSocket.opening(), source.world().facing())) {
							context.increment(DiagnosticCode.OPENING_SHAPE_MISMATCH);
							continue;
						}
						BlockPos candidateMinimum = adjacentMinimum(source, zeroSocket, zero.rotatedSize());
						GeometryPreparation placedResult = context.prepare(candidate, candidateMinimum, rotation);
						if (!placedResult.success() || placedResult.geometry() == null) {
							context.reject(DiagnosticCode.MISSING_TEMPLATE, placedResult.message(),
									candidate.id(), rotation, null, -1);
							continue;
						}
						DungeonRoomGeometry prepared = placedResult.geometry();
						if (!insideBuildHeight(context.level(), prepared.worldBounds())) {
							context.reject(DiagnosticCode.OUT_OF_BUILD_HEIGHT,
									"Candidate room extends outside build height.", candidate.id(), rotation,
									prepared.worldBounds(), -1);
							continue;
						}
						PlannedRoom collision = state.rooms().stream()
								.filter(existing -> existing.geometry().worldBounds().intersects(prepared.worldBounds()))
								.findFirst().orElse(null);
						if (collision != null) {
							context.reject(DiagnosticCode.ROOM_COLLISION,
									"Candidate " + candidate.id() + " overlaps room " + collision.index() + ".",
									candidate.id(), rotation, prepared.worldBounds(), collision.index());
							continue;
						}
						long nextVolume = state.totalVolume() + volume(prepared.worldBounds());
						if (nextVolume > MAX_TOTAL_TEMPLATE_VOLUME) {
							context.reject(DiagnosticCode.TOTAL_VOLUME_LIMIT,
									"Candidate would exceed the total template-volume limit.", candidate.id(), rotation,
									prepared.worldBounds(), -1);
							continue;
						}
						int roomIndex = state.rooms().size();
						PlannedRoom room = new PlannedRoom(roomIndex, source.room().depth() + 1,
								candidate, prepared, Set.of(candidateSocket.id()));
						TransformedSocket targetWorld = prepared.transformSocket(relative(candidateSocket.opening()),
								candidateSocket.facing());
						WorldBounds carve = union(source.world().opening(), targetWorld.opening());
						result.add(new Attachment(source, room,
								new PlannedConnection(source.room().index(), source.socket().id(), room.index(),
										candidateSocket.id(), carve), nextVolume));
						if (result.size() >= MAX_ATTACHMENTS_PER_STATE) {
							context.reject(DiagnosticCode.CANDIDATE_LIMIT_REACHED,
									"Feasible attachment list reached the per-state safety limit.",
									candidate.id(), rotation, prepared.worldBounds(), -1);
							break outer;
						}
					}
				}
			}
		}
		return result;
	}

	private static List<SourceSocket> sourceSockets(SearchState state, List<Integer> roomIndexes,
			boolean branchPhase) {
		List<SourceSocket> result = new ArrayList<>();
		for (int roomIndex : roomIndexes) {
			if (roomIndex < 0 || roomIndex >= state.rooms().size())
				continue;
			PlannedRoom room = state.rooms().get(roomIndex);
			if (room.preset())
				continue;
			for (Socket socket : room.definition().sockets()) {
				if (room.usedSockets().contains(socket.id()))
					continue;
				result.add(new SourceSocket(room, socket,
						room.geometry().transformSocket(relative(socket.opening()), socket.facing())));
			}
		}
		List<SourceSocket> required = result.stream().filter(source -> source.socket().required()).toList();
		if (!required.isEmpty())
			return required;
		if (branchPhase)
			return result.stream().filter(source -> !source.socket().required()).toList();
		return result;
	}

	private static boolean requiredSocketsUsed(SearchContext context, SearchState state) {
		boolean valid = true;
		for (PlannedRoom room : state.rooms()) {
			if (room.preset())
				continue;
			for (Socket socket : room.definition().sockets()) {
				if (socket.required() && !room.usedSockets().contains(socket.id())) {
					context.reject(DiagnosticCode.REQUIRED_SOCKET_UNUSED,
							"Required socket " + socket.id() + " in " + room.definitionId() + " is unused.",
							room.definitionId(), room.geometry().rotation(), room.geometry().worldBounds(), room.index());
					valid = false;
				}
			}
		}
		return valid;
	}

	private static List<RoomChoice> resolveChoices(List<WeightedRoom> choices, RoomCatalog catalog) {
		Map<ResourceLocation, Integer> combined = new LinkedHashMap<>();
		for (WeightedRoom choice : choices) {
			if (catalog.find(choice.room()) != null)
				combined.merge(choice.room(), Math.max(1, choice.weight()),
						(left, right) -> (int) Math.min(Integer.MAX_VALUE, (long) left + right));
		}
		List<RoomChoice> result = new ArrayList<>();
		combined.forEach((id, weight) -> {
			DungeonRoomDefinition definition = catalog.find(id);
			if (definition != null)
				result.add(new RoomChoice(definition, weight));
		});
		return result;
	}

	private static List<RoomChoice> weightedOrder(List<RoomChoice> source, long seed) {
		List<RoomChoice> remaining = new ArrayList<>(source);
		List<RoomChoice> result = new ArrayList<>(source.size());
		RandomSource random = RandomSource.create(seed);
		while (!remaining.isEmpty()) {
			long total = remaining.stream().mapToLong(choice -> Math.max(1, choice.weight())).sum();
			long roll = Math.floorMod(random.nextLong(), Math.max(1L, total));
			int selected = remaining.size() - 1;
			for (int index = 0; index < remaining.size(); index++) {
				roll -= Math.max(1, remaining.get(index).weight());
				if (roll < 0L) {
					selected = index;
					break;
				}
			}
			result.add(remaining.remove(selected));
		}
		return result;
	}

	private static <T> List<T> shuffled(Collection<T> source, long seed) {
		List<T> result = new ArrayList<>(source);
		RandomSource random = RandomSource.create(seed);
		for (int index = result.size() - 1; index > 0; index--) {
			int other = random.nextInt(index + 1);
			T value = result.get(index);
			result.set(index, result.get(other));
			result.set(other, value);
		}
		return result;
	}

	private static long fingerprint(SearchState state) {
		long result = 0x6A09_E667_F3BC_C909L;
		BlockPos origin = state.rooms().isEmpty() ? BlockPos.ZERO
				: state.rooms().get(0).geometry().worldBounds().min();
		for (PlannedRoom room : state.rooms()) {
			WorldBounds bounds = room.geometry().worldBounds();
			result = DungeonRandomStreams.mix(result ^ room.definitionId().hashCode());
			result = DungeonRandomStreams.mix(result ^ room.geometry().rotation().ordinal());
			result = DungeonRandomStreams.mix(result ^ ((long) bounds.min().getX() - origin.getX()));
			result = DungeonRandomStreams.mix(result ^ ((long) bounds.min().getY() - origin.getY()));
			result = DungeonRandomStreams.mix(result ^ ((long) bounds.min().getZ() - origin.getZ()));
			result = DungeonRandomStreams.mix(result ^ ((long) bounds.max().getX() - origin.getX()));
			result = DungeonRandomStreams.mix(result ^ ((long) bounds.max().getY() - origin.getY()));
			result = DungeonRandomStreams.mix(result ^ ((long) bounds.max().getZ() - origin.getZ()));
			List<String> used = room.usedSockets().stream().sorted().toList();
			for (String socket : used)
				result = DungeonRandomStreams.mix(result ^ socket.hashCode());
		}
		return result;
	}

	private static BlockPos adjacentMinimum(SourceSocket source, TransformedSocket candidateAtZero,
			net.minecraft.core.Vec3i rotatedSize) {
		WorldBounds sourceRoom = source.room().geometry().worldBounds();
		WorldBounds sourceOpening = source.world().opening();
		WorldBounds candidateOpening = candidateAtZero.opening();
		Direction direction = source.world().facing();
		int x = sourceOpening.min().getX() - candidateOpening.min().getX();
		int y = sourceOpening.min().getY() - candidateOpening.min().getY();
		int z = sourceOpening.min().getZ() - candidateOpening.min().getZ();
		switch (direction) {
			case EAST -> x = sourceRoom.max().getX() + 1;
			case WEST -> x = sourceRoom.min().getX() - rotatedSize.getX();
			case UP -> y = sourceRoom.max().getY() + 1;
			case DOWN -> y = sourceRoom.min().getY() - rotatedSize.getY();
			case SOUTH -> z = sourceRoom.max().getZ() + 1;
			case NORTH -> z = sourceRoom.min().getZ() - rotatedSize.getZ();
		}
		return new BlockPos(x, y, z);
	}

	private static boolean sameOpeningShape(WorldBounds first, WorldBounds second, Direction direction) {
		return switch (direction.getAxis()) {
			case X -> first.sizeY() == second.sizeY() && first.sizeZ() == second.sizeZ();
			case Y -> first.sizeX() == second.sizeX() && first.sizeZ() == second.sizeZ();
			case Z -> first.sizeX() == second.sizeX() && first.sizeY() == second.sizeY();
		};
	}

	private static boolean matchesSize(PreparedTemplate prepared, Int3 declared) {
		return prepared.sourceSize().getX() == declared.x() && prepared.sourceSize().getY() == declared.y()
				&& prepared.sourceSize().getZ() == declared.z();
	}

	private static boolean matchesSize(DungeonRoomGeometry prepared, Int3 declared) {
		return prepared.sourceSize().getX() == declared.x() && prepared.sourceSize().getY() == declared.y()
				&& prepared.sourceSize().getZ() == declared.z();
	}

	private static String displaySize(PreparedTemplate prepared) {
		return prepared.sourceSize().getX() + "x" + prepared.sourceSize().getY() + "x" + prepared.sourceSize().getZ();
	}

	private static String displaySize(Int3 size) {
		return size.x() + "x" + size.y() + "x" + size.z();
	}

	private static boolean insideBuildHeight(ServerLevel level, WorldBounds bounds) {
		return bounds.min().getY() >= level.getMinBuildHeight() && bounds.max().getY() < level.getMaxBuildHeight();
	}

	private static long volume(WorldBounds bounds) {
		return (long) bounds.sizeX() * bounds.sizeY() * bounds.sizeZ();
	}

	private static BlockPos block(Int3 vector) {
		return new BlockPos(vector.x(), vector.y(), vector.z());
	}

	private static RelativeBounds relative(Bounds3 bounds) {
		return new RelativeBounds(block(bounds.min()), block(bounds.max()));
	}

	private static WorldBounds union(WorldBounds first, WorldBounds second) {
		return new WorldBounds(new BlockPos(Math.min(first.min().getX(), second.min().getX()),
				Math.min(first.min().getY(), second.min().getY()), Math.min(first.min().getZ(), second.min().getZ())),
				new BlockPos(Math.max(first.max().getX(), second.max().getX()),
						Math.max(first.max().getY(), second.max().getY()), Math.max(first.max().getZ(), second.max().getZ())));
	}

	private static PlanResult invalidResult(ResourceLocation dungeonId, long revision, long seed,
			DungeonLayoutTopology topology, PlanFailure failure, String message) {
		DungeonLayoutPreview preview = new DungeonLayoutPreview(false, failure, message, dungeonId,
				revision, seed, topology, 0, List.of(), List.of(), DungeonLayoutPreview.Diagnostics.empty());
		return new PlanResult(false, failure, message, null, preview);
	}

	private static DungeonLayoutPreview preview(boolean success, PlanFailure failure, String message,
			ResourceLocation dungeonId, @Nullable DungeonDefinition dungeon, long revision, long seed, DungeonLayoutTopology topology,
			int targetRooms, List<PlannedRoom> rooms, List<PlannedConnection> connections,
			DungeonLayoutPreview.Diagnostics diagnostics, BlockPos origin) {
		List<DungeonLayoutPreview.RoomView> roomViews = new ArrayList<>();
		for (PlannedRoom room : rooms) {
			List<Socket> sockets = room.preset() && dungeon != null ? dungeon.sockets()
					: room.preset() ? List.of() : room.definition().sockets();
			List<Marker> markers = room.preset() && dungeon != null ? dungeon.markers()
					: room.preset() ? List.of() : room.definition().markers();
			List<DungeonLayoutPreview.SocketView> socketViews = new ArrayList<>();
			for (Socket socket : sockets) {
				TransformedSocket transformed = room.geometry().transformSocket(relative(socket.opening()), socket.facing());
				socketViews.add(new DungeonLayoutPreview.SocketView(socket.id(), socket.type(), socket.required(),
						room.usedSockets().contains(socket.id()), transformed.facing(),
						relativeBounds(transformed.opening(), origin)));
			}
			List<DungeonLayoutPreview.MarkerView> markerViews = new ArrayList<>();
			for (Marker marker : markers)
				markerViews.add(new DungeonLayoutPreview.MarkerView(marker.id(), marker.type(), marker.group(),
						relativePosition(room.geometry().transformRelative(block(marker.position())), origin)));
			String role = room.preset() ? "preset" : room.definition().role().name().toLowerCase(java.util.Locale.ROOT);
			roomViews.add(new DungeonLayoutPreview.RoomView(room.index(), room.depth(), room.placementKey(), room.definitionId(),
					room.geometry().templateId(), role, room.geometry().rotation(),
					relativeBounds(room.geometry().worldBounds(), origin), socketViews, markerViews));
		}
		List<DungeonLayoutPreview.ConnectionView> connectionViews = connections.stream()
				.map(connection -> new DungeonLayoutPreview.ConnectionView(connection.sourceRoom(),
						connection.sourceSocket(), connection.targetRoom(), connection.targetSocket(),
						relativeBounds(connection.bounds(), origin))).toList();
		return new DungeonLayoutPreview(success, failure, message, dungeonId, revision, seed,
				topology, targetRooms, roomViews, connectionViews, diagnostics);
	}

	private static PlanResult fixedFailure(ResourceLocation layoutId, long revision, long seed,
			BlockPos origin, int targetRooms, List<FixedPreparedRoom> preparedRooms,
			List<PlannedConnection> connections, FixedLog log, PlanFailure failure, String message) {
		int[] depths = new int[preparedRooms.size()];
		List<PlannedRoom> rooms = fixedPlannedRooms(preparedRooms, depths);
		DungeonLayoutPreview preview = preview(false, failure, message, layoutId, null, revision, seed,
				DungeonLayoutTopology.FIXED, targetRooms, rooms, connections, log.diagnostics(origin), origin);
		return new PlanResult(false, failure, message, null, preview);
	}

	private static List<PlannedRoom> fixedPlannedRooms(List<FixedPreparedRoom> source, int[] depths) {
		List<PlannedRoom> result = new ArrayList<>(source.size());
		for (FixedPreparedRoom room : source) {
			int depth = room.index() < depths.length ? Math.max(0, depths[room.index()]) : 0;
			result.add(new PlannedRoom(room.spec().placementKey(), room.index(), depth,
					room.spec().room(), room.geometry(), room.usedSockets()));
		}
		return result;
	}

	@Nullable
	private static Socket socket(DungeonRoomDefinition room, String id) {
		if (id == null)
			return null;
		return room.sockets().stream().filter(socket -> socket.id().equals(id)).findFirst().orElse(null);
	}

	private static boolean fixedPassageAligned(TransformedSocket source, TransformedSocket target) {
		WorldBounds first = source.opening();
		WorldBounds second = target.opening();
		return switch (source.facing()) {
			case EAST -> first.min().getY() == second.min().getY() && first.max().getY() == second.max().getY()
					&& first.min().getZ() == second.min().getZ() && first.max().getZ() == second.max().getZ()
					&& second.min().getX() > first.max().getX();
			case WEST -> first.min().getY() == second.min().getY() && first.max().getY() == second.max().getY()
					&& first.min().getZ() == second.min().getZ() && first.max().getZ() == second.max().getZ()
					&& second.max().getX() < first.min().getX();
			case UP -> first.min().getX() == second.min().getX() && first.max().getX() == second.max().getX()
					&& first.min().getZ() == second.min().getZ() && first.max().getZ() == second.max().getZ()
					&& second.min().getY() > first.max().getY();
			case DOWN -> first.min().getX() == second.min().getX() && first.max().getX() == second.max().getX()
					&& first.min().getZ() == second.min().getZ() && first.max().getZ() == second.max().getZ()
					&& second.max().getY() < first.min().getY();
			case SOUTH -> first.min().getX() == second.min().getX() && first.max().getX() == second.max().getX()
					&& first.min().getY() == second.min().getY() && first.max().getY() == second.max().getY()
					&& second.min().getZ() > first.max().getZ();
			case NORTH -> first.min().getX() == second.min().getX() && first.max().getX() == second.max().getX()
					&& first.min().getY() == second.min().getY() && first.max().getY() == second.max().getY()
					&& second.max().getZ() < first.min().getZ();
		};
	}

	private static int axisLength(WorldBounds bounds, Direction direction) {
		return switch (direction.getAxis()) {
			case X -> bounds.sizeX();
			case Y -> bounds.sizeY();
			case Z -> bounds.sizeZ();
		};
	}

	private static DungeonLayoutPreview.Bounds relativeBounds(WorldBounds bounds, BlockPos origin) {
		return new DungeonLayoutPreview.Bounds(relativePosition(bounds.min(), origin),
				relativePosition(bounds.max(), origin));
	}

	private static BlockPos relativePosition(BlockPos position, BlockPos origin) {
		return new BlockPos(position.getX() - origin.getX(), position.getY() - origin.getY(),
				position.getZ() - origin.getZ());
	}

	private record RoomChoice(DungeonRoomDefinition definition, int weight) {
	}

	private record SourceSocket(PlannedRoom room, Socket socket, TransformedSocket world) {
	}

	private record Attachment(SourceSocket source, PlannedRoom room,
			PlannedConnection connection, long totalVolume) {
	}

	private record VariantKey(ResourceLocation room, Rotation rotation) {
	}

	private record FixedPreparedRoom(int index, FixedRoomSpec spec, DungeonRoomGeometry geometry,
			Set<String> usedSockets) {
	}

	private static final class FixedLog {
		private final EnumMap<DiagnosticCode, Integer> counts = new EnumMap<>(DiagnosticCode.class);
		private final List<RawSample> samples = new ArrayList<>();

		private void add(DiagnosticCode code, int amount) {
			if (amount <= 0)
				return;
			counts.merge(code, amount, (left, right) -> (int) Math.min(Integer.MAX_VALUE, (long) left + right));
		}

		private void reject(DiagnosticCode code, String detail,
				@Nullable ResourceLocation candidateRoom, @Nullable Rotation rotation,
				@Nullable WorldBounds bounds, int conflictingRoom) {
			add(code, 1);
			if (samples.size() < MAX_DIAGNOSTIC_SAMPLES)
				samples.add(new RawSample(code, detail == null ? "" : detail,
						candidateRoom, rotation, bounds, conflictingRoom));
		}

		private DungeonLayoutPreview.Diagnostics diagnostics(BlockPos origin) {
			List<DungeonLayoutPreview.DiagnosticCount> countViews = counts.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(entry -> new DungeonLayoutPreview.DiagnosticCount(entry.getKey(), entry.getValue(),
							description(entry.getKey()))).toList();
			List<DungeonLayoutPreview.DiagnosticSample> sampleViews = samples.stream()
					.map(sample -> new DungeonLayoutPreview.DiagnosticSample(sample.code(), sample.detail(),
							sample.candidateRoom(), sample.rotation(),
							sample.bounds() == null ? null : relativeBounds(sample.bounds(), origin),
							sample.conflictingRoom())).toList();
			return new DungeonLayoutPreview.Diagnostics(0, false, countViews, sampleViews);
		}
	}

	private static final class SearchState {
		private final List<PlannedRoom> rooms;
		private final List<PlannedConnection> connections;
		private final long totalVolume;

		private SearchState(List<PlannedRoom> rooms, List<PlannedConnection> connections,
				long totalVolume) {
			this.rooms = List.copyOf(rooms);
			this.connections = List.copyOf(connections);
			this.totalVolume = totalVolume;
		}

		private List<PlannedRoom> rooms() {
			return rooms;
		}

		private List<PlannedConnection> connections() {
			return connections;
		}

		private long totalVolume() {
			return totalVolume;
		}

		private SearchState apply(Attachment attachment) {
			List<PlannedRoom> nextRooms = new ArrayList<>(rooms);
			int sourceIndex = attachment.source().room().index();
			nextRooms.set(sourceIndex,
					nextRooms.get(sourceIndex).withUsedSocket(attachment.source().socket().id()));
			nextRooms.add(attachment.room());
			List<PlannedConnection> nextConnections = new ArrayList<>(connections);
			nextConnections.add(attachment.connection());
			return new SearchState(nextRooms, nextConnections, attachment.totalVolume());
		}

		private DungeonLayoutPlan toPlan() {
			return new DungeonLayoutPlan(rooms, connections);
		}
	}

	private static final class SearchContext {
		private final ServerLevel level;
		private final RoomTemplateResolver resolver;
		private final DungeonDefinition dungeon;
		private final long layoutSeed;
		private final Map<VariantKey, GeometryPreparation> zeroVariants = new LinkedHashMap<>();
		private final EnumMap<DiagnosticCode, Integer> counts = new EnumMap<>(DiagnosticCode.class);
		private final List<RawSample> samples = new ArrayList<>();
		@Nullable private SearchState bestState;
		private int nodesVisited;
		private boolean budgetExhausted;

		private SearchContext(ServerLevel level, RoomCatalog catalog, RoomTemplateResolver resolver,
				DungeonDefinition dungeon, BlockPos origin, long rootSeed, long layoutSeed,
				DungeonLayoutTopology topology, int targetRooms) {
			this.level = level;
			this.resolver = resolver;
			this.dungeon = dungeon;
			this.layoutSeed = layoutSeed;
		}

		private ServerLevel level() {
			return level;
		}

		private DungeonDefinition dungeon() {
			return dungeon;
		}

		private long layoutSeed() {
			return layoutSeed;
		}

		private boolean enterNode() {
			if (budgetExhausted)
				return false;
			nodesVisited++;
			if (nodesVisited <= MAX_SEARCH_NODES)
				return true;
			budgetExhausted = true;
			increment(DiagnosticCode.SEARCH_BUDGET_EXHAUSTED);
			return false;
		}

		private boolean budgetExhausted() {
			return budgetExhausted;
		}

		private void observe(SearchState state) {
			if (bestState == null || state.rooms().size() > bestState.rooms().size()
					|| (state.rooms().size() == bestState.rooms().size()
							&& state.connections().size() > bestState.connections().size()))
				bestState = state;
		}

		@Nullable
		private SearchState bestState() {
			return bestState;
		}

		private GeometryPreparation prepare(DungeonRoomDefinition room, BlockPos minimum, Rotation rotation) {
			return resolver.prepare(level, room, minimum, rotation);
		}

		private GeometryPreparation zeroVariant(DungeonRoomDefinition room, Rotation rotation) {
			return zeroVariants.computeIfAbsent(new VariantKey(room.id(), rotation),
					key -> prepare(room, BlockPos.ZERO, key.rotation()));
		}

		private void increment(DiagnosticCode code) {
			counts.merge(code, 1, (left, right) -> left == Integer.MAX_VALUE ? left : left + right);
		}

		private void reject(DiagnosticCode code, String detail,
				@Nullable ResourceLocation candidateRoom, @Nullable Rotation rotation,
				@Nullable WorldBounds candidateBounds, int conflictingRoom) {
			increment(code);
			if (samples.size() < MAX_DIAGNOSTIC_SAMPLES)
				samples.add(new RawSample(code, detail == null ? "" : detail,
						candidateRoom, rotation, candidateBounds, conflictingRoom));
		}

		private void countCappedOptionalSockets(SearchState state) {
			int capped = 0;
			for (PlannedRoom room : state.rooms()) {
				if (room.preset())
					continue;
				for (Socket socket : room.definition().sockets())
					if (!socket.required() && !room.usedSockets().contains(socket.id()))
						capped++;
			}
			if (capped > 0)
				counts.merge(DiagnosticCode.OPTIONAL_SOCKET_CAPPED, capped, Integer::sum);
		}

		private PlanFailure failure() {
			if (budgetExhausted)
				return PlanFailure.SEARCH_BUDGET_EXHAUSTED;
			if (counts.containsKey(DiagnosticCode.REQUIRED_SOCKET_UNUSED))
				return PlanFailure.REQUIRED_SOCKET_UNUSED;
			if (bestState == null && counts.containsKey(DiagnosticCode.MISSING_TEMPLATE))
				return PlanFailure.MISSING_TEMPLATE;
			if (bestState == null && counts.containsKey(DiagnosticCode.SIZE_MISMATCH))
				return PlanFailure.INVALID_DEFINITION;
			if (bestState == null && counts.containsKey(DiagnosticCode.OUT_OF_BUILD_HEIGHT))
				return PlanFailure.OUT_OF_WORLD;
			if (counts.containsKey(DiagnosticCode.TOTAL_VOLUME_LIMIT))
				return PlanFailure.SAFETY_LIMIT;
			return PlanFailure.NO_LAYOUT;
		}

		private String failureMessage(DungeonLayoutTopology requestedTopology) {
			if (budgetExhausted)
				return "Layout search exhausted its deterministic " + MAX_SEARCH_NODES
						+ "-node safety budget. Reduce room count or room/socket variety.";
			if (requestedTopology == DungeonLayoutTopology.BRANCHING
					&& counts.containsKey(DiagnosticCode.BRANCH_SOCKET_UNAVAILABLE))
				return "Could not build the requested branching layout. Add optional side sockets and one-socket dead-end rooms that can cap branches.";
			if (counts.containsKey(DiagnosticCode.REQUIRED_SOCKET_UNUSED))
				return "No complete layout connected every required socket.";
			if (bestState == null || bestState.rooms().isEmpty())
				return "No valid start-room template and rotation could be prepared.";
			return "Could not complete a non-overlapping start-to-boss layout. Inspect simulation diagnostics for socket, collision, and depth rejections.";
		}

		private DungeonLayoutPreview.Diagnostics diagnostics(BlockPos previewOrigin) {
			List<DungeonLayoutPreview.DiagnosticCount> countViews = counts.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(entry -> new DungeonLayoutPreview.DiagnosticCount(entry.getKey(), entry.getValue(),
							description(entry.getKey()))).toList();
			List<DungeonLayoutPreview.DiagnosticSample> sampleViews = samples.stream()
					.map(sample -> new DungeonLayoutPreview.DiagnosticSample(sample.code(), sample.detail(),
							sample.candidateRoom(), sample.rotation(),
							sample.bounds() == null ? null : relativeBounds(sample.bounds(), previewOrigin),
							sample.conflictingRoom())).toList();
			return new DungeonLayoutPreview.Diagnostics(nodesVisited, budgetExhausted, countViews, sampleViews);
		}
	}

	private record RawSample(DiagnosticCode code, String detail,
			@Nullable ResourceLocation candidateRoom, @Nullable Rotation rotation,
			@Nullable WorldBounds bounds, int conflictingRoom) {
	}

	private static String description(DiagnosticCode code) {
		return switch (code) {
			case MISSING_TEMPLATE -> "Structure template could not be loaded.";
			case SIZE_MISMATCH -> "Room JSON size differs from its structure template.";
			case NO_SOURCE_SOCKET -> "No unused compatible source socket was available.";
			case DEPTH_LIMIT -> "A source socket had reached max_depth.";
			case SOCKET_TYPE_MISMATCH -> "Socket types differed.";
			case SOCKET_FACING_MISMATCH -> "Rotated sockets did not face each other.";
			case OPENING_SHAPE_MISMATCH -> "Socket opening width or height differed.";
			case OUT_OF_BUILD_HEIGHT -> "A room would extend outside build height.";
			case ROOM_COLLISION -> "A candidate room intersected an already planned room.";
			case DEAD_END_BEFORE_TARGET -> "The critical path ended before all requested rooms or boss were placed.";
			case REQUIRED_SOCKET_UNUSED -> "A required socket remained unconnected.";
			case TOTAL_VOLUME_LIMIT -> "A candidate exceeded the safe total template volume.";
			case SEARCH_BUDGET_EXHAUSTED -> "Deterministic backtracking reached its node budget.";
			case CANDIDATE_LIMIT_REACHED -> "Per-state candidate enumeration was truncated for safety.";
			case BRANCH_SOCKET_UNAVAILABLE -> "The requested branch could not be attached or capped.";
			case OPTIONAL_SOCKET_CAPPED -> "Unused optional sockets remain solid and uncarved.";
			case FIXED_DUPLICATE_PLACEMENT -> "A fixed placement key was missing or duplicated.";
			case FIXED_INVALID_CONNECTION -> "A fixed connection referenced incompatible rooms or sockets.";
			case FIXED_SOCKET_REUSED -> "A fixed socket was connected more than once.";
			case FIXED_PASSAGE_MISALIGNED -> "Fixed connection openings were not aligned along one axis.";
			case FIXED_PASSAGE_COLLISION -> "A fixed passage crossed an unrelated room or another passage.";
			case FIXED_GRAPH_DISCONNECTED -> "The fixed room graph was not fully reachable from start.";
		};
	}
}
