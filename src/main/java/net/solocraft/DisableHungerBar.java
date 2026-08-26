package net.solocraft;

import net.solocraft.network.SololevelingModVariables;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = "sololeveling", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class DisableHungerBar {
	@SubscribeEvent
	public static void RenderHealthBar(RenderGuiLayerEvent.Pre event) {
		Minecraft minecraft = Minecraft.getInstance();
		Entity entity = minecraft.player;
		if (entity == null)
			return;
			
		if (!minecraft.getDebugOverlay().showDebugScreen()
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).CustomHUD) {
			if (VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) {
				event.setCanceled(true);
			}
		}
	}
}
