
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

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
public class NewDailyQuestOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
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
		float alpha = (float) (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).overlay_alpha_dailyquestwarning;
		if (alpha <= 0.0F)
			return;
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, alpha);
		{
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/panel_rework_compressed.png"), w / 2 + -161, h / 2 + -76, 0, 0, 319, 155, 319, 155);
			event.getGuiGraphics().blit(ResourceLocation.parse("sololeveling:textures/screens/notice.png"), w / 2 + -8, h / 2 + -47, 0, 0, 16, 16, 16, 16);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.new_daily_quest.label_sslnew_daily_quest"), w / 2 + -58, h / 2 + -29, -26368, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.new_daily_quest.label_sslif_you_fail_to_complete_this_q"), w / 2 + -99, h / 2 + 14, -65536, false);
			event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.sololeveling.new_daily_quest.label_you_will_be_given_a_penalty"), w / 2 + -79, h / 2 + 31, -65536, false);
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
}
