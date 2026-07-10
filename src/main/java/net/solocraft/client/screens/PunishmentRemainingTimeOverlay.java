package net.solocraft.client.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.solocraft.procedures.IsInPunishmentZoneProcedure;
import net.solocraft.procedures.SurviveTextProcedure;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class PunishmentRemainingTimeOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.options.hideGui || mc.screen != null)
			return;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, 1);

		if (IsInPunishmentZoneProcedure.execute(player)) {
			renderTimer(event.getGuiGraphics(), mc.font, event.getWindow().getGuiScaledWidth(), player);
		}

		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static void renderTimer(GuiGraphics graphics, Font font, int screenW, Player player) {
		String text = SurviveTextProcedure.execute(player);
		int width = Math.max(150, font.width(text) + 28);
		int x = (screenW - width) / 2;
		int y = 16;
		drawPanel(graphics, x, y, width, 32);
		graphics.drawString(font, Component.literal("PENALTY ZONE"), x + 8, y + 6, 0xFFFF5151, false);
		graphics.drawString(font, Component.literal(text), x + 8, y + 18, 0xFFFFD7D7, false);
	}

	private static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, 0xD20A0710);
		graphics.fill(x, y, x + w, y + 1, 0xFFFF3D3D);
		graphics.fill(x, y + h - 1, x + w, y + h, 0xFF7A1F2A);
		graphics.fill(x, y, x + 1, y + h, 0xFF7A1F2A);
		graphics.fill(x + w - 1, y, x + w, y + h, 0xFFFF3D3D);
		graphics.fill(x + 3, y + 3, x + 14, y + 4, 0xAAFF3D3D);
		graphics.fill(x + 3, y + 3, x + 4, y + 14, 0xAAFF3D3D);
	}
}
