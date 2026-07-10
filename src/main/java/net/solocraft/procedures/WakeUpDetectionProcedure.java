package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class WakeUpDetectionProcedure {
	@SubscribeEvent
	public static void onEntityEndSleep(PlayerWakeUpEvent event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ActiveDaily) {
			if (new Object() {
				public boolean checkGamemode(Entity _ent) {
					if (_ent instanceof ServerPlayer _serverPlayer) {
						return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
					} else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
						return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
								&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SURVIVAL;
					}
					return false;
				}
			}.checkGamemode(entity)) {
				SololevelingMod.queueServerWork(20, () -> {
					if (world instanceof Level _lvl1 && _lvl1.isDay()) {
						SololevelingMod.queueServerWork(20, () -> {
							boolean keepSecret = DailyQuestHelper.isSecretQuest(entity)
									|| ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 30
											&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailysecrettrans != 0);
							DailyQuestHelper.sendQuestFailedChat(entity);
							DailyQuestHelper.resetDailyProgress(entity);
							if (keepSecret)
								DailyQuestHelper.keepSecretQuestPending(entity);
						});
					}
				});
			} else {
				boolean keepSecret = DailyQuestHelper.isSecretQuest(entity)
						|| ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 30
								&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailysecrettrans != 0);
				DailyQuestHelper.resetDailyProgress(entity);
				if (keepSecret)
					DailyQuestHelper.keepSecretQuestPending(entity);
			}
		}
	}
}
