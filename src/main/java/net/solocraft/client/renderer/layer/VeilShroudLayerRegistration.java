package net.solocraft.client.renderer.layer;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VeilShroudLayerRegistration {
	private VeilShroudLayerRegistration() {
	}

	@SubscribeEvent
	public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
		for (var skin : event.getSkins()) {
			PlayerRenderer renderer = event.getSkin(skin);
			if (renderer != null)
				renderer.addLayer(new VeilShroudLayer(renderer));
		}
	}
}
