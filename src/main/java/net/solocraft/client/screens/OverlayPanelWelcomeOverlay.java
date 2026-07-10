
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.OverlayPanelWelcomeConditionProcedure;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class OverlayPanelWelcomeOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		if (legacyWelcomeOverlayDisabled())
			return;
		int w = event.getWindow().getGuiScaledWidth();
		int h = event.getWindow().getGuiScaledHeight();
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
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, (float) (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).overlay_alpha_welcome);
		if (OverlayPanelWelcomeConditionProcedure.execute(entity)) {
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/notice.png"), w / 2 + -8, h / 2 + -68, 0, 0, 16, 16, 16, 16);
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_empty_small.png"), w / 2 + -96, h / 2 + -103, 0, 0, 200, 160, 200, 160);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.overlay_panel_welcome.label_sslssfwelcome_ssaplayer"), w / 2 + -36, h / 2 + -33, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.overlay_panel_welcome.label_sslnotice"), w / 2 + -16, h / 2 + -49, -13210, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, (float) (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).overlay_alpha_welcome);
	}

	private static boolean legacyWelcomeOverlayDisabled() {
		return true;
	}
}
