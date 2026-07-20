package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.util.SystemNotifications;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DailySelectorProcedure {
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
		if (entity == null || !SystemPlayerAccess.hasSystem(entity))
			return;
		double rand = 0;
		if (!world.isClientSide()) {
			if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_DAILY_QUEST)) {
				if (world.dayTime() % 48000 == 6000) {
					rand = Mth.nextInt(RandomSource.create(), 1, 1);
					{
						boolean _setval = true;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ActiveDaily = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						double _setval = 0;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.overlay_alpha_dailyquestwarning = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						double _setval = 0;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.pushup = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						double _setval = 0;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.situp = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						double _setval = 0;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.squat = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					{
						double _setval = 0;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.RUN = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					DailyQuestHelper.activateSecretQuestIfEligible(entity);
					if (entity instanceof ServerPlayer player) {
						SystemNotifications.showTitleUnder(player, SystemNotifications.ACCENT, 100,
								Component.literal("DAILY QUEST").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
								Component.literal("Training goals assigned.\nComplete them before the timer ends.").withStyle(ChatFormatting.GRAY));
					}
					{
						double _setval = 12000;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.dailytimer = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
			}
		}
	}
}
