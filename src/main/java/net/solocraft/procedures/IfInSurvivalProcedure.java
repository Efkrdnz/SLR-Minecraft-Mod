package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.Minecraft;

public class IfInSurvivalProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		GameType gameType;
		if (entity instanceof ServerPlayer serverPlayer) {
			gameType = serverPlayer.gameMode.getGameModeForPlayer();
		} else if (entity.level().isClientSide() && entity instanceof Player player) {
			var connection = Minecraft.getInstance().getConnection();
			if (connection == null)
				return false;
			var playerInfo = connection.getPlayerInfo(player.getGameProfile().getId());
			if (playerInfo == null)
				return false;
			gameType = playerInfo.getGameMode();
		} else {
			return false;
		}
		if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE)
			return false;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElseGet(SololevelingModVariables.PlayerVariables::new).CustomHUD;
	}
}
