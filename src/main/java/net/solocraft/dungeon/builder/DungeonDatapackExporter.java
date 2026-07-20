package net.solocraft.dungeon.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.dungeon.builder.model.BuilderMobPool;
import net.solocraft.dungeon.builder.model.DungeonDraft;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Captures an authored build and packages it as a safe, save-local datapack. */
public final class DungeonDatapackExporter {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final long MAX_RUNTIME_TEMPLATE_VOLUME = 500_000L;
	private static final long MAX_PROCEDURAL_CAPTURE_VOLUME = 2_000_000L;

	private DungeonDatapackExporter() {
	}

	public static ExportResult export(ServerPlayer player, DungeonBuilderProjectData.Project project) {
		List<DungeonBuilderProjectData.Issue> issues = project.validate();
		long errors = issues.stream().filter(issue -> issue.severity() == DungeonBuilderProjectData.Severity.ERROR).count();
		if (errors > 0)
			return ExportResult.failure("Export stopped: fix " + errors + " validation error" + (errors == 1 ? "" : "s") + " first.");

		DungeonBuilderProjectData.Bounds bounds = project.structureBounds();
		if (bounds == null)
			return ExportResult.failure("Export stopped: no structure bounds are selected.");

		ServerLevel level = player.serverLevel();
		if (bounds.min().getY() < level.getMinBuildHeight() || bounds.max().getY() >= level.getMaxBuildHeight())
			return ExportResult.failure("Export stopped: the selected bounds extend outside the world's build height.");
		for (int chunkX = bounds.min().getX() >> 4; chunkX <= bounds.max().getX() >> 4; chunkX++) {
			for (int chunkZ = bounds.min().getZ() >> 4; chunkZ <= bounds.max().getZ() >> 4; chunkZ++) {
				if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null)
					return ExportResult.failure("Export stopped: all selected chunks must be loaded. Move near the build and try again.");
			}
		}
		String socketProblem = socketBlockEntityProblem(level, project);
		if (socketProblem != null)
			return ExportResult.failure(socketProblem);
		Path datapackRoot = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
		String packBaseName = project.namespace() + "_" + project.name() + "_slr_dungeon";
		Path finalFolder;
		try {
			Files.createDirectories(datapackRoot);
			finalFolder = uniqueFolder(datapackRoot, packBaseName);
		} catch (IOException exception) {
			SololevelingMod.LOGGER.error("Could not prepare Dungeon Builder datapack directory", exception);
			return ExportResult.failure("Could not prepare the world's datapack directory.");
		}

		Path staging = datapackRoot.resolve(".slr_export_" + UUID.randomUUID()).normalize();
		if (!staging.startsWith(datapackRoot) || !finalFolder.startsWith(datapackRoot))
			return ExportResult.failure("Unsafe datapack path was rejected.");

		try {
			writePack(staging, player, project, bounds, finalFolder.getFileName().toString());
			moveFinishedPack(staging, finalFolder);
			try {
				level.getServer().getPackRepository().reload();
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.warn("Exported Dungeon Builder pack, but could not refresh datapack discovery", exception);
			}
			return ExportResult.success(finalFolder, "Exported datapack " + finalFolder.getFileName()
					+ ". Run /datapack list available, enable the exact file/<folder>, then run /reload.");
		} catch (Exception exception) {
			SololevelingMod.LOGGER.error("Dungeon Builder export failed for {}:{}", project.namespace(), project.name(), exception);
			deleteStaging(staging);
			return ExportResult.failure("Export failed. Check latest.log for the exact file error.");
		}
	}

	/** Snapshot-only, placeholder-free export for a single prebuilt Studio dungeon. */
	public static ExportResult exportStudioPreset(ServerPlayer player, DungeonBuilderProjectData data,
			DungeonBuilderProjectData.Project project) {
		if (project.kind() != DungeonBuilderProjectData.ProjectKind.PRESET)
			return ExportResult.failure("The selected room is not a prebuilt preset dungeon.");
		if (project.roomSnapshot().isEmpty())
			return ExportResult.failure("Capture this prebuilt dungeon before exporting it from the Studio.");
		long errors = project.errorCount();
		if (errors > 0)
			return ExportResult.failure("Export stopped: fix " + errors + " blocking validation error(s) first.");
		DungeonBuilderRoomStore.VerificationResult verification = DungeonBuilderRoomStore.verify(player, project);
		if (!verification.valid())
			return ExportResult.failure(verification.status() + ". Capture the structure again.");
		DungeonBuilderProjectData.Bounds bounds = project.structureBounds();
		if (bounds == null)
			return ExportResult.failure("Structure Bounds are missing.");

		Map<ResourceLocation, BuilderMobPool> authoredPools = new HashMap<>();
		for (BuilderMobPool pool : data.mobPools(player))
			authoredPools.put(pool.id(), pool);
		Set<ResourceLocation> requiredPools = new HashSet<>();
		Set<ResourceLocation> generatedPools = new HashSet<>();
		for (DungeonBuilderProjectData.Encounter encounter : project.encounters()) {
			boolean used = project.markersForExport().stream().anyMatch(marker -> marker.group().equals(encounter.id()));
			if (!used)
				continue;
			ResourceLocation configured = ResourceLocation.tryParse(encounter.pool());
			if (configured != null) {
				if (ForgeRegistries.ENTITY_TYPES.containsKey(configured)) {
					if (!BuilderMobPoolPreflight.resolvesSpawnable(player.serverLevel(),
							BuilderMobPool.SelectorKind.ENTITY, configured))
						return ExportResult.failure("Configured entity " + configured
								+ " is not a loaded spawnable mob.");
					generatedPools.add(generatedEncounterPoolId(project, encounter));
				} else
					requiredPools.add(configured);
			}
		}
		for (ResourceLocation generated : generatedPools)
			if (authoredPools.containsKey(generated))
				return ExportResult.failure("Mob pool " + generated
						+ " collides with a reserved one-entity encounter pool. Rename the authored pool.");
		for (ResourceLocation required : requiredPools) {
			BuilderMobPool pool = authoredPools.get(required);
			if (pool == null)
				return ExportResult.failure("Referenced mob pool " + required + " is not in this Studio workspace.");
			List<String> problems = BuilderMobPoolPreflight.problems(player.serverLevel(), pool);
			if (!problems.isEmpty())
				return ExportResult.failure(problems.get(0));
		}

		ServerLevel level = player.serverLevel();
		Path datapackRoot = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
		Path finalFolder;
		try {
			Files.createDirectories(datapackRoot);
			finalFolder = uniqueFolder(datapackRoot, project.namespace() + "_" + project.name() + "_slr_studio");
		} catch (IOException exception) {
			return ExportResult.failure("Could not prepare the world's datapack directory.");
		}
		Path staging = datapackRoot.resolve(".slr_export_" + UUID.randomUUID()).normalize();
		try {
			writePackMetadata(staging, "Solo Leveling Studio preset: " + humanName(project.name()));
			Path dataRoot = staging.resolve("data").resolve(project.namespace());
			Path structurePath = dataRoot.resolve("structures/slr_dungeons").resolve(project.name() + ".nbt");
			DungeonBuilderRoomStore.CopyResult copied = DungeonBuilderRoomStore.copyVerified(player, project, structurePath);
			if (!copied.success())
				throw new IOException(copied.message());
			writeJson(dataRoot.resolve("slr/dungeons").resolve(project.name() + ".json"),
					definition(project, bounds, false));
			writeConfiguredEntityPools(staging, project);
			for (ResourceLocation required : requiredPools)
				writeAuthoredPool(staging, authoredPools.get(required));
			Files.writeString(staging.resolve("README.md"),
					"# " + humanName(project.name()) + "\n\n"
							+ "Exported from a versioned Dungeon Builder Studio snapshot.\n\n"
							+ "1. Run `/datapack enable \"file/" + finalFolder.getFileName() + "\"`.\n"
							+ "2. Run `/reload` and `/slrdungeon issues`.\n"
							+ "3. Run `/slrdungeon generate " + project.id() + " seed 12345 confirm`.\n\n"
							+ "No placeholder mobs were inserted.\n", StandardCharsets.UTF_8);
			moveFinishedPack(staging, finalFolder);
			try {
				level.getServer().getPackRepository().reload();
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.warn("Exported Studio preset, but could not refresh datapack discovery", exception);
			}
			return ExportResult.success(finalFolder, "Exported prebuilt Studio dungeon " + project.id()
					+ " as " + finalFolder.getFileName()
					+ ". Enable the exact file/<folder>, then run /reload and /slrdungeon issues.");
		} catch (Exception exception) {
			SololevelingMod.LOGGER.error("Studio preset export failed for {}", project.id(), exception);
			deleteStaging(staging);
			return ExportResult.failure("Studio preset export failed. Check latest.log.");
		}
	}

	/** Packages every valid module in one namespace as a complete procedural dungeon datapack. */
	public static ExportResult exportProcedural(ServerPlayer player, List<DungeonBuilderProjectData.Project> workspace,
			String namespace, String dungeonName, int minRooms, int maxRooms) {
		if (!DungeonBuilderProjectData.sanitizeId(namespace, "builder").equals(namespace)
				|| !DungeonBuilderProjectData.sanitizeId(dungeonName, "dungeon").equals(dungeonName))
			return ExportResult.failure("Namespace and dungeon name must be lowercase resource-safe IDs.");
		if (minRooms < 3 || maxRooms < minRooms || maxRooms > 64)
			return ExportResult.failure("Procedural room count must be 3-64 and max must be at least min.");

		List<DungeonBuilderProjectData.Project> modules = workspace.stream()
				.filter(project -> project.kind() == DungeonBuilderProjectData.ProjectKind.MODULE)
				.filter(project -> project.namespace().equals(namespace))
				.sorted(Comparator.comparing(DungeonBuilderProjectData.Project::name)).toList();
		if (modules.isEmpty())
			return ExportResult.failure("No module projects exist in namespace " + namespace + ".");
		long totalCaptureVolume = modules.stream().map(DungeonBuilderProjectData.Project::structureBounds)
				.filter(java.util.Objects::nonNull).mapToLong(DungeonBuilderProjectData.Bounds::volume).sum();
		if (totalCaptureVolume > MAX_PROCEDURAL_CAPTURE_VOLUME)
			return ExportResult.failure("This namespace contains " + totalCaptureVolume
					+ " blocks of module captures, above the safe one-export limit of "
					+ MAX_PROCEDURAL_CAPTURE_VOLUME + ". Reduce module sizes or split the addon into smaller namespaces.");
		long starts = modules.stream().filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.START).count();
		long bosses = modules.stream().filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS).count();
		boolean middle = modules.stream().anyMatch(project -> project.roomRole() != DungeonBuilderProjectData.RoomRole.START
				&& project.roomRole() != DungeonBuilderProjectData.RoomRole.BOSS
				&& project.roomRole() != DungeonBuilderProjectData.RoomRole.CAP);
		if (starts == 0 || bosses == 0 || !middle)
			return ExportResult.failure("A procedural pack needs at least one start module, one boss module, and one middle module.");
		List<DungeonBuilderProjectData.Project> startModules = modules.stream()
				.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.START).toList();
		List<DungeonBuilderProjectData.Project> bossModules = modules.stream()
				.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS).toList();
		List<DungeonBuilderProjectData.Project> middleModules = modules.stream()
				.filter(project -> project.roomRole() != DungeonBuilderProjectData.RoomRole.START
						&& project.roomRole() != DungeonBuilderProjectData.RoomRole.BOSS
						&& project.roomRole() != DungeonBuilderProjectData.RoomRole.CAP).toList();
		for (int roomCount = minRooms; roomCount <= maxRooms; roomCount++) {
			if (!hasSocketPath(startModules, middleModules, bossModules, roomCount - 2))
				return ExportResult.failure("The modules cannot form a complete " + roomCount
						+ "-room start-to-boss path using different entrance and exit sockets. Match socket type, opening width, and opening height, add a repeatable two-socket middle module, or narrow the room-count range.");
		}
		long maximumPlannedVolume = maxVolume(startModules) + maxVolume(bossModules)
				+ (long) (maxRooms - 2) * maxVolume(middleModules);
		if (maximumPlannedVolume > MAX_RUNTIME_TEMPLATE_VOLUME)
			return ExportResult.failure("The largest possible " + maxRooms + "-room layout is " + maximumPlannedVolume
					+ " blocks, above the runtime safety limit of " + MAX_RUNTIME_TEMPLATE_VOLUME
					+ ". Reduce max_rooms or module sizes.");
		DungeonBuilderProjectData.Project shellSource = modules.get(0);
		if (modules.stream().anyMatch(module -> module.shellThickness() != shellSource.shellThickness()
				|| !module.shellBlock().equals(shellSource.shellBlock())))
			return ExportResult.failure("All modules in a procedural export must use the same shell block and thickness. Align /dungeonbuilder settings shell first.");
		if (modules.stream().anyMatch(module -> !module.allowedRanks().equals(shellSource.allowedRanks())))
			return ExportResult.failure("All modules in a procedural export must use the same allowed ranks. Run /dungeonbuilder settings rank <rank> while one module in this namespace is active.");
		for (DungeonBuilderProjectData.Project module : modules) {
			long errors = module.errorCount();
			if (errors > 0)
				return ExportResult.failure("Module " + module.id() + " has " + errors + " validation error(s).");
			ExportResult captureCheck = validateCapture(player, module);
			if (!captureCheck.success())
				return ExportResult.failure(module.id() + ": " + captureCheck.message());
		}

		ServerLevel level = player.serverLevel();
		Path datapackRoot = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
		Path finalFolder;
		try {
			Files.createDirectories(datapackRoot);
			finalFolder = uniqueFolder(datapackRoot, namespace + "_" + dungeonName + "_slr_procedural");
		} catch (IOException exception) {
			SololevelingMod.LOGGER.error("Could not prepare procedural Dungeon Builder datapack directory", exception);
			return ExportResult.failure("Could not prepare the world's datapack directory.");
		}
		Path staging = datapackRoot.resolve(".slr_export_" + UUID.randomUUID()).normalize();
		if (!staging.startsWith(datapackRoot) || !finalFolder.startsWith(datapackRoot))
			return ExportResult.failure("Unsafe datapack path was rejected.");

		try {
			writePackMetadata(staging, "Solo Leveling procedural dungeon: " + humanName(dungeonName));
			for (DungeonBuilderProjectData.Project module : modules)
				writeModule(staging, player, module);
			writeJson(staging.resolve("data").resolve(namespace).resolve("slr").resolve("dungeons")
					.resolve(dungeonName + ".json"), proceduralDefinition(namespace, dungeonName, modules, minRooms, maxRooms));
			Files.writeString(staging.resolve("README.md"), proceduralReadme(namespace, dungeonName, modules,
					finalFolder.getFileName().toString()), StandardCharsets.UTF_8);
			moveFinishedPack(staging, finalFolder);
			try {
				level.getServer().getPackRepository().reload();
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.warn("Exported procedural Dungeon Builder pack, but could not refresh datapack discovery", exception);
			}
			return ExportResult.success(finalFolder, "Exported procedural dungeon " + namespace + ":" + dungeonName
					+ " with " + modules.size() + " room modules. Enable it, then run /reload.");
		} catch (Exception exception) {
			SololevelingMod.LOGGER.error("Procedural Dungeon Builder export failed for {}:{}", namespace, dungeonName, exception);
			deleteStaging(staging);
			return ExportResult.failure("Procedural export failed. Check latest.log for the exact file error.");
		}
	}

	/**
	 * Compiles a Studio dungeon draft and its versioned room/pool assets into a
	 * self-contained datapack. Unlike the legacy command exporter this path never
	 * invents zombie placeholder pools and never recaptures live room blocks.
	 */
	public static ExportResult exportDraft(ServerPlayer player, DungeonBuilderProjectData data,
			ResourceLocation draftId) {
		if (draftId == null)
			return ExportResult.failure("Select a dungeon definition before exporting.");
		java.util.Optional<DungeonDraft> loaded = data.dungeonDraft(player, draftId);
		if (loaded.isEmpty())
			return ExportResult.failure("Unknown Studio dungeon definition " + draftId + ".");
		DungeonDraft draft = loaded.get();

		Map<String, DungeonBuilderProjectData.Project> projectsById = new HashMap<>();
		for (DungeonBuilderProjectData.Project project : data.projects(player))
			projectsById.put(project.id(), project);
		java.util.stream.Stream<ResourceLocation> referencedRooms = draft.mode() == DungeonDraft.Mode.FIXED
				? draft.fixedPlacements().stream().map(DungeonDraft.FixedPlacement::room)
				: draft.rooms().stream().map(DungeonDraft.RoomRef::room);
		List<ResourceLocation> referencedRoomIds = referencedRooms.distinct().toList();
		List<DungeonBuilderProjectData.Project> modules = referencedRoomIds.stream()
				.map(ResourceLocation::toString)
				.distinct().map(projectsById::get).filter(java.util.Objects::nonNull)
				.sorted(Comparator.comparing(DungeonBuilderProjectData.Project::id)).toList();
		if (modules.size() != referencedRoomIds.size())
			return ExportResult.failure("The dungeon references a room that no longer exists in this workspace.");
		if (modules.stream().anyMatch(project -> project.kind() != DungeonBuilderProjectData.ProjectKind.MODULE))
			return ExportResult.failure("Studio layout definitions may reference room-module projects only.");
		if (modules.stream().anyMatch(project -> project.roomSnapshot().isEmpty()))
			return ExportResult.failure("Every included room needs an explicit captured snapshot before Studio export.");
		for (DungeonBuilderProjectData.Project module : modules) {
			long errors = module.errorCount();
			if (errors > 0)
				return ExportResult.failure("Room " + module.id() + " has " + errors + " blocking validation error(s).");
			DungeonBuilderRoomStore.VerificationResult verification = DungeonBuilderRoomStore.verify(player, module);
			if (!verification.valid())
				return ExportResult.failure(module.id() + ": " + verification.status() + ". Capture the room again.");
		}

		if (draft.mode() == DungeonDraft.Mode.FIXED) {
			if (draft.fixedPlacements().isEmpty() || draft.fixedPlacements().size() > 64)
				return ExportResult.failure("A fixed layout needs between 1 and 64 placed rooms.");
			long starts = draft.fixedPlacements().stream().map(placement -> projectsById.get(placement.room().toString()))
					.filter(java.util.Objects::nonNull)
					.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.START).count();
			long bosses = draft.fixedPlacements().stream().map(placement -> projectsById.get(placement.room().toString()))
					.filter(java.util.Objects::nonNull)
					.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS).count();
			if (starts != 1 || bosses != 1)
				return ExportResult.failure("A fixed layout needs exactly one placed start room and one placed boss room.");
		} else {
			long starts = modules.stream().filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.START).count();
			long bosses = modules.stream().filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS).count();
			boolean hasMiddle = modules.stream().anyMatch(project -> project.roomRole() != DungeonBuilderProjectData.RoomRole.START
					&& project.roomRole() != DungeonBuilderProjectData.RoomRole.BOSS
					&& project.roomRole() != DungeonBuilderProjectData.RoomRole.CAP);
			if (starts == 0 || bosses == 0 || !hasMiddle)
				return ExportResult.failure("A procedural definition needs a start room, a boss room, and at least one middle room.");
			if (draft.minRooms() < 3 || draft.maxRooms() < draft.minRooms() || draft.maxRooms() > 64)
				return ExportResult.failure("Room count must stay between 3 and 64.");
			if (draft.topology() == DungeonDraft.Topology.BRANCHING && draft.minRooms() < 4)
				return ExportResult.failure("Branching layouts need a minimum of at least 4 rooms.");
		}
		DungeonBuilderStudioSimulation.CoverageResult coverage =
				DungeonBuilderStudioSimulation.validateCoverage(player, data, draft.id());
		if (!coverage.success())
			return ExportResult.failure((draft.mode() == DungeonDraft.Mode.FIXED ? "Fixed" : "Procedural")
					+ " layout failed deterministic runtime coverage: " + coverage.message()
					+ " Run Preview and repair its room/socket choices.");

		Map<ResourceLocation, BuilderMobPool> authoredPools = new HashMap<>();
		for (BuilderMobPool pool : data.mobPools(player))
			authoredPools.put(pool.id(), pool);
		Set<ResourceLocation> requiredPools = new HashSet<>();
		Set<ResourceLocation> generatedPools = new HashSet<>();
		for (DungeonBuilderProjectData.Project module : modules) {
			for (DungeonBuilderProjectData.Encounter encounter : module.encounters()) {
				boolean used = module.markersForExport().stream().anyMatch(marker -> marker.group().equals(encounter.id()));
				if (!used)
					continue;
				ResourceLocation configured = ResourceLocation.tryParse(encounter.pool());
				if (configured != null) {
					if (ForgeRegistries.ENTITY_TYPES.containsKey(configured)) {
						if (!BuilderMobPoolPreflight.resolvesSpawnable(player.serverLevel(),
								BuilderMobPool.SelectorKind.ENTITY, configured))
							return ExportResult.failure("Configured entity " + configured
									+ " is not a loaded spawnable mob.");
						generatedPools.add(generatedEncounterPoolId(module, encounter));
					} else
						requiredPools.add(configured);
				}
			}
		}
		for (ResourceLocation generated : generatedPools)
			if (authoredPools.containsKey(generated))
				return ExportResult.failure("Mob pool " + generated
						+ " collides with a reserved one-entity encounter pool. Rename the authored pool.");
		for (ResourceLocation required : requiredPools) {
			BuilderMobPool pool = authoredPools.get(required);
			if (pool == null)
				return ExportResult.failure("Referenced mob pool " + required
						+ " is not in this Studio workspace. Create it in Pools or export its dependency separately.");
			List<String> problems = BuilderMobPoolPreflight.problems(player.serverLevel(), pool);
			if (!problems.isEmpty())
				return ExportResult.failure(problems.get(0));
		}

		ServerLevel level = player.serverLevel();
		Path datapackRoot = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
		Path finalFolder;
		try {
			Files.createDirectories(datapackRoot);
			finalFolder = uniqueFolder(datapackRoot,
					draft.id().getNamespace() + "_" + draft.id().getPath().replace('/', '_') + "_slr_studio");
		} catch (IOException exception) {
			return ExportResult.failure("Could not prepare the world's datapack directory.");
		}
		Path staging = datapackRoot.resolve(".slr_export_" + UUID.randomUUID()).normalize();
		if (!staging.startsWith(datapackRoot) || !finalFolder.startsWith(datapackRoot))
			return ExportResult.failure("Unsafe datapack path was rejected.");

		try {
			writePackMetadata(staging, "Solo Leveling Studio dungeon: " + humanName(draft.id().getPath()));
			for (DungeonBuilderProjectData.Project module : modules)
				writeModule(staging, player, module, false);
			for (ResourceLocation required : requiredPools)
				writeAuthoredPool(staging, authoredPools.get(required));
			Path dungeonRoot = staging.resolve("data").resolve(draft.id().getNamespace())
					.resolve("slr").resolve("dungeons").toAbsolutePath().normalize();
			Path dungeonPath = dungeonRoot.resolve(draft.id().getPath() + ".json").normalize();
			if (!dungeonPath.startsWith(dungeonRoot))
				throw new IOException("Dungeon definition path escaped its datapack resource directory");
			writeJson(dungeonPath, draft.mode() == DungeonDraft.Mode.FIXED
					? fixedDefinition(draft) : proceduralDefinition(draft, modules));
			Files.writeString(staging.resolve("README.md"), studioReadme(draft, modules,
					finalFolder.getFileName().toString()), StandardCharsets.UTF_8);
			moveFinishedPack(staging, finalFolder);
			try {
				level.getServer().getPackRepository().reload();
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.warn("Exported Studio pack, but could not refresh datapack discovery", exception);
			}
			return ExportResult.success(finalFolder, "Exported "
					+ (draft.mode() == DungeonDraft.Mode.FIXED ? "fixed" : "procedural")
					+ " Studio dungeon " + draft.id() + " with " + modules.size()
					+ " captured room assets. Enable it, then run /reload.");
		} catch (Exception exception) {
			SololevelingMod.LOGGER.error("Studio Dungeon Builder export failed for {}", draft.id(), exception);
			deleteStaging(staging);
			return ExportResult.failure("Studio export failed. Check latest.log for the exact file error.");
		}
	}

	private static void writePack(Path root, ServerPlayer player, DungeonBuilderProjectData.Project project,
			DungeonBuilderProjectData.Bounds bounds, String generatedFolderName) throws IOException {
		String namespace = project.namespace();
		String name = project.name();
		Path dataRoot = root.resolve("data").resolve(namespace);
		Path structurePath = dataRoot.resolve("structures").resolve("slr_dungeons").resolve(name + ".nbt");
		String definitionFolder = project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET ? "dungeons" : "rooms";
		Path definitionPath = dataRoot.resolve("slr").resolve(definitionFolder).resolve(name + ".json");
		Path poolPath = dataRoot.resolve("slr").resolve("mob_pools").resolve(name + "_default.json");
		Path bossPoolPath = dataRoot.resolve("slr").resolve("mob_pools").resolve(name + "_boss.json");

		Files.createDirectories(structurePath.getParent());
		Files.createDirectories(definitionPath.getParent());
		Files.createDirectories(poolPath.getParent());

		writePackMetadata(root, "Solo Leveling dungeon: " + humanName(name));

		writeCapturedOrLiveStructure(player, project, bounds, structurePath);

		JsonObject definition = definition(project, bounds);
		writeJson(definitionPath, definition);

		writeJson(poolPath, examplePool(false));
		writeJson(bossPoolPath, examplePool(true));
		writeConfiguredEntityPools(root, project);

		Files.writeString(root.resolve("README.md"), readme(project, generatedFolderName), StandardCharsets.UTF_8);
	}

	private static ExportResult validateCapture(ServerPlayer player, DungeonBuilderProjectData.Project project) {
		ServerLevel level = player.serverLevel();
		DungeonBuilderProjectData.Bounds bounds = project.structureBounds();
		if (bounds == null)
			return ExportResult.failure("No structure bounds are selected.");
		if (project.roomSnapshot().isPresent()) {
			DungeonBuilderRoomStore.VerificationResult verification = DungeonBuilderRoomStore.verify(player, project);
			return verification.valid()
					? ExportResult.success(verification.path(), "Ready.")
					: ExportResult.failure(verification.status() + ". Capture the room again in the Studio.");
		}
		if (bounds.min().getY() < level.getMinBuildHeight() || bounds.max().getY() >= level.getMaxBuildHeight())
			return ExportResult.failure("Selected bounds extend outside the world's build height.");
		for (int chunkX = bounds.min().getX() >> 4; chunkX <= bounds.max().getX() >> 4; chunkX++)
			for (int chunkZ = bounds.min().getZ() >> 4; chunkZ <= bounds.max().getZ() >> 4; chunkZ++)
				if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null)
					return ExportResult.failure("All selected chunks must be loaded. Move near the build and try again.");
		String socketProblem = socketBlockEntityProblem(level, project);
		if (socketProblem != null)
			return ExportResult.failure(socketProblem);
		return ExportResult.success(null, "Ready.");
	}

	private static void writeCapturedOrLiveStructure(ServerPlayer player,
			DungeonBuilderProjectData.Project project, DungeonBuilderProjectData.Bounds bounds,
			Path structurePath) throws IOException {
		if (project.roomSnapshot().isPresent()) {
			DungeonBuilderRoomStore.CopyResult copy = DungeonBuilderRoomStore.copyVerified(player, project, structurePath);
			if (!copy.success())
				throw new IOException(copy.message());
			return;
		}
		// Legacy command-created projects remain exportable. Studio export requires an
		// explicit snapshot before it calls this exporter.
		StructureTemplate template = new StructureTemplate();
		BlockPos size = bounds.size();
		template.fillFromWorld(player.serverLevel(), bounds.min(),
				new Vec3i(size.getX(), size.getY(), size.getZ()), false, Blocks.STRUCTURE_VOID);
		template.setAuthor(player.getGameProfile().getName());
		NbtIo.writeCompressed(NbtUtils.addCurrentDataVersion(template.save(new CompoundTag())),
				structurePath.toFile());
	}

	private static String socketBlockEntityProblem(ServerLevel level,
			DungeonBuilderProjectData.Project project) {
		for (DungeonBuilderProjectData.Socket socket : project.sockets()) {
			DungeonBuilderProjectData.Bounds opening = socket.opening();
			for (int x = opening.min().getX(); x <= opening.max().getX(); x++)
				for (int y = opening.min().getY(); y <= opening.max().getY(); y++)
					for (int z = opening.min().getZ(); z <= opening.max().getZ(); z++)
						if (level.getBlockEntity(new BlockPos(x, y, z)) != null)
							return "Socket " + socket.id() + " contains a block entity. Move containers/signs out of doorway planes before export.";
		}
		return null;
	}

	private static void writeModule(Path root, ServerPlayer player,
			DungeonBuilderProjectData.Project project) throws IOException {
		writeModule(root, player, project, true);
	}

	private static void writeModule(Path root, ServerPlayer player,
			DungeonBuilderProjectData.Project project, boolean legacyExamplePools) throws IOException {
		DungeonBuilderProjectData.Bounds bounds = project.structureBounds();
		if (bounds == null)
			throw new IOException("Module " + project.id() + " lost its structure bounds during export");
		Path dataRoot = root.resolve("data").resolve(project.namespace());
		Path structurePath = dataRoot.resolve("structures").resolve("slr_dungeons").resolve(project.name() + ".nbt");
		Path definitionPath = dataRoot.resolve("slr").resolve("rooms").resolve(project.name() + ".json");
		Files.createDirectories(structurePath.getParent());
		writeCapturedOrLiveStructure(player, project, bounds, structurePath);
		writeJson(definitionPath, definition(project, bounds, legacyExamplePools));
		if (legacyExamplePools) {
			writeJson(dataRoot.resolve("slr").resolve("mob_pools").resolve(project.name() + "_default.json"), examplePool(false));
			writeJson(dataRoot.resolve("slr").resolve("mob_pools").resolve(project.name() + "_boss.json"), examplePool(true));
		}
		writeConfiguredEntityPools(root, project);
	}

	private static JsonObject proceduralDefinition(String namespace, String name,
			List<DungeonBuilderProjectData.Project> modules, int minRooms, int maxRooms) {
		JsonObject json = new JsonObject();
		json.addProperty("format_version", 2);
		json.addProperty("generation", "procedural");
		json.addProperty("display_name", humanName(name));
		addAllowedRanks(json, modules.get(0).allowedRanks());
		JsonObject pools = new JsonObject();
		for (DungeonBuilderProjectData.RoomRole role : DungeonBuilderProjectData.RoomRole.values()) {
			JsonArray entries = new JsonArray();
			for (DungeonBuilderProjectData.Project module : modules) {
				if (module.roomRole() != role)
					continue;
				JsonObject entry = new JsonObject();
				entry.addProperty("room", namespace + ":" + module.name());
				entry.addProperty("weight", module.roomWeight());
				entries.add(entry);
			}
			if (!entries.isEmpty())
				pools.add(role.name().toLowerCase(Locale.ROOT), entries);
		}
		json.add("room_pools", pools);
		JsonArray count = new JsonArray();
		count.add(minRooms);
		count.add(maxRooms);
		json.add("room_count", count);
		json.addProperty("max_depth", maxRooms);
		JsonObject level = new JsonObject();
		level.addProperty("source", "owner");
		JsonArray range = new JsonArray();
		range.add(1);
		range.add(DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL);
		level.add("range", range);
		json.add("level", level);
		DungeonBuilderProjectData.Project shellSource = modules.get(0);
		JsonObject shell = new JsonObject();
		shell.addProperty("enabled", shellSource.shellThickness() > 0);
		shell.addProperty("block", shellSource.shellBlock());
		if (shellSource.shellThickness() > 0)
			shell.addProperty("thickness", shellSource.shellThickness());
		shell.addProperty("cover_floor", true);
		shell.addProperty("cover_ceiling", true);
		json.add("shell", shell);
		return json;
	}

	private static JsonObject proceduralDefinition(DungeonDraft draft,
			List<DungeonBuilderProjectData.Project> modules) {
		Map<String, DungeonBuilderProjectData.Project> projects = new HashMap<>();
		for (DungeonBuilderProjectData.Project project : modules)
			projects.put(project.id(), project);
		JsonObject json = new JsonObject();
		json.addProperty("format_version", 2);
		json.addProperty("generation", "procedural");
		json.addProperty("topology", draft.topology().name().toLowerCase(Locale.ROOT));
		json.addProperty("display_name", humanName(draft.id().getPath()));
		addAllowedRanks(json, draft.allowedRanks());
		JsonObject pools = new JsonObject();
		for (DungeonBuilderProjectData.RoomRole role : DungeonBuilderProjectData.RoomRole.values()) {
			JsonArray entries = new JsonArray();
			for (DungeonDraft.RoomRef reference : draft.rooms()) {
				DungeonBuilderProjectData.Project project = projects.get(reference.room().toString());
				if (project == null || project.roomRole() != role)
					continue;
				JsonObject entry = new JsonObject();
				entry.addProperty("room", reference.room().toString());
				entry.addProperty("weight", reference.weight());
				entries.add(entry);
			}
			if (!entries.isEmpty())
				pools.add(role.name().toLowerCase(Locale.ROOT), entries);
		}
		json.add("room_pools", pools);
		JsonArray count = new JsonArray();
		count.add(draft.minRooms());
		count.add(draft.maxRooms());
		json.add("room_count", count);
		json.addProperty("max_depth", draft.maxDepth());
		JsonObject level = new JsonObject();
		level.addProperty("source", "owner");
		JsonArray range = new JsonArray();
		range.add(1);
		range.add(DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL);
		level.add("range", range);
		json.add("level", level);
		JsonObject shell = new JsonObject();
		shell.addProperty("enabled", draft.shellThickness() > 0);
		shell.addProperty("block", draft.shellBlock().toString());
		if (draft.shellThickness() > 0)
			shell.addProperty("thickness", draft.shellThickness());
		shell.addProperty("cover_floor", true);
		shell.addProperty("cover_ceiling", true);
		json.add("shell", shell);
		return json;
	}

	private static JsonObject fixedDefinition(DungeonDraft draft) {
		JsonObject json = new JsonObject();
		json.addProperty("format_version", 2);
		json.addProperty("generation", "fixed");
		json.addProperty("display_name", humanName(draft.id().getPath()));
		addAllowedRanks(json, draft.allowedRanks());

		JsonArray placements = new JsonArray();
		for (DungeonDraft.FixedPlacement placement : draft.fixedPlacements()) {
			JsonObject value = new JsonObject();
			value.addProperty("id", placement.id());
			value.addProperty("room", placement.room().toString());
			value.add("position", vector(new BlockPos(placement.x(), placement.y(), placement.z())));
			value.addProperty("rotation", switch (placement.rotation()) {
				case CLOCKWISE_90 -> 90;
				case CLOCKWISE_180 -> 180;
				case COUNTERCLOCKWISE_90 -> 270;
				default -> 0;
			});
			placements.add(value);
		}
		json.add("placements", placements);

		JsonArray connections = new JsonArray();
		for (DungeonDraft.FixedConnection connection : draft.fixedConnections()) {
			JsonObject value = new JsonObject();
			JsonObject from = new JsonObject();
			from.addProperty("room", connection.fromPlacement());
			from.addProperty("socket", connection.fromSocket());
			JsonObject to = new JsonObject();
			to.addProperty("room", connection.toPlacement());
			to.addProperty("socket", connection.toSocket());
			value.add("from", from);
			value.add("to", to);
			connections.add(value);
		}
		json.add("connections", connections);

		JsonObject level = new JsonObject();
		level.addProperty("source", "owner");
		JsonArray range = new JsonArray();
		range.add(1);
		range.add(DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL);
		level.add("range", range);
		json.add("level", level);
		JsonObject shell = new JsonObject();
		shell.addProperty("enabled", draft.shellThickness() > 0);
		shell.addProperty("block", draft.shellBlock().toString());
		if (draft.shellThickness() > 0)
			shell.addProperty("thickness", draft.shellThickness());
		shell.addProperty("cover_floor", true);
		shell.addProperty("cover_ceiling", true);
		json.add("shell", shell);
		return json;
	}

	private static void writeAuthoredPool(Path root, BuilderMobPool pool) throws IOException {
		Path namespaceRoot = root.resolve("data").resolve(pool.id().getNamespace())
				.resolve("slr").resolve("mob_pools").toAbsolutePath().normalize();
		Path path = namespaceRoot.resolve(pool.id().getPath() + ".json").normalize();
		if (!path.startsWith(namespaceRoot))
			throw new IOException("Mob-pool path escaped datapack root");
		JsonObject json = new JsonObject();
		json.addProperty("format_version", 2);
		JsonArray entries = new JsonArray();
		for (BuilderMobPool.Entry source : pool.entries()) {
			JsonObject entry = new JsonObject();
			entry.addProperty(source.selectorKind() == BuilderMobPool.SelectorKind.TAG ? "tag" : "entity",
					source.selector().toString());
			entry.addProperty("weight", source.weight());
			// Requiring the loader mod itself is redundant and made otherwise valid pools
			// look conditional. Keep optional-addon conditions only.
			source.requiredMod().filter(value -> !value.equals(SololevelingMod.MODID))
					.ifPresent(value -> entry.addProperty("required_mod", value));
			source.eligibleLevel().ifPresent(value -> entry.add("eligible_level", range(value)));
			source.spawnLevel().ifPresent(value -> entry.add("spawn_level", range(value)));
			source.baseXp().ifPresent(value -> entry.addProperty("xp", value));
			entries.add(entry);
		}
		json.add("entries", entries);
		writeJson(path, json);
	}

	private static JsonArray range(BuilderMobPool.LevelRange range) {
		JsonArray json = new JsonArray();
		json.add(range.min());
		json.add(range.max());
		return json;
	}

	private static void writePackMetadata(Path root, String description) throws IOException {
		JsonObject pack = new JsonObject();
		JsonObject body = new JsonObject();
		body.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
		body.addProperty("description", description);
		pack.add("pack", body);
		writeJson(root.resolve("pack.mcmeta"), pack);
	}

	private static void moveFinishedPack(Path staging, Path destination) throws IOException {
		try {
			Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException ignored) {
			Files.move(staging, destination);
		}
	}

	private static JsonObject definition(DungeonBuilderProjectData.Project project,
			DungeonBuilderProjectData.Bounds bounds) {
		return definition(project, bounds, true);
	}

	private static JsonObject definition(DungeonBuilderProjectData.Project project,
			DungeonBuilderProjectData.Bounds bounds, boolean includeLegacyDefaultPools) {
		JsonObject json = new JsonObject();
		json.addProperty("format_version", 2);
		if (project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			json.addProperty("generation", "preset");
		else {
			json.addProperty("role", project.roomRole().name().toLowerCase(Locale.ROOT));
			json.addProperty("weight", project.roomWeight());
		}
		json.addProperty("display_name", humanName(project.name()));
		if (project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			addAllowedRanks(json, project.allowedRanks());
		json.addProperty("structure", project.namespace() + ":slr_dungeons/" + project.name());
		if (includeLegacyDefaultPools)
			json.addProperty("default_mob_pool", project.defaultMobPool());
		if (includeLegacyDefaultPools && project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			json.addProperty("boss_mob_pool", project.bossMobPool());
		json.add("size", vector(bounds.size()));
		if (project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET) {
			JsonObject level = new JsonObject();
			level.addProperty("source", "owner");
			JsonArray range = new JsonArray();
			range.add(1);
			range.add(DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL);
			level.add("range", range);
			json.add("level", level);
		}

		JsonArray regions = new JsonArray();
		for (DungeonBuilderProjectData.Region region : project.regions()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("id", region.id());
			entry.addProperty("type", region.type());
			entry.add("min", relative(region.bounds().min(), bounds.min()));
			entry.add("max", relative(region.bounds().max(), bounds.min()));
			regions.add(entry);
		}
		json.add("regions", regions);

		JsonArray sockets = new JsonArray();
		for (DungeonBuilderProjectData.Socket socket : project.sockets()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("id", socket.id());
			entry.addProperty("type", socket.type());
			entry.addProperty("facing", socket.facing().getName());
			entry.addProperty("required", socket.required());
			entry.add("min", relative(socket.opening().min(), bounds.min()));
			entry.add("max", relative(socket.opening().max(), bounds.min()));
			sockets.add(entry);
		}
		json.add("sockets", sockets);

		JsonArray markers = new JsonArray();
		for (DungeonBuilderProjectData.Marker marker : project.markers()) {
			// Generic Studio anchors are authoring-only until assigned a concrete
			// encounter role. They must never leak into the runtime schema.
			if (marker.type().equals("spawn_point"))
				continue;
			JsonObject entry = new JsonObject();
			entry.addProperty("id", marker.id());
			entry.addProperty("type", marker.type());
			if (!marker.group().isBlank())
				entry.addProperty("group", marker.group());
			entry.add("position", relative(marker.position(), bounds.min()));
			markers.add(entry);
		}
		json.add("markers", markers);

		JsonArray encounters = new JsonArray();
		for (DungeonBuilderProjectData.Encounter encounter : project.encounters()) {
			long markerCount = project.markers().stream().filter(marker -> marker.group().equals(encounter.id())
					&& (marker.type().equals("mob_spawn") || marker.type().equals("elite_spawn") || marker.type().equals("boss_spawn"))).count();
			if (markerCount == 0)
				continue;
			JsonObject encounterJson = new JsonObject();
			encounterJson.addProperty("id", encounter.id());
			if (encounter.delayed())
				project.regions().stream()
						.filter(region -> region.type().equals("trigger_region") && region.group().equals(encounter.id()))
						.findFirst().ifPresent(region -> encounterJson.addProperty("trigger_region", region.id()));
			boolean boss = project.markers().stream().anyMatch(marker -> marker.group().equals(encounter.id())
					&& marker.type().equals("boss_spawn"));
			JsonObject wave = new JsonObject();
			wave.addProperty("id", encounter.id() + "_wave");
			wave.addProperty("marker_group", encounter.id());
			wave.addProperty("pool", exportedEncounterPool(project, encounter));
			wave.addProperty("count", Math.max(1L, markerCount));
			wave.addProperty("boss", boss);
			if (encounter.maxLevel() != Integer.MAX_VALUE) {
				JsonArray levelRange = new JsonArray();
				levelRange.add(encounter.minLevel());
				levelRange.add(encounter.maxLevel());
				wave.add("level", levelRange);
			}
			JsonArray waves = new JsonArray();
			waves.add(wave);
			encounterJson.add("waves", waves);
			encounters.add(encounterJson);
		}
		json.add("encounters", encounters);

		if (project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET) {
			JsonObject shell = new JsonObject();
			shell.addProperty("enabled", project.shellThickness() > 0);
			shell.addProperty("block", project.shellBlock());
			if (project.shellThickness() > 0)
				shell.addProperty("thickness", project.shellThickness());
			shell.addProperty("cover_floor", true);
			shell.addProperty("cover_ceiling", true);
			json.add("shell", shell);
		}
		return json;
	}

	private static JsonObject examplePool(boolean boss) {
		JsonObject pool = new JsonObject();
		pool.addProperty("format_version", 2);
		JsonArray entries = new JsonArray();
		JsonObject example = new JsonObject();
		example.addProperty("entity", "minecraft:zombie");
		example.addProperty("weight", 1);
		example.addProperty("xp", boss ? 50 : 5);
		entries.add(example);
		pool.add("entries", entries);
		return pool;
	}

	/**
	 * The builder accepts either a mob-pool ID or an entity-type ID in the simple
	 * encounter command. Entity IDs are converted into private one-entry pools so
	 * the exported schema stays addon-friendly while the in-game workflow remains
	 * simple.
	 */
	private static void writeConfiguredEntityPools(Path root,
			DungeonBuilderProjectData.Project project) throws IOException {
		for (DungeonBuilderProjectData.Encounter encounter : project.encounters()) {
			ResourceLocation entityId = ResourceLocation.tryParse(encounter.pool());
			if (entityId == null || !ForgeRegistries.ENTITY_TYPES.containsKey(entityId))
				continue;
			ResourceLocation poolId = generatedEncounterPoolId(project, encounter);
			Path poolRoot = root.resolve("data").resolve(poolId.getNamespace())
					.resolve("slr").resolve("mob_pools").toAbsolutePath().normalize();
			Path poolPath = poolRoot.resolve(poolId.getPath() + ".json").normalize();
			if (!poolPath.startsWith(poolRoot))
				throw new IOException("Generated encounter pool escaped datapack root");
			writeJson(poolPath, singleEntityPool(entityId));
		}
	}

	private static String exportedEncounterPool(DungeonBuilderProjectData.Project project,
			DungeonBuilderProjectData.Encounter encounter) {
		ResourceLocation configured = ResourceLocation.tryParse(encounter.pool());
		return configured != null && ForgeRegistries.ENTITY_TYPES.containsKey(configured)
				? generatedEncounterPoolId(project, encounter).toString()
				: encounter.pool();
	}

	private static ResourceLocation generatedEncounterPoolId(DungeonBuilderProjectData.Project project,
			DungeonBuilderProjectData.Encounter encounter) {
		return new ResourceLocation(project.namespace(),
				"generated/" + project.name() + "/" + encounter.id());
	}

	private static JsonObject singleEntityPool(ResourceLocation entityId) {
		JsonObject pool = new JsonObject();
		pool.addProperty("format_version", 2);
		JsonArray entries = new JsonArray();
		JsonObject entry = new JsonObject();
		entry.addProperty("entity", entityId.toString());
		entry.addProperty("weight", 1);
		entries.add(entry);
		pool.add("entries", entries);
		return pool;
	}

	private static void addAllowedRanks(JsonObject json, java.util.Set<ProceduralDungeonRank> ranks) {
		JsonArray values = new JsonArray();
		for (ProceduralDungeonRank rank : ProceduralDungeonRank.values())
			if (ranks.contains(rank))
				values.add(rank.name());
		json.add("ranks", values);
	}

	private static String rankText(java.util.Set<ProceduralDungeonRank> ranks) {
		return java.util.Arrays.stream(ProceduralDungeonRank.values()).filter(ranks::contains)
				.map(Enum::name).collect(java.util.stream.Collectors.joining(", "));
	}

	private static long maxVolume(List<DungeonBuilderProjectData.Project> projects) {
		return projects.stream().map(DungeonBuilderProjectData.Project::structureBounds)
				.filter(java.util.Objects::nonNull).mapToLong(DungeonBuilderProjectData.Bounds::volume)
				.max().orElse(0L);
	}

	private static boolean anyConnectable(List<DungeonBuilderProjectData.Project> first,
			List<DungeonBuilderProjectData.Project> second) {
		return first.stream().anyMatch(left -> second.stream().anyMatch(right -> left.sockets().stream()
				.anyMatch(leftSocket -> right.sockets().stream().anyMatch(rightSocket -> socketsConnect(leftSocket, rightSocket)))));
	}

	private static boolean hasSocketPath(List<DungeonBuilderProjectData.Project> starts,
			List<DungeonBuilderProjectData.Project> middles,
			List<DungeonBuilderProjectData.Project> bosses, int middleRoomCount) {
		if (middleRoomCount < 1)
			return false;
		java.util.Map<String, Boolean> memo = new java.util.HashMap<>();
		for (DungeonBuilderProjectData.Project start : starts)
			for (DungeonBuilderProjectData.Socket startSocket : start.sockets())
				for (int middleIndex = 0; middleIndex < middles.size(); middleIndex++) {
					DungeonBuilderProjectData.Project middle = middles.get(middleIndex);
					for (int entryIndex = 0; entryIndex < middle.sockets().size(); entryIndex++)
						if (socketsConnect(startSocket, middle.sockets().get(entryIndex))
								&& canReachBoss(middles, bosses, middleIndex, entryIndex,
										middleRoomCount, memo))
							return true;
				}
		return false;
	}

	private static boolean canReachBoss(List<DungeonBuilderProjectData.Project> middles,
			List<DungeonBuilderProjectData.Project> bosses, int currentMiddle, int entrySocket,
			int remainingMiddleRooms, java.util.Map<String, Boolean> memo) {
		String memoKey = currentMiddle + ":" + entrySocket + ":" + remainingMiddleRooms;
		Boolean known = memo.get(memoKey);
		if (known != null)
			return known;
		DungeonBuilderProjectData.Project current = middles.get(currentMiddle);
		for (int exitIndex = 0; exitIndex < current.sockets().size(); exitIndex++) {
			if (exitIndex == entrySocket)
				continue;
			DungeonBuilderProjectData.Socket exit = current.sockets().get(exitIndex);
			if (remainingMiddleRooms == 1) {
				if (bosses.stream().anyMatch(boss -> boss.sockets().stream()
						.anyMatch(socket -> socketsConnect(exit, socket)))) {
					memo.put(memoKey, true);
					return true;
				}
				continue;
			}
			for (int nextMiddle = 0; nextMiddle < middles.size(); nextMiddle++) {
				DungeonBuilderProjectData.Project next = middles.get(nextMiddle);
				for (int nextEntry = 0; nextEntry < next.sockets().size(); nextEntry++) {
					if (socketsConnect(exit, next.sockets().get(nextEntry))
							&& canReachBoss(middles, bosses, nextMiddle, nextEntry,
									remainingMiddleRooms - 1, memo)) {
						memo.put(memoKey, true);
						return true;
					}
				}
			}
		}
		memo.put(memoKey, false);
		return false;
	}

	private static boolean socketsConnect(DungeonBuilderProjectData.Socket first,
			DungeonBuilderProjectData.Socket second) {
		if (!first.type().equals(second.type()) || first.facing().getAxis().isHorizontal()
				!= second.facing().getAxis().isHorizontal())
			return false;
		if (first.facing().getAxis().isVertical() && first.facing() != second.facing().getOpposite())
			return false;
		int[] firstShape = socketShape(first);
		int[] secondShape = socketShape(second);
		return firstShape[0] == secondShape[0] && firstShape[1] == secondShape[1];
	}

	private static int[] socketShape(DungeonBuilderProjectData.Socket socket) {
		BlockPos size = socket.opening().size();
		if (socket.facing().getAxis().isHorizontal()) {
			int width = socket.facing().getAxis() == net.minecraft.core.Direction.Axis.X ? size.getZ() : size.getX();
			return new int[]{size.getY(), width};
		}
		return new int[]{Math.min(size.getX(), size.getZ()), Math.max(size.getX(), size.getZ())};
	}

	private static JsonArray relative(BlockPos position, BlockPos min) {
		return vector(position.subtract(min));
	}

	private static JsonArray vector(BlockPos position) {
		JsonArray array = new JsonArray();
		array.add(position.getX());
		array.add(position.getY());
		array.add(position.getZ());
		return array;
	}

	private static void writeJson(Path path, JsonObject json) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
	}

	private static Path uniqueFolder(Path root, String baseName) throws IOException {
		for (int version = 1; version < 10_000; version++) {
			String suffix = version == 1 ? "" : "_v" + version;
			Path candidate = root.resolve(baseName + suffix).normalize();
			if (!candidate.startsWith(root))
				throw new IOException("Resolved export escaped datapack root");
			if (!Files.exists(candidate))
				return candidate;
		}
		throw new IOException("Too many exports with the same name");
	}

	private static String readme(DungeonBuilderProjectData.Project project, String generatedFolderName) {
		String definitionType = project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET ? "dungeon" : "room module";
		String rankLine = project.kind() == DungeonBuilderProjectData.ProjectKind.PRESET
				? "- Allowed gate ranks: " + rankText(project.allowedRanks()) + ". A gate outside this list cannot be bound or entered.\n"
				: "";
		return "# " + humanName(project.name()) + "\n\n"
				+ "This save-local datapack was exported by the Solo Leveling Dungeon Builder.\n\n"
				+ "## Activate\n\n"
				+ "1. Run `/datapack list available`.\n"
				+ "2. Enable this folder with `/datapack enable \"file/" + generatedFolderName + "\"`.\n"
				+ "3. Run `/reload`.\n\n"
				+ "## Contents\n\n"
				+ "- The captured `.nbt` structure is under `data/" + project.namespace() + "/structures/slr_dungeons/`.\n"
				+ "- Capture includes blocks and block entities, but intentionally excludes free-standing entities.\n"
				+ "- The " + definitionType + " definition is under `data/" + project.namespace() + "/slr/`.\n"
				+ rankLine
				+ "- The generated normal and boss mob pools contain one test zombie so the dungeon works immediately. Replace it with exact entity IDs or entity-type tags before publishing.\n"
				+ "- Addons can extend a pool without replacing this pack by adding files under `slr/pool_modifiers/`.\n\n"
				+ "The exported definitions use Solo Leveling dungeon schema version 2.\n";
	}

	private static String proceduralReadme(String namespace, String dungeonName,
			List<DungeonBuilderProjectData.Project> modules, String generatedFolderName) {
		return "# " + humanName(dungeonName) + "\n\n"
				+ "This datapack contains a complete procedural Solo Leveling dungeon assembled from " + modules.size() + " authored room modules.\n\n"
				+ "## Activate and test\n\n"
				+ "1. Run `/datapack enable \"file/" + generatedFolderName + "\"`.\n"
				+ "2. Run `/reload`.\n"
				+ "3. Run `/slrdungeon generate " + namespace + ":" + dungeonName + " confirm`.\n\n"
				+ "Allowed gate ranks: **" + rankText(modules.get(0).allowedRanks()) + "**. Binding rejects any gate outside this list; compatible gates generate the dungeon in their normal rank dimension.\n\n"
				+ "## Customize mobs\n\n"
				+ "Generated `<room>_default` and `<room>_boss` pools start with a test zombie. Replace entries under `data/" + namespace + "/slr/mob_pools/` with exact registry IDs or entity-type tags. If a room references a different custom pool ID, that pool must be supplied by this or another enabled pack.\n"
				+ "Other addons can append/remove entries deterministically with JSON files under `slr/pool_modifiers/`. Optional entries can be skipped with `required_mod` or Forge conditions, but every referenced pool needs at least one active fallback entry.\n\n"
				+ "Structures intentionally contain blocks and block entities only. All sockets, spawns, triggers, levels, and boss rules live in schema-v2 JSON so they remain editable and addon-friendly.\n";
	}

	private static String studioReadme(DungeonDraft draft,
			List<DungeonBuilderProjectData.Project> modules, String generatedFolderName) {
		String ranks = rankText(draft.allowedRanks());
		String layout = draft.mode() == DungeonDraft.Mode.FIXED
				? "- Mode: fixed exact placement graph.\n- Placed rooms: " + draft.fixedPlacements().size() + ".\n"
				: "- Mode: procedural / " + draft.topology().name().toLowerCase(Locale.ROOT)
						+ ".\n- Room count: " + draft.minRooms() + "-" + draft.maxRooms() + ".\n";
		return "# " + humanName(draft.id().getPath()) + "\n\n"
				+ "Exported by Solo Leveling Dungeon Builder Studio from " + modules.size()
				+ " versioned room snapshot(s).\n\n"
				+ "## Activate and verify\n\n"
				+ "1. Run `/datapack enable \"file/" + generatedFolderName + "\"`.\n"
				+ "2. Run `/reload`.\n"
				+ "3. Run `/slrdungeon issues`.\n"
				+ "4. Run `/slrdungeon generate " + draft.id() + " seed 12345 confirm`.\n\n"
				+ "## Definition\n\n"
				+ layout
				+ "- Allowed gate ranks: " + ranks + ".\n"
				+ "- Protective shell: " + (draft.shellThickness() == 0 ? "disabled"
						: draft.shellThickness() + " layer(s) of " + draft.shellBlock()) + ".\n\n"
				+ "Room NBT contains blocks and block entities only. Spawn points, sockets, encounters, levels, XP, and portals are JSON metadata.\n"
				+ "Mob pools are the exact Studio-authored entries; no test mobs or placeholder pools were inserted.\n";
	}

	private static String humanName(String id) {
		String[] words = id.toLowerCase(Locale.ROOT).split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty())
				continue;
			if (!result.isEmpty())
				result.append(' ');
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return result.toString();
	}

	private static void deleteStaging(Path staging) {
		if (!Files.exists(staging))
			return;
		try (Stream<Path> paths = Files.walk(staging)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				try {
					Files.deleteIfExists(path);
				} catch (IOException exception) {
					SololevelingMod.LOGGER.warn("Could not remove failed Dungeon Builder export staging path {}", path, exception);
				}
			}
		} catch (IOException exception) {
			SololevelingMod.LOGGER.warn("Could not inspect failed Dungeon Builder export staging path {}", staging, exception);
		}
	}

	public record ExportResult(boolean success, Path path, String message) {
		private static ExportResult success(Path path, String message) {
			return new ExportResult(true, path, message);
		}

		private static ExportResult failure(String message) {
			return new ExportResult(false, null, message);
		}
	}
}
