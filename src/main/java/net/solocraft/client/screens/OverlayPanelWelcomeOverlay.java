
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.OverlayPanelWelcomeConditionProcedure;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@EventBusSubscriber({Dist.CLIENT})
public class OverlayPanelWelcomeOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		if (legacyWelcomeOverlayDisabled())
			return;
		int w = event.getGuiGraphics().guiWidth();
		int h = event.getGuiGraphics().guiHeight();
		Level world = null;
		double x = 0;
		double y = 0;
		double z = 0;
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {
			world = entity.level();
			x = entity.getX();
			y = entity.getY();
			z = entity.getZ();
		}
		// The alpha is read off the player, so there has to be one.
		if (entity == null)
			return;
		float alpha = (float) (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).overlay_alpha_welcome;
		if (alpha <= 0.0F)
			return;
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, alpha);
		if (OverlayPanelWelcomeConditionProcedure.execute(entity)) {
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/notice.png"), w / 2 + -8, h / 2 + -68, 0, 0, 16, 16, 16, 16);
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/panel_rework_empty_small.png"), w / 2 + -96, h / 2 + -103, 0, 0, 200, 160, 200, 160);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.overlay_panel_welcome.label_sslssfwelcome_ssaplayer"), w / 2 + -36, h / 2 + -33, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.overlay_panel_welcome.label_sslnotice"), w / 2 + -16, h / 2 + -49, -13210, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		// Opaque white, not this overlay's alpha. Restoring the alpha it drew
		// with left every later draw at that alpha, so a hidden overlay -- alpha
		// zero -- made the next screen render completely invisible.
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static boolean legacyWelcomeOverlayDisabled() {
		return true;
	}
}
