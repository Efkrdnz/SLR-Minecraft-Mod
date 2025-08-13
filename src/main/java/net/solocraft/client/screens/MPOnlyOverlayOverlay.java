
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.ManaTextProcedure;
import net.solocraft.procedures.Mana90Procedure;
import net.solocraft.procedures.Mana80Procedure;
import net.solocraft.procedures.Mana70Procedure;
import net.solocraft.procedures.Mana60Procedure;
import net.solocraft.procedures.Mana50Procedure;
import net.solocraft.procedures.Mana40Procedure;
import net.solocraft.procedures.Mana30Procedure;
import net.solocraft.procedures.Mana20Procedure;
import net.solocraft.procedures.Mana10Procedure;
import net.solocraft.procedures.Mana100Procedure;
import net.solocraft.procedures.Mana0Procedure;
import net.solocraft.procedures.MPOnlyOverlayDisplayOverlayIngameProcedure;

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
public class MPOnlyOverlayOverlay {
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
		if (MPOnlyOverlayDisplayOverlayIngameProcedure.execute(entity)) {
			if (Mana0Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/bar1.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana10Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana10.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana20Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana20.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana30Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana30.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana40Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana40.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana50Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana50.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana60Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana60.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana70Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana70.png"), 7, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana80Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana80.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana90Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana90.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			if (Mana100Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana100.png"), 6, 6, 0, 0, 90, 10, 90, 10);
			}
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					ManaTextProcedure.execute(entity), 16, 7, -1, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
