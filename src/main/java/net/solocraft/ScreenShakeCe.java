package net.solocraft;

import net.solocraft.init.SololevelingModMobEffects;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ScreenShakeCe {
	public ScreenShakeCe() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new ScreenShakeCe();
	}

	@EventBusSubscriber
	private static class ForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void clientLoad(FMLClientSetupEvent event) {
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void CameraShake(ViewportEvent.ComputeCameraAngles event) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				if (player.hasEffect(SololevelingModMobEffects.SCREEN_SHAKE)) {
					if (Math.random() < 0.5) {
						{
							event.setPitch((float) (event.getPitch() + (Math.random() * 2)));
							event.setRoll((float) (event.getRoll() + (Math.random() * 2)));
							event.setYaw((float) (event.getYaw() + (Math.random() * 2)));
						}
					} else {
						{
							event.setPitch((float) (event.getPitch() - (Math.random() * 2)));
							event.setRoll((float) (event.getRoll() - (Math.random() * 2)));
							event.setYaw((float) (event.getYaw() - (Math.random() * 2)));
						}
					}
				}
			}
		}
	}
}
