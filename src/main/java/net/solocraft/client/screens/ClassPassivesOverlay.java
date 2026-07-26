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
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ClassPassiveClientState;
import net.solocraft.util.RangerClientState;
import net.solocraft.util.StormClientState;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClassPassivesOverlay {
	private static final int PANEL_X = 8;
	private static final int NORMAL_PANEL_Y = 76;
	private static final int CREATIVE_PANEL_Y = 8;
	private static final int PANEL_W = 154;
	private static final int PANEL_H = 27;
	private static final int PANEL_GAP = 4;
	private static final String[] RANGER_STAGE_LABEL_KEYS = {
			"gui.sololeveling.ranger.quiver.stage.manifested",
			"gui.sololeveling.ranger.quiver.stage.linear",
			"gui.sololeveling.ranger.quiver.stage.seeking"
	};
	private static final int[] RANGER_STAGE_ACTIVE_COLORS = {
			0xFF69E892, 0xFF62DFFF, 0xFFFFD369
	};

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.screen != null)
			return;
		SololevelingModVariables.PlayerVariables vars = mc.player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		int cls = (int) Math.round(vars.Classes);
		boolean rangerCombatOnly = cls == 6 && shouldRenderRangerCombatHud(mc);
		if (!vars.CustomHUD && !rangerCombatOnly && !StormClientState.hasAccess)
			return;
		GameType gameType = gameType(mc);
		if (gameType == GameType.SPECTATOR)
			return;

		if (cls == 0 && !StormClientState.hasAccess)
			return;
		int panelY = gameType == GameType.CREATIVE ? CREATIVE_PANEL_Y : NORMAL_PANEL_Y;

		GuiGraphics graphics = event.getGuiGraphics();
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(0, 0, 210);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		if (vars.CustomHUD) {
			switch (cls) {
				case 1 -> renderSegmentPassive(graphics, mc.font, panelY, "Tempo", ClassPassiveClientState.assassinTempo, 5, 0xFF17121D, 0xFF9E55FF, 0xFFDCC2FF);
				case 3 -> renderBarPassive(graphics, mc.font, panelY, "Battle", ClassPassiveClientState.fighterPower, 100.0D, 0xFF24170D, 0xFFFF812E, 0xFFFFD14A);
				case 4 -> renderSegmentPassive(graphics, mc.font, panelY, "Guard", ClassPassiveClientState.tankWallStacks, 10, 0xFF101924, 0xFF3092FF, 0xFFA5D8FF);
				case 5 -> renderSegmentPassive(graphics, mc.font, panelY, "Resonance", ClassPassiveClientState.healerResonance, 5, 0xFF0D1D14, 0xFF37E384, 0xFFA2FFD0);
				case 6 -> renderRangerPassive(graphics, mc, panelY, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
			}
			if (StormClientState.hasAccess) {
				boolean nativeStormMage = cls == 2
						&& "storm".equalsIgnoreCase(vars.mageSpecialization);
				int stormY = nativeStormMage || !hasBaseClassPanel(cls)
						? panelY : panelY + PANEL_H + PANEL_GAP;
				renderStormPassive(graphics, mc.font, stormY);
			}
		} else {
			if (rangerCombatOnly)
				renderRangerCombatHud(graphics, mc.font,
						event.getWindow().getGuiScaledWidth() * 0.5F,
						event.getWindow().getGuiScaledHeight() * 0.5F);
			if (StormClientState.hasAccess)
				renderStormPassive(graphics, mc.font, panelY);
		}

		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		pose.popPose();
	}

	private static void renderBarPassive(GuiGraphics graphics, Font font, int panelY, String title, double value, double max, int track, int fill, int hot) {
		double ratio = clamp(value / Math.max(1.0D, max), 0.0D, 1.0D);
		drawPanel(graphics, PANEL_X, panelY, PANEL_W, PANEL_H, 0xC9080D14, fill);
		graphics.drawString(font, Component.literal(title), PANEL_X + 7, panelY + 4, hot, false);
		String valueText = value >= max ? "Ready" : Math.round(value) + "%";
		graphics.drawString(font, Component.literal(valueText), PANEL_X + PANEL_W - 7 - font.width(valueText), panelY + 4, 0xFFD9E2E8, false);

		int barX = PANEL_X + 7;
		int barY = panelY + 18;
		int barW = PANEL_W - 14;
		int filled = (int) Math.round(barW * ratio);
		int barH = 4;
		graphics.fill(barX, barY, barX + barW, barY + barH, track);
		if (filled > 0) {
			graphics.fill(barX, barY, barX + filled, barY + barH, value >= max ? hot : fill);
			graphics.fill(barX, barY, barX + filled, barY + 1, 0xAAFFFFFF);
		}
	}

	private static void renderSegmentPassive(GuiGraphics graphics, Font font, int panelY, String title, int filled, int max, int track, int fill, int hot) {
		drawPanel(graphics, PANEL_X, panelY, PANEL_W, PANEL_H, 0xC9080D14, fill);
		graphics.drawString(font, Component.literal(title), PANEL_X + 7, panelY + 4, hot, false);
		String valueText = Math.max(0, Math.min(max, filled)) + "/" + max;
		graphics.drawString(font, Component.literal(valueText), PANEL_X + PANEL_W - 7 - font.width(valueText), panelY + 4, 0xFFD9E2E8, false);

		int sx = PANEL_X + 7;
		int sy = panelY + 18;
		int gap = 2;
		int segW = (PANEL_W - 14 - gap * (max - 1)) / max;
		for (int i = 0; i < max; i++) {
			int x = sx + i * (segW + gap);
			boolean active = i < filled;
			graphics.fill(x, sy, x + segW, sy + 4, active ? fill : track);
			if (active)
				graphics.fill(x, sy, x + segW, sy + 1, hot);
		}
	}

	private static void renderRangerPassive(GuiGraphics graphics, Minecraft mc, int panelY, int screenW, int screenH) {
		double focus = ClassPassiveClientState.rangerFocus;
		String focusTitle = Component.translatable("gui.sololeveling.ranger.focus.title").getString();
		renderBarPassive(graphics, mc.font, panelY, focusTitle, focus, 100.0D,
				0xFF201A06, 0xFFFFB020, 0xFFFFE48A);

		float cx = screenW * 0.5F;
		float cy = screenH * 0.5F;
		renderRangerCombatHud(graphics, mc.font, cx, cy);
	}

	private static boolean hasBaseClassPanel(int cls) {
		return cls == 1 || cls == 3 || cls == 4 || cls == 5 || cls == 6;
	}

	private static void renderStormPassive(GuiGraphics graphics, Font font, int panelY) {
		int stage = Math.max(1, Math.min(6, StormClientState.effectiveStage));
		int voltage = Math.max(0, Math.min(100, StormClientState.voltage));
		int accent = StormClientState.spiritualizationBonus
				? 0xFFFFE8A3 : StormClientState.overcharged ? 0xFFFFFFFF : 0xFF54DFFF;
		int fill = StormClientState.overcharged ? 0xFFFFF4C7 : 0xFF3BBFEA;
		int track = 0xFF071B2B;

		drawPanel(graphics, PANEL_X, panelY, PANEL_W, PANEL_H, 0xCC071018, accent);
		String title = "Storm";
		String valueText = "Stage " + stage + "  " + voltage + "%";
		graphics.drawString(font, Component.literal(title), PANEL_X + 7, panelY + 4,
				0xFF85ECFF, false);
		graphics.drawString(font, Component.literal(valueText),
				PANEL_X + PANEL_W - 7 - font.width(valueText), panelY + 4, accent, false);

		int barX = PANEL_X + 7;
		int barY = panelY + 18;
		int barW = PANEL_W - 14;
		int barH = 4;
		int filled = Math.round(barW * voltage / 100.0F);
		graphics.fill(barX, barY, barX + barW, barY + barH, track);
		if (filled > 0) {
			graphics.fill(barX, barY, barX + filled, barY + barH, fill);
			graphics.fill(barX, barY, barX + filled, barY + 1,
					StormClientState.spiritualizationBonus
							? 0xFFFFFFFF : 0xAAE7FAFF);
		}
	}

	private static void renderRangerCombatHud(GuiGraphics graphics, Font font, float cx, float cy) {
		boolean quiverActive = RangerClientState.quiverActive
				&& Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.isUsingItem()
				&& Minecraft.getInstance().player.getUseItem().getItem() instanceof BowItem;
		int fivefoldCharges = Math.max(0, Math.min(5, RangerClientState.fivefoldCharges));
		if (!quiverActive && fivefoldCharges <= 0)
			return;

		if (quiverActive) {
			renderManaQuiverStages(graphics, font, Math.round(cx), Math.round(cy) + 27);
			renderRangerLock(graphics, font, cx, cy);
		}
		if (fivefoldCharges > 0)
			renderFivefoldCharges(graphics, font, Math.round(cx),
					Math.round(cy) + (quiverActive ? 46 : 29), fivefoldCharges);
	}

	private static boolean shouldRenderRangerCombatHud(Minecraft minecraft) {
		boolean drawingManaBow = RangerClientState.quiverActive
				&& minecraft.player != null
				&& minecraft.player.isUsingItem()
				&& minecraft.player.getUseItem().getItem() instanceof BowItem;
		return drawingManaBow || RangerClientState.fivefoldCharges > 0;
	}

	private static void renderManaQuiverStages(GuiGraphics graphics, Font font, int centerX, int y) {
		int stageWidth = 48;
		int gap = 3;
		int totalWidth = stageWidth * 3 + gap * 2;
		int startX = centerX - totalWidth / 2;
		int chargedStage = Math.max(0, Math.min(3, RangerClientState.chargeStage));
		int maximumStage = Math.max(1, Math.min(3, RangerClientState.maximumStage));

		for (int index = 0; index < 3; index++) {
			int stage = index + 1;
			int x = startX + index * (stageWidth + gap);
			boolean available = stage <= maximumStage;
			boolean charged = available && stage <= chargedStage;
			int background = available ? 0xB51A2530 : 0x9A0A0D12;
			int accent = charged ? RANGER_STAGE_ACTIVE_COLORS[index]
					: available ? 0xFF5A7180 : 0xFF262C33;
			int textColor = charged ? RANGER_STAGE_ACTIVE_COLORS[index]
					: available ? 0xFF9EB3BE : 0xFF4A5158;

			graphics.fill(x, y, x + stageWidth, y + 13, background);
			graphics.fill(x, y, x + stageWidth, y + 2, accent);
			graphics.fill(x, y + 12, x + stageWidth, y + 13, 0xAA000000);
			graphics.fill(x, y, x + 1, y + 13, accent);
			graphics.fill(x + stageWidth - 1, y, x + stageWidth, y + 13, accent);

			Component label = Component.translatable(RANGER_STAGE_LABEL_KEYS[index]);
			graphics.drawString(font, label, x + (stageWidth - font.width(label)) / 2,
					y + 4, textColor, false);
		}
	}

	private static void renderRangerLock(GuiGraphics graphics, Font font, float cx, float cy) {
		float progress = Math.max(0.0F, Math.min(1.0F, RangerClientState.lockProgress));
		boolean locked = RangerClientState.locked;
		if (!locked && progress <= 0.001F)
			return;

		PoseStack pose = graphics.pose();
		float radius = 13.0F;
		renderArcSegment(pose, cx, cy, radius, 0.0F, 360.0F, 2.8F,
				0.04F, 0.12F, 0.17F, 0.72F);
		renderArcSegment(pose, cx, cy, radius, 0.0F, 360.0F * (locked ? 1.0F : progress),
				2.0F, locked ? 0.36F : 0.25F, locked ? 1.0F : 0.82F,
				locked ? 0.48F : 1.0F, 0.96F);

		Component lockText = locked
				? Component.translatable("gui.sololeveling.ranger.lock.locked")
				: Component.translatable("gui.sololeveling.ranger.lock.progress",
						Math.round(progress * 100.0F));
		int x = Math.round(cx) - font.width(lockText) / 2;
		graphics.drawString(font, lockText, x, Math.round(cy) - 34,
				locked ? 0xFF75FFA0 : 0xFF77E8FF, false);
	}

	private static void renderFivefoldCharges(GuiGraphics graphics, Font font, int centerX,
			int y, int charges) {
		Component label = Component.translatable("gui.sololeveling.ranger.fivefold", charges);
		graphics.drawString(font, label, centerX - font.width(label) / 2, y,
				0xFFFFD77A, false);

		int pipWidth = 8;
		int gap = 2;
		int totalWidth = pipWidth * 5 + gap * 4;
		int startX = centerX - totalWidth / 2;
		int pipY = y + font.lineHeight + 1;
		for (int index = 0; index < 5; index++) {
			int x = startX + index * (pipWidth + gap);
			boolean active = index < charges;
			graphics.fill(x, pipY, x + pipWidth, pipY + 4,
					active ? 0xFFFFB83D : 0x9A332510);
			if (active)
				graphics.fill(x, pipY, x + pipWidth, pipY + 1, 0xFFFFF0B2);
		}
	}

	private static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, int bg, int accent) {
		graphics.fill(x, y, x + w, y + h, bg);
		graphics.fill(x, y, x + 2, y + h, accent);
		graphics.fill(x + 2, y + h - 1, x + w, y + h, 0x552E3D48);
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
