package net.solocraft.client.screens;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.SungIlHwanVfxClientState;
import net.solocraft.client.renderer.SungIlHwanVfxClientState.OverlayFrame;
import net.solocraft.client.renderer.SungIlHwanVfxClientState.OverlayState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Owner-only Spatial Execution targeting HUD plus bounded exhaustion/risk
 * feedback. It uses GuiGraphics exclusively and does not mutate global GL state.
 */
@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.GAME,
		value = Dist.CLIENT
)
public final class SungIlHwanVfxOverlay {
	private static final int VOID = 0xD707090D;
	private static final int VOID_SOFT = 0x720B0E13;
	private static final int SILVER = 0xE8F3CE66;
	private static final int SILVER_DIM = 0xA0B87A16;
	private static final int GOLD = 0xF0FFF1AC;

	private SungIlHwanVfxOverlay() {
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void render(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null
				|| minecraft.options.hideGui)
			return;
		OverlayState state = SungIlHwanVfxClientState.overlay(
				event.getPartialTick().getGameTimeDeltaPartialTick(false));
		if (!state.active())
			return;

		GuiGraphics graphics = event.getGuiGraphics();
		int width = event.getGuiGraphics().guiWidth();
		int height = event.getGuiGraphics().guiHeight();
		if (state.targeting().active())
			renderTargeting(graphics, minecraft, state.targeting(),
					width, height);
		if (state.exhaustion().active())
			renderExhaustion(graphics, minecraft, state.exhaustion(),
					width, height);
		if (state.risk().active())
			renderRisk(graphics, minecraft, state.risk(), width, height);
	}

	private static void renderTargeting(GuiGraphics graphics,
			Minecraft minecraft, OverlayFrame frame, int width,
			int height) {
		int centerX = width / 2;
		int centerY = height / 2;
		float pulse = 1.0F + 0.035F * Mth.sin(
				(minecraft.level.getGameTime() + frame.progress() * 4.0F)
						* 0.54F);
		int radius = Math.max(28, Math.min(62,
				Math.round((34.0F + frame.radius() * 2.4F) * pulse)));

		// Quiet black/gold framing keeps the expanding area readable over bright
		// worlds without implying that the sphere center is an individual target.
		drawCorner(graphics, centerX - radius, centerY - radius,
				1, 1, 13, VOID, SILVER);
		drawCorner(graphics, centerX + radius, centerY - radius,
				-1, 1, 13, VOID, SILVER);
		drawCorner(graphics, centerX - radius, centerY + radius,
				1, -1, 13, VOID, SILVER);
		drawCorner(graphics, centerX + radius, centerY + radius,
				-1, -1, 13, VOID, SILVER);

		int segments = 40;
		float charge = Math.max(frame.progress(), frame.intensity());
		int charged = Mth.clamp(Math.round(charge * segments),
				0, segments);
		for (int index = 0; index < segments; index++) {
			double angle = index * Math.PI * 2.0D / segments
					- Math.PI * 0.5D;
			int x = centerX + Mth.floor(Math.cos(angle) * radius);
			int y = centerY + Mth.floor(Math.sin(angle) * radius);
			int color = index <= charged ? GOLD : SILVER_DIM;
			graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
		}

		// Sphere latitude/longitude hints and the actual lock mark.
		drawDashedHorizontal(graphics, centerX, centerY,
				Math.round(radius * 0.82F), SILVER_DIM);
		drawDashedVertical(graphics, centerX, centerY,
				Math.round(radius * 0.82F), SILVER_DIM);
		graphics.fill(centerX - 12, centerY - 1, centerX - 3,
				centerY + 1, GOLD);
		graphics.fill(centerX + 3, centerY - 1, centerX + 12,
				centerY + 1, GOLD);
		graphics.fill(centerX - 1, centerY - 12, centerX + 1,
				centerY - 3, GOLD);
		graphics.fill(centerX - 1, centerY + 3, centerX + 1,
				centerY + 12, GOLD);
		graphics.fill(centerX - 2, centerY - 2, centerX + 2,
				centerY + 2, 0xF4FFF0BC);

		String distanceText = String.format(java.util.Locale.ROOT,
				"AREA %.1fm", Math.min(99.9D, frame.radius()));
		int textWidth = minecraft.font.width(distanceText);
		int labelY = centerY + radius + 8;
		graphics.fill(centerX - textWidth / 2 - 4, labelY - 2,
				centerX + textWidth / 2 + 4, labelY + 10, VOID);
		graphics.drawString(minecraft.font,
				Component.literal(distanceText),
				centerX - textWidth / 2, labelY, 0xE7D7A5, false);
	}

	private static void renderExhaustion(GuiGraphics graphics,
			Minecraft minecraft, OverlayFrame frame, int width,
			int height) {
		float remaining = 1.0F - frame.progress();
		float strength = frame.intensity() * remaining;
		int edgeAlpha = Mth.clamp(Math.round(72.0F * strength),
				0, 72);
		if (edgeAlpha > 0) {
			int edge = edgeAlpha << 24 | 0x07090D;
			int thickness = 7 + Math.round(strength * 11.0F);
			graphics.fill(0, 0, width, thickness, edge);
			graphics.fill(0, height - thickness, width, height, edge);
			graphics.fill(0, thickness, thickness, height - thickness,
					edge);
			graphics.fill(width - thickness, thickness, width,
					height - thickness, edge);
		}

		int barWidth = 74;
		int barX = width / 2 - barWidth / 2;
		int barY = height - 29;
		int fill = Math.round((barWidth - 4) * strength);
		graphics.fill(barX - 2, barY - 2, barX + barWidth + 2,
				barY + 7, VOID_SOFT);
		graphics.fill(barX, barY, barX + barWidth, barY + 5,
				0xD90A0D12);
		if (fill > 0)
			graphics.fill(barX + 2, barY + 1, barX + 2 + fill,
					barY + 4, strength > 0.72F ? GOLD : SILVER_DIM);
	}

	private static void renderRisk(GuiGraphics graphics,
			Minecraft minecraft, OverlayFrame frame, int width,
			int height) {
		float envelope = Mth.sin(Mth.clamp(frame.progress(),
				0.0F, 1.0F) * Mth.PI);
		float intensity = frame.intensity() * envelope;
		int centerX = width / 2;
		int centerY = height / 2;
		int spread = 72 + Math.round((1.0F - envelope) * 20.0F);
		int alpha = Mth.clamp(Math.round(210.0F * intensity), 0, 210);
		int color = alpha << 24 | 0xE7D7A5;

		for (int side = -1; side <= 1; side += 2) {
			int x = centerX + side * spread;
			graphics.fill(x - 1, centerY - 18, x + 1,
					centerY + 18, color);
			graphics.fill(x - side * 8, centerY - 18,
					x + side, centerY - 16, color);
			graphics.fill(x - side * 8, centerY + 16,
					x + side, centerY + 18, color);
		}
		if (frame.intensity() > 0.74F) {
			int flash = Mth.clamp(Math.round(34.0F * intensity), 0, 34);
			graphics.fill(0, 0, width, height,
					flash << 24 | 0xD8C99B);
		}
	}

	private static void drawCorner(GuiGraphics graphics, int x, int y,
			int horizontalDirection, int verticalDirection, int length,
			int backing, int accent) {
		int endX = x + horizontalDirection * length;
		int endY = y + verticalDirection * length;
		graphics.fill(Math.min(x, endX) - 2, y - 2,
				Math.max(x, endX) + 2, y + 2, backing);
		graphics.fill(x - 2, Math.min(y, endY) - 2, x + 2,
				Math.max(y, endY) + 2, backing);
		graphics.fill(Math.min(x, endX), y - 1,
				Math.max(x, endX) + 1, y + 1, accent);
		graphics.fill(x - 1, Math.min(y, endY), x + 1,
				Math.max(y, endY) + 1, accent);
	}

	private static void drawDashedHorizontal(GuiGraphics graphics,
			int centerX, int centerY, int radius, int color) {
		for (int x = -radius; x <= radius; x += 7)
			graphics.fill(centerX + x, centerY,
					centerX + Math.min(radius + 1, x + 3),
					centerY + 1, color);
	}

	private static void drawDashedVertical(GuiGraphics graphics,
			int centerX, int centerY, int radius, int color) {
		for (int y = -radius; y <= radius; y += 7)
			graphics.fill(centerX, centerY + y, centerX + 1,
					centerY + Math.min(radius + 1, y + 3), color);
	}
}
