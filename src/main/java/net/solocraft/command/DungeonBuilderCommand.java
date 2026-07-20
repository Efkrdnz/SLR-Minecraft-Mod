package net.solocraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.solocraft.dungeon.builder.DungeonBuilderPreview;
import net.solocraft.dungeon.builder.DungeonBuilderProjectData;
import net.solocraft.dungeon.builder.DungeonDatapackExporter;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.item.DungeonBuilderWandItem;
import net.solocraft.util.DungeonBuilderMode;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Recovery and automation commands for the otherwise tool-first builder workflow. */
@Mod.EventBusSubscriber
public final class DungeonBuilderCommand {
	private DungeonBuilderCommand() {
	}

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		var roomRoleCommand = Commands.literal("role")
				.then(Commands.argument("role", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(
								new String[]{"start", "normal", "corridor", "junction", "dead_end", "treasure", "stair", "boss"}, builder))
						.executes(context -> setRoomRole(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "role"))));
		var newProject = Commands.literal("new")
				.then(Commands.argument("namespace", StringArgumentType.word())
						.then(Commands.argument("name", StringArgumentType.word())
								.then(Commands.argument("kind", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"preset", "module"}, builder))
										.executes(context -> createProject(context.getSource().getPlayerOrException(),
												StringArgumentType.getString(context, "namespace"),
												StringArgumentType.getString(context, "name"),
												StringArgumentType.getString(context, "kind"))))));
		var selectProjectCommand = Commands.literal("select")
				.then(Commands.argument("namespace", StringArgumentType.word())
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> selectProject(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "namespace"),
										StringArgumentType.getString(context, "name")))));
		var deleteProjectCommand = Commands.literal("delete")
				.then(Commands.argument("namespace", StringArgumentType.word())
						.then(Commands.argument("name", StringArgumentType.word())
								.then(Commands.literal("confirm")
										.executes(context -> deleteProject(context.getSource().getPlayerOrException(),
												StringArgumentType.getString(context, "namespace"),
												StringArgumentType.getString(context, "name"))))));
		var projectCommand = Commands.literal("project")
				.executes(context -> listProjects(context.getSource().getPlayerOrException()))
				.then(Commands.literal("list").executes(context -> listProjects(context.getSource().getPlayerOrException())))
				.then(newProject)
				.then(selectProjectCommand)
				.then(Commands.literal("reset").then(Commands.literal("confirm")
						.executes(context -> resetProject(context.getSource().getPlayerOrException()))))
				.then(roomRoleCommand)
				.then(deleteProjectCommand);
		var encounterCommand = Commands.literal("encounter")
				.executes(context -> listEncounters(context.getSource().getPlayerOrException()))
				.then(Commands.literal("list").executes(context -> listEncounters(context.getSource().getPlayerOrException())))
				.then(Commands.literal("select")
						.then(Commands.argument("group", StringArgumentType.word())
								.executes(context -> selectEncounter(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "group")))))
				.then(Commands.literal("configure")
						.then(Commands.argument("group", StringArgumentType.word())
								.then(Commands.argument("pool", ResourceLocationArgument.id())
										.then(Commands.argument("min_level", IntegerArgumentType.integer(1, DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL))
												.then(Commands.argument("max_level", IntegerArgumentType.integer(1, DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL))
														.executes(context -> configureEncounter(context.getSource().getPlayerOrException(),
																StringArgumentType.getString(context, "group"),
												ResourceLocationArgument.getId(context, "pool").toString(),
																IntegerArgumentType.getInteger(context, "min_level"),
																IntegerArgumentType.getInteger(context, "max_level"))))))));
		var settingsCommand = Commands.literal("settings")
				.then(Commands.literal("rank")
						.then(Commands.argument("rank", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										new String[]{"E", "D", "C", "B", "A", "S", "all"}, builder))
								.executes(context -> setRank(context.getSource().getPlayerOrException(),
										StringArgumentType.getString(context, "rank")))))
				.then(Commands.literal("ranks")
						.then(Commands.argument("minimum", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										new String[]{"E", "D", "C", "B", "A", "S"}, builder))
								.then(Commands.argument("maximum", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(
												new String[]{"E", "D", "C", "B", "A", "S"}, builder))
										.executes(context -> setRankRange(context.getSource().getPlayerOrException(),
												StringArgumentType.getString(context, "minimum"),
												StringArgumentType.getString(context, "maximum"))))))
				.then(Commands.literal("pool")
						.then(Commands.literal("normal").then(Commands.argument("id", ResourceLocationArgument.id())
								.executes(context -> setPool(context.getSource().getPlayerOrException(),
										ResourceLocationArgument.getId(context, "id").toString(), false))))
						.then(Commands.literal("boss").then(Commands.argument("id", ResourceLocationArgument.id())
								.executes(context -> setPool(context.getSource().getPlayerOrException(),
										ResourceLocationArgument.getId(context, "id").toString(), true)))))
				.then(Commands.literal("weight").then(Commands.argument("value", IntegerArgumentType.integer(1, 10000))
						.executes(context -> setWeight(context.getSource().getPlayerOrException(),
								IntegerArgumentType.getInteger(context, "value")))))
				.then(Commands.literal("shell").then(Commands.argument("block", ResourceLocationArgument.id())
						.then(Commands.argument("thickness", IntegerArgumentType.integer(0, 4))
								.executes(context -> setShell(context.getSource().getPlayerOrException(),
										ResourceLocationArgument.getId(context, "block").toString(),
										IntegerArgumentType.getInteger(context, "thickness"))))));
		var exportCommand = Commands.literal("export")
				.executes(context -> export(context.getSource().getPlayerOrException()))
				.then(Commands.literal("procedural")
						.then(Commands.argument("name", StringArgumentType.word())
								.then(Commands.argument("min_rooms", IntegerArgumentType.integer(3, 64))
										.then(Commands.argument("max_rooms", IntegerArgumentType.integer(3, 64))
												.executes(context -> exportProcedural(context.getSource().getPlayerOrException(),
														StringArgumentType.getString(context, "name"),
														IntegerArgumentType.getInteger(context, "min_rooms"),
														IntegerArgumentType.getInteger(context, "max_rooms")))))));
		var builderCommand = Commands.literal("dungeonbuilder").requires(source ->
				source.getEntity() instanceof ServerPlayer player
						&& (source.hasPermission(2) || player.getServer().isSingleplayerOwner(player.getGameProfile())))
				.executes(context -> help(context.getSource().getPlayerOrException()))
				.then(Commands.literal("help").executes(context -> help(context.getSource().getPlayerOrException())))
				.then(Commands.literal("tutorial")
						.executes(context -> tutorial(context.getSource().getPlayerOrException(), 0))
						.then(Commands.argument("step", IntegerArgumentType.integer(1, 4))
								.executes(context -> tutorial(context.getSource().getPlayerOrException(),
										IntegerArgumentType.getInteger(context, "step")))))
				.then(Commands.literal("status").executes(context -> status(context.getSource().getPlayerOrException())))
				.then(Commands.literal("preview").executes(context -> preview(context.getSource().getPlayerOrException())))
				.then(Commands.literal("validate").executes(context -> validate(context.getSource().getPlayerOrException())))
				.then(Commands.literal("undo").executes(context -> undo(context.getSource().getPlayerOrException())))
				.then(exportCommand)
				.then(encounterCommand)
				.then(settingsCommand)
				.then(projectCommand);
		event.getDispatcher().register(builderCommand);
	}

	private static int help(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		player.sendSystemMessage(Component.literal("Dungeon Builder Studio workflow").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		player.sendSystemMessage(Component.literal("Press N here to open the Studio. Run /dungeonbuilder tutorial for a guided three-room build.").withStyle(ChatFormatting.GREEN));
		player.sendSystemMessage(Component.literal("1. ROOMS: create one module per physical room; mark it with the wands and explicitly Capture Room.").withStyle(ChatFormatting.WHITE));
		player.sendSystemMessage(Component.literal("2. POOLS + ANCHORS: author weighted mobs, then assign every generic spawn point a role, pool, and encounter ID.").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("3. LAYOUT: open Dungeons, create a saved definition, include rooms, set ranks/counts/shell, then Apply.").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("4. SIMULATE: preview a seed. EXPORT: Validate, resolve errors, then Export Pack.").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("Encounters are automatic unless you deliberately assign a Trigger Region to their encounter ID.").withStyle(ChatFormatting.YELLOW));
		player.sendSystemMessage(Component.literal("Commands remain available for automation and recovery; Studio is the recommended authoring path.").withStyle(ChatFormatting.DARK_GRAY));
		player.sendSystemMessage(Component.literal("Runtime diagnostics: /slrdungeon list, issues, pool test, generate, instances, and bindgate.").withStyle(ChatFormatting.DARK_AQUA));
		player.sendSystemMessage(Component.literal("Sneak + right-click air to cycle modes. Sneak-right-click interactive blocks to mark them.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int tutorial(ServerPlayer player, int step) {
		if (!builderWorld(player))
			return 0;
		if (step == 0) {
			player.sendSystemMessage(Component.literal("Studio tutorial: simple three-room procedural dungeon").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
			player.sendSystemMessage(Component.literal("Critical rule: one module project = one physical room. Do not mark start, normal, and boss rooms inside one project.").withStyle(ChatFormatting.RED));
			player.sendSystemMessage(Component.literal("Press N and follow tutorial steps 1-4. Room metadata autosaves; block snapshots and Layout changes require their Apply/Capture buttons.").withStyle(ChatFormatting.WHITE));
			player.sendSystemMessage(Component.literal("Step 1: /dungeonbuilder tutorial 1 - start room").withStyle(ChatFormatting.GREEN));
			player.sendSystemMessage(Component.literal("Step 2: /dungeonbuilder tutorial 2 - normal room").withStyle(ChatFormatting.GREEN));
			player.sendSystemMessage(Component.literal("Step 3: /dungeonbuilder tutorial 3 - boss room").withStyle(ChatFormatting.GREEN));
			player.sendSystemMessage(Component.literal("Step 4: /dungeonbuilder tutorial 4 - validate, export, and test").withStyle(ChatFormatting.GREEN));
			return 1;
		}
		player.sendSystemMessage(Component.literal("Tutorial " + step + "/4").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		switch (step) {
			case 1 -> {
				player.sendSystemMessage(Component.literal("START ROOM - press N, ROOMS > New Room, create test:simple_start as MODULE, and set Role to START.").withStyle(ChatFormatting.WHITE));
				player.sendSystemMessage(Component.literal("Build one sealed room. Surveyor: mark tight Structure Bounds, then its walkable Room Bounds.").withStyle(ChatFormatting.AQUA));
				player.sendSystemMessage(Component.literal("Socket Wand: mark exactly one Required Corridor doorway rectangle, facing outward. Keep the wall sealed.").withStyle(ChatFormatting.GOLD));
				player.sendSystemMessage(Component.literal("Feature Wand: mark Player Start and Return Portal inside. Refresh, inspect, then press Capture Room.").withStyle(ChatFormatting.GREEN));
			}
			case 2 -> {
				player.sendSystemMessage(Component.literal("NORMAL ROOM - create test:simple_normal as MODULE with Role NORMAL and mark only this second room's bounds.").withStyle(ChatFormatting.WHITE));
				player.sendSystemMessage(Component.literal("Mark two Required Corridor sockets. Adjacent walls make a turning room; opposite walls make a straight room.").withStyle(ChatFormatting.GOLD));
				player.sendSystemMessage(Component.literal("Encounter Wand: add generic Spawn Points. Refresh and Capture Room; these points are metadata, not saved entities.").withStyle(ChatFormatting.GREEN));
				player.sendSystemMessage(Component.literal("POOLS: create test:room_mobs, add a loaded mob, leave Eligible unset as a safe fallback, then Save Draft.").withStyle(ChatFormatting.RED));
				player.sendSystemMessage(Component.literal("ANCHORS: select each point, choose NORMAL, choose test:room_mobs, Configure encounter room_mobs, and leave Delayed off.").withStyle(ChatFormatting.RED));
			}
			case 3 -> {
				player.sendSystemMessage(Component.literal("BOSS ROOM - create test:simple_boss as MODULE with Role BOSS and mark only the third room's bounds.").withStyle(ChatFormatting.WHITE));
				player.sendSystemMessage(Component.literal("Mark one Required Corridor entrance and one generic Spawn Point, then Refresh and Capture Room.").withStyle(ChatFormatting.GOLD));
				player.sendSystemMessage(Component.literal("POOLS: create test:boss_pool with a loaded boss and Save Draft.").withStyle(ChatFormatting.RED));
				player.sendSystemMessage(Component.literal("ANCHORS: assign the point BOSS + test:boss_pool; Configure encounter boss and leave Delayed off.").withStyle(ChatFormatting.GREEN));
			}
			case 4 -> {
				player.sendSystemMessage(Component.literal("LAYOUT - press Dungeons > New and create test:simple_dungeon. Include all three rooms.").withStyle(ChatFormatting.WHITE));
				player.sendSystemMessage(Component.literal("Use PROCEDURAL + LINEAR, Min 3, Max 3. Setup: D rank, minecraft:bedrock, thickness 1; then press the Layout Apply button.").withStyle(ChatFormatting.YELLOW));
				player.sendSystemMessage(Component.literal("SIMULATE seed 12345. EXPORT: Validate, fix every blocking issue, then Export Pack.").withStyle(ChatFormatting.GREEN));
				player.sendSystemMessage(Component.literal("Enable the exact printed file/<folder>, run /reload, then inspect /slrdungeon issues.").withStyle(ChatFormatting.GRAY));
				player.sendSystemMessage(Component.literal("Test selection: /slrdungeon generate test:simple_dungeon seed 12345 confirm").withStyle(ChatFormatting.AQUA));
				player.sendSystemMessage(Component.literal("Gameplay: bind an unused D-rank procedural gate with /slrdungeon bindgate test:simple_dungeon.").withStyle(ChatFormatting.AQUA));
			}
			default -> {
				return 0;
			}
		}
		return 1;
	}

	private static int createProject(ServerPlayer player, String namespace, String name, String kindName) {
		if (!builderWorld(player))
			return 0;
		if (!kindName.equalsIgnoreCase("preset") && !kindName.equalsIgnoreCase("module")) {
			player.sendSystemMessage(Component.literal("Project kind must be preset or module.").withStyle(ChatFormatting.RED));
			return 0;
		}
		DungeonBuilderProjectData.ProjectKind kind = DungeonBuilderProjectData.ProjectKind.parse(kindName);
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		DungeonBuilderProjectData.ProjectResult result = data.createProject(player, namespace, name, kind);
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		if (result.success() && result.project() != null)
			player.sendSystemMessage(Component.literal("Active project: " + result.project().summary()).withStyle(ChatFormatting.AQUA));
		return result.success() ? 1 : 0;
	}

	private static int selectProject(ServerPlayer player, String namespace, String name) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData.ProjectResult result = DungeonBuilderProjectData.get(player.serverLevel())
				.selectProject(player, namespace, name);
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		return result.success() ? 1 : 0;
	}

	private static int resetProject(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData.ProjectResult result = DungeonBuilderProjectData.get(player.serverLevel())
				.resetActiveProject(player);
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		return result.success() ? 1 : 0;
	}

	private static int deleteProject(ServerPlayer player, String namespace, String name) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData.ProjectResult result = DungeonBuilderProjectData.get(player.serverLevel())
				.deleteProject(player, namespace, name);
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		return result.success() ? 1 : 0;
	}

	private static int listProjects(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		DungeonBuilderProjectData.Project active = data.project(player);
		player.sendSystemMessage(Component.literal("Your Dungeon Builder projects:").withStyle(ChatFormatting.AQUA));
		for (DungeonBuilderProjectData.Project project : data.projects(player)) {
			String prefix = project.id().equals(active.id()) ? "* " : "  ";
			player.sendSystemMessage(Component.literal(prefix + project.summary())
					.withStyle(project.id().equals(active.id()) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
		return 1;
	}

	private static int setRoomRole(ServerPlayer player, String roleName) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData.Project project = DungeonBuilderProjectData.get(player.serverLevel()).project(player);
		if (project.kind() != DungeonBuilderProjectData.ProjectKind.MODULE) {
			player.sendSystemMessage(Component.literal("Room roles only apply to procedural module projects.").withStyle(ChatFormatting.RED));
			return 0;
		}
		DungeonBuilderProjectData.RoomRole role;
		try {
			role = DungeonBuilderProjectData.RoomRole.valueOf(roleName.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			player.sendSystemMessage(Component.literal("Unknown room role: " + roleName + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		String message = project.setRoomRole(role);
		DungeonBuilderProjectData.get(player.serverLevel()).setDirty();
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int selectEncounter(ServerPlayer player, String group) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		String message = data.project(player).selectEncounterGroup(group);
		data.setDirty();
		boolean success = message.startsWith("Active encounter");
		player.sendSystemMessage(Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
		return success ? 1 : 0;
	}

	private static int configureEncounter(ServerPlayer player, String group, String pool, int minLevel, int maxLevel) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		String message = data.project(player).configureEncounter(group, pool, minLevel, maxLevel);
		data.setDirty();
		boolean success = message.startsWith("Encounter ");
		player.sendSystemMessage(Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
		if (success) {
			var configuredId = net.minecraft.resources.ResourceLocation.tryParse(pool);
			if (configuredId != null && ForgeRegistries.ENTITY_TYPES.containsKey(configuredId))
				player.sendSystemMessage(Component.literal("Recognized mob " + pool
						+ ". Export will automatically create the required one-mob pool.")
						.withStyle(ChatFormatting.AQUA));
			else
				player.sendSystemMessage(Component.literal("Using existing/custom mob pool " + pool
						+ ". It must be supplied by an enabled datapack.")
						.withStyle(ChatFormatting.YELLOW));
		}
		return success ? 1 : 0;
	}

	private static int listEncounters(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData.Project project = DungeonBuilderProjectData.get(player.serverLevel()).project(player);
		player.sendSystemMessage(Component.literal("Encounter groups (active: " + project.activeEncounterGroup() + "):").withStyle(ChatFormatting.AQUA));
		if (project.encounters().isEmpty()) {
			player.sendSystemMessage(Component.literal("  None yet. Select a group and place a spawn/trigger, or configure it directly.").withStyle(ChatFormatting.GRAY));
			return 1;
		}
		for (DungeonBuilderProjectData.Encounter encounter : project.encounters()) {
			String prefix = encounter.id().equals(project.activeEncounterGroup()) ? "* " : "  ";
			player.sendSystemMessage(Component.literal(prefix + encounter.id() + " -> " + encounter.pool()
					+ " (levels " + encounter.minLevel() + "-" + (encounter.maxLevel() == Integer.MAX_VALUE ? "dungeon" : encounter.maxLevel())
					+ ", " + (encounter.delayed() ? "delayed trigger" : "auto-spawn") + ")")
					.withStyle(encounter.id().equals(project.activeEncounterGroup()) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
		return 1;
	}

	private static int setPool(ServerPlayer player, String pool, boolean boss) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		String message = data.project(player).setDefaultMobPool(pool, boss);
		data.setDirty();
		boolean success = message.endsWith(".") && message.contains(" mob pool set to ");
		player.sendSystemMessage(Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
		return success ? 1 : 0;
	}

	private static int setRank(ServerPlayer player, String rankName) {
		if (!builderWorld(player))
			return 0;
		if (rankName.equalsIgnoreCase("all"))
			return applyRanks(player, java.util.EnumSet.allOf(ProceduralDungeonRank.class));
		var parsed = ProceduralDungeonRank.tryParse(rankName);
		if (parsed.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown dungeon rank " + rankName
					+ ". Use E, D, C, B, A, S, or all.").withStyle(ChatFormatting.RED));
			return 0;
		}
		return applyRanks(player, java.util.EnumSet.of(parsed.get()));
	}

	private static int setRankRange(ServerPlayer player, String minimumName, String maximumName) {
		if (!builderWorld(player))
			return 0;
		var minimum = ProceduralDungeonRank.tryParse(minimumName);
		var maximum = ProceduralDungeonRank.tryParse(maximumName);
		if (minimum.isEmpty() || maximum.isEmpty()) {
			player.sendSystemMessage(Component.literal("Rank ranges must use E, D, C, B, A, or S.")
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		if (minimum.get().numericRank > maximum.get().numericRank) {
			player.sendSystemMessage(Component.literal("Minimum rank cannot be above maximum rank.")
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		java.util.EnumSet<ProceduralDungeonRank> ranks = java.util.EnumSet.noneOf(ProceduralDungeonRank.class);
		for (ProceduralDungeonRank rank : ProceduralDungeonRank.values())
			if (rank.numericRank >= minimum.get().numericRank && rank.numericRank <= maximum.get().numericRank)
				ranks.add(rank);
		return applyRanks(player, ranks);
	}

	private static int applyRanks(ServerPlayer player, java.util.Set<ProceduralDungeonRank> ranks) {
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		DungeonBuilderProjectData.Project active = data.project(player);
		java.util.List<DungeonBuilderProjectData.Project> targets = active.kind() == DungeonBuilderProjectData.ProjectKind.MODULE
				? data.projects(player).stream()
						.filter(project -> project.kind() == DungeonBuilderProjectData.ProjectKind.MODULE)
						.filter(project -> project.namespace().equals(active.namespace())).toList()
				: java.util.List.of(active);
		for (DungeonBuilderProjectData.Project project : targets)
			project.setAllowedRanks(ranks);
		data.setDirty();
		String rankText = ranks.size() == ProceduralDungeonRank.values().length ? "all"
				: java.util.Arrays.stream(ProceduralDungeonRank.values()).filter(ranks::contains)
						.map(Enum::name).collect(java.util.stream.Collectors.joining(","));
		player.sendSystemMessage(Component.literal("Allowed dungeon ranks set to " + rankText
				+ (targets.size() > 1 ? " across " + targets.size() + " modules in " + active.namespace() : "") + ".")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int setWeight(ServerPlayer player, int weight) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		String message = data.project(player).setRoomWeight(weight);
		data.setDirty();
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int setShell(ServerPlayer player, String block, int thickness) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		String message = data.project(player).setShell(block, thickness);
		data.setDirty();
		boolean success = message.startsWith("Protective shell");
		player.sendSystemMessage(Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
		return success ? 1 : 0;
	}

	private static int status(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		player.sendSystemMessage(Component.literal(DungeonBuilderProjectData.get(player.serverLevel()).project(player).summary()).withStyle(ChatFormatting.AQUA));
		return 1;
	}

	private static int preview(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderPreview.show(player, DungeonBuilderProjectData.get(player.serverLevel()).project(player));
		return 1;
	}

	private static int validate(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderWandItem.sendValidation(player, DungeonBuilderProjectData.get(player.serverLevel()).project(player));
		return 1;
	}

	private static int undo(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		String result = data.project(player).undoLast();
		data.setDirty();
		player.sendSystemMessage(Component.literal(result).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int export(ServerPlayer player) {
		if (!builderWorld(player))
			return 0;
		DungeonDatapackExporter.ExportResult result = DungeonDatapackExporter.export(player,
				DungeonBuilderProjectData.get(player.serverLevel()).project(player));
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		if (result.success() && result.path() != null)
			player.sendSystemMessage(Component.literal("Saved to: " + result.path()).withStyle(ChatFormatting.DARK_GRAY));
		return result.success() ? 1 : 0;
	}

	private static int exportProcedural(ServerPlayer player, String name, int minRooms, int maxRooms) {
		if (!builderWorld(player))
			return 0;
		DungeonBuilderProjectData data = DungeonBuilderProjectData.get(player.serverLevel());
		DungeonBuilderProjectData.Project active = data.project(player);
		DungeonDatapackExporter.ExportResult result = DungeonDatapackExporter.exportProcedural(player,
				data.projects(player), active.namespace(), name, minRooms, maxRooms);
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		if (result.success() && result.path() != null)
			player.sendSystemMessage(Component.literal("Saved to: " + result.path()).withStyle(ChatFormatting.DARK_GRAY));
		return result.success() ? 1 : 0;
	}

	private static boolean builderWorld(ServerPlayer player) {
		if (DungeonBuilderMode.isActive(player.level()) && DungeonBuilderMode.isBuilderWorld(player.getServer()))
			return true;
		player.sendSystemMessage(Component.literal("This command only works in a Dungeon Builder world.").withStyle(ChatFormatting.RED));
		return false;
	}
}
