package net.solocraft.dungeon.builder;

import net.solocraft.dungeon.builder.model.DungeonDraft;
import net.solocraft.dungeon.builder.model.RoomSnapshot;
import net.solocraft.dungeon.data.DungeonDefinition;
import net.solocraft.dungeon.data.DungeonRoomDefinition;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPlanner;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPreview;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutTopology;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;

/** Converts unexported builder metadata into the canonical runtime planner. */
public final class DungeonBuilderStudioSimulation {
	private static final long[] COVERAGE_SEEDS = {
			0L,
			0x4D59_5DF4_D0F3_3173L,
			0x9E37_79B9_7F4A_7C15L
	};

	private DungeonBuilderStudioSimulation() {
	}

	/** Concise result shared by Studio validation and the final export gate. */
	public record CoverageResult(boolean success, int casesTested, long failedSeed,
			int targetRooms, String message) {
		public CoverageResult {
			casesTested = Math.max(0, casesTested);
			targetRooms = Math.max(0, targetRooms);
			message = message == null || message.isBlank()
					? (success ? "Planner coverage passed." : "Planner coverage failed.") : message;
		}
	}

	/** A preset has no layout decisions; preview its frozen one-room footprint directly. */
	public static CompoundTag simulatePreset(ServerPlayer player,
			DungeonBuilderProjectData.Project project, long seed) {
		Optional<RoomSnapshot> snapshot = project.roomSnapshot();
		if (snapshot.isEmpty())
			return failure(seed, "Capture the prebuilt dungeon before previewing it.");
		DungeonBuilderRoomStore.VerificationResult verification = DungeonBuilderRoomStore.verify(player, project);
		if (!verification.valid())
			return failure(seed, verification.status() + ". Capture the prebuilt dungeon again.");
		BlockPos size = snapshot.get().size();
		CompoundTag tag = new CompoundTag();
		tag.putLong("Seed", seed);
		tag.putString("Status", "SUCCESS");
		tag.putString("Message", "Prebuilt dungeon preview uses its exact captured structure.");
		tag.putInt("Attempts", 1);
		ListTag rooms = new ListTag();
		CompoundTag room = new CompoundTag();
		room.putString("Id", "room_0");
		room.putString("ProjectId", project.id());
		room.putString("Role", "START");
		room.putInt("X", 0);
		room.putInt("Z", 0);
		room.putInt("Width", size.getX());
		room.putInt("Depth", size.getZ());
		room.putInt("Rotation", 0);
		rooms.add(room);
		tag.put("Rooms", rooms);
		tag.put("Connections", new ListTag());
		tag.put("Diagnostics", new ListTag());
		return tag;
	}

	public static CompoundTag simulate(ServerPlayer player, DungeonBuilderProjectData data,
			ResourceLocation dungeonId, long seed) {
		Preparation preparation = prepare(player, data, dungeonId);
		if (!preparation.success())
			return failure(seed, preparation.message());
		return encode(plan(player, preparation.simulation(), seed, 0).preview());
	}

	/**
	 * Runs a bounded deterministic planner matrix before validation/export.
	 * Procedural drafts test every configured room count at seed zero, then
	 * min/mid/max against two additional well-spaced seeds. The 3-64 room cap
	 * keeps this at no more than 68 deduplicated plans. Fixed layouts need one
	 * seed because their graph has no random placement choices.
	 */
	public static CoverageResult validateCoverage(ServerPlayer player, DungeonBuilderProjectData data,
			ResourceLocation dungeonId) {
		Preparation preparation = prepare(player, data, dungeonId);
		if (!preparation.success())
			return new CoverageResult(false, 0, 0L, 0, preparation.message());
		PreparedSimulation simulation = preparation.simulation();
		if (simulation.draft().mode() == DungeonDraft.Mode.FIXED) {
			DungeonLayoutPlanner.PlanResult result = plan(player, simulation, 0L, 0);
			if (!result.success())
				return coverageFailure(1, 1, 0L, simulation.draft().fixedPlacements().size(), result);
			return new CoverageResult(true, 1, 0L, simulation.draft().fixedPlacements().size(),
					"Planner coverage passed 1 deterministic fixed-layout case.");
		}

		int minimum = simulation.draft().minRooms();
		int maximum = simulation.draft().maxRooms();
		LinkedHashSet<CoverageCase> cases = new LinkedHashSet<>();
		for (int targetRooms = minimum; targetRooms <= maximum; targetRooms++)
			cases.add(new CoverageCase(targetRooms, COVERAGE_SEEDS[0]));
		LinkedHashSet<Integer> representativeCounts = new LinkedHashSet<>();
		representativeCounts.add(minimum);
		representativeCounts.add(minimum + (maximum - minimum) / 2);
		representativeCounts.add(maximum);
		for (int targetRooms : representativeCounts)
			for (int seedIndex = 1; seedIndex < COVERAGE_SEEDS.length; seedIndex++)
				cases.add(new CoverageCase(targetRooms, COVERAGE_SEEDS[seedIndex]));

		int tested = 0;
		for (CoverageCase coverageCase : cases) {
			tested++;
			DungeonLayoutPlanner.PlanResult result = plan(player, simulation,
					coverageCase.seed(), coverageCase.targetRooms());
			if (!result.success())
				return coverageFailure(tested, cases.size(), coverageCase.seed(),
						coverageCase.targetRooms(), result);
		}
		return new CoverageResult(true, tested, 0L, 0,
				"Planner coverage passed " + tested + " deterministic cases: every room count "
						+ minimum + "-" + maximum + " at seed 0, plus " + representativeCounts
						+ " at " + (COVERAGE_SEEDS.length - 1) + " additional seeds.");
	}

	private static CoverageResult coverageFailure(int tested, int total, long seed, int targetRooms,
			DungeonLayoutPlanner.PlanResult result) {
		String detail = result.preview() == null || result.preview().message().isBlank()
				? result.message() : result.preview().message();
		return new CoverageResult(false, tested, seed, targetRooms,
				"Planner coverage case " + tested + "/" + total + " failed (target " + targetRooms
						+ " rooms, seed " + seed + "): " + detail);
	}

	private static DungeonLayoutPlanner.PlanResult plan(ServerPlayer player,
			PreparedSimulation simulation, long seed, int targetRooms) {
		return DungeonLayoutPlanner.plan(player.serverLevel(), simulation.catalog(), simulation.definition(),
				simulation.origin(), seed,
				new DungeonLayoutPlanner.PlanOptions(simulation.topology(), targetRooms, 0),
				DungeonLayoutPlanner.RoomTemplateResolver.declaredGeometry());
	}

	private static Preparation prepare(ServerPlayer player, DungeonBuilderProjectData data,
			ResourceLocation dungeonId) {
		Optional<DungeonDraft> optionalDraft = data.dungeonDraft(player, dungeonId);
		if (optionalDraft.isEmpty())
			return Preparation.failure("Save the Layout before simulating it.");
		DungeonDraft draft = optionalDraft.get();
		Map<String, DungeonBuilderProjectData.Project> projects = new HashMap<>();
		for (DungeonBuilderProjectData.Project project : data.projects(player))
			projects.put(project.id(), project);
		Map<ResourceLocation, DungeonRoomDefinition> rooms = new HashMap<>();
		LinkedHashSet<ResourceLocation> referencedRooms = new LinkedHashSet<>();
		if (draft.mode() == DungeonDraft.Mode.FIXED)
			draft.fixedPlacements().forEach(placement -> referencedRooms.add(placement.room()));
		else
			draft.rooms().forEach(reference -> referencedRooms.add(reference.room()));
		for (ResourceLocation roomId : referencedRooms) {
			DungeonBuilderProjectData.Project project = projects.get(roomId.toString());
			if (project == null)
				return Preparation.failure("Missing room asset " + roomId + ".");
			if (project.roomSnapshot().isEmpty())
				return Preparation.failure("Capture room " + project.id() + " before simulating this dungeon.");
			DungeonBuilderRoomStore.VerificationResult verification = DungeonBuilderRoomStore.verify(player, project);
			if (!verification.valid())
				return Preparation.failure(project.id() + ": " + verification.status()
						+ ". Capture the room again before previewing it.");
			rooms.put(roomId, room(project));
		}
		if (rooms.isEmpty())
			return Preparation.failure(draft.mode() == DungeonDraft.Mode.FIXED
					? "Place at least a start and boss room in Layout."
					: "Enable at least a start, middle, and boss room in Layout.");

		DungeonLayoutPlanner.RoomCatalog catalog = new DungeonLayoutPlanner.RoomCatalog() {
			@Override
			public DungeonRoomDefinition find(ResourceLocation id) {
				return rooms.get(id);
			}

			@Override
			public long revision() {
				return data.revision(player);
			}
		};
		BlockPos origin = new BlockPos(0, Math.max(player.serverLevel().getMinBuildHeight() + 8,
				Math.min(player.getBlockY(), player.serverLevel().getMaxBuildHeight() - 64)), 0);
		DungeonDefinition definition = draft.mode() == DungeonDraft.Mode.FIXED
				? fixedDungeon(draft) : dungeon(draft, rooms);
		DungeonLayoutTopology topology = draft.mode() == DungeonDraft.Mode.FIXED
				? DungeonLayoutTopology.FIXED
				: draft.topology() == DungeonDraft.Topology.BRANCHING
						? DungeonLayoutTopology.BRANCHING : DungeonLayoutTopology.LINEAR;
		return Preparation.success(new PreparedSimulation(draft, catalog, definition, origin, topology));
	}

	private record PreparedSimulation(DungeonDraft draft, DungeonLayoutPlanner.RoomCatalog catalog,
			DungeonDefinition definition, BlockPos origin, DungeonLayoutTopology topology) {
	}

	private record CoverageCase(int targetRooms, long seed) {
	}

	private record Preparation(@Nullable PreparedSimulation simulation, String message) {
		private static Preparation success(PreparedSimulation simulation) {
			return new Preparation(simulation, "");
		}

		private static Preparation failure(String message) {
			return new Preparation(null, message == null ? "Studio layout preparation failed." : message);
		}

		private boolean success() {
			return simulation != null;
		}
	}

	private static DungeonDefinition fixedDungeon(DungeonDraft draft) {
		List<FixedRoomPlacement> placements = draft.fixedPlacements().stream()
				.map(placement -> new FixedRoomPlacement(placement.id(), placement.room(),
						new Int3(placement.x(), placement.y(), placement.z()),
						Rotation.valueOf(placement.rotation().name())))
				.toList();
		List<FixedRoomConnection> connections = draft.fixedConnections().stream()
				.map(connection -> new FixedRoomConnection(connection.fromPlacement(), connection.fromSocket(),
						connection.toPlacement(), connection.toSocket()))
				.toList();
		int count = Math.max(1, placements.size());
		return new DungeonDefinition(draft.id(), 2, DungeonKind.FIXED, draft.allowedRanks(),
				Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				Map.of(), new IntRange(count, count), count,
				new LevelRule("owner", new IntRange(1, DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL), 0),
				new ShellSettings(draft.shellThickness() > 0, draft.shellBlock(), draft.shellThickness(), true, true),
				List.of(), List.of(), List.of(), List.of(), placements, connections);
	}

	private static DungeonDefinition dungeon(DungeonDraft draft,
			Map<ResourceLocation, DungeonRoomDefinition> rooms) {
		EnumMap<RoomRole, List<WeightedRoom>> pools = new EnumMap<>(RoomRole.class);
		for (DungeonDraft.RoomRef reference : draft.rooms()) {
			DungeonRoomDefinition room = rooms.get(reference.room());
			if (room != null)
				pools.computeIfAbsent(room.role(), ignored -> new ArrayList<>())
						.add(new WeightedRoom(reference.room(), reference.weight()));
		}
		return new DungeonDefinition(draft.id(), 2, DungeonKind.PROCEDURAL, draft.allowedRanks(),
				Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				pools, new IntRange(draft.minRooms(), draft.maxRooms()), draft.maxDepth(),
				new LevelRule("owner", new IntRange(1, DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL), 0),
				new ShellSettings(draft.shellThickness() > 0, draft.shellBlock(), draft.shellThickness(), true, true),
				List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
				draft.topology() == DungeonDraft.Topology.BRANCHING
						? DungeonTopology.BRANCHING : DungeonTopology.LINEAR);
	}

	private static DungeonRoomDefinition room(DungeonBuilderProjectData.Project project) {
		RoomSnapshot snapshot = project.roomSnapshot().orElseThrow();
		BlockPos base = snapshot.captureMin();
		Int3 size = int3(snapshot.size());
		List<Region> regions = project.regions().stream().map(region -> new Region(region.id(), region.type(),
				new Bounds3(int3(region.bounds().min().subtract(base)), int3(region.bounds().max().subtract(base))))).toList();
		List<Socket> sockets = project.sockets().stream().map(socket -> new Socket(socket.id(), socket.type(),
				new Bounds3(int3(socket.opening().min().subtract(base)), int3(socket.opening().max().subtract(base))),
				socket.facing(), socket.required(), 1)).toList();
		List<Marker> markers = project.markers().stream().map(marker -> new Marker(marker.id(), marker.type(),
				marker.group(), int3(marker.position().subtract(base)))).toList();
		return new DungeonRoomDefinition(new ResourceLocation(project.namespace(), project.name()), 2,
				snapshot.structureKey(), roomRole(project.roomRole()), project.roomWeight(), size,
				new Int3(0, 0, 0), Optional.empty(), Optional.empty(), regions, sockets, markers, List.of());
	}

	private static RoomRole roomRole(DungeonBuilderProjectData.RoomRole role) {
		try {
			return RoomRole.valueOf(role.name());
		} catch (IllegalArgumentException ignored) {
			return RoomRole.NORMAL;
		}
	}

	private static Int3 int3(BlockPos position) {
		return new Int3(position.getX(), position.getY(), position.getZ());
	}

	private static CompoundTag encode(DungeonLayoutPreview preview) {
		CompoundTag tag = new CompoundTag();
		tag.putLong("Seed", preview.seed());
		tag.putString("Status", preview.success() ? "SUCCESS" : "FAILED");
		tag.putString("Message", preview.success()
				? "Planned " + preview.rooms().size() + " rooms with " + preview.connections().size() + " connections."
				: preview.message());
		tag.putInt("Attempts", preview.diagnostics().nodesVisited());
		ListTag rooms = new ListTag();
		for (DungeonLayoutPreview.RoomView room : preview.rooms()) {
			CompoundTag value = new CompoundTag();
			value.putString("Id", "room_" + room.index());
			value.putString("ProjectId", room.definitionId().toString());
			value.putString("Role", room.role());
			value.putInt("X", room.bounds().min().getX());
			value.putInt("Z", room.bounds().min().getZ());
			value.putInt("Width", room.bounds().max().getX() - room.bounds().min().getX() + 1);
			value.putInt("Depth", room.bounds().max().getZ() - room.bounds().min().getZ() + 1);
			value.putInt("Rotation", degrees(room.rotation()));
			rooms.add(value);
		}
		tag.put("Rooms", rooms);
		ListTag connections = new ListTag();
		for (DungeonLayoutPreview.ConnectionView connection : preview.connections()) {
			CompoundTag value = new CompoundTag();
			value.putString("FromRoom", "room_" + connection.sourceRoom());
			value.putString("ToRoom", "room_" + connection.targetRoom());
			connections.add(value);
		}
		tag.put("Connections", connections);
		ListTag diagnostics = new ListTag();
		for (DungeonLayoutPreview.DiagnosticCount diagnostic : preview.diagnostics().counts()) {
			CompoundTag value = new CompoundTag();
			value.putString("Code", diagnostic.code().name());
			value.putInt("Count", diagnostic.count());
			value.putString("Message", diagnostic.description());
			diagnostics.add(value);
		}
		tag.put("Diagnostics", diagnostics);
		return tag;
	}

	private static int degrees(Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> 90;
			case CLOCKWISE_180 -> 180;
			case COUNTERCLOCKWISE_90 -> 270;
			default -> 0;
		};
	}

	private static CompoundTag failure(long seed, String message) {
		CompoundTag tag = new CompoundTag();
		tag.putLong("Seed", seed);
		tag.putString("Status", "FAILED");
		tag.putString("Message", message);
		tag.put("Rooms", new ListTag());
		tag.put("Connections", new ListTag());
		return tag;
	}
}
