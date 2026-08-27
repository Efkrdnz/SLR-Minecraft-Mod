package net.solocraft.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.solocraft.client.gui.CurseWheelClientState;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.CurseType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import org.joml.Matrix4f;

import java.util.List;

/**
 * The curse wheel.
 *
 * <p>Geometry follows the Frozen Architecture radial so both wheels feel like the
 * same tool. Two differences: each wedge is tinted with its own curse colour
 * rather than a single palette, and a curse still on lockout is drawn dimmed with
 * its remaining seconds, because choosing a curse you cannot cast yet is the
 * mistake this wheel most needs to prevent.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CurseWheelRadialOverlay {
	private static final float INNER_RADIUS = 31.0F;
	private static final float OUTER_RADIUS = 94.0F;

	private CurseWheelRadialOverlay() {
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!CurseWheelClientState.isActive())
			return;
		CurseWheelClientState.updateMouseFromFrame();
		if (!CurseWheelClientState.isActive())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		GuiGraphics graphics = event.getGuiGraphics();
		int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
		int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
		CurseType selected = CurseWheelClientState.selectedCurse();
		List<CurseType> options = CurseWheelClientState.options();
		double rotation = CurseWheelClientState.rotation();
		double step = Math.PI * 2.0D / options.size();

		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(0.0D, 0.0D, 400.0D);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		for (int i = 0; i < options.size(); i++) {
			CurseType curse = options.get(i);
			double middle = -Math.PI / 2.0D + i * step + rotation;
			boolean top = curse == selected;
			boolean ready = minecraft.player == null
					|| !CooldownManager.isOnCooldown(minecraft.player, curse.cooldownKey());
			float start = (float) (middle - step * 0.475D);
			float end = (float) (middle + step * 0.475D);

			float[] tint = rgb(curse.accentColor());
			float fill = top ? 1.0F : 0.16F;
			float dim = ready ? 1.0F : 0.45F;
			renderSector(pose, centerX, centerY, start, end,
					tint[0] * fill * dim, tint[1] * fill * dim, tint[2] * fill * dim,
					top ? 0.90F : 0.86F);
			renderArc(pose, centerX, centerY, OUTER_RADIUS - 1.5F, OUTER_RADIUS, start, end,
					tint[0] * dim, tint[1] * dim, tint[2] * dim, top ? 0.95F : 0.65F);
			renderArc(pose, centerX, centerY, INNER_RADIUS, INNER_RADIUS + 1.5F, start, end,
					tint[0] * dim, tint[1] * dim, tint[2] * dim, top ? 0.95F : 0.65F);

			float labelRadius = (INNER_RADIUS + OUTER_RADIUS) * 0.5F;
			int labelX = Math.round(centerX + (float) Math.cos(middle) * labelRadius);
			int labelY = Math.round(centerY + (float) Math.sin(middle) * labelRadius);
			int color = top ? 0xFFFFFFFF : (ready ? 0xFFD8CCEA : 0xFF7A6E8C);
			graphics.drawCenteredString(minecraft.font,
					Component.literal(curse.displayName()), labelX, labelY - 4, color);
			if (!ready && minecraft.player != null) {
				int seconds = Math.max(1, CooldownManager.getRemainingSeconds(
						minecraft.player, curse.cooldownKey()));
				graphics.drawCenteredString(minecraft.font,
						Component.literal(seconds + "s"), labelX, labelY + 6, 0xFFB05C6E);
			}
		}

		renderCenter(pose, centerX, centerY, rgb(selected.accentColor()));
		drawTopPointer(pose, centerX, centerY, rgb(selected.accentColor()));
		graphics.drawCenteredString(minecraft.font,
				Component.literal(selected.displayName()), centerX, centerY - 5, 0xFFFFFFFF);
		graphics.drawCenteredString(minecraft.font,
				Component.literal(selected.description()),
				centerX, centerY + Math.round(OUTER_RADIUS) + 16, 0xFFB9A6D6);
		graphics.drawCenteredString(minecraft.font,
				Component.literal("Move the mouse to choose - release to arm"),
				centerX, centerY + Math.round(OUTER_RADIUS) + 29, 0xFFFFFFFF);

		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		pose.popPose();
	}

	private static float[] rgb(int argb) {
		return new float[] {
				((argb >> 16) & 0xFF) / 255.0F,
				((argb >> 8) & 0xFF) / 255.0F,
				(argb & 0xFF) / 255.0F
		};
	}

	private static void renderSector(PoseStack pose, float centerX, float centerY,
			float start, float end, float red, float green, float blue, float alpha) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f matrix = pose.last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP,
				DefaultVertexFormat.POSITION_COLOR);
		int segments = 18;
		for (int i = 0; i <= segments; i++) {
			float angle = start + (end - start) * i / segments;
			float cosine = (float) Math.cos(angle);
			float sine = (float) Math.sin(angle);
			buffer.addVertex(matrix, centerX + cosine * OUTER_RADIUS,
					centerY + sine * OUTER_RADIUS, 0.0F).setColor(red, green, blue, alpha);
			buffer.addVertex(matrix, centerX + cosine * INNER_RADIUS,
					centerY + sine * INNER_RADIUS, 0.0F)
					.setColor(red * 0.42F, green * 0.42F, blue * 0.52F, alpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void renderArc(PoseStack pose, float centerX, float centerY,
			float inner, float outer, float start, float end,
			float red, float green, float blue, float alpha) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f matrix = pose.last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP,
				DefaultVertexFormat.POSITION_COLOR);
		int segments = 18;
		for (int i = 0; i <= segments; i++) {
			float angle = start + (end - start) * i / segments;
			float cosine = (float) Math.cos(angle);
			float sine = (float) Math.sin(angle);
			buffer.addVertex(matrix, centerX + cosine * outer, centerY + sine * outer, 0.3F)
					.setColor(red, green, blue, alpha);
			buffer.addVertex(matrix, centerX + cosine * inner, centerY + sine * inner, 0.3F)
					.setColor(red, green, blue, alpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void renderCenter(PoseStack pose, float centerX, float centerY, float[] tint) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f matrix = pose.last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,
				DefaultVertexFormat.POSITION_COLOR);
		buffer.addVertex(matrix, centerX, centerY, 0.6F).setColor(0.05F, 0.03F, 0.09F, 0.98F);
		for (int i = 0; i <= 48; i++) {
			float angle = (float) (Math.PI * 2.0D * i / 48.0D);
			buffer.addVertex(matrix, centerX + (float) Math.cos(angle) * (INNER_RADIUS - 2.0F),
					centerY + (float) Math.sin(angle) * (INNER_RADIUS - 2.0F), 0.6F)
					.setColor(tint[0] * 0.45F, tint[1] * 0.45F, tint[2] * 0.55F, 0.98F);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void drawTopPointer(PoseStack pose, float centerX, float centerY, float[] tint) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f matrix = pose.last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
				DefaultVertexFormat.POSITION_COLOR);
		float y = centerY - OUTER_RADIUS - 5.0F;
		buffer.addVertex(matrix, centerX, y + 10.0F, 1.0F).setColor(1.0F, 0.95F, 1.0F, 1.0F);
		buffer.addVertex(matrix, centerX - 7.0F, y, 1.0F).setColor(tint[0], tint[1], tint[2], 1.0F);
		buffer.addVertex(matrix, centerX + 7.0F, y, 1.0F).setColor(tint[0], tint[1], tint[2], 1.0F);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}
}
