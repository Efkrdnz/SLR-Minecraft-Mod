
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.Kamishcharge9Procedure;
import net.solocraft.procedures.Kamishcharge8Procedure;
import net.solocraft.procedures.Kamishcharge7Procedure;
import net.solocraft.procedures.Kamishcharge6Procedure;
import net.solocraft.procedures.Kamishcharge5Procedure;
import net.solocraft.procedures.Kamishcharge4Procedure;
import net.solocraft.procedures.Kamishcharge3Procedure;
import net.solocraft.procedures.Kamishcharge2Procedure;
import net.solocraft.procedures.Kamishcharge1Procedure;
import net.solocraft.procedures.Kamishcharge0Procedure;
import net.solocraft.procedures.CeismicslashbarDisplayOverlayIngameProcedure;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class CeismicslashbarOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
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
		RenderSystem.setShaderColor(1, 1, 1, 1);
		if (CeismicslashbarDisplayOverlayIngameProcedure.execute(entity)) {
			if (Kamishcharge0Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_1.png"), w / 2 + -31, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge1Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_2.png"), w / 2 + -31, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge2Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_3.png"), w / 2 + -31, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge3Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_4.png"), w / 2 + -32, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge4Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_5.png"), w / 2 + -32, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge5Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_6.png"), w / 2 + -33, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge6Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_7.png"), w / 2 + -33, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge7Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_8.png"), w / 2 + -33, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge8Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_9.png"), w / 2 + -33, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
			if (Kamishcharge9Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/progress_10.png"), w / 2 + -33, h / 2 + -115, 0, 0, 63, 8, 63, 8);
			}
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
