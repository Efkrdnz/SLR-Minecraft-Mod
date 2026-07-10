package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

@Mod.EventBusSubscriber
public class ShadowCommandTickProcedure {
	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || event.level.getGameTime() % 10 != 0 || !(event.level instanceof ServerLevel level))
			return;
		for (Entity entity : level.getAllEntities()) {
			if (ShadowMonarchManager.isTrackedShadowEntity(entity))
				ShadowMonarchManager.tickCommandedShadow(entity);
		}
	}
}
