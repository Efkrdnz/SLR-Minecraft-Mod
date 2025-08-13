
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
import net.solocraft.procedures.LevelBarProcedure;
import net.solocraft.procedures.IfInSurvivalProcedure;
import net.solocraft.procedures.HungerBarProcedure;
import net.solocraft.procedures.HealthTextProcedure;
import net.solocraft.procedures.Health90Procedure;
import net.solocraft.procedures.Health80Procedure;
import net.solocraft.procedures.Health70Procedure;
import net.solocraft.procedures.Health60Procedure;
import net.solocraft.procedures.Health50Procedure;
import net.solocraft.procedures.Health40Procedure;
import net.solocraft.procedures.Health30Procedure;
import net.solocraft.procedures.Health20Procedure;
import net.solocraft.procedures.Health10Procedure;
import net.solocraft.procedures.Health100Procedure;
import net.solocraft.procedures.Health0Procedure;
import net.solocraft.procedures.FatigueTextProcedure;
import net.solocraft.procedures.ArmorBarProcedure;

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
public class MPOverlayOverlay {
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
		if (IfInSurvivalProcedure.execute(entity)) {
			if (Mana0Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/bar1.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana10Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana10.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana20Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana20.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana30Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana30.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana40Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana40.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana50Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana50.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana60Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana60.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana70Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana70.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana80Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana80.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana90Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana90.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Mana100Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barmana100.png"), w / 2 + -85, h - 48, 0, 0, 90, 10, 90, 10);
			}
			if (Health0Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/bar1.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health10Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth10.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health20Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth20.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health30Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth30.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health40Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth40.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health50Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth50.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health60Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth60.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health70Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth70.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health80Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth80.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health90Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth90.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			if (Health100Procedure.execute(entity)) {
				event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/barhealth100.png"), w / 2 + -85, h - 35, 0, 0, 90, 10, 90, 10);
			}
			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/armorbar.png"), w / 2 + 6, h - 36, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/hungerbar.png"), w / 2 + 6, h - 49, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/levelbar.png"), w / 2 + 44, h - 49, 0, 0, 12, 12, 12, 12);

			event.getGuiGraphics().blit(new ResourceLocation("sololeveling:textures/screens/fatiguebar.png"), w / 2 + 42, h - 38, 0, 0, 16, 16, 16, 16);

			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					ManaTextProcedure.execute(entity), w / 2 + -76, h - 47, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					HealthTextProcedure.execute(entity), w / 2 + -76, h - 34, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					ArmorBarProcedure.execute(entity), w / 2 + 19, h - 35, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					HungerBarProcedure.execute(entity), w / 2 + 19, h - 47, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					LevelBarProcedure.execute(entity), w / 2 + 55, h - 47, -1, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font,

					FatigueTextProcedure.execute(entity), w / 2 + 55, h - 34, -1, false);
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
