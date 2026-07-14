package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.WhiteFlameVfxRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.WhiteFlameVfxEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class WhiteFlameVfxRenderer extends EntityRenderer<WhiteFlameVfxEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling", "textures/particle/glow_yellow.png");

	public WhiteFlameVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(WhiteFlameVfxEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(buffers,
				WhiteFlameVfxRenderTypes.effect(FALLBACK));
		poseStack.pushPose();
		switch (entity.getStyle()) {
			case WhiteFlameVfxEntity.LIGHTNING_BREATH -> renderBreath(entity, poseStack, vertices, fade);
			case WhiteFlameVfxEntity.HELLSTORM_STRIKE -> renderStrike(entity, poseStack, vertices, fade);
			case WhiteFlameVfxEntity.KINGS_VERDICT -> renderVerdict(entity, poseStack, vertices, fade);
			case WhiteFlameVfxEntity.DOPPELGANGER -> renderDoppelgangers(entity, poseStack, vertices, fade);
			case WhiteFlameVfxEntity.HELL_GATE -> renderGate(entity, poseStack, vertices, fade);
			case WhiteFlameVfxEntity.DODGE -> renderDodge(entity, poseStack, vertices, fade);
			default -> renderImpact(entity, poseStack, vertices, fade);
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private void renderBreath(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
		float width = entity.getScale();
		float length = entity.getLength();
		drawBeam(vertices, stack.last(), width, length, 0.0F, alpha(210 * fade), 0.0F);
		drawBeam(vertices, stack.last(), width * 0.72F, length, 90.0F, alpha(170 * fade), 0.0F);
	}

	private void renderStrike(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		stack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
		float width = entity.getScale();
		drawVertical(vertices, stack.last(), width, entity.getLength(), alpha(220 * fade), 1.0F);
		stack.mulPose(Axis.YP.rotationDegrees(90.0F));
		drawVertical(vertices, stack.last(), width * 0.72F, entity.getLength(), alpha(150 * fade), 1.0F);
	}

	private void renderVerdict(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		float radius = entity.getScale() * (0.85F + entity.tickCount * 0.035F);
		drawHorizontal(vertices, stack.last(), radius, alpha(205 * fade), 2.0F);
		stack.translate(0.0D, entity.getLength() * 0.45D, 0.0D);
		stack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
		drawVertical(vertices, stack.last(), radius * 0.65F, entity.getLength(), alpha(150 * fade), 2.0F);
	}

	private void renderDoppelgangers(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		stack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
		for (int i = -1; i <= 1; i++) {
			stack.pushPose();
			stack.translate(i * entity.getScale() * 1.35F, 0.0D, Math.abs(i) * -0.2D);
			drawVertical(vertices, stack.last(), entity.getScale() * 0.42F, entity.getLength(),
					alpha((i == 0 ? 95 : 175) * fade), 3.0F);
			stack.popPose();
		}
	}

	private void renderGate(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		stack.mulPose(Axis.YP.rotationDegrees(-this.entityRenderDispatcher.camera.getYRot()));
		drawVertical(vertices, stack.last(), entity.getScale(), entity.getLength(), alpha(200 * fade), 4.0F);
	}

	private void renderDodge(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float size = entity.getScale() * (1.0F + entity.tickCount * 0.12F);
		drawBillboard(vertices, stack.last(), size, size, alpha(190 * fade), 5.0F);
	}

	private void renderImpact(WhiteFlameVfxEntity entity, PoseStack stack, VertexConsumer vertices, float fade) {
		float radius = entity.getScale() * (1.0F + entity.tickCount * 0.09F);
		drawHorizontal(vertices, stack.last(), radius, alpha(220 * fade), 6.0F);
	}

	private static void drawBeam(VertexConsumer out, PoseStack.Pose pose, float width, float length,
			float roll, int alpha, float style) {
		float sin = (float) Math.sin(Math.toRadians(roll));
		float cos = (float) Math.cos(Math.toRadians(roll));
		beamVertex(out, pose, -width, 0, 0, style, 1, alpha, sin, cos);
		beamVertex(out, pose, width, 0, 0, style + 1, 1, alpha, sin, cos);
		beamVertex(out, pose, width * 0.2F, 0, length, style + 1, 0, alpha, sin, cos);
		beamVertex(out, pose, -width * 0.2F, 0, length, style, 0, alpha, sin, cos);
	}

	private static void beamVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int alpha, float sin, float cos) {
		vertex(out, pose, x * cos - y * sin, x * sin + y * cos, z, u, v, alpha);
	}

	private static void drawVertical(VertexConsumer out, PoseStack.Pose pose, float halfWidth, float height, int alpha, float style) {
		vertex(out, pose, -halfWidth, 0, 0, style, 1, alpha);
		vertex(out, pose, halfWidth, 0, 0, style + 1, 1, alpha);
		vertex(out, pose, halfWidth * 0.38F, height, 0, style + 1, 0, alpha);
		vertex(out, pose, -halfWidth * 0.38F, height, 0, style, 0, alpha);
	}

	private static void drawHorizontal(VertexConsumer out, PoseStack.Pose pose, float radius, int alpha, float style) {
		vertex(out, pose, -radius, 0.03F, -radius, style, 1, alpha);
		vertex(out, pose, -radius, 0.03F, radius, style + 1, 1, alpha);
		vertex(out, pose, radius, 0.03F, radius, style + 1, 0, alpha);
		vertex(out, pose, radius, 0.03F, -radius, style, 0, alpha);
	}

	private static void drawBillboard(VertexConsumer out, PoseStack.Pose pose, float width, float height, int alpha, float style) {
		vertex(out, pose, -width, -height, 0, style, 1, alpha);
		vertex(out, pose, width, -height, 0, style + 1, 1, alpha);
		vertex(out, pose, width, height, 0, style + 1, 0, alpha);
		vertex(out, pose, -width, height, 0, style, 0, alpha);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int alpha) {
		out.vertex(pose.pose(), x, y, z).color(255, 255, 255, alpha).uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
				.normal(pose.normal(), 0, 1, 0).endVertex();
	}

	private static int alpha(float value) {
		return Math.max(0, Math.min(255, Math.round(value)));
	}

	@Override
	public ResourceLocation getTextureLocation(WhiteFlameVfxEntity entity) {
		return FALLBACK;
	}
}
