package net.solocraft.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ClassPassiveClientState;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClassPassivesOverlay {
	private static final int PANEL_X = 8;
	private static final int NORMAL_PANEL_Y = 76;
	private static final int CREATIVE_PANEL_Y = 8;
	private static final int PANEL_W = 176;
	private static final int PANEL_H = 34;

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.screen != null)
			return;
		SololevelingModVariables.PlayerVariables vars = mc.player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (!vars.CustomHUD)
			return;
		GameType gameType = gameType(mc);
		if (gameType == GameType.SPECTATOR)
			return;

		int cls = (int) Math.round(vars.Classes);
		if (cls == 0)
			return;
		int panelY = gameType == GameType.CREATIVE ? CREATIVE_PANEL_Y : NORMAL_PANEL_Y;

		GuiGraphics graphics = event.getGuiGraphics();
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(0, 0, 210);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		switch (cls) {
			case 1 -> renderSegmentPassive(graphics, mc.font, panelY, "SHADOW COMBO", ClassPassiveClientState.assassinComboTier, 10, 0xFF12091E, 0xFF9E55FF, 0xFFD4A5FF, "CHAIN");
			case 3 -> renderBarPassive(graphics, mc.font, panelY, "BATTLE GAUGE", ClassPassiveClientState.fighterPower, 100.0D, 0xFF211006, 0xFFFF812E, 0xFFFFD14A, ClassPassiveClientState.fighterPower >= 100.0D ? "BERSERKER READY" : "POWER");
			case 4 -> renderSegmentPassive(graphics, mc.font, panelY, "IRON WALL", ClassPassiveClientState.tankWallStacks, 10, 0xFF071525, 0xFF3092FF, 0xFFA5D8FF, "-" + Math.min(ClassPassiveClientState.tankWallStacks * 3, 30) + "% DMG");
			case 5 -> renderSegmentPassive(graphics, mc.font, panelY, "RESONANCE", ClassPassiveClientState.healerResonance, 5, 0xFF071D10, 0xFF37E384, 0xFFA2FFD0, ClassPassiveClientState.healerResonance >= 5 ? "BURST READY" : "STACKS");
			case 6 -> renderRangerPassive(graphics, mc, panelY, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
		}

		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		pose.popPose();
	}

	private static void renderBarPassive(GuiGraphics graphics, Font font, int panelY, String title, double value, double max, int track, int fill, int hot, String status) {
		double ratio = clamp(value / Math.max(1.0D, max), 0.0D, 1.0D);
		drawPanel(graphics, PANEL_X, panelY, PANEL_W, PANEL_H, 0xD5060B16, fill);
		graphics.drawString(font, Component.literal(title), PANEL_X + 8, panelY + 5, hot, false);
		graphics.drawString(font, Component.literal(status), PANEL_X + PANEL_W - 8 - font.width(status), panelY + 5, 0xFFEAF8FF, false);

		int barX = PANEL_X + 8;
		int barY = panelY + 19;
		int barW = PANEL_W - 16;
		int filled = (int) Math.round(barW * ratio);
		int barH = 9;
		graphics.fill(barX, barY, barX + barW, barY + barH, track);
		if (filled > 0) {
			graphics.fill(barX, barY, barX + filled, barY + barH, value >= max ? hot : fill);
			graphics.fill(barX, barY, barX + filled, barY + 1, 0xAAFFFFFF);
		}
		String pct = Math.round(value) + "%";
		graphics.drawString(font, Component.literal(pct), barX + (barW - font.width(pct)) / 2, barY + Math.max(0, (barH - font.lineHeight) / 2), 0xFFFFFFFF, false);
	}

	private static void renderSegmentPassive(GuiGraphics graphics, Font font, int panelY, String title, int filled, int max, int track, int fill, int hot, String status) {
		drawPanel(graphics, PANEL_X, panelY, PANEL_W, PANEL_H, 0xD5060B16, fill);
		graphics.drawString(font, Component.literal(title), PANEL_X + 8, panelY + 5, hot, false);
		graphics.drawString(font, Component.literal(status), PANEL_X + PANEL_W - 8 - font.width(status), panelY + 5, 0xFFEAF8FF, false);

		int sx = PANEL_X + 8;
		int sy = panelY + 20;
		int gap = 2;
		int segW = (PANEL_W - 16 - gap * (max - 1)) / max;
		for (int i = 0; i < max; i++) {
			int x = sx + i * (segW + gap);
			boolean active = i < filled;
			graphics.fill(x, sy, x + segW, sy + 7, active ? fill : track);
			if (active)
				graphics.fill(x, sy, x + segW, sy + 1, hot);
			graphics.fill(x + segW - 1, sy, x + segW, sy + 7, 0x66000000);
		}
	}

	private static void renderRangerPassive(GuiGraphics graphics, Minecraft mc, int panelY, int screenW, int screenH) {
		double focus = ClassPassiveClientState.rangerFocus;
		renderBarPassive(graphics, mc.font, panelY, "FOCUS", focus, 100.0D, 0xFF201A06, 0xFFFFB020, 0xFFFFE48A, focus >= 100.0D ? "SHOT PRIMED" : "STILLNESS");
		if (focus < 2.0D)
			return;

		float cx = screenW * 0.5F;
		float cy = screenH * 0.5F;
		float radius = 19.0F;
		float thickness = focus >= 100.0D ? 4.2F : 3.0F;
		PoseStack pose = graphics.pose();
		renderArcSegment(pose, cx, cy, radius, 0.0F, 360.0F, thickness + 1.5F, 0.10F, 0.08F, 0.03F, 0.42F);
		renderArcSegment(pose, cx, cy, radius, 0.0F, (float) (360.0D * clamp(focus / 100.0D, 0.0D, 1.0D)), thickness, 1.0F, focus >= 100.0D ? 0.92F : 0.68F, 0.12F, 0.92F);
		if (focus >= 100.0D) {
			renderArcSegment(pose, cx, cy, radius + 3.0F, 0.0F, 360.0F, 2.0F, 1.0F, 0.84F, 0.22F, 0.34F);
		}
	}

	private static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, int bg, int accent) {
		graphics.fill(x, y, x + w, y + h, bg);
		graphics.fill(x, y, x + w, y + 1, accent);
		graphics.fill(x, y + h - 1, x + w, y + h, 0x99356F91);
		graphics.fill(x, y, x + 1, y + h, 0x99356F91);
		graphics.fill(x + w - 1, y, x + w, y + h, accent);
		graphics.fill(x + 3, y + 3, x + 14, y + 4, 0x9913B8FF);
		graphics.fill(x + 3, y + 3, x + 4, y + 14, 0x9913B8FF);
	}

	private static void renderArcSegment(PoseStack pose, float cx, float cy, float radius, float startDeg, float endDeg, float thickness, float red, float green, float blue, float alpha) {
		float outerR = radius + thickness * 0.5F;
		float innerR = radius - thickness * 0.5F;
		float startRad = (float) Math.toRadians(startDeg - 90.0F);
		float endRad = (float) Math.toRadians(endDeg - 90.0F);
		if (endRad < startRad)
			endRad += (float) (Math.PI * 2.0D);

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f matrix = pose.last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		int segments = 72;
		for (int i = 0; i <= segments; i++) {
			float angle = startRad + (endRad - startRad) * ((float) i / segments);
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			buffer.vertex(matrix, cx + cos * outerR, cy + sin * outerR, 0).color(red, green, blue, alpha).endVertex();
			buffer.vertex(matrix, cx + cos * innerR, cy + sin * innerR, 0).color(red, green, blue, alpha).endVertex();
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static GameType gameType(Minecraft mc) {
		if (mc.player == null || mc.getConnection() == null)
			return GameType.SURVIVAL;
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getGameProfile().getId());
		return info == null ? GameType.SURVIVAL : info.getGameMode();
	}
}
