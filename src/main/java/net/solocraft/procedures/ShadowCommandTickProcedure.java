package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber
public class ShadowCommandTickProcedure {
	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide() || event.level.getGameTime() % 10 != 0 || !(event.level instanceof ServerLevel level)
				|| level.players().isEmpty())
			return;
		Set<Integer> processed = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			AABB nearby = player.getBoundingBox().inflate(256.0D);
			for (Entity entity : level.getEntities(player, nearby, ShadowMonarchManager::isTrackedShadowEntity)) {
				if (processed.add(entity.getId()))
					ShadowMonarchManager.tickCommandedShadow(entity);
			}
		}
	}
}
