package net.solocraft.client.gui.system;

import net.solocraft.client.renderer.shader.SystemBackgroundRenderTypes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

import java.util.List;

/**
 * A "System"-styled tooltip: a small framed box whose background is the same
 * animated shader used by {@link SystemScreen} (with a Java fallback), plus a
 * glowing azure border and corner ticks. Rendered on top of everything, so call
 * it at the very end of a screen's {@code render}.
 */
public final class SystemTooltip {
	private static final int ACCENT = 0xFF3FC6FF;
	private static final int ACCENT_DIM = 0xFF2273A8;
	private static final int TEXT_MAIN = 0xFFE8F6FF;

	private SystemTooltip() {
	}

	public static void render(GuiGraphics g, Font font, List<Component> lines, int mouseX, int mouseY, int screenW, int screenH) {
		if (lines == null || lines.isEmpty())
			return;
		int pad = 5;
		int textW = 0;
		for (Component c : lines)
			textW = Math.max(textW, font.width(c));
		int boxW = textW + pad * 2;
		int boxH = lines.size() * 10 + pad * 2 - 2;

		int bx = mouseX + 12;
		int by = mouseY - 12;
		if (bx + boxW > screenW - 2)
			bx = mouseX - boxW - 12;
		if (bx < 2)
			bx = 2;
		if (by + boxH > screenH - 2)
			by = screenH - boxH - 2;
		if (by < 2)
			by = 2;

		// commit anything queued so the raw shader quad lands beneath our border/text
		g.flush();
		drawBackground(g, bx, by, boxW, boxH, mouseX, mouseY);

		// darken slightly to guarantee readable text over the animated background
		g.fill(bx, by, bx + boxW, by + boxH, 0x66000010);
		// border
		g.fill(bx, by, bx + boxW, by + 1, ACCENT_DIM);
		g.fill(bx, by + boxH - 1, bx + boxW, by + boxH, ACCENT_DIM);
		g.fill(bx, by, bx + 1, by + boxH, ACCENT_DIM);
		g.fill(bx + boxW - 1, by, bx + boxW, by + boxH, ACCENT_DIM);
		// corner ticks
		int l = 5;
		g.fill(bx - 1, by - 1, bx + l, by, ACCENT);
		g.fill(bx - 1, by - 1, bx, by + l, ACCENT);
		g.fill(bx + boxW - l, by - 1, bx + boxW + 1, by, ACCENT);
		g.fill(bx + boxW - 1, by - 1, bx + boxW + 1, by + l, ACCENT);
		g.fill(bx - 1, by + boxH, bx + l, by + boxH + 1, ACCENT);
		g.fill(bx - 1, by + boxH - l, bx, by + boxH + 1, ACCENT);
		g.fill(bx + boxW - l, by + boxH, bx + boxW + 1, by + boxH + 1, ACCENT);
		g.fill(bx + boxW - 1, by + boxH - l, bx + boxW + 1, by + boxH + 1, ACCENT);

		// text
		int ty = by + pad;
		for (Component c : lines) {
			g.drawString(font, c, bx + pad, ty, TEXT_MAIN, false);
			ty += 10;
		}
	}

	private static void drawBackground(GuiGraphics g, int bx, int by, int boxW, int boxH, int mouseX, int mouseY) {
		ShaderInstance shader = SystemBackgroundRenderTypes.get();
		if (shader == null) {
			g.fillGradient(bx, by, bx + boxW, by + boxH, 0xF0060D1F, 0xF0010209);
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(SystemBackgroundRenderTypes::get);
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(clamp01((mouseX - bx) / (float) boxW), clamp01((mouseY - by) / (float) boxH));
		shader.safeGetUniform("MouseGlitch").set(0.0f);

		Matrix4f matrix = g.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, bx, by + boxH, 0).uv(0f, 1f).endVertex();
		buffer.vertex(matrix, bx + boxW, by + boxH, 0).uv(1f, 1f).endVertex();
		buffer.vertex(matrix, bx + boxW, by, 0).uv(1f, 0f).endVertex();
		buffer.vertex(matrix, bx, by, 0).uv(0f, 0f).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}
