package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;
import net.solocraft.util.DkcQuestManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.AbstractClientPlayer;

import javax.annotation.Nullable;

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;

@Mod.EventBusSubscriber
public class TrainingOnTickProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		boolean cancel = false;
		String r1 = "";
		String r2 = "";
		String r3 = "";
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ActiveDaily) {
			if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).traintype).equals("")) {
				if (Math.sqrt(Math.pow(entity.getX() - (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).TX, 2)
						+ Math.pow(entity.getY() - (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).TY, 2)
						+ Math.pow(entity.getZ() - (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).TZ, 2)) >= 0.25) {
					if (world.isClientSide()) {
						if (entity instanceof AbstractClientPlayer player) {
							var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation("sololeveling", "player_animation"));
							if (animation != null) {
								animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation("sololeveling", "whatever"))));
							}
						}
					}
					{
						String _setval = "";
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.traintype = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						boolean _setval = false;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.isdailytraining = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
			boolean secretQuest = DailyQuestHelper.isSecretQuest(entity);
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).squat >= DailyQuestHelper.trainingTarget(entity)
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).pushup >= DailyQuestHelper.trainingTarget(entity)
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).situp >= DailyQuestHelper.trainingTarget(entity)
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).RUN >= DailyQuestHelper.runTarget(entity)) {
				{
					boolean _setval = false;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ActiveDaily = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = 0;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.dailytimer = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				if (secretQuest) {
					DkcQuestManager.unlock(entity);
					RewardCollectProcedure.execute(entity, "FR");
					RewardCollectProcedure.execute(entity, "SP20");
					RewardCollectProcedure.execute(entity, "ITEM:sololeveling:redkey");
					DailyQuestHelper.completeSecretQuest(entity);
				} else {
					r1 = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_1;
					r2 = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_2;
					r3 = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_3;
					RewardCollectProcedure.execute(entity, r1);
					RewardCollectProcedure.execute(entity, r2);
					RewardCollectProcedure.execute(entity, r3);
				}
				{
					String _setval = "FR";
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.reward_1 = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "SP10";
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.reward_2 = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					String _setval = "ITEMBOX";
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.reward_3 = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				if (entity instanceof ServerPlayer player && !player.level().isClientSide()) {
					if (secretQuest) {
						SystemNotifications.showTitleUnder(player, 0xFFFF3D8D, 120,
								Component.literal("SECRET QUEST COMPLETE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
								Component.literal("DKC Key\n20 Skill Points\nFull Recovery").withStyle(ChatFormatting.LIGHT_PURPLE));
					} else {
						SystemNotifications.showTitleUnder(player, 0xFFFF9A3D, 100,
								Component.literal("DAILY QUEST COMPLETE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
								Component.literal("Rewards received.\nCheck your inventory and stats.").withStyle(ChatFormatting.GRAY));
					}
				}
				{
					String _setval = "";
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.traintype = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				if (world.isClientSide()) {
					if (entity instanceof AbstractClientPlayer player) {
						var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation("sololeveling", "player_animation"));
						if (animation != null) {
							animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation("sololeveling", "whatever"))));
						}
					}
				}
			}
		}
	}
}
