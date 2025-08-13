
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.SMonTextProcedure;
import net.solocraft.procedures.DoesHaveShadowManifestationProcedure;
import net.solocraft.procedures.DoesHaveExchangeProcedure;
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
public class ShadowSoldierCounterOverlay {
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
		if (SMonTextProcedure.execute(entity)) {
			if (DoesHaveExchangeProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newshadowimgknight.png"), w - 24, h - 70, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newshadowsummon.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newshadowdismiss.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);

			if (DoesHaveShadowManifestationProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newshadowarmor.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);
			}
			if (DoesHaveShadowManifestationProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 89, 0, 0, 12, 12, 12, 12);
			}
			if (DoesHaveExchangeProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 66, 0, 0, 12, 12, 12, 12);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 43, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 20, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability1ReturnProcedure.execute(), w - 28, h - 18, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability2ReturnProcedure.execute(), w - 28, h - 41, -1, false);
			if (DoesHaveExchangeProcedure.execute(entity))
				event.getGuiGraphics().drawString(Minecraft.getInstance().font,

						Ability3ReturnProcedure.execute(), w - 28, h - 64, -1, false);
			if (DoesHaveShadowManifestationProcedure.execute(entity))
				event.getGuiGraphics().drawString(Minecraft.getInstance().font,

						Ability4ReturnProcedure.execute(), w - 27, h - 87, -1, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
