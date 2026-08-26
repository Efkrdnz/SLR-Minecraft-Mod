package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.DkcQuestManager;
import net.solocraft.util.SystemNotifications;
import net.solocraft.util.daily.DailyQuestObjectiveManager;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber
public class DailyQuestHelper {
	public static final double NORMAL_MINING_TARGET = DailyQuestObjectiveManager.NORMAL_MINING_TARGET;
	public static final double SECRET_MINING_TARGET = DailyQuestObjectiveManager.SECRET_MINING_TARGET;
	public static final double NORMAL_THREAT_TARGET = DailyQuestObjectiveManager.NORMAL_THREAT_TARGET;
	public static final double SECRET_THREAT_TARGET = DailyQuestObjectiveManager.SECRET_THREAT_TARGET;
	public static final double NORMAL_RUN_TARGET = DailyQuestObjectiveManager.NORMAL_DISTANCE_TARGET;
	public static final double SECRET_RUN_TARGET = DailyQuestObjectiveManager.SECRET_DISTANCE_TARGET;

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			clearSecretQuestIfDkcUnlocked(player);
	}

	public static boolean isSecretQuest(Entity entity) {
		if (entity == null)
			return false;
		return !DkcQuestManager.isUnlocked(entity)
				&& vars(entity).dailysecrettrans >= 2;
	}

	public static boolean isSecretQuestRevealed(Entity entity) {
		if (entity == null)
			return false;
		return !DkcQuestManager.isUnlocked(entity)
				&& vars(entity).dailysecrettrans >= 3;
	}

	public static boolean canActivateSecretQuest(Entity entity) {
		if (entity == null || DkcQuestManager.isUnlocked(entity))
			return false;
		SololevelingModVariables.PlayerVariables variables = vars(entity);
		return variables.Level >= 30 && variables.dailysecrettrans != 0;
	}

	public static boolean clearSecretQuestIfDkcUnlocked(Entity entity) {
		if (entity == null || !DkcQuestManager.isUnlocked(entity)
				|| vars(entity).dailysecrettrans == 0)
			return false;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.dailysecrettrans = 0;
					capability.syncPlayerVariables(entity);
				});
		return true;
	}

	public static void keepSecretQuestPending(Entity entity) {
		if (entity == null)
			return;
		if (DkcQuestManager.isUnlocked(entity)) {
			clearSecretQuestIfDkcUnlocked(entity);
			return;
		}
		if (canActivateSecretQuest(entity)) {
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.dailysecrettrans = 2;
				capability.syncPlayerVariables(entity);
			});
		}
	}

	public static void completeSecretQuest(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dailysecrettrans = 0;
			capability.syncPlayerVariables(entity);
		});
	}

	public static void checkSecretTransition(Entity entity, double previousValue, double newValue, double normalTarget) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (!DkcQuestManager.isUnlocked(entity)
				&& vars.dailysecrettrans == 2
				&& previousValue <= normalTarget && newValue > normalTarget) {
			sendSecretTransitionMessage(entity);
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.dailysecrettrans = 3;
				capability.syncPlayerVariables(entity);
			});
		}
	}

	public static void resetDailyProgress(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.ActiveDaily = false;
			capability.dailytimer = 0;
			capability.dailyMinedBlocks = 0;
			capability.dailyThreatPoints = 0;
			capability.dailyCombatWaived = false;
			capability.situp = 0;
			capability.squat = 0;
			capability.pushup = 0;
			capability.RUN = 0;
			capability.traintype = "";
			capability.isdailytraining = false;
			capability.syncPlayerVariables(entity);
		});
		if (entity instanceof ServerPlayer serverPlayer)
			DailyQuestObjectiveManager.resetQuestRuntime(serverPlayer);
	}

	public static void sendQuestFailedChat(Entity entity) {
		if (entity instanceof Player player && !player.level().isClientSide()) {
			player.displayClientMessage(Component.literal("Daily Quest failed. You can try again tomorrow.").withStyle(ChatFormatting.RED), false);
			if (player instanceof ServerPlayer serverPlayer) {
				SystemNotifications.showNegativeTitleUnder(serverPlayer, 0xFFFF3D3D, 80,
						Component.literal("DAILY QUEST FAILED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
						Component.literal("You can try again tomorrow.").withStyle(ChatFormatting.RED));
			}
		}
	}

	private static void sendSecretTransitionMessage(Entity entity) {
		if (entity instanceof Player player && !player.level().isClientSide()) {
			player.displayClientMessage(Component.literal("")
					.append(Component.literal("Daily Quest").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
					.append(Component.literal(" turned into ").withStyle(ChatFormatting.RED))
					.append(Component.literal("Secret Quest").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
					.append(Component.literal(".").withStyle(ChatFormatting.RED)), false);
			if (player instanceof ServerPlayer serverPlayer) {
				SystemNotifications.showTitleUnder(serverPlayer, 0xFFFF3D3D, 100,
						Component.literal("SECRET QUEST").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
						Component.literal("Daily Quest has changed.\nComplete the hidden requirements.").withStyle(ChatFormatting.RED));
			}
		}
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}
}
