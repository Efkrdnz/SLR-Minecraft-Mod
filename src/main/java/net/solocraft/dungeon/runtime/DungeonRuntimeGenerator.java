package net.solocraft.dungeon.runtime;

import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.dungeon.data.DungeonDataSnapshot;
import net.solocraft.dungeon.data.DungeonDefinition;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPlan;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPlan.PlannedConnection;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPlan.PlannedRoom;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPlanner;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutPreview;
import net.solocraft.dungeon.runtime.layout.DungeonLayoutTopology;
import net.solocraft.dungeon.runtime.layout.DungeonRandomStreams;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;
import static net.solocraft.dungeon.runtime.DungeonTemplatePlacer.*;

/** Places schema-v2 preset dungeons or assembles procedural room modules. */
public final class DungeonRuntimeGenerator {
	private static final int LEGACY_OCCUPANCY_PADDING = 4;

	private DungeonRuntimeGenerator() {
	}

	public static GenerationResult generate(ServerPlayer owner, ResourceLocation dungeonId,
			BlockPos worldMinimum, long seed, @Nullable Integer explicitLevel) {
		if (owner == null)
			return GenerationResult.failure(GenerationFailure.INVALID_ARGUMENT, "A generating player is required.");
		return generate(owner.serverLevel(), dungeonId, worldMinimum, seed, List.of(owner), explicitLevel,
				null);
	}

	public static GenerationResult generate(ServerPlayer owner, ResourceLocation dungeonId,
			BlockPos worldMinimum, long seed, @Nullable Integer explicitLevel,
			DungeonLayoutTopology topology) {
		if (owner == null)
			return GenerationResult.failure(GenerationFailure.INVALID_ARGUMENT, "A generating player is required.");
		return generate(owner.serverLevel(), dungeonId, worldMinimum, seed, List.of(owner), explicitLevel, topology);
	}

	public static GenerationResult generate(ServerLevel level, ResourceLocation dungeonId,
			BlockPos worldMinimum, long seed, Collection<ServerPlayer> participants,
			@Nullable Integer explicitLevel) {
		return generate(level, dungeonId, worldMinimum, seed, participants, explicitLevel, null);
	}

	public static GenerationResult generate(ServerLevel level, ResourceLocation dungeonId,
			BlockPos worldMinimum, long seed, Collection<ServerPlayer> participants,
			@Nullable Integer explicitLevel, @Nullable DungeonLayoutTopology topology) {
		if (level == null || dungeonId == null || worldMinimum == null)
			return GenerationResult.failure(GenerationFailure.INVALID_ARGUMENT,
					"Level, dungeon id, and world position are required.");
		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		Optional<DungeonDefinition> loaded = snapshot.dungeon(dungeonId);
		if (loaded.isEmpty())
			return GenerationResult.failure(GenerationFailure.UNKNOWN_DUNGEON,
					"Unknown or invalid dungeon definition " + dungeonId + ". Run /reload and inspect latest.log.");

		DungeonDefinition dungeon = loaded.get();
		DungeonLayoutTopology effectiveTopology = topology == null ? runtimeTopology(dungeon) : topology;
		// Gate entrants are intentionally captured before their cross-dimension teleport.
		List<ServerPlayer> safeParticipants = participants == null ? List.of() : participants.stream()
				.filter(player -> player != null && player.getServer() == level.getServer()).distinct().toList();
		if (safeParticipants.isEmpty())
			return GenerationResult.failure(GenerationFailure.INVALID_ARGUMENT,
					"At least one server player participant is required for a dungeon instance.");
		if (safeParticipants.size() > DungeonInstanceSavedData.MAX_PARTICIPANTS)
			return GenerationResult.failure(GenerationFailure.SAFETY_LIMIT,
					"A dungeon party is limited to " + DungeonInstanceSavedData.MAX_PARTICIPANTS + " participants.");
		RandomSource levelRandom = DungeonRandomStreams.random(seed, DungeonRandomStreams.Stream.LEVEL);
		RandomSource encounterRandom = DungeonRandomStreams.random(seed, DungeonRandomStreams.Stream.ENCOUNTER);
		int effectiveLevel = resolveEffectiveLevel(dungeon.level(), safeParticipants, levelRandom, explicitLevel);
		DungeonLayoutPlanner.PlanResult planning = DungeonLayoutPlanner.plan(level, snapshot, dungeon,
				worldMinimum, seed, new DungeonLayoutPlanner.PlanOptions(effectiveTopology, 0, 0));
		if (!planning.success())
			return GenerationResult.failure(mapFailure(planning.failure()), planning.message());

		DungeonLayoutPlan layout = planning.layout();
		if (layout == null)
			return GenerationResult.failure(GenerationFailure.INTERNAL_ERROR, "Layout planner returned no layout.");
		if (!layout.runtimePlaceable())
			return GenerationResult.failure(GenerationFailure.INTERNAL_ERROR,
					"Layout contains metadata-only room geometry and cannot be committed to the world.");
		String metadataProblem = runtimeMetadataProblem(dungeon, layout);
		if (metadataProblem != null)
			return GenerationResult.failure(GenerationFailure.SAFETY_LIMIT, metadataProblem);
		String passageProblem = connectionPreflightProblem(level, layout);
		if (passageProblem != null)
			return GenerationResult.failure(GenerationFailure.CARVING_FAILED, passageProblem);
		String placementProblem = placementAreaProblem(level, dungeon, layout);
		if (placementProblem != null)
			return GenerationResult.failure(GenerationFailure.OUT_OF_WORLD, placementProblem);
		String shellProblem = shellPreflightProblem(level, dungeon, layout);
		if (shellProblem != null)
			return GenerationResult.failure(GenerationFailure.SHELL_FAILED, shellProblem);
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(level);
		DungeonInstanceSavedData.MutationResult<DungeonInstanceSavedData.Instance> created = registry.create(
				dungeon.id(), level.dimension(), seed, effectiveLevel, level.getGameTime());
		if (!created.success() || created.value() == null)
			return GenerationResult.failure(GenerationFailure.INSTANCE_LIMIT, created.message());
		DungeonInstanceSavedData.Instance instance = created.value();
		for (ServerPlayer player : safeParticipants) {
			if (!instance.addParticipant(player.getUUID())) {
				registry.remove(instance.id());
				return GenerationResult.failure(GenerationFailure.INSTANCE_LIMIT,
						"Could not persist dungeon participant " + player.getGameProfile().getName() + ".");
			}
		}
		String persistenceProblem = persistLayout(instance, dungeon, layout, effectiveLevel, encounterRandom);
		if (persistenceProblem != null) {
			registry.remove(instance.id());
			return GenerationResult.failure(GenerationFailure.INSTANCE_LIMIT, persistenceProblem);
		}

		for (PlannedRoom room : layout.rooms()) {
			PlacementResult placement = DungeonTemplatePlacer.place(level, room.prepared());
			if (!placement.success()) {
				registry.remove(instance.id());
				return GenerationResult.failure(GenerationFailure.PLACEMENT_FAILED,
						placement.message() + " Some already placed blocks may remain; inspect the generated area before retrying.");
			}
		}

		for (PlannedConnection connection : layout.connections()) {
			DungeonShellBuilder.CarveResult carved = DungeonShellBuilder.carveConnection(level, connection.bounds());
			if (!carved.success()) {
				registry.remove(instance.id());
				return GenerationResult.failure(GenerationFailure.CARVING_FAILED,
						carved.message() + " Placed structures were retained for manual inspection.");
			}
		}

		if (dungeon.shell().enabled() && dungeon.shell().thickness() > 0) {
			Block block = ForgeRegistries.BLOCKS.getValue(dungeon.shell().block());
			if (block == null) {
				registry.remove(instance.id());
				return GenerationResult.failure(GenerationFailure.SHELL_FAILED,
						"Protective shell block " + dungeon.shell().block() + " is no longer registered.");
			}
			BlockState state = block.defaultBlockState();
			DungeonShellBuilder.ShellResult shell = DungeonShellBuilder.buildShell(level,
					layout.rooms().stream().map(room -> room.prepared().worldBounds()).toList(),
					layout.connections().stream().map(PlannedConnection::bounds).toList(), state,
					dungeon.shell().thickness());
			if (!shell.success()) {
				registry.remove(instance.id());
				return GenerationResult.failure(GenerationFailure.SHELL_FAILED,
						shell.message() + " Placed structures were retained for manual inspection.");
			}
		}

		BlockPos start = instance.playerStart().orElse(layout.rooms().get(0).prepared().worldBounds().min().above());
		if (instance.playerStart().isEmpty() && !instance.setPlayerStart(start)) {
			registry.remove(instance.id());
			return GenerationResult.failure(GenerationFailure.INSTANCE_LIMIT,
					"Could not persist the dungeon player-start position.");
		}
		return GenerationResult.success(instance.id(), start, instance.exit().orElse(null),
				instance.exitFacing().orElse(null), effectiveLevel,
				layout.rooms().size(), layout.connections().size(), seed,
				"Generated " + dungeon.id() + " as instance " + instance.id() + ".");
	}

	/** Plans a loaded datapack dungeon without creating SavedData or changing blocks. */
	public static DungeonLayoutPreview simulate(ServerLevel level, ResourceLocation dungeonId,
			BlockPos worldMinimum, long seed) {
		return simulate(level, dungeonId, worldMinimum, seed, null);
	}

	/** Plans a loaded datapack dungeon with explicit topology/count preview options. */
	public static DungeonLayoutPreview simulate(ServerLevel level, ResourceLocation dungeonId,
			BlockPos worldMinimum, long seed, DungeonLayoutPlanner.PlanOptions options) {
		DungeonLayoutTopology topology = options == null ? DungeonLayoutTopology.LINEAR : options.topology();
		ResourceLocation safeId = dungeonId == null ? new ResourceLocation("minecraft", "empty") : dungeonId;
		if (level == null || dungeonId == null || worldMinimum == null)
			return emptyPreview(safeId, seed, topology, DungeonLayoutPlanner.PlanFailure.INVALID_ARGUMENT,
					"Level, dungeon id, and world position are required.", 0L);
		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		Optional<DungeonDefinition> loaded = snapshot.dungeon(dungeonId);
		if (loaded.isEmpty())
			return emptyPreview(dungeonId, seed, topology, DungeonLayoutPlanner.PlanFailure.INVALID_DEFINITION,
					"Unknown or invalid dungeon definition " + dungeonId + ". Run /reload and inspect latest.log.",
					snapshot.revision());
		DungeonDefinition dungeon = loaded.get();
		DungeonLayoutPlanner.PlanOptions resolved = options == null
				? new DungeonLayoutPlanner.PlanOptions(runtimeTopology(dungeon), 0, 0) : options;
		return DungeonLayoutPlanner.plan(level, snapshot, dungeon, worldMinimum, seed, resolved).preview();
	}

	private static DungeonLayoutTopology runtimeTopology(DungeonDefinition dungeon) {
		if (dungeon.kind() == DungeonKind.FIXED)
			return DungeonLayoutTopology.FIXED;
		return dungeon.topology() == DungeonTopology.BRANCHING
				? DungeonLayoutTopology.BRANCHING : DungeonLayoutTopology.LINEAR;
	}

	@Nullable
	private static String persistLayout(DungeonInstanceSavedData.Instance instance, DungeonDefinition dungeon,
			DungeonLayoutPlan layout, int effectiveLevel, RandomSource random) {
		for (PlannedRoom room : layout.rooms()) {
			WorldBounds world = room.prepared().worldBounds();
			if (!instance.addRoom(room.definitionId().toString(), room.prepared().templateId(),
					room.prepared().rotation(), savedBounds(world)))
				return "Could not persist room " + room.definitionId() + ". Check ID length and runtime safety limits.";
			if (room.preset()) {
				persistAnchors(instance, room, dungeon.markers());
				String problem = persistEncounters(instance, room, dungeon.formatVersion(), dungeon.regions(),
						dungeon.markers(), dungeon.encounters(), effectiveLevel, random);
				if (problem != null)
					return problem;
			} else {
				persistAnchors(instance, room, room.definition().markers());
				String problem = persistEncounters(instance, room, room.definition().formatVersion(),
						room.definition().regions(), room.definition().markers(), room.definition().encounters(),
						effectiveLevel, random);
				if (problem != null)
					return problem;
			}
		}
		for (PlannedConnection connection : layout.connections()) {
			DungeonInstanceSavedData.Bounds bounds = savedBounds(connection.bounds());
			if (!instance.carvedConnections().contains(bounds) && !instance.addCarvedConnection(bounds))
				return "Could not persist a planned connection because a runtime safety limit was reached.";
		}
		int shellThickness = dungeon.shell().enabled() ? dungeon.shell().thickness() : 0;
		for (WorldBounds occupied : plannedOccupiedVolumes(layout, shellThickness)) {
			DungeonInstanceSavedData.Bounds bounds = savedBounds(occupied);
			if (!instance.occupiedVolumes().contains(bounds) && !instance.addOccupiedVolume(bounds))
				return "Could not persist an occupied dungeon volume because a runtime safety limit was reached.";
		}
		if (instance.playerStart().isEmpty())
			return "The planned dungeon did not persist a player_start marker.";
		if (instance.exit().isEmpty())
			return "The planned dungeon did not persist an exit or return_portal marker.";
		return null;
	}

	private static void persistAnchors(DungeonInstanceSavedData.Instance instance, PlannedRoom room,
			List<Marker> markers) {
		for (Marker marker : markers) {
			BlockPos world = room.prepared().transformRelative(block(marker.position()));
			if (marker.type().equals("player_start") && instance.playerStart().isEmpty())
				instance.setPlayerStart(world);
			if ((marker.type().equals("exit") || marker.type().equals("return_portal")) && instance.exit().isEmpty()) {
				instance.setExit(world);
				startRoomExitFacing(room).ifPresent(instance::setExitFacing);
			}
		}
	}

	private static Optional<Direction> startRoomExitFacing(PlannedRoom room) {
		if (room.preset() || room.definition().role() != RoomRole.START)
			return Optional.empty();
		List<Direction> exits = room.definition().sockets().stream()
				.filter(socket -> room.usedSockets().contains(socket.id()))
				.map(socket -> room.geometry().transformDirection(socket.facing()))
				.filter(direction -> direction.getAxis().isHorizontal()).toList();
		return exits.size() == 1 ? Optional.of(exits.get(0)) : Optional.empty();
	}

	@Nullable
	private static String persistEncounters(DungeonInstanceSavedData.Instance instance, PlannedRoom room,
			int formatVersion, List<Region> regions, List<Marker> markers, List<Encounter> encounters,
			int effectiveLevel, RandomSource random) {
		for (Encounter encounter : encounters) {
			Optional<WorldBounds> trigger = encounter.triggerRegion().flatMap(id -> regions.stream()
					.filter(region -> region.id().equals(id)).findFirst()
					.map(region -> room.prepared().transformRelativeBounds(relative(region.bounds()))));
			String sequenceKey = "r" + room.index() + ":" + encounter.id();
			for (int waveIndex = 0; waveIndex < encounter.waves().size(); waveIndex++) {
				EncounterWave wave = encounter.waves().get(waveIndex);
				String key = "r" + room.index() + ":" + encounter.id() + ":" + wave.id();
				IntRange levelRange = wave.levelRange().orElse(new IntRange(effectiveLevel, effectiveLevel));
				DungeonInstanceSavedData.MutationResult<DungeonInstanceSavedData.EncounterState> created =
						formatVersion >= 3
								? instance.createEncounter(key, wave.mobPool(), wave.boss(), wave.levelRange().isPresent(),
										levelRange.min(), levelRange.max(), sequenceKey, waveIndex, wave.delayTicks())
								: instance.createEncounter(key, wave.mobPool(), wave.boss(), wave.levelRange().isPresent(),
										levelRange.min(), levelRange.max());
				if (!created.success() || created.value() == null)
					return "Could not persist encounter " + key + ": " + created.message();
				DungeonInstanceSavedData.EncounterState state = created.value();
				if (trigger.isPresent() && !state.setTriggerBounds(savedBounds(trigger.get())))
					return "Could not persist trigger bounds for encounter " + key + ".";
				List<Marker> spawnMarkers = markers.stream()
						.filter(marker -> marker.belongsTo(wave.markerGroup()))
						.filter(marker -> wave.boss() ? marker.type().equals("boss_spawn")
								: marker.type().equals("mob_spawn") || marker.type().equals("elite_spawn"))
						.sorted(Comparator.comparing(Marker::id)).toList();
				int count = wave.count().random(random);
				for (int index = 0; index < count && !spawnMarkers.isEmpty(); index++) {
					Marker marker = spawnMarkers.get(index % spawnMarkers.size());
					String role = wave.boss() || marker.type().equals("boss_spawn") ? "boss"
							: marker.type().equals("elite_spawn") ? "elite" : "normal";
					if (!state.addMarker(marker.id() + "_" + index, role,
							room.prepared().transformRelative(block(marker.position()))))
						return "Could not persist spawn marker " + marker.id() + " for encounter " + key + ".";
				}
				if (count == 0)
					state.markCompleted();
			}
		}
		return null;
	}

	private static int resolveEffectiveLevel(LevelRule rule, List<ServerPlayer> participants,
			RandomSource random, @Nullable Integer explicit) {
		int minimum = rule.range().min();
		int maximum = rule.range().max();
		if (explicit != null)
			return DungeonLevelHelper.clampLevel(explicit, minimum, maximum);
		ServerPlayer owner = participants.isEmpty() ? null : participants.get(0);
		DungeonLevelHelper.EffectiveLevelSource source = switch (rule.source().toLowerCase(Locale.ROOT)) {
			case "owner", "player" -> DungeonLevelHelper.EffectiveLevelSource.OWNER;
			case "party_average", "average" -> DungeonLevelHelper.EffectiveLevelSource.PARTY_AVERAGE;
			case "party_highest", "highest" -> DungeonLevelHelper.EffectiveLevelSource.PARTY_HIGHEST;
			default -> DungeonLevelHelper.EffectiveLevelSource.FIXED;
		};
		int fixed = rule.range().min();
		int resolved = DungeonLevelHelper.resolveEffectiveLevel(source, owner, participants, fixed, minimum, maximum);
		if (rule.variance() > 0) {
			int variance = random.nextInt(rule.variance() * 2 + 1) - rule.variance();
			resolved = DungeonLevelHelper.clampLevel((long) resolved + variance, minimum, maximum);
		}
		return resolved;
	}

	@Nullable
	private static String runtimeMetadataProblem(DungeonDefinition dungeon, DungeonLayoutPlan layout) {
		if ((long) layout.rooms().size() + layout.connections().size()
				> DungeonInstanceSavedData.MAX_OCCUPIED_VOLUMES)
			return "Planned layout exceeds the persistent occupied-volume limit of "
					+ DungeonInstanceSavedData.MAX_OCCUPIED_VOLUMES + ".";
		int waves = 0;
		for (PlannedRoom room : layout.rooms()) {
			List<Encounter> encounters = room.preset() ? dungeon.encounters() : room.definition().encounters();
			for (Encounter encounter : encounters) {
				for (EncounterWave wave : encounter.waves()) {
					waves++;
					if (waves > DungeonInstanceSavedData.MAX_ENCOUNTERS)
						return "Planned layout exceeds the persistent encounter limit of "
								+ DungeonInstanceSavedData.MAX_ENCOUNTERS + ".";
					if (wave.count().max() > DungeonInstanceSavedData.MAX_MARKERS_PER_ENCOUNTER)
						return "Wave " + wave.id() + " can spawn " + wave.count().max()
								+ " mobs, above the per-wave safety limit of "
								+ DungeonInstanceSavedData.MAX_MARKERS_PER_ENCOUNTER + ".";
					String key = "r" + room.index() + ":" + encounter.id() + ":" + wave.id();
					if (key.length() > 128)
						return "Encounter key " + key + " exceeds the 128-character persistence limit. Shorten encounter and wave IDs.";
				}
			}
		}
		return null;
	}

	/**
	 * Validates every planned passage against current world data and the prepared
	 * templates before any room is committed. It also rejects implicit crossings
	 * through unrelated rooms or passages, which would otherwise create graph
	 * connections that were never authored.
	 */
	@Nullable
	private static String connectionPreflightProblem(ServerLevel level, DungeonLayoutPlan layout) {
		List<WorldBounds> acceptedConnections = new ArrayList<>();
		for (PlannedConnection connection : layout.connections()) {
			PlannedRoom source = layout.rooms().stream()
					.filter(room -> room.index() == connection.sourceRoom()).findFirst().orElse(null);
			PlannedRoom target = layout.rooms().stream()
					.filter(room -> room.index() == connection.targetRoom()).findFirst().orElse(null);
			if (source == null || target == null)
				return "A planned connection references a missing room.";
			for (PlannedRoom room : layout.rooms()) {
				if (room.index() != source.index() && room.index() != target.index()
						&& connection.bounds().intersects(room.prepared().worldBounds()))
					return "Passage " + connection.sourceRoom() + ":" + connection.sourceSocket()
							+ " -> " + connection.targetRoom() + ":" + connection.targetSocket()
							+ " crosses unrelated room " + room.index() + ". Move the room or reroute the passage.";
			}
			if (acceptedConnections.stream().anyMatch(connection.bounds()::intersects))
				return "Two planned passages overlap or cross without an authored junction. Move a room or use a junction module.";
			DungeonShellBuilder.CarveResult preflight = DungeonShellBuilder.preflightConnection(level,
					connection.bounds());
			if (!preflight.success())
				return "Passage preflight failed: " + preflight.message();
			try {
				for (PlannedRoom endpoint : List.of(source, target)) {
					boolean protectedBlockEntity = endpoint.prepared().transformedBlockEntityPositions().stream()
							.anyMatch(position -> contains(connection.bounds(), position));
					if (protectedBlockEntity)
						return "Passage touches a saved block entity in room " + endpoint.definitionId()
								+ ". Move the container/sign out of the socket opening and recapture the room.";
				}
			} catch (RuntimeException exception) {
				return "Could not inspect room block entities during passage preflight: " + safeMessage(exception);
			}
			acceptedConnections.add(connection.bounds());
		}
		return null;
	}

	@Nullable
	private static String placementAreaProblem(ServerLevel level, DungeonDefinition dungeon, DungeonLayoutPlan layout) {
		int shell = dungeon.shell().enabled() ? dungeon.shell().thickness() : 0;
		List<WorldBounds> plannedOccupied = plannedOccupiedVolumes(layout, shell);
		for (WorldBounds bounds : plannedOccupied) {
			long minY = bounds.min().getY();
			long maxY = bounds.max().getY();
			if (minY < level.getMinBuildHeight() || maxY >= level.getMaxBuildHeight())
				return "A room and its protective shell would extend outside build height.";
			if (!level.getWorldBorder().isWithinBounds(bounds.min())
					|| !level.getWorldBorder().isWithinBounds(bounds.max()))
				return "A room and its protective shell would extend outside the world border.";
		}
		for (DungeonInstanceSavedData.InstanceView existing : DungeonInstanceSavedData.get(level).views()) {
			if (!existing.dimension().equals(level.dimension()))
				continue;
			List<WorldBounds> existingOccupied = existingOccupiedVolumes(existing);
			if (plannedOccupied.stream().anyMatch(planned -> existingOccupied.stream().anyMatch(planned::intersects)))
				return "The planned dungeon (rooms, passages, or protective shell) overlaps existing instance "
						+ existing.id() + ". Choose another origin.";
		}
		return null;
	}

	private static List<WorldBounds> plannedOccupiedVolumes(DungeonLayoutPlan layout, int shellThickness) {
		int expansion = Math.max(0, Math.min(4, shellThickness));
		List<WorldBounds> occupied = new ArrayList<>(layout.rooms().size() + layout.connections().size());
		for (PlannedRoom room : layout.rooms())
			occupied.add(expand(room.prepared().worldBounds(), expansion));
		for (PlannedConnection connection : layout.connections())
			occupied.add(expand(connection.bounds(), expansion));
		return List.copyOf(occupied);
	}

	/** Schema-v2 saves have no explicit occupancy. Their full layout envelope is a safe fallback. */
	private static List<WorldBounds> existingOccupiedVolumes(DungeonInstanceSavedData.InstanceView instance) {
		if (!instance.occupiedVolumes().isEmpty())
			return instance.occupiedVolumes().stream()
					.map(bounds -> new WorldBounds(bounds.min(), bounds.max())).toList();
		List<WorldBounds> legacy = new ArrayList<>(instance.rooms().size() + instance.carvedConnections().size());
		for (DungeonInstanceSavedData.PlacedRoom room : instance.rooms())
			legacy.add(new WorldBounds(room.bounds().min(), room.bounds().max()));
		for (DungeonInstanceSavedData.Bounds connection : instance.carvedConnections())
			legacy.add(new WorldBounds(connection.min(), connection.max()));
		if (legacy.isEmpty())
			return List.of();
		WorldBounds envelope = legacy.get(0);
		for (int index = 1; index < legacy.size(); index++)
			envelope = union(envelope, legacy.get(index));
		return List.of(expand(envelope, LEGACY_OCCUPANCY_PADDING));
	}

	private static WorldBounds expand(WorldBounds bounds, int amount) {
		return amount <= 0 ? bounds : new WorldBounds(bounds.min().offset(-amount, -amount, -amount),
				bounds.max().offset(amount, amount, amount));
	}

	private static WorldBounds union(WorldBounds first, WorldBounds second) {
		return new WorldBounds(new BlockPos(Math.min(first.min().getX(), second.min().getX()),
				Math.min(first.min().getY(), second.min().getY()), Math.min(first.min().getZ(), second.min().getZ())),
				new BlockPos(Math.max(first.max().getX(), second.max().getX()),
						Math.max(first.max().getY(), second.max().getY()), Math.max(first.max().getZ(), second.max().getZ())));
	}

	private static boolean contains(WorldBounds bounds, BlockPos position) {
		return position.getX() >= bounds.min().getX() && position.getX() <= bounds.max().getX()
				&& position.getY() >= bounds.min().getY() && position.getY() <= bounds.max().getY()
				&& position.getZ() >= bounds.min().getZ() && position.getZ() <= bounds.max().getZ();
	}

	private static String safeMessage(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

	@Nullable
	private static String shellPreflightProblem(ServerLevel level, DungeonDefinition dungeon, DungeonLayoutPlan layout) {
		if (!dungeon.shell().enabled() || dungeon.shell().thickness() <= 0)
			return null;
		Block block = ForgeRegistries.BLOCKS.getValue(dungeon.shell().block());
		if (block == null)
			return "Protective shell block " + dungeon.shell().block() + " is no longer registered.";
		DungeonShellBuilder.ShellResult preflight = DungeonShellBuilder.preflightShell(level,
				layout.rooms().stream().map(room -> room.prepared().worldBounds()).toList(),
				layout.connections().stream().map(PlannedConnection::bounds).toList(),
				block.defaultBlockState(), dungeon.shell().thickness());
		return preflight.success() ? null : preflight.message();
	}

	private static BlockPos block(Int3 vector) {
		return new BlockPos(vector.x(), vector.y(), vector.z());
	}

	private static RelativeBounds relative(Bounds3 bounds) {
		return new RelativeBounds(block(bounds.min()), block(bounds.max()));
	}

	private static DungeonInstanceSavedData.Bounds savedBounds(WorldBounds bounds) {
		return new DungeonInstanceSavedData.Bounds(bounds.min(), bounds.max());
	}

	private static GenerationFailure mapFailure(DungeonLayoutPlanner.PlanFailure failure) {
		return switch (failure) {
			case NONE -> GenerationFailure.NONE;
			case INVALID_ARGUMENT -> GenerationFailure.INVALID_ARGUMENT;
			case INVALID_DEFINITION -> GenerationFailure.INVALID_DEFINITION;
			case MISSING_TEMPLATE -> GenerationFailure.MISSING_TEMPLATE;
			case NO_LAYOUT, SEARCH_BUDGET_EXHAUSTED -> GenerationFailure.NO_LAYOUT;
			case REQUIRED_SOCKET_UNUSED -> GenerationFailure.REQUIRED_SOCKET_UNUSED;
			case OUT_OF_WORLD -> GenerationFailure.OUT_OF_WORLD;
			case SAFETY_LIMIT -> GenerationFailure.SAFETY_LIMIT;
		};
	}

	private static DungeonLayoutPreview emptyPreview(ResourceLocation dungeonId, long seed,
			DungeonLayoutTopology topology, DungeonLayoutPlanner.PlanFailure failure,
			String message, long revision) {
		return new DungeonLayoutPreview(false, failure, message, dungeonId, revision, seed,
				topology, 0, List.of(), List.of(), DungeonLayoutPreview.Diagnostics.empty());
	}

	public enum GenerationFailure {
		NONE,
		INVALID_ARGUMENT,
		UNKNOWN_DUNGEON,
		INVALID_DEFINITION,
		MISSING_TEMPLATE,
		NO_LAYOUT,
		REQUIRED_SOCKET_UNUSED,
		OUT_OF_WORLD,
		SAFETY_LIMIT,
		INSTANCE_LIMIT,
		PLACEMENT_FAILED,
		CARVING_FAILED,
		SHELL_FAILED,
		INTERNAL_ERROR
	}

	public record GenerationResult(boolean success, GenerationFailure failure, String message,
			@Nullable UUID instanceId, @Nullable BlockPos playerStart, @Nullable BlockPos exit,
			@Nullable Direction exitFacing, int effectiveLevel, int roomCount, int connectionCount, long seed) {
		private static GenerationResult success(UUID instanceId, BlockPos start, @Nullable BlockPos exit,
				@Nullable Direction exitFacing, int level, int rooms, int connections, long seed, String message) {
			return new GenerationResult(true, GenerationFailure.NONE, message, instanceId, start, exit, exitFacing,
					level, rooms, connections, seed);
		}

		private static GenerationResult failure(GenerationFailure failure, String message) {
			return new GenerationResult(false, failure, message, null, null, null, null, 0, 0, 0, 0L);
		}
	}
}
