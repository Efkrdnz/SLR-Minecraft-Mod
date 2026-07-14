package net.solocraft.client.screens;

import net.solocraft.client.gui.system.SystemNotificationManager;
import net.solocraft.client.gui.system.SystemNotificationManager.Notification;
import net.solocraft.client.renderer.shader.IrisCompat;
import net.solocraft.client.renderer.shader.SystemBackgroundRenderTypes;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@link SystemNotificationManager} notifications as real 3D panels in
 * the world (not a flat HUD overlay), anchored at a fixed offset relative to
 * the camera and billboarded to face the player, then tilted around Y so the
 * right edge recedes with genuine perspective. Uses the System shader
 * background (Java fallback) and glitch/expand animation.
 *
 * <p>All placement is tunable: {@link #DIST} forward, {@link #LEFT_AMT} left,
 * {@link #UP_AMT} up (blocks from the eye), {@link #TILT} degrees, and
 * {@link #WORLD_SCALE} (design-pixels → blocks). Always drawn on top (depth off).
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SystemNotificationWorldRenderer {
	private static final int ACCENT = 0xFF3FC6FF;
	private static final int LIGHT = LightTexture.FULL_BRIGHT;

	// ── placement / look tunables ──────────────────────────────────────────────
	private static final float DIST = 1.4f;       // blocks in front of the eye
	private static final float LEFT_AMT = 0.95f;  // blocks to the player's left
	private static final float UP_AMT = 0.02f;    // blocks up from eye level
	private static final float STACK_GAP_WORLD = 0.08f; // clear space between stacked panels (blocks)
	private static final float PANEL_TILT = 16.0f; // small camera-local yaw tilt for depth
	private static final float WORLD_SCALE = 0.008f; // design pixel -> block scale (~20% smaller)
	private static final float TITLE_SCALE = 1.7f;

	// ── layout tunables (design pixels) ────────────────────────────────────────
	private static final int PAD_X = 9;
	private static final int PAD_Y = 6;
	private static final int LINE_GAP = 3;
	private static final int FH = 9;
	private static final int UNDER_MAX_WIDTH = 150;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui)
			return;
		List<Notification> list = SystemNotificationManager.INSTANCE.active();
		if (list.isEmpty())
			return;
		Font font = mc.font;
		Camera cam = event.getCamera();
		long now = System.currentTimeMillis();

		PoseStack ps = event.getPoseStack();
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();

		float notificationScale = net.solocraft.util.SystemClientConfig.getNotificationScale();
		float s = WORLD_SCALE * notificationScale;
		float stackCenter = 0f;
		float previousHalfHeight = 0f;
		float stackGap = STACK_GAP_WORLD * notificationScale;

		for (int idx = 0; idx < list.size(); idx++) {
			Notification n = list.get(list.size() - 1 - idx); // newest lowest
			int[] size = measure(font, n);
			float currentHalfHeight = size[1] * s * 0.5f;
			if (idx > 0)
				stackCenter += previousHalfHeight + stackGap + currentHalfHeight;
			Vec3 offset = cameraScreenOffset(cam, stackCenter);

			ps.pushPose();
			ps.translate(offset.x, offset.y, offset.z);
			ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
			ps.mulPose(Axis.YP.rotationDegrees(PANEL_TILT));
			ps.scale(-s, -s, s);
			renderPanel(ps, font, buffer, n, now, size);
			ps.popPose();

			previousHalfHeight = currentHalfHeight;
		}

		buffer.endBatch();
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	private static Vec3 cameraScreenOffset(Camera cam, float stackOffset) {
		Vec3 forward = Vec3.directionFromRotation(cam.getXRot(), cam.getYRot()).normalize();
		Vec3 left = new Vec3(cam.getLeftVector()).normalize();
		Vec3 up = new Vec3(cam.getUpVector()).normalize();
		return forward.scale(DIST)
				.add(left.scale(LEFT_AMT))
				.add(up.scale(UP_AMT + stackOffset));
	}

	private static void renderPanel(PoseStack ps, Font font, MultiBufferSource buffer, Notification n, long now, int[] size) {
		float reveal = n.reveal(now);
		if (reveal <= 0.02f)
			return;
		boolean trans = n.transitioning(now);
		float jitter = trans ? (float) ((Math.random() - 0.5) * 6.0 * (1f - reveal)) : 0f;

		ps.pushPose();
		ps.translate(jitter, 0f, 0f);
		ps.scale(reveal, 1f, 1f); // horizontal expand/collapse

		int tabW = size[0], tabH = size[1];
		int hw = tabW / 2, hh = tabH / 2;
		Matrix4f m = ps.last().pose();

		// accent frame (2px expanded) + shader background on top -> rim border
		drawRect(m, -hw - 2, -hh - 2, hw + 2, hh + 2, ACCENT);
		drawShaderRect(m, hw, hh);
		if (trans && reveal > 0.06f) {
			for (int s = 0; s < 2; s++) {
				float sy = (float) ((Math.random() - 0.5) * tabH);
				drawRect(m, -hw, sy, hw, sy + 1, 0x99BFE9FF);
			}
		}

		// text
		boolean hasT = n.title != null, hasU = n.under != null;
		if (hasT && hasU) {
			int titleH = Math.round(FH * TITLE_SCALE);
			int underH = size[2];
			float titleCY = -hh + PAD_Y + titleH / 2f;
			float underCY = hh - PAD_Y - underH / 2f;
			drawTitle(ps, font, buffer, n.title, titleCY);
			drawUnder(ps, font, buffer, n.under, underCY);
		} else if (hasT) {
			drawTitle(ps, font, buffer, n.title, 0);
		} else {
			drawUnder(ps, font, buffer, n.under, 0);
		}
		ps.popPose();
	}

	private static void drawTitle(PoseStack ps, Font font, MultiBufferSource buffer, Component title, float cy) {
		ps.pushPose();
		ps.translate(0, cy, 0.1f);
		ps.scale(TITLE_SCALE, TITLE_SCALE, 1f);
		int tw = font.width(title);
		font.drawInBatch(title, -tw / 2f, -4f, 0xFFFFFFFF, false, ps.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, LIGHT);
		ps.popPose();
	}

	private static void drawUnder(PoseStack ps, Font font, MultiBufferSource buffer, Component under, float cy) {
		List<FormattedCharSequence> lines = underLines(font, under);
		if (lines.isEmpty())
			return;
		ps.pushPose();
		ps.translate(0, cy, 0.1f);
		float totalH = underHeight(lines);
		float y = -totalH / 2f;
		for (FormattedCharSequence line : lines) {
			int uw = font.width(line);
			font.drawInBatch(line, -uw / 2f, y, 0xFFE8F6FF, false, ps.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, LIGHT);
			y += FH + LINE_GAP;
		}
		ps.popPose();
	}

	private static int[] measure(Font font, Notification n) {
		boolean hasT = n.title != null, hasU = n.under != null;
		int titleW = hasT ? Math.round(font.width(n.title) * TITLE_SCALE) : 0;
		int titleH = hasT ? Math.round(FH * TITLE_SCALE) : 0;
		List<FormattedCharSequence> underLines = hasU ? underLines(font, n.under) : List.of();
		int underW = 0;
		for (FormattedCharSequence line : underLines)
			underW = Math.max(underW, font.width(line));
		int underH = hasU ? underHeight(underLines) : 0;
		int innerW = Math.max(titleW, underW);
		int innerH = titleH + underH + ((hasT && hasU) ? LINE_GAP : 0);
		int tabW = Math.max(52, innerW + PAD_X * 2);
		int tabH = innerH + PAD_Y * 2;
		return new int[] { tabW, tabH, underH };
	}

	private static List<FormattedCharSequence> underLines(Font font, Component under) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		if (under == null)
			return lines;
		String text = under.getString().replace("\\n", "\n");
		for (String paragraph : text.split("\n", -1)) {
			List<FormattedCharSequence> split = font.split(Component.literal(paragraph.isEmpty() ? " " : paragraph), UNDER_MAX_WIDTH);
			lines.addAll(split);
		}
		return lines;
	}

	private static int underHeight(List<FormattedCharSequence> lines) {
		if (lines.isEmpty())
			return 0;
		return lines.size() * FH + (lines.size() - 1) * LINE_GAP;
	}

	// ── raw quad helpers ───────────────────────────────────────────────────────

	private static void drawShaderRect(Matrix4f m, float hw, float hh) {
		ShaderInstance shader = IrisCompat.isShaderPackInUse()
				? null
				: SystemBackgroundRenderTypes.get();
		if (shader == null) {
			drawRect(m, -hw, -hh, hw, hh, 0xF00A1830);
			return;
		}
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.setShader(SystemBackgroundRenderTypes::get);
		shader.safeGetUniform("MousePos").set(0.5f, 0.35f);
		shader.safeGetUniform("MouseGlitch").set(0.0f);
		BufferBuilder buf = Tesselator.getInstance().getBuilder();
		buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buf.vertex(m, -hw, hh, 0).uv(0f, 1f).endVertex();
		buf.vertex(m, hw, hh, 0).uv(1f, 1f).endVertex();
		buf.vertex(m, hw, -hh, 0).uv(1f, 0f).endVertex();
		buf.vertex(m, -hw, -hh, 0).uv(0f, 0f).endVertex();
		Tesselator.getInstance().end();
	}

	private static void drawRect(Matrix4f m, float x0, float y0, float x1, float y1, int argb) {
		float a = ((argb >>> 24) & 0xFF) / 255f;
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buf = Tesselator.getInstance().getBuilder();
		buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buf.vertex(m, x0, y1, 0).color(r, g, b, a).endVertex();
		buf.vertex(m, x1, y1, 0).color(r, g, b, a).endVertex();
		buf.vertex(m, x1, y0, 0).color(r, g, b, a).endVertex();
		buf.vertex(m, x0, y0, 0).color(r, g, b, a).endVertex();
		Tesselator.getInstance().end();
	}
}
