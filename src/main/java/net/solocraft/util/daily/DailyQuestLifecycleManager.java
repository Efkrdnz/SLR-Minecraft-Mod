package net.solocraft.util.daily;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.DailyQuestHelper;
import net.solocraft.util.DkcQuestManager;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.RewardManager;
import net.solocraft.util.SystemNotifications;
import net.solocraft.util.SystemPlayerAccess;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Owns assignment, migration, timing, failure and rewards for the Daily Quest.
 * Objective progress itself lives in {@link DailyQuestObjectiveManager}.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DailyQuestLifecycleManager {
	public static final int SAVE_SCHEMA = 2;
	public static final double QUEST_DURATION_TICKS = 20.0D * 60.0D * 20.0D;

	private static final double FIVE_MINUTES_TICKS = 5.0D * 60.0D * 20.0D;
	private static final double ONE_MINUTE_TICKS = 60.0D * 20.0D;
	private static final String DAILY_FULL_RECOVERY_REWARD = "FR";
	private static final String DAILY_SKILL_POINTS_REWARD =
			"SP" + net.solocraft.util.SkillPointRules.DAILY_QUEST;
	private static final String DAILY_ITEM_REWARD = "ITEMBOX";
	private static final String SECRET_SKILL_POINTS_REWARD =
			"SP" + net.solocraft.util.SkillPointRules.SECRET_DAILY_QUEST;
	private static final String SECRET_DKC_KEY_REWARD = "ITEM:sololeveling:redkey";
	private static final ResourceKey<Level> SURVIVAL_DIMENSION = ResourceKey.create(
			Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "survival_dimension"));

	private DailyQuestLifecycleManager() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false || !(event.getEntity() instanceof ServerPlayer player))
			return;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null)
			return;

		DailyQuestHelper.clearSecretQuestIfDkcUnlocked(player);
		long minecraftDay = minecraftDay(player);
		boolean migratedActiveQuest = migrateIfNeeded(player, variables, minecraftDay);
		if (!SystemPlayerAccess.hasSystem(player))
			return;
		if (migratedActiveQuest)
			return;

		if (variables.ActiveDaily) {
			tickActiveQuest(player, variables);
			return;
		}

		if (dailyQuestsEnabled(player) && variables.lastDailyQuestDay != minecraftDay)
			startQuest(player, variables, minecraftDay);
	}

	@SubscribeEvent
	public static void onObjectivesCompleted(DailyQuestObjectivesCompletedEvent event) {
		completeQuest(event.getPlayer());
	}

	public static void startQuestNow(ServerPlayer player) {
		if (player == null)
			return;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables != null)
			startQuest(player, variables, minecraftDay(player));
	}

	/**
	 * Used by admin/debug commands. Completion still travels through the same
	 * one-shot event and reward path as real gameplay.
	 */
	public static boolean finishQuestNow(ServerPlayer player) {
		if (player == null)
			return false;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || !variables.ActiveDaily)
			return false;
		variables.dailyMinedBlocks = DailyQuestObjectiveManager.miningTarget(player);
		variables.dailyThreatPoints = DailyQuestObjectiveManager.threatTarget(player);
		variables.RUN = DailyQuestObjectiveManager.distanceTarget(player);
		variables.syncPlayerVariables(player);
		return DailyQuestObjectiveManager.evaluateCompletion(player);
	}

	public static void resetQuestState(ServerPlayer player,
			boolean allowAssignmentToday) {
		if (player == null)
			return;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null)
			return;
		variables.dailyQuestSchema = SAVE_SCHEMA;
		variables.ActiveDaily = false;
		variables.dailytimer = 0.0D;
		variables.dailyMinedBlocks = 0.0D;
		variables.dailyThreatPoints = 0.0D;
		variables.RUN = 0.0D;
		variables.dailyCombatWaived = false;
		variables.lastDailyQuestDay = allowAssignmentToday
				? Long.MIN_VALUE
				: minecraftDay(player);
		clearLegacyWorkoutState(variables);
		variables.syncPlayerVariables(player);
		DailyQuestObjectiveManager.resetQuestRuntime(player);
	}

	private static boolean migrateIfNeeded(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables, long minecraftDay) {
		if (variables.dailyQuestSchema >= SAVE_SCHEMA)
			return false;

		boolean hadActiveLegacyQuest = variables.ActiveDaily;
		variables.dailyQuestSchema = SAVE_SCHEMA;
		variables.dailyMinedBlocks = 0.0D;
		variables.dailyThreatPoints = 0.0D;
		variables.RUN = 0.0D;
		clearLegacyWorkoutState(variables);
		variables.dailyCombatWaived = player.level().getDifficulty() == Difficulty.PEACEFUL;

		if (hadActiveLegacyQuest) {
			variables.ActiveDaily = true;
			variables.dailytimer = QUEST_DURATION_TICKS;
			variables.lastDailyQuestDay = minecraftDay;
			activateSecretQuestIfEligible(player, variables);
			variables.syncPlayerVariables(player);
			DailyQuestObjectiveManager.resetQuestRuntime(player);
			announceAssignment(player, true, variables.dailyCombatWaived);
			return true;
		}

		variables.ActiveDaily = false;
		variables.dailytimer = 0.0D;
		// A legacy save has no reliable "last assigned day", so allow today's
		// Minecraft-native quest to be assigned immediately.
		variables.lastDailyQuestDay = Long.MIN_VALUE;
		variables.syncPlayerVariables(player);
		DailyQuestObjectiveManager.resetQuestRuntime(player);
		return false;
	}

	private static void startQuest(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables, long minecraftDay) {
		// Guarded here rather than only at the daily tick because startQuestNow
		// reaches this same funnel from the admin command. A released System has
		// nobody left to set the player a task.
		if (variables.systemReleased)
			return;
		variables.dailyQuestSchema = SAVE_SCHEMA;
		variables.ActiveDaily = true;
		variables.dailytimer = QUEST_DURATION_TICKS;
		variables.lastDailyQuestDay = minecraftDay;
		variables.dailyMinedBlocks = 0.0D;
		variables.dailyThreatPoints = 0.0D;
		variables.RUN = 0.0D;
		variables.dailyCombatWaived = player.level().getDifficulty() == Difficulty.PEACEFUL;
		variables.overlay_alpha_dailyquestwarning = 0.0D;
		clearLegacyWorkoutState(variables);
		activateSecretQuestIfEligible(player, variables);
		variables.syncPlayerVariables(player);
		DailyQuestObjectiveManager.resetQuestRuntime(player);
		announceAssignment(player, false, variables.dailyCombatWaived);
	}

	private static void tickActiveQuest(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables) {
		boolean mustSync = false;
		if (!variables.dailyCombatWaived
				&& player.level().getDifficulty() == Difficulty.PEACEFUL) {
			// Once waived for a quest, keep it waived even if difficulty changes
			// again; otherwise a completed objective could become incomplete.
			variables.dailyCombatWaived = true;
			mustSync = true;
		}

		if (variables.dailytimer > 0.0D) {
			double previous = variables.dailytimer;
			variables.dailytimer = Math.max(0.0D, previous - 1.0D);
			if (crossed(previous, variables.dailytimer, FIVE_MINUTES_TICKS)) {
				SystemNotifications.showTitleUnder(player, 0xFFFFB83D, 80,
						Component.literal("5 MINUTES LEFT")
								.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
						Component.literal("Daily quest timer is running out.")
								.withStyle(ChatFormatting.GOLD));
				mustSync = true;
			}
			if (crossed(previous, variables.dailytimer, ONE_MINUTE_TICKS)) {
				SystemNotifications.showTitleUnder(player, 0xFFFF3D3D, 80,
						Component.literal("1 MINUTE LEFT")
								.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
						Component.literal("Finish the daily quest now.")
								.withStyle(ChatFormatting.RED));
				mustSync = true;
			}
		}

		if (variables.dailytimer <= 0.0D) {
			variables.dailytimer = 0.0D;
			if (player.level().dimension() != SURVIVAL_DIMENSION) {
				failQuest(player);
				return;
			}
		}

		if (mustSync || player.level().getGameTime() % 20L == 0L)
			variables.syncPlayerVariables(player);
	}

	private static void failQuest(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || !variables.ActiveDaily)
			return;
		boolean applyPunishment = shouldPunishFailure(player);
		boolean keepSecret = DailyQuestHelper.isSecretQuest(player)
				|| DailyQuestHelper.canActivateSecretQuest(player);
		// A quest can remain active across a Minecraft-day boundary. Record the
		// day it actually ended so the idle tick cannot assign a replacement
		// immediately after failure.
		variables.lastDailyQuestDay = minecraftDay(player);
		DailyQuestHelper.sendQuestFailedChat(player);
		DailyQuestHelper.resetDailyProgress(player);
		if (keepSecret)
			DailyQuestHelper.keepSecretQuestPending(player);
		if (applyPunishment)
			enterPunishmentZone(player);
	}

	private static boolean shouldPunishFailure(ServerPlayer player) {
		return player.level().getGameRules()
				.getBoolean(SololevelingModGameRules.SOLO_PUNISHMENT)
				&& !JobChangeQuestManager.isFinished(player);
	}

	private static boolean enterPunishmentZone(ServerPlayer player) {
		return DailyPunishmentManager.enter(player);
	}

	private static void completeQuest(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null || !variables.ActiveDaily)
			return;

		boolean secretQuest = DailyQuestHelper.isSecretQuest(player);

		// Commit completion before granting anything, making duplicate completion
		// events harmless even if a reward path causes another capability sync.
		variables.ActiveDaily = false;
		variables.dailytimer = 0.0D;
		// Daily quests can span a day boundary. Mark the completion day, rather
		// than retaining only the assignment day, so a new quest waits for the
		// next reset window.
		variables.lastDailyQuestDay = minecraftDay(player);
		clearLegacyWorkoutState(variables);
		variables.syncPlayerVariables(player);
		DailyQuestObjectiveManager.resetQuestRuntime(player);

		// Append fixed rewards without consuming unrelated inbox entries. A Full
		// Recovery remains pending unless one already exists, in which case the
		// overflow recovery is immediately applied by RewardManager.
		RewardManager.appendReward(player, DAILY_FULL_RECOVERY_REWARD);
		RewardManager.appendReward(player, DAILY_SKILL_POINTS_REWARD);
		RewardManager.appendReward(player, DAILY_ITEM_REWARD);
		if (secretQuest) {
			DkcQuestManager.unlock(player);
			DailyQuestHelper.completeSecretQuest(player);
			RewardManager.appendReward(player, DAILY_FULL_RECOVERY_REWARD);
			RewardManager.appendReward(player, SECRET_SKILL_POINTS_REWARD);
			RewardManager.appendReward(player, SECRET_DKC_KEY_REWARD);
		}

		variables.syncPlayerVariables(player);

		if (secretQuest) {
			SystemNotifications.showTitleUnder(player, 0xFFFF3D8D, 120,
					Component.literal("SECRET QUEST COMPLETE")
							.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
					Component.literal("Rewards added to System Rewards.")
							.withStyle(ChatFormatting.LIGHT_PURPLE));
		} else {
			SystemNotifications.showTitleUnder(player, 0xFFFF9A3D, 100,
					Component.literal("DAILY QUEST COMPLETE")
							.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal("Rewards added to System Rewards.")
							.withStyle(ChatFormatting.GRAY));
		}
	}

	private static void activateSecretQuestIfEligible(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables) {
		if (DkcQuestManager.isUnlocked(player)) {
			variables.dailysecrettrans = 0.0D;
			return;
		}
		if (DailyQuestHelper.canActivateSecretQuest(player))
			variables.dailysecrettrans = 2.0D;
	}

	private static void clearLegacyWorkoutState(
			SololevelingModVariables.PlayerVariables variables) {
		// Retain these fields in the save format for one compatibility release,
		// but the workout system no longer reads or advances them.
		variables.pushup = 0.0D;
		variables.situp = 0.0D;
		variables.squat = 0.0D;
		variables.traintype = "";
		variables.isdailytraining = false;
	}

	private static void announceAssignment(ServerPlayer player, boolean migrated,
			boolean combatWaived) {
		String detail = combatWaived
				? "Mine eligible blocks and travel 10 KM.\nThreat Suppression is waived on Peaceful."
				: "Mine, suppress threats, and travel 10 KM.\nComplete all objectives within 20 minutes.";
		SystemNotifications.showTitleUnder(player, SystemNotifications.ACCENT, 120,
				Component.literal(migrated ? "DAILY QUEST UPDATED" : "DAILY QUEST")
						.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal(detail).withStyle(ChatFormatting.GRAY));
	}

	private static boolean crossed(double previous, double current, double threshold) {
		return previous > threshold && current <= threshold;
	}

	private static boolean dailyQuestsEnabled(ServerPlayer player) {
		return player.level().getGameRules()
				.getBoolean(SololevelingModGameRules.SOLO_DAILY_QUEST);
	}

	private static long minecraftDay(ServerPlayer player) {
		// Custom dimensions may expose a different day-time value. Daily resets
		// always use the server Overworld clock so changing dimensions cannot
		// manufacture another assignment.
		return Math.floorDiv(player.server.overworld().getDayTime(), 24000L);
	}

	@Nullable
	private static SololevelingModVariables.PlayerVariables variables(
			@Nullable ServerPlayer player) {
		if (player == null)
			return null;
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
