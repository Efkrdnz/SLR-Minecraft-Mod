package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ShadowInventoryDeathDropProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide())
			return;
		if (ShadowMonarchManager.isTrackedShadowEntity(event.getEntity()))
			ShadowMonarchManager.dropStoredShadowInventory(event.getEntity());
	}
}
