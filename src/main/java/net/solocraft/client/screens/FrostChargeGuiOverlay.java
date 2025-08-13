
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.ReturnFrostBaseProcedure;
import net.solocraft.procedures.ReturnFrost5Procedure;
import net.solocraft.procedures.ReturnFrost4Procedure;
import net.solocraft.procedures.ReturnFrost3Procedure;
import net.solocraft.procedures.ReturnFrost2Procedure;
import net.solocraft.procedures.ReturnFrost1Procedure;

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
public class FrostChargeGuiOverlay {
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
		if (true) {
			if (ReturnFrostBaseProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotempty.png"), w / 2 + -10, h / 2 + -90, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrostBaseProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotempty.png"), w / 2 + -4, h / 2 + -82, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrostBaseProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotempty.png"), w / 2 + -16, h / 2 + -82, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrostBaseProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotempty.png"), w / 2 + 2, h / 2 + -90, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrostBaseProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotempty.png"), w / 2 + 8, h / 2 + -82, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrost1Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotuse.png"), w / 2 + -16, h / 2 + -82, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrost2Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotuse.png"), w / 2 + -10, h / 2 + -90, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrost3Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotuse.png"), w / 2 + -4, h / 2 + -82, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrost4Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotuse.png"), w / 2 + 2, h / 2 + -90, 0, 0, 8, 8, 8, 8);
			}
			if (ReturnFrost5Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/frostslotuse.png"), w / 2 + 8, h / 2 + -82, 0, 0, 8, 8, 8, 8);
			}
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
