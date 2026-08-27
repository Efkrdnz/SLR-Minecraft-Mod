package net.solocraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LiuBetterCombatBootstrap {
	private LiuBetterCombatBootstrap() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		if (!ModList.get().isLoaded("bettercombat"))
			return;
		event.enqueueWork(() -> {
			try {
				Class.forName("net.solocraft.client.compat.bettercombat.LiuBetterCombatCompat")
						.getMethod("register").invoke(null);
				Class.forName("net.solocraft.client.compat.bettercombat.SungIlHwanBetterCombatCompat")
						.getMethod("register").invoke(null);
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Unable to register vessel Better Combat hooks", exception);
			}
		});
	}
}
