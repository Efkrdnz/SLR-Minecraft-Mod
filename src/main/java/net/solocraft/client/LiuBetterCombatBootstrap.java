package net.solocraft.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Unable to register Liu Zhigang Better Combat hooks", exception);
			}
		});
	}
}
