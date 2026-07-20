package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.client.renderer.shader.FireMageVfxRenderTypes;
import net.solocraft.entity.FireMageVfxEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class FireMageVfxRenderer extends EntityRenderer<FireMageVfxEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling", "textures/particle/glow_yellow.png");

	public FireMageVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(FireMageVfxEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.002F)
			return;
		VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(buffers,
				FireMageVfxRenderTypes.effect(FALLBACK));
		poseStack.pushPose();
		switch (entity.getStyle()) {
			case FireMageVfxEntity.FLAME_WEAVING -> renderWeaving(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.IGNITION_ORB -> renderOrb(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.ORB_IMPACT -> renderOrbImpact(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.INFERNO_LANCE -> renderLance(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.FLASHFIRE -> renderFlashfire(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.CREMATION -> renderCremation(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.FURNACE_DOMINION -> renderDominion(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.HEAVENFALL -> renderHeavenfall(entity, partialTick, poseStack, vertices, fade);
			case FireMageVfxEntity.HEAVENFALL_IMPACT -> renderHeavenfallImpact(entity, partialTick, poseStack, vertices, fade);
			default -> renderScorch(entity, partialTick, poseStack, vertices, fade);
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private void renderWeaving(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float pulse = 0.92F + Mth.sin((entity.tickCount + partialTick) * 0.8F + entity.getSeed()) * 0.08F;
		float width = entity.getScale() * pulse;
		float length = entity.getLength();
		int stage = entity.getStage();
		int layers = stage >= 5 ? 3 : stage >= 3 ? 2 : 1;
		for (int layer = layers - 1; layer >= 0; layer--) {
			float spread = 1.0F + layer * 0.32F;
			int color = layer == 0 ? lighten(entity.getSecondaryColor(), 0.58F) : entity.getPrimaryColor();
			drawDirectional(out, stack.last(), width * spread, length * (1.0F + layer * 0.08F),
					0.0F, 0.0F + layer * 0.01F, color, alpha((225 - layer * 55) * fade), stage);
			if (stage >= 2)
				drawDirectional(out, stack.last(), width * spread * 0.72F, length,
						stage >= 4 ? 34.0F : 90.0F, -0.02F, entity.getSecondaryColor(),
						alpha((155 - layer * 34) * fade), stage);
		}
	}

	private void renderOrb(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int stage = entity.getStage();
		float time = entity.tickCount + partialTick;
		float size = entity.getScale() * (0.93F + Mth.sin(time * 0.45F) * 0.07F);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), size * 1.55F, 1.0F, entity.getPrimaryColor(), alpha(92 * fade), stage);
		drawBillboard(out, stack.last(), size, 1.0F, entity.getSecondaryColor(), alpha(220 * fade), stage);
		drawBillboard(out, stack.last(), size * 0.48F, 1.0F, 0xFFF4C2, alpha(245 * fade), stage);
		if (stage >= 4) {
			int satellites = stage == 4 ? 3 : 5;
			for (int i = 0; i < satellites; i++) {
				double angle = time * (0.16D + i * 0.008D) + Math.PI * 2.0D * i / satellites;
				stack.pushPose();
				stack.translate(Math.cos(angle) * size * 1.55D, Math.sin(angle * 1.37D) * size * 0.52D, 0.02D);
				drawBillboard(out, stack.last(), size * 0.22F, 1.0F, 0xFFD85A, alpha(205 * fade), stage);
				stack.popPose();
			}
		}
	}

	private void renderOrbImpact(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float progress = entity.getProgress(partialTick);
		float radius = entity.getScale() * (0.28F + progress * 0.9F);
		int stage = entity.getStage();
		drawHorizontal(out, stack.last(), radius, 2.0F, entity.getPrimaryColor(), alpha(170 * fade), stage);
		drawHorizontalRing(out, stack.last(), radius * 1.22F, radius * 0.77F, 2.0F,
				entity.getSecondaryColor(), alpha(230 * fade), stage, 28);
		stack.translate(0.0D, radius * 0.32D, 0.0D);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), radius * 0.85F, 5.0F, entity.getSecondaryColor(), alpha(190 * fade), stage);
		drawBillboard(out, stack.last(), radius * 0.42F, 5.0F, 0xFFF5D0, alpha(240 * fade), stage);
	}

	private void renderLance(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		int stage = entity.getStage();
		float time = entity.tickCount + partialTick;
		float width = entity.getScale();
		float length = entity.getLength();
		float twist = Mth.sin(time * 0.72F) * (stage >= 4 ? 12.0F : 4.0F);
		drawDirectional(out, stack.last(), width * 1.52F, length, twist, 0.0F,
				entity.getPrimaryColor(), alpha(105 * fade), stage);
		drawDirectional(out, stack.last(), width, length * 1.06F, 0.0F, 0.01F,
				entity.getSecondaryColor(), alpha(230 * fade), stage);
		drawDirectional(out, stack.last(), width * 0.34F, length * 1.12F, 90.0F, 0.02F,
				0xFFF4CC, alpha(245 * fade), stage);
		if (stage >= 4) {
			drawDirectional(out, stack.last(), width * 0.52F, length, 46.0F + twist, -0.03F,
					0xFF9A22, alpha(145 * fade), stage);
			drawDirectional(out, stack.last(), width * 0.52F, length, -46.0F - twist, -0.04F,
					0xFF5A0A, alpha(145 * fade), stage);
		}
	}

	private void renderFlashfire(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		int stage = entity.getStage();
		float length = entity.getLength();
		float width = entity.getScale();
		int echoes = stage >= 5 ? 4 : stage >= 3 ? 3 : 2;
		for (int i = echoes; i >= 0; i--) {
			stack.pushPose();
			stack.translate(0.0D, 0.0D, -i * length * 0.16D);
			drawDirectional(out, stack.last(), width * (1.0F + i * 0.18F), length,
					stage >= 3 ? (i % 2 == 0 ? 22.0F : -22.0F) : 0.0F, 0.0F,
					i == 0 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha((215 - i * 38) * fade), stage);
			stack.popPose();
		}
	}

	private void renderCremation(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float progress = entity.getProgress(partialTick);
		float radius = entity.getScale() * (0.65F + progress * 0.65F);
		int stage = entity.getStage();
		drawHorizontalRing(out, stack.last(), radius, radius * 0.58F, 2.0F,
				entity.getPrimaryColor(), alpha(210 * fade), stage, 24);
		stack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
		drawVertical(out, stack.last(), radius * 0.72F, entity.getLength(), 3.0F,
				entity.getSecondaryColor(), alpha(225 * fade), stage);
		drawVertical(out, stack.last(), radius * 0.27F, entity.getLength() * 1.18F, 3.0F,
				0xFFF4D0, alpha(240 * fade), stage);
	}

	private void renderDominion(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int stage = entity.getStage();
		float time = entity.tickCount + partialTick;
		float radius = entity.getScale() * (0.98F + Mth.sin(time * 0.12F) * 0.02F);
		drawHorizontal(out, stack.last(), radius, 2.0F, entity.getPrimaryColor(), alpha(70 * fade), stage);
		drawHorizontalRing(out, stack.last(), radius, radius * 0.88F, 2.0F,
				entity.getSecondaryColor(), alpha(215 * fade), stage, stage >= 5 ? 48 : 32);
		drawHorizontalRing(out, stack.last(), radius * 0.62F, radius * 0.54F, 2.0F,
				0xFFD85A, alpha(145 * fade), stage, 28);
		drawWallRing(out, stack.last(), radius, entity.getLength(), 3.0F,
				entity.getPrimaryColor(), alpha((stage >= 4 ? 118 : 88) * fade), stage,
				stage >= 5 ? 24 : 16, time * 0.018F);
		if (stage >= 4)
			drawWallRing(out, stack.last(), radius * 0.72F, entity.getLength() * 0.78F, 3.0F,
					entity.getSecondaryColor(), alpha(72 * fade), stage, 14, -time * 0.025F);
	}

	private void renderHeavenfall(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int stage = entity.getStage();
		float time = entity.tickCount + partialTick;
		float size = entity.getScale() * (0.94F + Mth.sin(time * 0.19F) * 0.06F);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), size * 1.65F, 4.0F, 0x6D1207, alpha(105 * fade), stage);
		drawBillboard(out, stack.last(), size, 1.0F, entity.getPrimaryColor(), alpha(225 * fade), stage);
		drawBillboard(out, stack.last(), size * 0.52F, 1.0F, entity.getSecondaryColor(), alpha(245 * fade), stage);
		stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
		drawVertical(out, stack.last(), size * 0.48F, entity.getLength(), 4.0F,
				entity.getPrimaryColor(), alpha(145 * fade), stage);
	}

	private void renderHeavenfallImpact(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int stage = entity.getStage();
		float progress = entity.getProgress(partialTick);
		float radius = entity.getScale() * (0.16F + progress * 0.98F);
		drawHorizontal(out, stack.last(), radius, 5.0F, entity.getPrimaryColor(), alpha(130 * fade), stage);
		drawHorizontalRing(out, stack.last(), radius * 1.12F, radius * 0.79F, 2.0F,
				entity.getSecondaryColor(), alpha(235 * fade), stage, stage >= 5 ? 48 : 30);
		drawHorizontalRing(out, stack.last(), radius * 0.62F, radius * 0.34F, 2.0F,
				0xFFF1B0, alpha(215 * fade), stage, 28);
		stack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
		drawVertical(out, stack.last(), radius * 0.3F, entity.getLength(), 3.0F,
				0xFFF2BE, alpha(230 * fade), stage);
		drawVertical(out, stack.last(), radius * 0.82F, entity.getLength() * 0.78F, 3.0F,
				entity.getPrimaryColor(), alpha(105 * fade), stage);
	}

	private void renderScorch(FireMageVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float size = entity.getScale() * (0.72F + entity.getProgress(partialTick) * 0.38F);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), size, 5.0F, entity.getPrimaryColor(), alpha(185 * fade), entity.getStage());
		drawBillboard(out, stack.last(), size * 0.42F, 1.0F, entity.getSecondaryColor(), alpha(225 * fade), entity.getStage());
	}

	private static void orientForward(FireMageVfxEntity entity, PoseStack stack) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
	}

	private static void drawDirectional(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float length, float roll, float zOffset, int color, int alpha, int stage) {
		float radians = roll * Mth.DEG_TO_RAD;
		float sin = Mth.sin(radians);
		float cos = Mth.cos(radians);
		float near = length * 0.22F;
		beamVertex(out, pose, -halfWidth * 0.22F, 0.0F, near + zOffset, 0.0F, 0.0F, 0.0F,
				color, alpha, stage, sin, cos);
		beamVertex(out, pose, halfWidth * 0.22F, 0.0F, near + zOffset, 1.0F, 0.0F, 0.0F,
				color, alpha, stage, sin, cos);
		beamVertex(out, pose, halfWidth, 0.0F, -length + zOffset, 1.0F, 1.0F, 0.0F,
				color, alpha, stage, sin, cos);
		beamVertex(out, pose, -halfWidth, 0.0F, -length + zOffset, 0.0F, 1.0F, 0.0F,
				color, alpha, stage, sin, cos);
	}

	private static void beamVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, float kind, int color, int alpha, int stage, float sin, float cos) {
		vertex(out, pose, x * cos - y * sin, x * sin + y * cos, z, kind + u,
				stage + 0.001F + v * 0.998F,
				color, alpha, 0.0F, 0.0F, 1.0F);
	}

	private static void drawBillboard(VertexConsumer out, PoseStack.Pose pose, float radius, float kind,
			int color, int alpha, int stage) {
		vertex(out, pose, -radius, -radius, 0.0F, kind, stage + 0.999F, color, alpha, 0, 0, 1);
		vertex(out, pose, radius, -radius, 0.0F, kind + 1.0F, stage + 0.999F, color, alpha, 0, 0, 1);
		vertex(out, pose, radius, radius, 0.0F, kind + 1.0F, stage + 0.001F, color, alpha, 0, 0, 1);
		vertex(out, pose, -radius, radius, 0.0F, kind, stage + 0.001F, color, alpha, 0, 0, 1);
	}

	private static void drawVertical(VertexConsumer out, PoseStack.Pose pose, float halfWidth, float height,
			float kind, int color, int alpha, int stage) {
		vertex(out, pose, -halfWidth, 0, 0, kind, stage + 0.999F, color, alpha, 0, 0, 1);
		vertex(out, pose, halfWidth, 0, 0, kind + 1, stage + 0.999F, color, alpha, 0, 0, 1);
		vertex(out, pose, halfWidth * 0.42F, height, 0, kind + 1, stage + 0.001F, color, alpha, 0, 0, 1);
		vertex(out, pose, -halfWidth * 0.42F, height, 0, kind, stage + 0.001F, color, alpha, 0, 0, 1);
	}

	private static void drawHorizontal(VertexConsumer out, PoseStack.Pose pose, float radius, float kind,
			int color, int alpha, int stage) {
		vertex(out, pose, -radius, 0.025F, -radius, kind, stage + 0.999F, color, alpha, 0, 1, 0);
		vertex(out, pose, -radius, 0.025F, radius, kind + 1, stage + 0.001F, color, alpha, 0, 1, 0);
		vertex(out, pose, radius, 0.025F, radius, kind + 1, stage + 0.999F, color, alpha, 0, 1, 0);
		vertex(out, pose, radius, 0.025F, -radius, kind, stage + 0.001F, color, alpha, 0, 1, 0);
	}

	private static void drawHorizontalRing(VertexConsumer out, PoseStack.Pose pose, float outer,
			float inner, float kind, int color, int alpha, int stage, int segments) {
		int count = Math.max(12, segments);
		float safeInner = Mth.clamp(inner, 0.0F, outer - 0.01F);
		for (int i = 0; i < count; i++) {
			double a0 = Math.PI * 2.0D * i / count;
			double a1 = Math.PI * 2.0D * (i + 1) / count;
			ringVertex(out, pose, (float) Math.cos(a0) * outer, (float) Math.sin(a0) * outer,
					outer, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a1) * outer, (float) Math.sin(a1) * outer,
					outer, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a1) * safeInner, (float) Math.sin(a1) * safeInner,
					outer, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a0) * safeInner, (float) Math.sin(a0) * safeInner,
					outer, kind, color, alpha, stage);
		}
	}

	private static void ringVertex(VertexConsumer out, PoseStack.Pose pose, float x, float z,
			float radius, float kind, int color, int alpha, int stage) {
		float u = kind + 0.5F + 0.499F * x / radius;
		float v = stage + 0.5F + 0.499F * z / radius;
		vertex(out, pose, x, 0.026F, z, u, v, color, alpha, 0, 1, 0);
	}

	private static void drawWallRing(VertexConsumer out, PoseStack.Pose pose, float radius, float height,
			float kind, int color, int alpha, int stage, int segments, float offset) {
		int count = Math.max(10, segments);
		for (int i = 0; i < count; i++) {
			double a0 = Math.PI * 2.0D * i / count + offset;
			double a1 = Math.PI * 2.0D * (i + 1) / count + offset;
			float x0 = (float) Math.cos(a0) * radius;
			float z0 = (float) Math.sin(a0) * radius;
			float x1 = (float) Math.cos(a1) * radius;
			float z1 = (float) Math.sin(a1) * radius;
			vertex(out, pose, x0, 0, z0, kind, stage + 0.999F, color, alpha, x0 / radius, 0, z0 / radius);
			vertex(out, pose, x1, 0, z1, kind + 1, stage + 0.999F, color, alpha, x1 / radius, 0, z1 / radius);
			vertex(out, pose, x1, height, z1, kind + 1, stage + 0.001F, color, alpha, x1 / radius, 0, z1 / radius);
			vertex(out, pose, x0, height, z0, kind, stage + 0.001F, color, alpha, x0 / radius, 0, z0 / radius);
		}
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int color, int alpha, float nx, float ny, float nz) {
		out.vertex(pose.pose(), x, y, z)
				.color((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
				.uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
				.normal(pose.normal(), nx, ny, nz).endVertex();
	}

	private static int lighten(int color, float amount) {
		int r = (color >> 16) & 255;
		int g = (color >> 8) & 255;
		int b = color & 255;
		r += Math.round((255 - r) * amount);
		g += Math.round((255 - g) * amount);
		b += Math.round((255 - b) * amount);
		return (r << 16) | (g << 8) | b;
	}

	private static int alpha(float value) {
		return Mth.clamp(Math.round(value), 0, 255);
	}

	@Override
	public ResourceLocation getTextureLocation(FireMageVfxEntity entity) {
		return FALLBACK;
	}
}
