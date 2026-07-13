package net.solocraft.client.gui;

import net.solocraft.client.renderer.shader.WeaponTooltipRenderTypes;

import net.minecraftforge.client.event.RenderTooltipEvent;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;
import org.joml.Vector2ic;

/** Draws rank-scaled, profile-themed backgrounds under weapon tooltips. */
public final class WeaponTooltipRenderer {
	private WeaponTooltipRenderer() {
	}

	public static void render(RenderTooltipEvent.Pre event, WeaponTooltipProfiles.Profile profile) {
		Font font = event.getFont();
		int tooltipWidth = 0;
		int tooltipHeight = event.getComponents().size() == 1 ? -2 : 0;
		for (ClientTooltipComponent component : event.getComponents()) {
			tooltipWidth = Math.max(tooltipWidth, component.getWidth(font));
			tooltipHeight += component.getHeight();
		}
		if (tooltipWidth <= 0 || tooltipHeight <= 0)
			return;

		Vector2ic position = event.getTooltipPositioner().positionTooltip(event.getScreenWidth(),
				event.getScreenHeight(), event.getX(), event.getY(), tooltipWidth, tooltipHeight);
		int x = position.x() - 6;
		int y = position.y() - 6;
		int width = tooltipWidth + 12;
		int height = tooltipHeight + 12;
		GuiGraphics graphics = event.getGraphics();

		graphics.flush();
		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 400.0F);
		if (!drawShader(graphics, x, y, width, height, profile)) {
			graphics.fillGradient(x, y, x + width, y + height,
					withAlpha(darken(profile.primaryColor(), 0.16F), 0xFA),
					withAlpha(darken(profile.secondaryColor(), 0.08F), 0xFA));
		}

		// Keeps appraisal text crisp while allowing animated energy around and beneath it.
		graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2,
				profile.tier() >= 6 ? 0x51000000 : 0x64000000);
		drawFrame(graphics, x, y, width, height, profile);
		graphics.flush();
		graphics.pose().popPose();
	}

	private static boolean drawShader(GuiGraphics graphics, int x, int y, int width,
			int height, WeaponTooltipProfiles.Profile profile) {
		ShaderInstance shader = WeaponTooltipRenderTypes.get();
		if (shader == null)
			return false;

		set(shader, "RankLevel", profile.tier());
		set(shader, "Theme", profile.theme());
		set(shader, "Seed", profile.seed());
		setColor(shader, "PrimaryColor", profile.primaryColor());
		setColor(shader, "SecondaryColor", profile.secondaryColor());

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(WeaponTooltipRenderTypes::get);

		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, x, y + height, 0.0F).uv(0.0F, 1.0F).endVertex();
		buffer.vertex(matrix, x + width, y + height, 0.0F).uv(1.0F, 1.0F).endVertex();
		buffer.vertex(matrix, x + width, y, 0.0F).uv(1.0F, 0.0F).endVertex();
		buffer.vertex(matrix, x, y, 0.0F).uv(0.0F, 0.0F).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		return true;
	}

	private static void set(ShaderInstance shader, String name, float value) {
		AbstractUniform uniform = shader.safeGetUniform(name);
		uniform.set(value);
	}

	private static void setColor(ShaderInstance shader, String name, int color) {
		AbstractUniform uniform = shader.safeGetUniform(name);
		uniform.set(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
				(color & 255) / 255.0F);
	}

	private static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height,
			WeaponTooltipProfiles.Profile profile) {
		int primary = withAlpha(profile.primaryColor(), 0xFF);
		int secondary = withAlpha(profile.secondaryColor(), 0xFF);
		int dimPrimary = withAlpha(darken(profile.primaryColor(), 0.58F), 0xDD);
		int dimSecondary = withAlpha(darken(profile.secondaryColor(), 0.58F), 0xDD);

		graphics.fill(x, y, x + width, y + 1, dimPrimary);
		graphics.fill(x, y + height - 1, x + width, y + height, dimSecondary);
		graphics.fill(x, y, x + 1, y + height, dimPrimary);
		graphics.fill(x + width - 1, y, x + width, y + height, dimSecondary);

		int corner = Math.min(7 + profile.tier(), Math.max(7, width / 3));
		graphics.fill(x - 1, y - 1, x + corner, y, primary);
		graphics.fill(x - 1, y - 1, x, y + corner, primary);
		graphics.fill(x + width - corner, y - 1, x + width + 1, y, primary);
		graphics.fill(x + width, y - 1, x + width + 1, y + corner, primary);
		graphics.fill(x - 1, y + height, x + corner, y + height + 1, secondary);
		graphics.fill(x - 1, y + height - corner, x, y + height + 1, secondary);
		graphics.fill(x + width - corner, y + height, x + width + 1, y + height + 1, secondary);
		graphics.fill(x + width, y + height - corner, x + width + 1, y + height + 1, secondary);

		if (profile.tier() >= 3) {
			int inset = profile.tier() >= 6 ? 3 : 4;
			graphics.fill(x + inset, y + 2, x + width - inset, y + 3,
					withAlpha(profile.primaryColor(), profile.tier() >= 6 ? 0x88 : 0x55));
		}
		if (profile.tier() >= 5) {
			int center = x + width / 2;
			int notch = Math.min(12, width / 6);
			graphics.fill(center - notch, y - 2, center + notch, y - 1, primary);
			graphics.fill(center - notch, y + height + 1, center + notch, y + height + 2, secondary);
		}
	}

	private static int darken(int color, float amount) {
		int r = Math.round(((color >> 16) & 255) * amount);
		int g = Math.round(((color >> 8) & 255) * amount);
		int b = Math.round((color & 255) * amount);
		return (r << 16) | (g << 8) | b;
	}

	private static int withAlpha(int rgb, int alpha) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}
}
