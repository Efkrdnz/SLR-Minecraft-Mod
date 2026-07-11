package net.solocraft.client.renderer.layer;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GoliathArmorGlowLayerRegistration {
	private GoliathArmorGlowLayerRegistration() {
	}

	@SubscribeEvent
	public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
		for (String skin : event.getSkins()) {
			PlayerRenderer renderer = event.getSkin(skin);
			if (renderer != null)
				renderer.addLayer(new GoliathArmorGlowLayer(renderer, event.getEntityModels()));
		}
	}
}
