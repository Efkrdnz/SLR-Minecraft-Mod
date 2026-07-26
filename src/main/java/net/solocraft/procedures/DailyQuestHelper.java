package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.DkcQuestManager;
import net.solocraft.util.RewardManager;
import net.solocraft.util.SystemNotifications;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber
public class DailyQuestHelper {
	public static final double NORMAL_TRAINING_TARGET = 25;
	public static final double SECRET_TRAINING_TARGET = 50;
	public static final double NORMAL_RUN_TARGET = 500;
	public static final double SECRET_RUN_TARGET = 1000;
	private static final String SECRET_SKILL_POINTS_REWARD = "SP20";
	private static final String SECRET_DKC_KEY_REWARD = "ITEM:sololeveling:redkey";

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			recoverQueuedSecretRewards(player);
	}

	public static boolean isSecretQuest(Entity entity) {
		if (entity == null)
			return false;
		return vars(entity).dailysecrettrans >= 2;
	}

	public static boolean isSecretQuestRevealed(Entity entity) {
		if (entity == null)
			return false;
		return vars(entity).dailysecrettrans >= 3;
	}

	public static double trainingTarget(Entity entity) {
		return isSecretQuest(entity) ? SECRET_TRAINING_TARGET : NORMAL_TRAINING_TARGET;
	}

	public static double runTarget(Entity entity) {
		return isSecretQuest(entity) ? SECRET_RUN_TARGET : NORMAL_RUN_TARGET;
	}

	public static double visibleTrainingTarget(Entity entity) {
		return NORMAL_TRAINING_TARGET;
	}

	public static double visibleRunTarget(Entity entity) {
		return NORMAL_RUN_TARGET;
	}

	public static void activateSecretQuestIfEligible(Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (vars.Level >= 30 && vars.dailysecrettrans != 0) {
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.dailysecrettrans = 2;
				capability.syncPlayerVariables(entity);
			});
		}
	}

	public static void keepSecretQuestPending(Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (vars.Level >= 30 && vars.dailysecrettrans != 0) {
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

	private static void recoverQueuedSecretRewards(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = vars(player);
		if (vars.dailysecrettrans != 0 || vars.dkc_started || !DkcQuestManager.isUnlocked(player))
			return;

		java.util.List<String> pending = RewardManager.allRewards(player);
		if (!pending.contains(SECRET_DKC_KEY_REWARD))
			return;

		boolean recoveredSkillPoints = claimPendingReward(player, SECRET_SKILL_POINTS_REWARD);
		boolean recoveredKey = claimPendingReward(player, SECRET_DKC_KEY_REWARD);
		if (!recoveredKey)
			return;

		String detail = recoveredSkillPoints
				? "Demon King's Castle Key and 20 Skill Points delivered."
				: "Demon King's Castle Key delivered.";
		player.displayClientMessage(Component.literal(detail).withStyle(ChatFormatting.LIGHT_PURPLE), false);
		SystemNotifications.showTitleUnder(player, 0xFFFF3D8D, 100,
				Component.literal("SECRET REWARDS RECOVERED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
				Component.literal(detail).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	private static boolean claimPendingReward(Entity entity, String reward) {
		java.util.List<String> pending = RewardManager.allRewards(entity);
		int slot = pending.indexOf(reward);
		return slot >= 0 && RewardManager.claimReward(entity, slot + 1);
	}

	public static void checkSecretTransition(Entity entity, double previousValue, double newValue, double normalTarget) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (vars.dailysecrettrans == 2 && previousValue <= normalTarget && newValue > normalTarget) {
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
			capability.situp = 0;
			capability.squat = 0;
			capability.pushup = 0;
			capability.RUN = 0;
			capability.traintype = "";
			capability.isdailytraining = false;
			capability.syncPlayerVariables(entity);
		});
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
