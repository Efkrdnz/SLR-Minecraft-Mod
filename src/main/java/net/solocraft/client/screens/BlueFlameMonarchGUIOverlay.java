
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.ShadowSoldDisplay1Procedure;
import net.solocraft.procedures.ReturnJobCD4Procedure;
import net.solocraft.procedures.ReturnJobCD3Procedure;
import net.solocraft.procedures.ReturnJobCD2Procedure;
import net.solocraft.procedures.ReturnJobCD1Procedure;
import net.solocraft.procedures.IsCD4OnCooldownProcedure;
import net.solocraft.procedures.IsCD3OnCooldownProcedure;
import net.solocraft.procedures.IsCD2OnCooldownProcedure;
import net.solocraft.procedures.IsCD1OnCooldownProcedure;
import net.solocraft.procedures.BlueFlameMonarchGUIDisplayOverlayIngameProcedure;
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
public class BlueFlameMonarchGUIOverlay {
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
		if (BlueFlameMonarchGUIDisplayOverlayIngameProcedure.execute(entity)) {
			if (ShadowSoldDisplay1Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbaranstormburst.png"), w - 24, h - 70, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbaranlaser.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbaranlightningstrike.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbaransummon.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);

			if (IsCD4OnCooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 93, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 89, 0, 0, 12, 12, 12, 12);

			if (IsCD3OnCooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 70, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 66, 0, 0, 12, 12, 12, 12);

			if (IsCD2OnCooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 47, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 43, 0, 0, 12, 12, 12, 12);

			if (IsCD1OnCooldownProcedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/newbasiccdcover.png"), w - 24, h - 24, 0, 0, 20, 20, 20, 20);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/keyplaceholder.png"), w - 31, h - 20, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability1ReturnProcedure.execute(), w - 28, h - 18, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability2ReturnProcedure.execute(), w - 28, h - 41, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability3ReturnProcedure.execute(), w - 28, h - 64, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					Ability4ReturnProcedure.execute(), w - 27, h - 87, -1, false);
			if (IsCD4OnCooldownProcedure.execute(entity))
				event.getGuiGraphics().drawString(Minecraft.getInstance().font,

						ReturnJobCD4Procedure.execute(entity), w - 17, h - 87, -1, false);
			if (IsCD3OnCooldownProcedure.execute(entity))
				event.getGuiGraphics().drawString(Minecraft.getInstance().font,

						ReturnJobCD3Procedure.execute(entity), w - 17, h - 64, -1, false);
			if (IsCD2OnCooldownProcedure.execute(entity))
				event.getGuiGraphics().drawString(Minecraft.getInstance().font,

						ReturnJobCD2Procedure.execute(entity), w - 17, h - 41, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					ReturnJobCD1Procedure.execute(entity), w - 17, h - 18, -1, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
