package net.solocraft;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemClientConfig;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = "sololeveling", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DisableLevelBar {
	@SubscribeEvent
	public static void RenderHealthBar(RenderGuiOverlayEvent.Pre event) {
		Entity entity = Minecraft.getInstance().player;
		if (entity == null)
			return;
			
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).CustomHUD
				&& SystemClientConfig.isLegacyOverlayEnabled()) {
			if (VanillaGuiOverlay.EXPERIENCE_BAR.type() == event.getOverlay()) {
				event.setCanceled(true);
			}
		}
	}
}
