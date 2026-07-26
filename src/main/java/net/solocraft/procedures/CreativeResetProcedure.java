package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class CreativeResetProcedure {
	private static final double CREATIVE_MANA = 1000000.0D;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player);
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (!isCreative(entity))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.Fatigue != 0.0D || capability.Mana != CREATIVE_MANA
					|| capability.MP != CREATIVE_MANA) {
				capability.Fatigue = 0.0D;
				capability.Mana = CREATIVE_MANA;
				capability.MP = CREATIVE_MANA;
				capability.syncPlayerVariables(entity);
			}
		});
	}

	private static boolean isCreative(Entity entity) {
		if (entity instanceof ServerPlayer serverPlayer)
			return serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
		if (!entity.level().isClientSide() || !(entity instanceof Player player))
			return false;
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null)
			return false;
		var playerInfo = connection.getPlayerInfo(player.getGameProfile().getId());
		return playerInfo != null && playerInfo.getGameMode() == GameType.CREATIVE;
	}
}
