package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DailytaskTimerProcedure {
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
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailytimer > 0) {
			{
				double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailytimer - 1;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.dailytimer = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailytimer == 6000) {
				if (entity instanceof ServerPlayer player && !player.level().isClientSide()) {
					SystemNotifications.showTitleUnder(player, 0xFFFFB83D, 80,
							Component.literal("5 MINUTES LEFT").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
							Component.literal("Daily quest timer is running out.").withStyle(ChatFormatting.GOLD));
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailytimer == 1200) {
				if (entity instanceof ServerPlayer player && !player.level().isClientSide()) {
					SystemNotifications.showTitleUnder(player, 0xFFFF3D3D, 80,
							Component.literal("1 MINUTE LEFT").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
							Component.literal("Finish the daily quest now.").withStyle(ChatFormatting.RED));
				}
			}
		}
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailytimer == 0
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ActiveDaily == true) {
			if (!((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:survival_dimension"))))) {
				boolean keepSecret = DailyQuestHelper.isSecretQuest(entity)
						|| ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 30
								&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailysecrettrans != 0);
				DailyQuestHelper.sendQuestFailedChat(entity);
				DailyQuestHelper.resetDailyProgress(entity);
				if (keepSecret)
					DailyQuestHelper.keepSecretQuestPending(entity);
			}
		}
	}
}
