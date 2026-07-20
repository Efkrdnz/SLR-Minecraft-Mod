package net.solocraft.dungeon.builder;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.dungeon.builder.model.BuilderMobPool;
import net.solocraft.dungeon.builder.model.DungeonDraft;
import net.solocraft.dungeon.builder.model.RoomSnapshot;
import net.solocraft.network.DungeonBuilderStudioStateMessage;
import net.solocraft.util.DungeonBuilderMode;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/** Server-authoritative application service for Dungeon Builder Studio. */
public final class DungeonBuilderStudioService {
	private static final int MAX_ACTION_LIST = 256;
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private DungeonBuilderStudioService() {
	}

	/** Releases session-only UI state when a player leaves the server. */
	public static void playerLoggedOut(ServerPlayer player) {
		if (player != null)
			SESSIONS.remove(player.getUUID());
	}

	public static void requestOpen(ServerPlayer player) {
		if (!canEdit(player, true))
			return;
		sendState(player, true, "INFO", "Workspace loaded.");
	}

	public static void apply(ServerPlayer player, long expectedRevision, String action,
			CompoundTag payload) {
		if (!canEdit(player, true))
			return;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		long actualRevision = data.revision(player);
		if (expectedRevision != actualRevision) {
			sendState(player, false, "WARNING", "This workspace changed from revision "
					+ expectedRevision + " to " + actualRevision + ". Refreshed without overwriting it.");
			return;
		}
		CompoundTag safePayload = payload == null ? new CompoundTag() : payload;
		String normalized = action == null ? "" : action.toLowerCase(Locale.ROOT);
		ActionResult result;
		try {
			result = switch (normalized) {
				case "request_snapshot" -> ActionResult.info("Workspace refreshed.");
				case "create_project" -> createProject(player, data, safePayload);
				case "delete_project" -> deleteProject(player, data, safePayload);
				case "select_project" -> selectProject(player, data, safePayload);
				case "select_dungeon" -> selectDungeon(player, data, safePayload);
				case "new_dungeon" -> newDungeon(player, data, safePayload);
				case "delete_dungeon" -> deleteDungeon(player, data, safePayload);
				case "set_room_role" -> setRoomRole(player, data, safePayload);
				case "set_room_weight" -> setRoomWeight(player, data, safePayload);
				case "set_project_settings" -> setProjectSettings(player, data, safePayload);
				case "capture_snapshot" -> captureSnapshot(player, data, safePayload);
				case "assign_anchor" -> assignAnchor(player, data, safePayload);
				case "edit_socket" -> editSocket(player, data, safePayload);
				case "create_pool" -> createPool(player, data, safePayload);
				case "delete_pool" -> deletePool(player, data, safePayload);
				case "save_pool", "save_pool_draft", "upsert_pool_entry", "remove_pool_entry" ->
						savePool(player, data, safePayload);
				case "update_layout" -> saveDraft(player, data, safePayload);
				case "run_simulation" -> runSimulation(player, data, safePayload);
				case "validate_dungeon" -> validateDungeon(player, data, safePayload);
				case "export_dungeon" -> exportDungeon(player, data, safePayload);
				case "begin_pool_entry_draft" -> ActionResult.info("Add an entity or #entity_type tag, then save the row.");
				default -> ActionResult.error("Unknown Studio action " + normalized + ".");
			};
		} catch (RuntimeException exception) {
			SololevelingMod.LOGGER.error("Dungeon Builder Studio action {} failed", normalized, exception);
			result = ActionResult.error("The Studio rejected this edit. Check latest.log for details.");
		}
		sendState(player, false, result.severity, result.message);
	}

	private static ActionResult createProject(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		String namespace = payload.getString("Namespace");
		String name = payload.getString("Name");
		DungeonBuilderProjectData.ProjectKind kind = enumValue(DungeonBuilderProjectData.ProjectKind.class,
				payload.getString("Kind"), DungeonBuilderProjectData.ProjectKind.MODULE);
		DungeonBuilderProjectData.ProjectResult result = data.createProject(player, namespace, name, kind);
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult deleteProject(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		ResourceLocation id = resource(payload.getString("ProjectId"));
		if (id == null)
			return ActionResult.error("Select a valid room project first.");
		DungeonBuilderProjectData.ProjectResult result = data.deleteProject(player, id.getNamespace(), id.getPath());
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult selectDungeon(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		if (data.project(player).kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			return ActionResult.error("Prebuilt presets export directly. Select a module to open saved dungeon drafts.");
		ResourceLocation id = resource(payload.getString("DungeonId"));
		if (id == null || data.dungeonDraft(player, id).isEmpty())
			return ActionResult.error("No saved dungeon draft named " + payload.getString("DungeonId") + " exists.");
		Session session = session(player);
		session.dungeonId = id.toString();
		clearDerived(session);
		return ActionResult.pass("Opened dungeon draft " + id + ".");
	}

	private static ActionResult newDungeon(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		if (data.project(player).kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			return ActionResult.error("Prebuilt presets export directly. Select a module to create a dungeon draft.");
		ResourceLocation id = resource(payload.getString("DungeonId"));
		if (id == null)
			return ActionResult.error("Dungeon ID must look like addon:my_dungeon.");
		if (data.dungeonDraft(player, id).isPresent())
			return ActionResult.error("Dungeon draft " + id + " already exists. Use Open instead of overwriting it.");
		DungeonDraft blank = new DungeonDraft(id, DungeonDraft.Mode.PROCEDURAL, DungeonDraft.Topology.LINEAR,
				List.of(), EnumSet.allOf(ProceduralDungeonRank.class), 3, 8, 16,
				new ResourceLocation("minecraft", "bedrock"), 1, List.of(), List.of());
		DungeonBuilderProjectData.MutationResult result = data.upsertDungeonDraft(player, blank);
		if (!result.success())
			return ActionResult.error(result.message());
		Session session = session(player);
		session.dungeonId = id.toString();
		clearDerived(session);
		return ActionResult.pass("Created and opened dungeon draft " + id + ". Add rooms, then press Apply Layout.");
	}

	private static ActionResult deleteDungeon(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		if (data.project(player).kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			return ActionResult.error("Select a module before managing saved dungeon drafts.");
		ResourceLocation id = resource(payload.getString("DungeonId"));
		if (id == null)
			return ActionResult.error("Select a saved dungeon draft first.");
		DungeonBuilderProjectData.MutationResult result = data.deleteDungeonDraft(player, id);
		if (!result.success())
			return ActionResult.error(result.message());
		Session session = session(player);
		if (session.dungeonId.equals(id.toString()))
			session.dungeonId = data.dungeonDrafts(player).stream().findFirst()
					.map(draft -> draft.id().toString()).orElse("");
		clearDerived(session);
		return ActionResult.pass(result.message());
	}

	private static ActionResult setRoomRole(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		DungeonBuilderProjectData.Project project = project(data, player, payload.getString("ProjectId"));
		if (project == null)
			return ActionResult.error("The selected room no longer exists.");
		DungeonBuilderProjectData.RoomRole role = enumValue(DungeonBuilderProjectData.RoomRole.class,
				payload.getString("Role"), null);
		if (role == null)
			return ActionResult.error("Choose a room role.");
		String message = project.setRoomRole(role);
		return ActionResult.pass(message);
	}

	private static ActionResult setRoomWeight(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		DungeonBuilderProjectData.Project project = project(data, player, payload.getString("ProjectId"));
		if (project == null)
			return ActionResult.error("The selected room no longer exists.");
		String message = project.setRoomWeight(payload.getInt("Weight"));
		return message.startsWith("Room generation") ? ActionResult.pass(message) : ActionResult.error(message);
	}

	private static ActionResult setProjectSettings(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		DungeonBuilderProjectData.Project project = project(data, player, payload.getString("ProjectId"));
		if (project == null)
			return ActionResult.error("The selected project no longer exists.");
		EnumSet<ProceduralDungeonRank> ranks = EnumSet.noneOf(ProceduralDungeonRank.class);
		for (String rank : stringList(payload, "Ranks", 8))
			ProceduralDungeonRank.tryParse(rank).ifPresent(ranks::add);
		if (ranks.isEmpty())
			ranks = EnumSet.allOf(ProceduralDungeonRank.class);
		ResourceLocation shell = resource(payload.getString("ShellBlock"));
		int thickness = payload.getInt("ShellThickness");
		if (shell == null || !ForgeRegistries.BLOCKS.containsKey(shell)
				|| ForgeRegistries.BLOCKS.getValue(shell) == net.minecraft.world.level.block.Blocks.AIR)
			return ActionResult.error("Choose a loaded, solid shell block.");
		if (thickness < 0 || thickness > 4)
			return ActionResult.error("Shell thickness must be 0-4.");
		String rankMessage = project.setAllowedRanks(ranks);
		String shellMessage = project.setShell(shell.toString(), thickness);
		return ActionResult.pass(rankMessage + " " + shellMessage);
	}

	public static boolean canEdit(ServerPlayer player, boolean notify) {
		if (!DungeonBuilderMode.isActive(player.level()) || !DungeonBuilderMode.isBuilderWorld(player.getServer())) {
			if (notify)
				player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
						"Dungeon Builder Studio is available only in a Dungeon Builder world."));
			return false;
		}
		boolean authorized = player.isCreative() && (player.hasPermissions(2)
				|| player.getServer().isSingleplayerOwner(player.getGameProfile()));
		if (!authorized && notify)
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
					"Creative mode and operator permission are required to edit dungeons."));
		return authorized;
	}

	private static ActionResult selectProject(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		ResourceLocation id = resource(payload.getString("ProjectId"));
		if (id == null)
			return ActionResult.error("Select a valid room ID.");
		DungeonBuilderProjectData.ProjectResult result = data.selectProject(player, id.getNamespace(), id.getPath());
		if (result.success()) {
			Session session = session(player);
			session.simulation = Session.emptySimulation();
			session.validation = Session.emptyValidation();
			session.simulationRevision = -1L;
			session.validationRevision = -1L;
		}
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult captureSnapshot(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		DungeonBuilderProjectData.Project project = project(data, player, payload.getString("ProjectId"));
		if (project == null)
			return ActionResult.error("The selected room no longer exists.");
		boolean update = payload.getBoolean("UpdateExisting");
		if (!update && project.roomSnapshot().isPresent())
			return ActionResult.warning("This room already has a snapshot. Use Update Snapshot to replace it intentionally.");
		DungeonBuilderRoomStore.CaptureResult result = DungeonBuilderRoomStore.capture(player, project,
				data.revision(player) + 1L);
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult assignAnchor(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		DungeonBuilderProjectData.Project project = project(data, player, payload.getString("ProjectId"));
		if (project == null)
			return ActionResult.error("The selected room no longer exists.");
		String markerId = localId(payload.getString("AnchorId"));
		if (payload.getString("AnchorKind").equalsIgnoreCase("TRIGGER")) {
			String requestedEncounter = payload.getString("EncounterId");
			String encounterId = requestedEncounter.isBlank() ? "room_mobs" : localId(requestedEncounter);
			if (markerId == null || encounterId == null)
				return ActionResult.error("Trigger Region and encounter IDs are required.");
			String assigned = project.assignTriggerRegion(markerId, encounterId);
			return assigned.startsWith("Assigned Trigger Region ")
					? ActionResult.pass(assigned) : ActionResult.error(assigned);
		}
		String poolId = payload.getString("PoolId");
		ResourceLocation pool = resource(poolId);
		DungeonBuilderProjectData.Project.SpawnRole role;
		try {
			role = DungeonBuilderProjectData.Project.SpawnRole.valueOf(
					payload.getString("SpawnRole").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return ActionResult.error("Choose whether this point spawns a normal, elite, or boss mob.");
		}
		String requestedEncounter = payload.getString("EncounterId");
		String encounterId = requestedEncounter.isBlank()
				? (role == DungeonBuilderProjectData.Project.SpawnRole.BOSS ? "boss" : "room_mobs")
				: localId(requestedEncounter);
		if (markerId == null || encounterId == null || pool == null)
			return ActionResult.error("Anchor, encounter, and mob-pool IDs are required.");
		boolean levelOverride = payload.getBoolean("LevelOverride");
		int min = clamp(payload.getInt("MinLevel"), 1, DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL);
		int max = levelOverride
				? clamp(payload.getInt("MaxLevel"), min, DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL)
				: Integer.MAX_VALUE;
		DungeonBuilderProjectData.Marker marker = project.markers().stream()
				.filter(value -> value.id().equals(markerId)).findFirst().orElse(null);
		if (marker == null)
			return ActionResult.error("Spawn point " + markerId + " no longer exists.");
		if (!Set.of("spawn_point", "mob_spawn", "elite_spawn", "boss_spawn").contains(marker.type()))
			return ActionResult.error("Anchor " + markerId + " is not an encounter spawn point.");
		String assigned = project.configureSpawnPoint(markerId, role, encounterId);
		if (!assigned.startsWith("Assigned "))
			return ActionResult.error(assigned);
		String configured = project.configureEncounter(encounterId, pool.toString(), min, max);
		if (!configured.startsWith("Encounter "))
			return ActionResult.error(configured);
		boolean delayed = payload.getBoolean("Delayed");
		String activation = project.configureEncounterActivation(encounterId, delayed);
		if (delayed && activation.contains("cannot be delayed"))
			return ActionResult.warning(assigned + " " + configured + " " + activation
					+ " The encounter remains automatic.");
		if (activation.startsWith("No encounter "))
			return ActionResult.error(activation);
		return ActionResult.pass(assigned + " " + configured + " " + activation);
	}

	private static ActionResult editSocket(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		DungeonBuilderProjectData.Project project = project(data, player, payload.getString("ProjectId"));
		if (project == null)
			return ActionResult.error("The selected room no longer exists.");
		String socketId = localId(payload.getString("SocketId"));
		if (socketId == null)
			return ActionResult.error("Select a socket first.");
		String value = payload.getString("SocketType").toLowerCase(Locale.ROOT);
		String type = value.contains("stair") ? "stair" : "corridor";
		String message = project.configureSocket(socketId, type, payload.getBoolean("Required"));
		return message.startsWith("Socket ") ? ActionResult.pass(message) : ActionResult.error(message);
	}

	private static ActionResult createPool(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		ResourceLocation id = resource(payload.getString("PoolId"));
		if (id == null)
			return ActionResult.error("Mob-pool ID must look like addon:room_mobs.");
		DungeonBuilderProjectData.MutationResult result = data.upsertMobPool(player,
				new BuilderMobPool(id, List.of()));
		if (result.success())
			session(player).selectedPoolId = id.toString();
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult deletePool(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		ResourceLocation id = resource(payload.getString("PoolId"));
		if (id == null)
			return ActionResult.error("Select a mob pool first.");
		DungeonBuilderProjectData.MutationResult result = data.deleteMobPool(player, id);
		if (result.success() && id.toString().equals(session(player).selectedPoolId))
			session(player).selectedPoolId = "";
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult savePool(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		ResourceLocation id = resource(payload.getString("PoolId"));
		if (id == null)
			return ActionResult.error("Mob-pool ID must look like addon:room_mobs.");
		List<BuilderMobPool.Entry> entries = new ArrayList<>();
		ListTag list = payload.getList("Entries", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(MAX_ACTION_LIST, list.size()); index++) {
			CompoundTag entry = list.getCompound(index);
			ResourceLocation selector = resource(entry.getString("Selector"));
			if (selector == null)
				return ActionResult.error("Pool entry " + (index + 1) + " has an invalid entity or tag ID.");
			BuilderMobPool.SelectorKind kind = entry.getBoolean("Tag")
					? BuilderMobPool.SelectorKind.TAG : BuilderMobPool.SelectorKind.ENTITY;
			Optional<String> requiredMod = entry.getString("RequiredMod").isBlank()
					? Optional.empty() : Optional.of(entry.getString("RequiredMod"));
			Optional<BuilderMobPool.LevelRange> eligible = optionalRange(entry, "EligibleMin", "EligibleMax");
			Optional<BuilderMobPool.LevelRange> spawn = optionalRange(entry, "SpawnMin", "SpawnMax");
			Optional<Integer> xp = entry.contains("Xp", Tag.TAG_INT)
					? Optional.of(Math.max(0, entry.getInt("Xp"))) : Optional.empty();
			entries.add(new BuilderMobPool.Entry(kind, selector, Math.max(1, entry.getInt("Weight")),
					requiredMod, eligible, spawn, xp));
		}
		DungeonBuilderProjectData.MutationResult result = data.upsertMobPool(player,
				new BuilderMobPool(id, entries));
		if (result.success())
			session(player).selectedPoolId = id.toString();
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult saveDraft(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		if (data.project(player).kind() == DungeonBuilderProjectData.ProjectKind.PRESET)
			return ActionResult.error("Prebuilt presets export directly and do not use a layout draft.");
		ResourceLocation requestedId = resource(payload.getString("DungeonId"));
		Session session = session(player);
		if (requestedId == null || !requestedId.toString().equals(session.dungeonId))
			return ActionResult.error("Layout edits can only update the open dungeon. Use New or Open first.");
		if (data.dungeonDraft(player, requestedId).isEmpty())
			return ActionResult.error("Create this dungeon with New before applying its layout.");
		DungeonDraft draft;
		try {
			draft = readDraft(payload, data, player);
		} catch (IllegalArgumentException exception) {
			return ActionResult.error(exception.getMessage());
		}
		DungeonBuilderProjectData.MutationResult result = data.upsertDungeonDraft(player, draft);
		if (result.success())
			session.dungeonId = draft.id().toString();
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static ActionResult runSimulation(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		Session session = session(player);
		long now = player.serverLevel().getGameTime();
		if (now - session.lastSimulationTick < 5L)
			return ActionResult.warning("Wait a moment before requesting another simulation.");
		session.lastSimulationTick = now;
		ResourceLocation id = resource(payload.getString("DungeonId"));
		if (id == null)
			return ActionResult.error("Select a dungeon definition first.");
		long seed = payload.getLong("Seed");
		DungeonBuilderProjectData.Project active = data.project(player);
		if (active.kind() != DungeonBuilderProjectData.ProjectKind.PRESET)
			session.dungeonId = id.toString();
		session.simulation = active.kind() == DungeonBuilderProjectData.ProjectKind.PRESET
				? DungeonBuilderStudioSimulation.simulatePreset(player, active, seed)
				: DungeonBuilderStudioSimulation.simulate(player, data, id, seed);
		session.simulationRevision = data.revision(player);
		return "SUCCESS".equals(session.simulation.getString("Status"))
				? ActionResult.pass(session.simulation.getString("Message"))
				: ActionResult.error(session.simulation.getString("Message"));
	}

	private static ActionResult validateDungeon(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		ResourceLocation id = resource(payload.getString("DungeonId"));
		Session session = session(player);
		DungeonBuilderProjectData.Project active = data.project(player);
		boolean preset = active.kind() == DungeonBuilderProjectData.ProjectKind.PRESET;
		if (id != null && !preset)
			session.dungeonId = id.toString();
		session.validation = buildValidation(player, data, preset ? null : id);
		session.validationRevision = data.revision(player);
		int errors = session.validation.getInt("Errors");
		int warnings = session.validation.getInt("Warnings");
		return errors == 0 ? ActionResult.pass("Validation passed with " + warnings + " warning(s).")
				: ActionResult.error("Validation found " + errors + " blocking error(s).");
	}

	private static ActionResult exportDungeon(ServerPlayer player, DungeonBuilderProjectData data,
			CompoundTag payload) {
		Session session = session(player);
		long now = player.serverLevel().getGameTime();
		if (now - session.lastExportTick < 40L)
			return ActionResult.warning("Export is already being handled. Wait two seconds before retrying.");
		session.lastExportTick = now;
		ResourceLocation id = resource(payload.getString("DungeonId"));
		DungeonDatapackExporter.ExportResult result;
		DungeonBuilderProjectData.Project active = data.project(player);
		if (active.kind() == DungeonBuilderProjectData.ProjectKind.PRESET) {
			result = DungeonDatapackExporter.exportStudioPreset(player, data, active);
		} else if (id != null && data.dungeonDraft(player, id).isPresent()) {
			result = DungeonDatapackExporter.exportDraft(player, data, id);
			session.dungeonId = id.toString();
		} else {
			result = new DungeonDatapackExporter.ExportResult(false, null,
					"Save and validate a procedural or fixed Layout before exporting room modules.");
		}
		return result.success() ? ActionResult.pass(result.message()) : ActionResult.error(result.message());
	}

	private static void sendState(ServerPlayer player, boolean open, String severity, String message) {
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		Session session = session(player);
		session.noticeSeverity = severity;
		session.notice = message;
		CompoundTag state = buildState(player, data, session);
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new DungeonBuilderStudioStateMessage(open, state));
	}

	private static CompoundTag buildState(ServerPlayer player, DungeonBuilderProjectData data,
			Session session) {
		CompoundTag root = new CompoundTag();
		long revision = data.revision(player);
		root.putLong("Revision", revision);
		if (session.simulationRevision != revision) {
			session.simulation = Session.emptySimulation();
			session.simulationRevision = -1L;
		}
		if (session.validationRevision != revision) {
			session.validation = Session.emptyValidation();
			session.validationRevision = -1L;
		}
		DungeonBuilderProjectData.Project active = data.project(player);
		if (!session.selectedPoolId.isBlank() && data.mobPools(player).stream()
				.noneMatch(pool -> pool.id().toString().equals(session.selectedPoolId)))
			session.selectedPoolId = "";
		List<DungeonDraft> savedDrafts = data.dungeonDrafts(player);
		ResourceLocation rememberedDraft = resource(session.dungeonId);
		if (rememberedDraft == null || savedDrafts.stream().noneMatch(draft -> draft.id().equals(rememberedDraft)))
			session.dungeonId = savedDrafts.stream().findFirst().map(draft -> draft.id().toString())
					.orElse(active.namespace() + ":new_dungeon");
		root.putString("SelectedProjectId", active.id());
		root.putString("SelectedPoolId", session.selectedPoolId);
		String effectiveDungeonId = active.kind() == DungeonBuilderProjectData.ProjectKind.PRESET
				? active.id() : session.dungeonId;
		root.putString("DungeonId", effectiveDungeonId);

		ListTag projects = new ListTag();
		for (DungeonBuilderProjectData.Project project : data.projects(player))
			projects.add(projectTag(player, data, project, project.id().equals(active.id())));
		root.put("Projects", projects);
		ListTag pools = new ListTag();
		for (BuilderMobPool pool : data.mobPools(player))
			pools.add(poolTag(pool));
		root.put("Pools", pools);
		ListTag dungeonDrafts = new ListTag();
		for (DungeonDraft saved : savedDrafts) {
			CompoundTag summary = new CompoundTag();
			summary.putString("Id", saved.id().toString());
			summary.putString("Mode", saved.mode().name());
			summary.putString("Topology", saved.topology().name());
			summary.putInt("RoomCount", saved.rooms().size());
			summary.putInt("PlacementCount", saved.fixedPlacements().size());
			dungeonDrafts.add(summary);
		}
		root.put("DungeonDrafts", dungeonDrafts);

		ResourceLocation dungeonId = resource(effectiveDungeonId);
		DungeonDraft draft = dungeonId == null ? null : data.dungeonDraft(player, dungeonId).orElse(null);
		root.put("Layout", layoutTag(draft, active));
		root.put("Simulation", session.simulation.copy());
		root.put("Validation", session.validation.copy());
		CompoundTag notice = new CompoundTag();
		notice.putString("Severity", session.noticeSeverity);
		notice.putString("Message", session.notice);
		root.put("Notice", notice);
		return root;
	}

	private static CompoundTag projectTag(ServerPlayer player, DungeonBuilderProjectData data,
			DungeonBuilderProjectData.Project project, boolean includeDetails) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Detailed", includeDetails);
		tag.putString("Id", project.id());
		tag.putString("Name", project.name());
		tag.putString("Kind", project.kind().name());
		tag.putString("Role", project.roomRole().name());
		tag.putInt("Weight", project.roomWeight());
		tag.put("Ranks", strings(project.allowedRanks().stream().map(Enum::name).sorted().toList()));
		tag.putString("ShellBlock", project.shellBlock());
		tag.putInt("ShellThickness", project.shellThickness());
		DungeonBuilderProjectData.Bounds bounds = project.structureBounds();
		BlockPos base = bounds == null ? BlockPos.ZERO : bounds.min();
		if (bounds != null) {
			CompoundTag boundsTag = new CompoundTag();
			putPoint(boundsTag, "Min", BlockPos.ZERO);
			putPoint(boundsTag, "Max", bounds.size().offset(-1, -1, -1));
			tag.put("Bounds", boundsTag);
		}
		Optional<RoomSnapshot> snapshot = project.roomSnapshot();
		tag.putBoolean("SnapshotCaptured", snapshot.isPresent());
		tag.putBoolean("SnapshotOutdated", false);
		tag.putLong("SnapshotRevision", snapshot.map(RoomSnapshot::metadataRevision).orElse(0L));

		if (includeDetails) {
			ListTag sockets = new ListTag();
			for (DungeonBuilderProjectData.Socket socket : project.sockets()) {
				CompoundTag socketTag = new CompoundTag();
				socketTag.putString("Id", socket.id());
				socketTag.putString("Type", socket.type());
				socketTag.putBoolean("Required", socket.required());
				socketTag.putString("Facing", socket.facing().name());
				BlockPos localMin = socket.opening().min().subtract(base);
				BlockPos localMax = socket.opening().max().subtract(base);
				putPoint(socketTag, "Position", new BlockPos((localMin.getX() + localMax.getX()) / 2,
						(localMin.getY() + localMax.getY()) / 2, (localMin.getZ() + localMax.getZ()) / 2));
				BlockPos size = socket.opening().size();
				int width = socket.facing().getAxis() == Direction.Axis.X ? size.getZ() : size.getX();
				int height = size.getY();
				if (socket.facing().getAxis() == Direction.Axis.Y) {
					// For vertical sockets these two fields preserve local X/Z; the client rotates
					// them with the room instead of sorting away a 2x3 versus 3x2 distinction.
					width = size.getX();
					height = size.getZ();
				}
				socketTag.putInt("OpeningWidth", Math.max(1, width));
				socketTag.putInt("OpeningHeight", Math.max(1, height));
				sockets.add(socketTag);
			}
			tag.put("Sockets", sockets);

			Map<String, DungeonBuilderProjectData.Encounter> encounters = new HashMap<>();
			for (DungeonBuilderProjectData.Encounter encounter : project.encounters())
				encounters.put(encounter.id(), encounter);
			ListTag anchors = new ListTag();
			for (DungeonBuilderProjectData.Marker marker : project.markers()) {
				CompoundTag anchor = new CompoundTag();
				anchor.putString("Id", marker.id());
				anchor.putString("Kind", markerKind(marker.type()));
				putPoint(anchor, "Position", marker.position().subtract(base));
				anchor.putString("EncounterId", marker.group());
				DungeonBuilderProjectData.Encounter encounter = encounters.get(marker.group());
				if (encounter != null) {
					anchor.putString("PoolId", encounter.pool());
					boolean levelOverride = encounter.maxLevel() != Integer.MAX_VALUE;
					anchor.putBoolean("LevelOverride", levelOverride);
					anchor.putInt("MinLevel", levelOverride ? encounter.minLevel() : 1);
					anchor.putInt("MaxLevel", levelOverride ? encounter.maxLevel() : 10);
					anchor.putBoolean("Delayed", encounter.delayed());
				}
				anchors.add(anchor);
			}
			for (DungeonBuilderProjectData.Region region : project.regions()) {
				if (!region.type().equals("trigger_region"))
					continue;
				CompoundTag anchor = new CompoundTag();
				anchor.putString("Id", region.id());
				anchor.putString("Kind", "TRIGGER");
				BlockPos min = region.bounds().min().subtract(base);
				BlockPos max = region.bounds().max().subtract(base);
				putPoint(anchor, "Position", new BlockPos((min.getX() + max.getX()) / 2,
						(min.getY() + max.getY()) / 2, (min.getZ() + max.getZ()) / 2));
				CompoundTag triggerBounds = new CompoundTag();
				putPoint(triggerBounds, "Min", min);
				putPoint(triggerBounds, "Max", max);
				anchor.put("Bounds", triggerBounds);
				anchor.putString("EncounterId", region.group());
				anchor.putBoolean("Delayed", true);
				anchors.add(anchor);
			}
			tag.put("Anchors", anchors);
		}

		int errors = 0;
		int warnings = 0;
		for (DungeonBuilderProjectData.Issue issue : project.validate()) {
			if (issue.severity() == DungeonBuilderProjectData.Severity.ERROR)
				errors++;
			else
				warnings++;
		}
		tag.putInt("Errors", errors);
		tag.putInt("Warnings", warnings);
		if (includeDetails) {
			DungeonBuilderRoomStore.Footprint footprint = DungeonBuilderRoomStore.footprint(player, project);
			tag.putInt("FootprintWidth", footprint.width());
			tag.putInt("FootprintDepth", footprint.depth());
			tag.putIntArray("FootprintColors", footprint.colors());
		}
		return tag;
	}

	private static CompoundTag poolTag(BuilderMobPool pool) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", pool.id().toString());
		ListTag entries = new ListTag();
		for (BuilderMobPool.Entry entry : pool.entries()) {
			CompoundTag value = new CompoundTag();
			value.putString("SelectorKind", entry.selectorKind().name());
			value.putString("Selector", entry.selector().toString());
			value.putInt("Weight", entry.weight());
			entry.requiredMod().ifPresent(mod -> value.putString("RequiredMod", mod));
			entry.eligibleLevel().ifPresent(range -> putRange(value, "Eligible", range));
			entry.spawnLevel().ifPresent(range -> putRange(value, "Spawn", range));
			entry.baseXp().ifPresent(xp -> value.putInt("Xp", xp));
			entries.add(value);
		}
		tag.put("Entries", entries);
		return tag;
	}

	private static CompoundTag layoutTag(DungeonDraft draft,
			DungeonBuilderProjectData.Project active) {
		CompoundTag tag = new CompoundTag();
		if (draft == null) {
			tag.putString("DungeonId", active.namespace() + ":new_dungeon");
			tag.putString("Mode", "PROCEDURAL");
			tag.putString("Topology", "LINEAR");
			tag.putInt("MinRooms", 3);
			tag.putInt("MaxRooms", 8);
			tag.putInt("MaxDepth", 8);
			tag.putString("ShellBlock", "minecraft:bedrock");
			tag.putInt("ShellThickness", 1);
			return tag;
		}
		tag.putString("DungeonId", draft.id().toString());
		tag.putString("Mode", draft.mode().name());
		tag.putString("Topology", draft.topology().name());
		tag.putInt("MinRooms", draft.minRooms());
		tag.putInt("MaxRooms", draft.maxRooms());
		tag.putInt("MaxDepth", draft.maxDepth());
		tag.putString("ShellBlock", draft.shellBlock().toString());
		tag.putInt("ShellThickness", draft.shellThickness());
		tag.put("Ranks", strings(draft.allowedRanks().stream().map(Enum::name).sorted().toList()));
		ListTag rooms = new ListTag();
		for (DungeonDraft.RoomRef room : draft.rooms()) {
			CompoundTag value = new CompoundTag();
			value.putString("ProjectId", room.room().toString());
			value.putInt("Weight", room.weight());
			rooms.add(value);
		}
		tag.put("Rooms", rooms);
		ListTag nodes = new ListTag();
		for (DungeonDraft.FixedPlacement placement : draft.fixedPlacements()) {
			CompoundTag value = new CompoundTag();
			value.putString("Id", placement.id());
			value.putString("ProjectId", placement.room().toString());
			value.putInt("X", placement.x());
			value.putInt("Y", placement.y());
			value.putInt("Z", placement.z());
			value.putString("Rotation", placement.rotation().name());
			nodes.add(value);
		}
		tag.put("Nodes", nodes);
		ListTag connections = new ListTag();
		for (DungeonDraft.FixedConnection connection : draft.fixedConnections()) {
			CompoundTag value = new CompoundTag();
			value.putString("FromNode", connection.fromPlacement());
			value.putString("FromSocket", connection.fromSocket());
			value.putString("ToNode", connection.toPlacement());
			value.putString("ToSocket", connection.toSocket());
			connections.add(value);
		}
		tag.put("Connections", connections);
		return tag;
	}

	private static CompoundTag buildValidation(ServerPlayer player, DungeonBuilderProjectData data,
			ResourceLocation dungeonId) {
		CompoundTag result = new CompoundTag();
		result.putBoolean("HasRun", true);
		ListTag issues = new ListTag();
		int errors = 0;
		int warnings = 0;
		DungeonDraft draft = dungeonId == null ? null : data.dungeonDraft(player, dungeonId).orElse(null);
		Set<String> included = new java.util.LinkedHashSet<>();
		if (draft != null) {
			if (draft.mode() == DungeonDraft.Mode.FIXED)
				draft.fixedPlacements().forEach(room -> included.add(room.room().toString()));
			else
				draft.rooms().forEach(room -> included.add(room.room().toString()));
		} else {
			included.add(data.project(player).id());
		}
		Map<String, DungeonBuilderProjectData.Project> byId = new HashMap<>();
		data.projects(player).forEach(project -> byId.put(project.id(), project));
		for (DungeonBuilderProjectData.Project project : data.projects(player)) {
			if (!included.contains(project.id()))
				continue;
			if (project.roomSnapshot().isEmpty()) {
				issues.add(validationIssue("ERROR", "SNAPSHOT_MISSING",
						"Capture this room before simulation or export.", project.id(), ""));
				errors++;
			} else {
				DungeonBuilderRoomStore.VerificationResult verification = DungeonBuilderRoomStore.verify(player, project);
				if (!verification.valid()) {
					issues.add(validationIssue("ERROR", "SNAPSHOT_INVALID",
							verification.status() + ". Capture this room again.", project.id(), ""));
					errors++;
				}
			}
			for (DungeonBuilderProjectData.Issue issue : project.validate()) {
				issues.add(validationIssue(issue.severity().name(), "ROOM_VALIDATION",
						issue.message(), project.id(), ""));
				if (issue.severity() == DungeonBuilderProjectData.Severity.ERROR)
					errors++;
				else
					warnings++;
				if (issues.size() >= 128)
					break;
			}
		}
		if (dungeonId != null && draft == null) {
			issues.add(validationIssue("ERROR", "MISSING_DUNGEON",
					"Save the Layout as " + dungeonId + " before simulation or export.", "", ""));
			errors++;
		}
		if (draft != null) {
			for (String projectId : included) {
				if (!byId.containsKey(projectId)) {
					issues.add(validationIssue("ERROR", "ROOM_MISSING",
							"The layout references a room that no longer exists: " + projectId + ".",
							projectId, ""));
					errors++;
				}
			}
			long starts = included.stream().map(byId::get).filter(java.util.Objects::nonNull)
					.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.START).count();
			long bosses = included.stream().map(byId::get).filter(java.util.Objects::nonNull)
					.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS).count();
			boolean middle = included.stream().map(byId::get).filter(java.util.Objects::nonNull)
					.anyMatch(project -> project.roomRole() != DungeonBuilderProjectData.RoomRole.START
							&& project.roomRole() != DungeonBuilderProjectData.RoomRole.BOSS);
			if (draft.mode() == DungeonDraft.Mode.PROCEDURAL && (starts == 0 || bosses == 0 || !middle)) {
				issues.add(validationIssue("ERROR", "ROOM_ROLES",
						"Procedural layouts need a start room, a middle room, and a boss room.", "", ""));
				errors++;
			}
			long placedStarts = draft.fixedPlacements().stream()
					.map(placement -> byId.get(placement.room().toString()))
					.filter(java.util.Objects::nonNull)
					.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.START).count();
			long placedBosses = draft.fixedPlacements().stream()
					.map(placement -> byId.get(placement.room().toString()))
					.filter(java.util.Objects::nonNull)
					.filter(project -> project.roomRole() == DungeonBuilderProjectData.RoomRole.BOSS).count();
			if (draft.mode() == DungeonDraft.Mode.FIXED && (placedStarts != 1 || placedBosses != 1)) {
				issues.add(validationIssue("ERROR", "FIXED_TERMINALS",
						"Fixed layouts need exactly one placed start room and one placed boss room.", "", ""));
				errors++;
			}
		}
		Map<ResourceLocation, BuilderMobPool> pools = new HashMap<>();
		data.mobPools(player).forEach(pool -> pools.put(pool.id(), pool));
		Set<ResourceLocation> checkedPools = new java.util.HashSet<>();
		Set<ResourceLocation> checkedDirectEntities = new java.util.HashSet<>();
		for (String projectId : included) {
			DungeonBuilderProjectData.Project project = byId.get(projectId);
			if (project == null)
				continue;
			for (DungeonBuilderProjectData.Encounter encounter : project.encounters()) {
				boolean used = project.markersForExport().stream()
						.anyMatch(marker -> marker.group().equals(encounter.id()));
				ResourceLocation poolId = resource(encounter.pool());
				if (!used || poolId == null)
					continue;
				if (ForgeRegistries.ENTITY_TYPES.containsKey(poolId)) {
					if (checkedDirectEntities.add(poolId)
							&& !BuilderMobPoolPreflight.resolvesSpawnable(player.serverLevel(),
									BuilderMobPool.SelectorKind.ENTITY, poolId)) {
						issues.add(validationIssue("ERROR", "ENTITY_NOT_SPAWNABLE",
								"Configured entity " + poolId + " is not a loaded spawnable mob.",
								project.id(), encounter.id()));
						errors++;
					}
					continue;
				}
				BuilderMobPool pool = pools.get(poolId);
				if (pool == null) {
					issues.add(validationIssue("ERROR", "POOL_MISSING",
							"Encounter " + encounter.id() + " references missing pool " + poolId + ".",
							project.id(), encounter.id()));
					errors++;
				} else if (checkedPools.add(poolId)) {
					for (String problem : BuilderMobPoolPreflight.problems(player.serverLevel(), pool)) {
						if (issues.size() < 128)
							issues.add(validationIssue("ERROR", "POOL_UNRESOLVED", problem,
									project.id(), encounter.id()));
						errors++;
					}
				}
			}
		}
		if (draft != null && errors == 0) {
			DungeonBuilderStudioSimulation.CoverageResult coverage =
					DungeonBuilderStudioSimulation.validateCoverage(player, data, draft.id());
			if (!coverage.success()) {
				if (issues.size() >= 128)
					issues.remove(issues.size() - 1);
				issues.add(validationIssue("ERROR", "LAYOUT_COVERAGE",
						coverage.message(), "", ""));
				errors++;
			} else if (issues.size() < 128)
				issues.add(validationIssue("PASS", "LAYOUT_COVERAGE", coverage.message(), "", ""));
		}
		result.putInt("Errors", errors);
		result.putInt("Warnings", warnings);
		result.put("Issues", issues);
		return result;
	}

	private static CompoundTag validationIssue(String severity, String code, String message,
			String projectId, String elementId) {
		CompoundTag value = new CompoundTag();
		value.putString("Severity", severity);
		value.putString("Code", code);
		value.putString("Message", message);
		value.putString("ProjectId", projectId);
		value.putString("ElementId", elementId);
		return value;
	}

	private static DungeonDraft readDraft(CompoundTag payload, DungeonBuilderProjectData data,
			ServerPlayer player) {
		ResourceLocation id = resource(payload.getString("DungeonId"));
		if (id == null)
			throw new IllegalArgumentException("Dungeon ID must look like addon:my_dungeon.");
		DungeonDraft.Mode mode = enumValue(DungeonDraft.Mode.class, payload.getString("Mode"),
				DungeonDraft.Mode.PROCEDURAL);
		DungeonDraft.Topology topology = enumValue(DungeonDraft.Topology.class,
				payload.getString("Topology"), DungeonDraft.Topology.LINEAR);
		List<DungeonDraft.RoomRef> rooms = new ArrayList<>();
		ListTag roomTags = payload.getList("Rooms", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(MAX_ACTION_LIST, roomTags.size()); index++) {
			CompoundTag room = roomTags.getCompound(index);
			ResourceLocation roomId = resource(room.getString("ProjectId"));
			if (roomId != null)
				rooms.add(new DungeonDraft.RoomRef(roomId, Math.max(1, room.getInt("Weight"))));
		}
		if (rooms.isEmpty()) {
			for (String projectId : stringList(payload, "EnabledProjectIds", 128)) {
				ResourceLocation roomId = resource(projectId);
				if (roomId != null)
					rooms.add(new DungeonDraft.RoomRef(roomId, 1));
			}
		}
		EnumSet<ProceduralDungeonRank> ranks = EnumSet.noneOf(ProceduralDungeonRank.class);
		for (String rank : stringList(payload, "Ranks", 8))
			ProceduralDungeonRank.tryParse(rank).ifPresent(ranks::add);
		if (ranks.isEmpty())
			ranks = EnumSet.allOf(ProceduralDungeonRank.class);
		ResourceLocation shell = resource(payload.getString("ShellBlock"));
		if (shell == null)
			shell = new ResourceLocation("minecraft", "bedrock");
		List<DungeonDraft.FixedPlacement> placements = new ArrayList<>();
		ListTag nodes = payload.getList("Nodes", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(64, nodes.size()); index++) {
			CompoundTag node = nodes.getCompound(index);
			ResourceLocation room = resource(node.getString("ProjectId"));
			String nodeId = localId(node.getString("Id"));
			if (room == null || nodeId == null)
				continue;
			placements.add(new DungeonDraft.FixedPlacement(nodeId, room, node.getInt("X"), node.getInt("Y"),
					node.getInt("Z"), enumValue(DungeonDraft.PlacementRotation.class,
						node.getString("Rotation"), DungeonDraft.PlacementRotation.NONE)));
		}
		List<DungeonDraft.FixedConnection> connections = new ArrayList<>();
		ListTag connectionTags = payload.getList("Connections", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(128, connectionTags.size()); index++) {
			CompoundTag connection = connectionTags.getCompound(index);
			connections.add(new DungeonDraft.FixedConnection(connection.getString("FromNode"),
					connection.getString("FromSocket"), connection.getString("ToNode"),
					connection.getString("ToSocket")));
		}
		return new DungeonDraft(id, mode, topology, rooms, ranks,
				clamp(payload.getInt("MinRooms"), 1, 64),
				clamp(payload.getInt("MaxRooms"), 1, 64),
				clamp(payload.getInt("MaxDepth"), 1, 64), shell,
				clamp(payload.getInt("ShellThickness"), 0, 4), placements, connections);
	}

	@Nullable
	private static DungeonBuilderProjectData.Project project(DungeonBuilderProjectData data,
			ServerPlayer player, String id) {
		return data.projects(player).stream().filter(project -> project.id().equals(id)).findFirst().orElse(null);
	}

	private static Session session(ServerPlayer player) {
		return SESSIONS.compute(player.getUUID(), (ignored, existing) ->
				existing == null || existing.serverIdentity != player.getServer()
						? new Session(player.getServer()) : existing);
	}

	private static String markerKind(String type) {
		return switch (type) {
			case "spawn_point" -> "SPAWN_POINT";
			case "mob_spawn" -> "MOB_SPAWN";
			case "elite_spawn" -> "ELITE_SPAWN";
			case "boss_spawn" -> "BOSS_SPAWN";
			case "player_start" -> "PLAYER_START";
			case "exit", "return_portal" -> "RETURN_PORTAL";
			case "loot" -> "LOOT";
			default -> "CUSTOM";
		};
	}

	private static void putPoint(CompoundTag owner, String key, BlockPos point) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", point.getX());
		tag.putInt("Y", point.getY());
		tag.putInt("Z", point.getZ());
		owner.put(key, tag);
	}

	private static void putRange(CompoundTag owner, String prefix, BuilderMobPool.LevelRange range) {
		owner.putInt(prefix + "Min", range.min());
		owner.putInt(prefix + "Max", range.max());
	}

	private static Optional<BuilderMobPool.LevelRange> optionalRange(CompoundTag tag,
			String minKey, String maxKey) {
		if (!tag.contains(minKey, Tag.TAG_INT) || !tag.contains(maxKey, Tag.TAG_INT))
			return Optional.empty();
		return Optional.of(new BuilderMobPool.LevelRange(tag.getInt(minKey), tag.getInt(maxKey)));
	}

	private static ListTag strings(List<String> values) {
		ListTag result = new ListTag();
		values.forEach(value -> result.add(StringTag.valueOf(value)));
		return result;
	}

	private static List<String> stringList(CompoundTag tag, String key, int maximum) {
		ListTag list = tag.getList(key, Tag.TAG_STRING);
		List<String> values = new ArrayList<>();
		for (int index = 0; index < Math.min(maximum, list.size()); index++)
			values.add(list.getString(index));
		return values;
	}

	@Nullable
	private static ResourceLocation resource(String value) {
		return value == null || value.length() > 192 ? null : ResourceLocation.tryParse(value);
	}

	@Nullable
	private static String localId(String value) {
		if (value == null || value.isBlank() || value.length() > 64
				|| !value.matches("[a-z0-9][a-z0-9_.-]*"))
			return null;
		return value;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
		try {
			return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}

	private static void clearDerived(Session session) {
		session.simulation = Session.emptySimulation();
		session.validation = Session.emptyValidation();
		session.simulationRevision = -1L;
		session.validationRevision = -1L;
	}

	private static final class Session {
		private final Object serverIdentity;
		private String selectedPoolId = "";
		private String dungeonId = "";
		private CompoundTag simulation = emptySimulation();
		private CompoundTag validation = emptyValidation();
		private long simulationRevision = -1L;
		private long validationRevision = -1L;
		private String noticeSeverity = "INFO";
		private String notice = "";
		private long lastSimulationTick = Long.MIN_VALUE / 2L;
		private long lastExportTick = Long.MIN_VALUE / 2L;

		private Session(Object serverIdentity) {
			this.serverIdentity = serverIdentity;
		}

		private static CompoundTag emptySimulation() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Status", "IDLE");
			tag.putLong("Seed", 12345L);
			tag.put("Rooms", new ListTag());
			tag.put("Connections", new ListTag());
			return tag;
		}

		private static CompoundTag emptyValidation() {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean("HasRun", false);
			tag.put("Issues", new ListTag());
			return tag;
		}
	}

	private record ActionResult(String severity, String message) {
		private static ActionResult info(String message) { return new ActionResult("INFO", message); }
		private static ActionResult pass(String message) { return new ActionResult("PASS", message); }
		private static ActionResult warning(String message) { return new ActionResult("WARNING", message); }
		private static ActionResult error(String message) { return new ActionResult("ERROR", message); }
	}
}
