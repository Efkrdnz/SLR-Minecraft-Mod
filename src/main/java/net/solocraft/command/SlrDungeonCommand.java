package net.solocraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.dungeon.data.DungeonDataTypes;
import net.solocraft.dungeon.data.MobPoolResolver;
import net.solocraft.dungeon.ProceduralDungeonGateHandler;
import net.solocraft.dungeon.runtime.DungeonInstanceSavedData;
import net.solocraft.dungeon.runtime.DungeonRuntimeGenerator;
import net.solocraft.dungeon.runtime.DungeonEncounterRuntime;
import net.solocraft.dungeon.runtime.DungeonReturnPortalSpawner;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Operator-facing runtime, diagnostics, and addon testing commands. */
@Mod.EventBusSubscriber
public final class SlrDungeonCommand {
	private SlrDungeonCommand() {
	}

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		var levelBranch = Commands.literal("level")
				.then(Commands.argument("level", IntegerArgumentType.integer(1, DungeonInstanceSavedData.MAX_EFFECTIVE_LEVEL))
						.then(Commands.literal("confirm")
								.executes(context -> generate(context.getSource().getPlayerOrException(),
										ResourceLocationArgument.getId(context, "dungeon"),
										IntegerArgumentType.getInteger(context, "level"), null)))
						.then(Commands.literal("seed")
								.then(Commands.argument("seed", LongArgumentType.longArg())
										.then(Commands.literal("confirm")
												.executes(context -> generate(context.getSource().getPlayerOrException(),
														ResourceLocationArgument.getId(context, "dungeon"),
														IntegerArgumentType.getInteger(context, "level"),
												LongArgumentType.getLong(context, "seed")))))));
		var seedBranch = Commands.literal("seed")
				.then(Commands.argument("seed", LongArgumentType.longArg())
						.then(Commands.literal("confirm")
								.executes(context -> generate(context.getSource().getPlayerOrException(),
										ResourceLocationArgument.getId(context, "dungeon"), null,
										LongArgumentType.getLong(context, "seed")))));
		var generate = Commands.literal("generate")
				.then(Commands.argument("dungeon", ResourceLocationArgument.id())
						.then(Commands.literal("confirm")
								.executes(context -> generate(context.getSource().getPlayerOrException(),
										ResourceLocationArgument.getId(context, "dungeon"), null, null)))
						.then(levelBranch)
						.then(seedBranch));

		var pool = Commands.literal("pool")
				.then(Commands.literal("test")
						.then(Commands.argument("pool", ResourceLocationArgument.id())
								.then(Commands.argument("level", IntegerArgumentType.integer(1, DungeonInstanceSavedData.MAX_EFFECTIVE_LEVEL))
										.executes(context -> testPool(context.getSource().getPlayerOrException(),
												ResourceLocationArgument.getId(context, "pool"),
												IntegerArgumentType.getInteger(context, "level"))))));
		var bindGate = Commands.literal("bindgate")
				.then(Commands.argument("dungeon", ResourceLocationArgument.id())
						.executes(context -> bindGate(context.getSource().getPlayerOrException(),
								ResourceLocationArgument.getId(context, "dungeon"))));
		var encounter = Commands.literal("encounter")
				.then(Commands.literal("reset")
						.then(Commands.argument("instance_uuid", StringArgumentType.word())
								.then(Commands.argument("encounter_key", StringArgumentType.word())
										.then(Commands.literal("confirm")
												.executes(context -> resetEncounter(
														context.getSource().getPlayerOrException(),
														StringArgumentType.getString(context, "instance_uuid"),
														StringArgumentType.getString(context, "encounter_key")))))));

		var root = Commands.literal("slrdungeon")
				.requires(source -> source.hasPermission(2))
				.executes(context -> help(context.getSource().getPlayerOrException()))
				.then(Commands.literal("help").executes(context -> help(context.getSource().getPlayerOrException())))
				.then(Commands.literal("list").executes(context -> listDefinitions(context.getSource().getPlayerOrException())))
				.then(Commands.literal("issues").executes(context -> listIssues(context.getSource().getPlayerOrException())))
				.then(Commands.literal("instances").executes(context -> listInstances(context.getSource().getPlayerOrException())))
				.then(Commands.literal("instance").then(Commands.argument("uuid", StringArgumentType.word())
						.executes(context -> inspectInstance(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "uuid")))))
				.then(Commands.literal("prune").executes(context -> prune(context.getSource().getPlayerOrException())))
				.then(encounter)
				.then(Commands.literal("portal").then(Commands.argument("instance_uuid", StringArgumentType.word())
						.executes(context -> repairPortal(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "instance_uuid")))))
				.then(bindGate)
				.then(Commands.literal("unbindgate").then(Commands.literal("confirm")
						.executes(context -> unbindGate(context.getSource().getPlayerOrException()))))
				.then(Commands.literal("enter").then(Commands.argument("uuid", StringArgumentType.word())
						.executes(context -> enter(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "uuid")))))
				.then(Commands.literal("remove").then(Commands.argument("uuid", StringArgumentType.word())
						.then(Commands.literal("confirm").executes(context -> remove(context.getSource().getPlayerOrException(),
								StringArgumentType.getString(context, "uuid"))))))
				.then(generate)
				.then(pool);
		event.getDispatcher().register(root);
	}

	private static int help(ServerPlayer player) {
		player.sendSystemMessage(Component.literal("SLR Dungeon runtime").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		player.sendSystemMessage(Component.literal("/slrdungeon list — loaded dungeon definitions").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon issues — datapack validation messages").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon generate <namespace:id> [level <n>] [seed <n>] confirm").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon instances | instance <uuid> | enter <uuid> | remove <uuid> confirm | prune").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon encounter reset <instance-uuid> <encounter-key> confirm — recover a stuck wave").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon portal <instance-uuid> — create or repair its return portal").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon pool test <namespace:id> <dungeon-level>").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("/slrdungeon bindgate <namespace:dungeon> — bind the nearest unused procedural gate within 8 blocks").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.literal("Generate writes blocks from a safe origin 64 blocks in front of you; use a clear builder/dungeon area.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int listDefinitions(ServerPlayer player) {
		var ids = DungeonDataManager.dungeonIds();
		player.sendSystemMessage(Component.literal("Loaded dungeon definitions: " + ids.size()).withStyle(ChatFormatting.AQUA));
		ids.stream().limit(50).forEach(id -> {
			String details = DungeonDataManager.dungeon(id).map(definition -> {
				String ranks = java.util.Arrays.stream(net.solocraft.dungeon.ProceduralDungeonRank.values())
						.filter(definition.allowedRanks()::contains).map(Enum::name)
						.collect(java.util.stream.Collectors.joining(","));
				return definition.kind().name().toLowerCase() + ", ranks " + ranks;
			}).orElse("invalid");
			player.sendSystemMessage(Component.literal("  " + id + " [" + details + "]").withStyle(ChatFormatting.GRAY));
		});
		if (ids.size() > 50)
			player.sendSystemMessage(Component.literal("  ...and " + (ids.size() - 50) + " more").withStyle(ChatFormatting.DARK_GRAY));
		return ids.size();
	}

	private static int listIssues(ServerPlayer player) {
		var snapshot = DungeonDataManager.snapshot();
		player.sendSystemMessage(Component.literal("Dungeon data revision " + snapshot.revision() + ": "
				+ snapshot.issues().size() + " validation message(s).").withStyle(ChatFormatting.AQUA));
		for (int index = 0; index < Math.min(50, snapshot.issues().size()); index++) {
			DungeonDataTypes.ValidationIssue issue = snapshot.issues().get(index);
			player.sendSystemMessage(Component.literal("[" + issue.severity().name() + "] " + issue.resource() + ": " + issue.message())
					.withStyle(issue.severity() == DungeonDataTypes.Severity.ERROR ? ChatFormatting.RED : ChatFormatting.YELLOW));
		}
		return snapshot.issues().isEmpty() ? 1 : 0;
	}

	private static int generate(ServerPlayer player, ResourceLocation id, Integer level, Long suppliedSeed) {
		long seed = suppliedSeed == null ? player.getRandom().nextLong() : suppliedSeed;
		BlockPos minimum = player.blockPosition().relative(player.getDirection(), 64);
		player.sendSystemMessage(Component.literal("Planning " + id + " at a safe build origin " + minimum.toShortString()
				+ " with seed " + seed + "...").withStyle(ChatFormatting.YELLOW));
		DungeonRuntimeGenerator.GenerationResult result = DungeonRuntimeGenerator.generate(player, id, minimum, seed, level);
		player.sendSystemMessage(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
		if (!result.success())
			return 0;
		boolean portalReady = spawnReturnPortal(player.serverLevel(), result.instanceId(),
				result.exit() == null ? result.playerStart() : result.exit(), result.exitFacing());
		if (!portalReady)
			player.sendSystemMessage(Component.literal("The dungeon was generated, but its return portal could not spawn. Repair it with /slrdungeon portal "
					+ result.instanceId() + " before entering.").withStyle(ChatFormatting.RED));
		player.sendSystemMessage(Component.literal("Rooms: " + result.roomCount() + ", level: " + result.effectiveLevel()
				+ ", player start: " + result.playerStart().toShortString()).withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal("Enter with /slrdungeon enter " + result.instanceId()).withStyle(ChatFormatting.GRAY));
		return portalReady ? 1 : 0;
	}

	private static int listInstances(ServerPlayer player) {
		var views = DungeonInstanceSavedData.get(player.serverLevel()).views();
		player.sendSystemMessage(Component.literal("Dungeon instances: " + views.size()).withStyle(ChatFormatting.AQUA));
		for (DungeonInstanceSavedData.InstanceView view : views) {
			player.sendSystemMessage(Component.literal("  " + view.id() + " " + view.dungeonId() + " ["
					+ (view.completed() ? "complete" : "active") + ", " + view.rooms().size() + " rooms, level "
					+ view.effectiveLevel() + "]").withStyle(view.completed() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
		return views.size();
	}

	private static int inspectInstance(ServerPlayer player, String uuidText) {
		UUID uuid = parseUuid(player, uuidText);
		if (uuid == null)
			return 0;
		Optional<DungeonInstanceSavedData.Instance> found = DungeonInstanceSavedData.get(player.serverLevel()).getInstance(uuid);
		if (found.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown dungeon instance " + uuid + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		DungeonInstanceSavedData.Instance instance = found.get();
		player.sendSystemMessage(Component.literal(instance.id() + " — " + instance.dungeonId())
				.withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal("Dimension: " + instance.dimension().location() + ", level: "
				+ instance.effectiveLevel() + ", participants: " + instance.participants().size()
				+ ", state: " + (instance.completed() ? "complete" : "active")).withStyle(ChatFormatting.GRAY));
		for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters()) {
			String state = encounter.completed() ? "complete" : encounter.activated() ? "active" : "waiting";
			player.sendSystemMessage(Component.literal("  " + encounter.key() + " [" + state + ", pool "
					+ encounter.poolId() + ", tracked " + encounter.trackedMobs().size() + "]")
					.withStyle(encounter.completed() ? ChatFormatting.GREEN
							: encounter.activated() ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
		}
		return 1;
	}

	private static int enter(ServerPlayer player, String uuidText) {
		if (player.level().dimension() != Level.OVERWORLD) {
			player.sendSystemMessage(Component.literal("Use /slrdungeon enter from the overworld so a safe return position can be saved.")
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		UUID uuid = parseUuid(player, uuidText);
		if (uuid == null)
			return 0;
		Optional<DungeonInstanceSavedData.Instance> found = DungeonInstanceSavedData.get(player.serverLevel()).getInstance(uuid);
		if (found.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown dungeon instance " + uuid + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		DungeonInstanceSavedData.Instance instance = found.get();
		ServerLevel target = player.getServer().getLevel(instance.dimension());
		if (target == null || instance.playerStart().isEmpty()) {
			player.sendSystemMessage(Component.literal("Instance dimension or player start is unavailable.").withStyle(ChatFormatting.RED));
			return 0;
		}
		BlockPos start = instance.playerStart().get();
		if (!instance.participants().contains(player.getUUID()) && !instance.addParticipant(player.getUUID())) {
			player.sendSystemMessage(Component.literal("This instance already has the maximum number of participants.")
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = player.getX();
			capability.DunY = player.getY();
			capability.DunZ = player.getZ();
			capability.dungeoning = true;
			capability.BossKilled = false;
			capability.syncPlayerVariables(player);
		});
		player.getPersistentData().putString("slr_dungeon_instance", instance.id().toString());
		player.getPersistentData().putString("dungeon_tag", instance.id().toString());
		DungeonEncounterRuntime.restoreCompletionFor(player, instance);
		player.teleportTo(target, start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
		player.sendSystemMessage(Component.literal("Entered dungeon instance " + uuid + ".").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static boolean spawnReturnPortal(ServerLevel level, UUID instanceId, BlockPos position,
			@javax.annotation.Nullable net.minecraft.core.Direction facing) {
		if (level == null || instanceId == null || position == null)
			return false;
		return DungeonReturnPortalSpawner.spawn(level, position, facing, instanceId, instanceId.toString()) != null;
	}

	private static int repairPortal(ServerPlayer player, String uuidText) {
		UUID uuid = parseUuid(player, uuidText);
		if (uuid == null)
			return 0;
		Optional<DungeonInstanceSavedData.Instance> found = DungeonInstanceSavedData.get(player.serverLevel()).getInstance(uuid);
		if (found.isEmpty() || found.get().exit().isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown instance or missing saved exit marker: " + uuid)
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		ServerLevel level = player.getServer().getLevel(found.get().dimension());
		if (!spawnReturnPortal(level, uuid, found.get().exit().get(), found.get().exitFacing().orElse(null))) {
			player.sendSystemMessage(Component.literal("Could not spawn a return portal for " + uuid + ".")
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		player.sendSystemMessage(Component.literal("Spawned a scoped return portal for " + uuid + ".")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int remove(ServerPlayer player, String uuidText) {
		UUID uuid = parseUuid(player, uuidText);
		if (uuid == null)
			return 0;
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(player.serverLevel());
		Optional<DungeonInstanceSavedData.Instance> found = registry.getInstance(uuid);
		if (found.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown dungeon instance " + uuid + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		DungeonInstanceSavedData.Instance instance = found.get();
		ServerLevel level = player.getServer().getLevel(instance.dimension());
		int discarded = 0;
		if (level != null) {
			for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters()) {
				for (UUID mobId : encounter.trackedMobs()) {
					if (level.getEntity(mobId) != null) {
						level.getEntity(mobId).discard();
						discarded++;
					}
				}
			}
		}
		registry.remove(uuid);
		player.sendSystemMessage(Component.literal("Removed instance state and " + discarded
				+ " loaded tracked mob(s). Generated blocks were intentionally left in place to avoid unsafe world deletion.")
				.withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int resetEncounter(ServerPlayer player, String uuidText, String encounterKey) {
		UUID uuid = parseUuid(player, uuidText);
		if (uuid == null)
			return 0;
		Optional<DungeonInstanceSavedData.Instance> found = DungeonInstanceSavedData.get(player.serverLevel()).getInstance(uuid);
		if (found.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown dungeon instance " + uuid + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		DungeonInstanceSavedData.Instance instance = found.get();
		if (instance.completed()) {
			player.sendSystemMessage(Component.literal("Completed instances cannot restart encounters.").withStyle(ChatFormatting.RED));
			return 0;
		}
		Optional<DungeonInstanceSavedData.EncounterState> state = instance.encounter(encounterKey);
		if (state.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown encounter key " + encounterKey + ". Use /slrdungeon instance " + uuid + ".")
					.withStyle(ChatFormatting.RED));
			return 0;
		}
		ServerLevel level = player.getServer().getLevel(instance.dimension());
		int discarded = 0;
		if (level != null) {
			for (UUID mobId : state.get().trackedMobs()) {
				if (level.getEntity(mobId) != null) {
					level.getEntity(mobId).discard();
					discarded++;
				}
			}
		}
		state.get().resetProgress();
		player.sendSystemMessage(Component.literal("Reset encounter " + encounterKey + " and removed " + discarded
				+ " loaded mob(s). It will activate automatically, or wait for its optional trigger if configured.")
				.withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int prune(ServerPlayer player) {
		int removed = DungeonInstanceSavedData.get(player.serverLevel()).pruneCompletedEmptyInstances();
		player.sendSystemMessage(Component.literal("Pruned " + removed + " completed dungeon instance record(s) with no remaining participants.")
				.withStyle(removed > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		return 1;
	}

	private static int testPool(ServerPlayer player, ResourceLocation pool, int dungeonLevel) {
		Optional<MobPoolResolver.Selection> selection = MobPoolResolver.select(player.serverLevel(), pool,
				dungeonLevel, player.getRandom());
		if (selection.isEmpty()) {
			player.sendSystemMessage(Component.literal("Pool " + pool + " has no eligible, currently loaded mob entries at level "
					+ dungeonLevel + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		MobPoolResolver.Selection value = selection.get();
		String xp = value.baseXp().map(amount -> Integer.toString(amount)).orElse("automatic by level/role");
		player.sendSystemMessage(Component.literal("Pool roll: " + value.entityTypeId() + " at mob level " + value.level()
				+ ", base XP " + xp + " via " + value.selector().key()).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int bindGate(ServerPlayer player, ResourceLocation dungeon) {
		var definition = DungeonDataManager.dungeon(dungeon);
		if (definition.isEmpty()) {
			player.sendSystemMessage(Component.literal("Unknown or invalid dungeon " + dungeon + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		Entity gate = nearestProceduralGate(player);
		if (gate == null) {
			player.sendSystemMessage(Component.literal("No procedural gate was found within 8 blocks.").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (ProceduralDungeonGateHandler.isGenerated(gate)) {
			player.sendSystemMessage(Component.literal("That gate was already generated and cannot be rebound safely.").withStyle(ChatFormatting.RED));
			return 0;
		}
		var gateRank = ProceduralDungeonGateHandler.rankFor(gate);
		if (!definition.get().supportsRank(gateRank)) {
			String ranks = java.util.Arrays.stream(net.solocraft.dungeon.ProceduralDungeonRank.values())
					.filter(definition.get().allowedRanks()::contains).map(Enum::name)
					.collect(java.util.stream.Collectors.joining(","));
			player.sendSystemMessage(Component.literal("Cannot bind " + dungeon + " to this " + gateRank.name()
					+ "-rank gate. The dungeon allows ranks " + ranks + ".").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!ProceduralDungeonGateHandler.bindDatapackDungeon(gate, dungeon)) {
			player.sendSystemMessage(Component.literal("Could not bind that gate.").withStyle(ChatFormatting.RED));
			return 0;
		}
		player.sendSystemMessage(Component.literal("Bound " + gateRank.name() + "-rank gate " + gate.getStringUUID()
				+ " to " + dungeon + ". It will generate in the gate's rank-routed dungeon dimension.")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int unbindGate(ServerPlayer player) {
		Entity gate = nearestProceduralGate(player);
		if (gate == null) {
			player.sendSystemMessage(Component.literal("No procedural gate was found within 8 blocks.").withStyle(ChatFormatting.RED));
			return 0;
		}
		if (!ProceduralDungeonGateHandler.unbindDatapackDungeon(gate)) {
			player.sendSystemMessage(Component.literal("That gate is already generated or cannot be changed.").withStyle(ChatFormatting.RED));
			return 0;
		}
		player.sendSystemMessage(Component.literal("Removed the custom dungeon binding from that gate.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static Entity nearestProceduralGate(ServerPlayer player) {
		return player.serverLevel().getEntities(player, player.getBoundingBox().inflate(8.0D),
				ProceduralDungeonGateHandler::isProceduralGate).stream()
				.min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
	}

	private static UUID parseUuid(ServerPlayer player, String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException exception) {
			player.sendSystemMessage(Component.literal("Invalid UUID: " + value).withStyle(ChatFormatting.RED));
			return null;
		}
	}
}
