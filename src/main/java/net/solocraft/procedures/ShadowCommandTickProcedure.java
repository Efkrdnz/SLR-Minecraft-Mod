package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber
public class ShadowCommandTickProcedure {
	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (false || event.getLevel().isClientSide() || event.getLevel().getGameTime() % 10 != 0 || !(event.getLevel() instanceof ServerLevel level)
				|| level.players().isEmpty())
			return;
		for (ServerPlayer player : level.players())
			ShadowMonarchManager.tickCommandedShadows(player);
	}
}
