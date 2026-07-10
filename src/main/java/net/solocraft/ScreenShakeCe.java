package net.solocraft;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScreenShakeCe {
	public ScreenShakeCe() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new ScreenShakeCe();
	}

	@Mod.EventBusSubscriber
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
				if (player.hasEffect(SololevelingModMobEffects.SCREEN_SHAKE.get())) {
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
