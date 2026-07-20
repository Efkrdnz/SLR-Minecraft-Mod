package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.VesselSelectionStateMessage;
import net.solocraft.procedures.JobChangeCleanupProcedure;
import net.solocraft.util.VesselManager.AssignmentResult;
import net.solocraft.util.VesselManager.VesselDefinition;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public final class JobChangeQuestManager {
	public static final String QUEST_ID = "job_change";
	public static final int STATE_IDLE = 0;
	public static final int STATE_DUNGEON_ACTIVE = -4;
	public static final int STATE_SHADOW_PRESENTATION = -3;
	public static final int STATE_ADVANCEMENT = -2;
	public static final int STATE_SELECTION = -1;

	private static final String TOKEN = QUEST_ID + ",";
	private static final String SELECTION_AUTHORIZED = "slr_job_change_selection_authorized";
	private static final String COMMAND_SELECTION_AUTHORIZED = "slr_command_vessel_selection_authorized";
	private static final int ACCENT = 0xFF7A5CFF;
	private static final double PARTY_RANGE_SQR = 256.0D * 256.0D;

	private JobChangeQuestManager() {
	}

	public static boolean isUnlocked(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return vars.JOB > 0 || contains(vars.unlocked_quests, QUEST_ID);
	}

	public static boolean isFinished(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return contains(vars.finished_quests, QUEST_ID) || (vars.JOB > 0 && state(vars) == STATE_IDLE);
	}

	public static boolean isVisible(Entity entity) {
		return isUnlocked(entity) && !isFinished(entity);
	}

	public static boolean isDungeonActive(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_DUNGEON_ACTIVE;
	}

	public static boolean isAdvancementActive(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_ADVANCEMENT;
	}

	public static boolean isSelectionPending(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_SELECTION;
	}

	public static boolean isShadowPresentation(Entity entity) {
		return entity != null && state(vars(entity)) == STATE_SHADOW_PRESENTATION;
	}

	public static boolean canResumeDungeon(Entity entity) {
		return isDungeonActive(entity) || isAdvancementActive(entity);
	}

	public static int advancementPoints(Entity entity) {
		return entity == null ? 0 : Math.max(0, (int) Math.round(vars(entity).jobadvpoint));
	}

	public static int requiredPoints(Entity entity) {
		if (entity == null)
			return 50;
		return Math.max(1, entity.level().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_POINTS));
	}

	public static void unlock(Entity entity) {
		if (entity == null || isFinished(entity))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!contains(capability.unlocked_quests, QUEST_ID))
				capability.unlocked_quests = append(capability.unlocked_quests);
			capability.jobkey = true;
			capability.syncPlayerVariables(entity);
		});
		if (entity instanceof ServerPlayer player)
			SystemNotifications.showTitleUnder(player, ACCENT, 100,
					Component.literal("QUEST UNLOCKED").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					Component.literal("Job Change Quest").withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	public static void startDungeonRun(ServerPlayer player) {
		if (player == null)
			return;
		player.getPersistentData().remove(SELECTION_AUTHORIZED);
		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.jobtimer = STATE_DUNGEON_ACTIVE;
			capability.jobadvpoint = 0;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.syncPlayerVariables(player);
		});
	}

	/** Starts the post-Igris trial for the killer and nearby members of their party. */
	public static List<ServerPlayer> beginAdvancementPhase(ServerPlayer killer) {
		List<ServerPlayer> participants = questParticipants(killer, false);
		for (ServerPlayer player : participants) {
			if (isAdvancementActive(player) || isSelectionPending(player) || isShadowPresentation(player))
				continue;
			player.getPersistentData().remove(SELECTION_AUTHORIZED);
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.giftstatus = true;
				capability.jobtimer = STATE_ADVANCEMENT;
				capability.jobadvpoint = 0;
				capability.JobChange_timer = 0;
				capability.instancecomplete = false;
				capability.syncPlayerVariables(player);
			});
			SystemNotifications.showTitleUnder(player, 0xFFDF9607, 100,
					Component.literal("BOSS SLAIN").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal("Defeat the summoned knights [0/" + requiredPoints(player) + "]")
							.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return participants;
	}

	/** Grants one shared party point for a qualifying knight kill. */
	public static void grantAdvancementPoint(ServerPlayer killer, Entity defeated) {
		if (killer == null || defeated == null || !isAdvancementActive(killer))
			return;
		List<ServerPlayer> recipients = questParticipants(killer, true);
		if (recipients.isEmpty())
			return;

		List<ServerPlayer> completed = new ArrayList<>();
		for (ServerPlayer player : recipients) {
			int required = requiredPoints(player);
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobadvpoint = Math.min(required, Math.max(0, capability.jobadvpoint) + 1);
				capability.syncPlayerVariables(player);
			});
			int progress = advancementPoints(player);
			player.displayClientMessage(Component.literal("Advancement Points [" + progress + "/" + required + "]")
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
			if (progress >= required)
				completed.add(player);
		}

		if (!completed.isEmpty()) {
			JobChangeCleanupProcedure.execute(defeated.level(), defeated.getX(), defeated.getY(), defeated.getZ());
			for (ServerPlayer player : completed)
				enterSelection(player);
		}
	}

	public static void selectVessel(ServerPlayer player, String type, String identity) {
		if (player == null)
			return;
		if (!isSelectionPending(player) || !isSelectionAuthorized(player)) {
			if (isFinished(player) || vars(player).JOB > 0)
				closeSelection(player);
			else
				selectionError(player, "The Job Change trial is not complete.");
			return;
		}

		VesselDefinition definition = VesselManager.definition(type, identity);
		if (definition == null) {
			selectionError(player, "That vessel does not exist.");
			return;
		}

		AssignmentResult result = VesselManager.assignPlayer(player, definition, true);
		if (result == AssignmentResult.LOCKED) {
			selectionError(player, definition.name() + " has already reached the server limit.");
			return;
		}
		if (result != AssignmentResult.SUCCESS) {
			selectionError(player, "The System could not assign that vessel.");
			return;
		}

		player.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
		closeSelection(player);
		if ("ashborn".equals(definition.identity())) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_SHADOW_PRESENTATION;
				capability.JobChange_timer = 1;
				capability.instancecomplete = false;
				capability.syncPlayerVariables(player);
			});
			return;
		}

		finish(player);
		int color = VesselManager.RULER.equals(definition.type()) ? 0xFF3FC6FF : 0xFFB965FF;
		SystemNotifications.showTitleUnder(player, color, 120,
				Component.literal("VESSEL SELECTED").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
				Component.literal(definition.commandDisplay()).withStyle(
						VesselManager.RULER.equals(definition.type()) ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE,
						ChatFormatting.BOLD));
	}

	public static void requestSelectionScreen(ServerPlayer player) {
		if (player == null)
			return;
		if (isSelectionPending(player) && isSelectionAuthorized(player))
			sendSelectionState(player);
	}

	/** Opens the same server-authorized choice used after the advancement trial. */
	public static void openSelectionFromCommand(ServerPlayer player) {
		if (player == null)
			return;
		player.getPersistentData().putBoolean(SELECTION_AUTHORIZED, true);
		player.getPersistentData().putBoolean(COMMAND_SELECTION_AUTHORIZED, true);
		int required = requiredPoints(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.jobtimer = STATE_SELECTION;
			capability.jobadvpoint = required;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.syncPlayerVariables(player);
		});
		sendSelectionState(player);
	}

	public static void finish(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!contains(capability.unlocked_quests, QUEST_ID))
				capability.unlocked_quests = append(capability.unlocked_quests);
			if (!contains(capability.finished_quests, QUEST_ID))
				capability.finished_quests = append(capability.finished_quests);
			capability.jobtimer = STATE_IDLE;
			capability.JobChange_timer = 0;
			capability.instancecomplete = true;
			capability.jobkey = false;
			capability.syncPlayerVariables(entity);
		});
		entity.setNoGravity(false);
		entity.getPersistentData().putBoolean("slr_job_change_dungeon", false);
		entity.getPersistentData().remove(SELECTION_AUTHORIZED);
		entity.getPersistentData().remove(COMMAND_SELECTION_AUTHORIZED);
	}

	public static void unlockIfEligible(LevelAccessor world, Entity entity, int requiredLevel) {
		if (entity == null || world == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (vars.JOB > 0 && state(vars) == STATE_IDLE) {
			finish(entity);
			return;
		}
		if (vars.Player && vars.Level >= requiredLevel && vars.JOB == 0 && !contains(vars.finished_quests, QUEST_ID) && !contains(vars.unlocked_quests, QUEST_ID))
			unlock(entity);
	}

	public static boolean hasAdvancementPlayerNear(Entity portal, double range) {
		if (portal == null || portal.level().isClientSide())
			return false;
		double rangeSqr = range * range;
		for (ServerPlayer player : portal.level().getServer().getPlayerList().getPlayers()) {
			if (player.level() == portal.level() && isAdvancementActive(player) && player.distanceToSqr(portal) <= rangeSqr)
				return true;
		}
		return false;
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			recover(player, true);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			recover(player, true);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0)
			return;
		recover(player, player.tickCount % 40 == 0);
	}

	private static void enterSelection(ServerPlayer player) {
		if (player == null || !isAdvancementActive(player)
				|| advancementPoints(player) < requiredPoints(player))
			return;
		player.getPersistentData().putBoolean(SELECTION_AUTHORIZED, true);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.jobtimer = STATE_SELECTION;
			capability.JobChange_timer = 0;
			capability.instancecomplete = false;
			capability.syncPlayerVariables(player);
		});
		SystemNotifications.showTitleUnder(player, ACCENT, 90,
				Component.literal("ADVANCEMENT COMPLETE").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal("Choose the power that will become your vessel.").withStyle(ChatFormatting.LIGHT_PURPLE));
		sendSelectionState(player);
	}

	private static void recover(ServerPlayer player, boolean refreshScreen) {
		SololevelingModVariables.PlayerVariables data = vars(player);
		int currentState = state(data);

		// Migrate worlds that were saved during the old 100-second survival timer.
		if (data.JOB == 0 && data.jobtimer > 0) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_ADVANCEMENT;
				capability.jobadvpoint = Math.max(0, capability.jobadvpoint - 1);
				capability.syncPlayerVariables(player);
			});
			currentState = STATE_ADVANCEMENT;
		}

		// The old flow could already be inside the automatic Shadow poem.
		if (data.JOB == 0 && data.JobChange_timer > 0) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_SELECTION;
				capability.JobChange_timer = 0;
				capability.instancecomplete = false;
				capability.syncPlayerVariables(player);
			});
			currentState = STATE_SELECTION;
		}

		if (data.JOB == 0 && currentState == STATE_SELECTION && !isSelectionAuthorized(player)) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.jobtimer = STATE_IDLE;
				capability.jobadvpoint = 0;
				capability.JobChange_timer = 0;
				capability.syncPlayerVariables(player);
			});
			player.getPersistentData().remove(SELECTION_AUTHORIZED);
			closeSelection(player);
			return;
		}

		if (currentState == STATE_SELECTION && data.JOB > 0 && !isCommandSelectionAuthorized(player)) {
			VesselDefinition definition = VesselManager.currentDefinition(player);
			if (definition != null && "ashborn".equals(definition.identity())) {
				player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.jobtimer = STATE_SHADOW_PRESENTATION;
					capability.JobChange_timer = Math.max(1, capability.JobChange_timer);
					capability.syncPlayerVariables(player);
				});
			} else {
				finish(player);
			}
			closeSelection(player);
			return;
		}

		if (currentState == STATE_SELECTION && refreshScreen) {
			sendSelectionState(player);
		} else if (data.JOB > 0 && currentState == STATE_IDLE && !contains(data.finished_quests, QUEST_ID)) {
			finish(player);
		}
	}

	private static List<ServerPlayer> questParticipants(ServerPlayer anchor, boolean requireAdvancement) {
		List<ServerPlayer> result = new ArrayList<>();
		if (anchor == null)
			return result;
		String party = vars(anchor).party;
		for (ServerPlayer candidate : anchor.serverLevel().players()) {
			boolean samePlayer = candidate.getUUID().equals(anchor.getUUID());
			boolean sameParty = party != null && !party.isBlank() && party.equals(vars(candidate).party);
			if ((!samePlayer && !sameParty) || candidate.distanceToSqr(anchor) > PARTY_RANGE_SQR)
				continue;
			if (requireAdvancement && !isAdvancementActive(candidate))
				continue;
			if (!requireAdvancement && (vars(candidate).JOB > 0 || isFinished(candidate)))
				continue;
			result.add(candidate);
		}
		if (result.isEmpty() && (!requireAdvancement || isAdvancementActive(anchor)))
			result.add(anchor);
		return result;
	}

	private static void selectionError(ServerPlayer player, String message) {
		SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D5A, 80,
				Component.literal("VESSEL UNAVAILABLE").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal(message).withStyle(ChatFormatting.GRAY));
		if (isSelectionPending(player) && isSelectionAuthorized(player))
			sendSelectionState(player);
		else
			closeSelection(player);
	}

	private static void sendSelectionState(ServerPlayer player) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new VesselSelectionStateMessage(true, advancementPoints(player), requiredPoints(player),
						VesselManager.vesselLimit(player), VesselManager.claimCounts(player)));
	}

	private static void closeSelection(ServerPlayer player) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new VesselSelectionStateMessage(false, 0, requiredPoints(player), VesselManager.vesselLimit(player), new int[0]));
	}

	private static boolean isSelectionAuthorized(Entity entity) {
		return entity != null && (entity.getPersistentData().getBoolean(SELECTION_AUTHORIZED)
				|| advancementPoints(entity) >= requiredPoints(entity));
	}

	private static boolean isCommandSelectionAuthorized(Entity entity) {
		return entity != null && entity.getPersistentData().getBoolean(COMMAND_SELECTION_AUTHORIZED);
	}

	private static int state(SololevelingModVariables.PlayerVariables vars) {
		return (int) Math.round(vars.jobtimer);
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static boolean contains(String list, String id) {
		return list != null && list.contains(id + ",");
	}

	private static String append(String list) {
		if (list == null || list.equals("\"\"") || list.isBlank())
			return TOKEN;
		return list + TOKEN;
	}
}
