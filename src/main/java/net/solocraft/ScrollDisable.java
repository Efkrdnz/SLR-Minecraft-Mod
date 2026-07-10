package net.solocraft;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = "sololeveling", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScrollDisable {
	@SubscribeEvent
	public static void Scroll(InputEvent.MouseScrollingEvent event) {
		// Intentionally allow mouse-wheel input in combat mode.
	}
}
