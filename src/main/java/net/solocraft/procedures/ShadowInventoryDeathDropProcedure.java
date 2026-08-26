package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class ShadowInventoryDeathDropProcedure {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event == null)
			return;
		ShadowMonarchManager.preserveProgressAfterPlayerClone(
				event.getOriginal(), event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide())
			return;
		if (ShadowMonarchManager.isTrackedShadowEntity(event.getEntity())) {
			ShadowMonarchManager.dropStoredShadowInventory(event.getEntity());
			ShadowMonarchManager.handleTrackedShadowDeath(event.getEntity());
		}
	}
}
