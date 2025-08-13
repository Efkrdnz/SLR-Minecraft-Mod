
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.FMonTextProcedure;
import net.solocraft.procedures.Ability4ReturnProcedure;
import net.solocraft.procedures.Ability3ReturnProcedure;
import net.solocraft.procedures.Ability2ReturnProcedure;
import net.solocraft.procedures.Ability1ReturnProcedure;

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
public class FrostMonarchGUIOverlay {
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
		if (FMonTextProcedure.execute(entity)) {
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newfrostmonarchspear.png"), w - 24, h - 72, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newfrostmonarchiceball.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newfrostmonarchchunk1.png"), w - 24, h - 48, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newfrostmonarchsnowscreen.png"), w - 24, h - 96, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 92, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 68, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 44, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 20, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability1ReturnProcedure.execute(), w - 28, h - 19, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability2ReturnProcedure.execute(), w - 28, h - 43, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability3ReturnProcedure.execute(), w - 28, h - 67, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability4ReturnProcedure.execute(), w - 28, h - 91, -1, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
