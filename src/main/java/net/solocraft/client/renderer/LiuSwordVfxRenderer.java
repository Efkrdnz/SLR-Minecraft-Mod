package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.LiuSwordRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.LiuSwordVfxEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class LiuSwordVfxRenderer extends EntityRenderer<LiuSwordVfxEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling", "textures/particle/slashgood1.png");
	private static final ResourceLocation FIRE_FALLBACK = new ResourceLocation("sololeveling", "textures/particle/fire_particle.png");

	public LiuSwordVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(LiuSwordVfxEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		boolean executionFire = entity.getStyle() == LiuSwordVfxEntity.EXECUTION_EXPLOSION
				|| entity.getStyle() == LiuSwordVfxEntity.EXECUTION_LINK;
		VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(buffers,
				LiuSwordRenderTypes.effect(executionFire ? FIRE_FALLBACK : FALLBACK));
		poseStack.pushPose();
		switch (entity.getStyle()) {
			case LiuSwordVfxEntity.ARC, LiuSwordVfxEntity.DANCE -> renderArc(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.CHARGE -> renderCharge(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.TARGET, LiuSwordVfxEntity.MARK -> renderMark(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.DASH -> renderDash(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.COUNTER -> renderCounter(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.DOMAIN -> renderDomain(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.DETONATION -> renderDetonation(entity, poseStack, vertices, fade);
			case LiuSwordVfxEntity.EXECUTION_EXPLOSION -> renderExecutionExplosion(entity, poseStack, vertices, fade, partialTick);
			case LiuSwordVfxEntity.EXECUTION_LINK -> renderExecutionLink(entity, poseStack, vertices, fade, partialTick);
			default -> renderMark(entity, poseStack, vertices, fade);
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private void renderArc(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		orientForward(entity, stack);
		if (entity.isDual()) {
			drawRolled(stack, out, entity.getScale(), entity.getLength() * 0.34F,
					18.0F + entity.getRoll(), 0.0F, entity.getPrimaryColor(), fade);
			drawRolled(stack, out, entity.getScale(), entity.getLength() * 0.34F,
					-18.0F - entity.getRoll(), -0.035F, entity.getSecondaryColor(), fade);
		} else {
			drawRolled(stack, out, entity.getScale(), entity.getLength() * 0.31F,
					entity.getRoll(), 0.0F, entity.getPrimaryColor(), fade);
		}
	}

	private void renderCharge(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float pulse = 0.9F + 0.12F * (float) Math.sin((entity.tickCount + 1.0F) * 0.8F);
		float size = entity.getScale() * pulse;
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), size, entity.getLength() * 0.52F,
				6.0F, entity.getPrimaryColor(), alpha(178.0F * fade));
		stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), size * 0.68F, entity.getLength() * 0.44F,
				6.0F, entity.getSecondaryColor(), alpha(116.0F * fade));
	}

	private void renderMark(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float pulse = 1.0F + 0.08F * (float) Math.sin(entity.tickCount * 0.7F);
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * pulse,
				entity.getLength() * 0.52F * pulse, 3.0F, entity.getPrimaryColor(), alpha(198.0F * fade));
		if (entity.isDual() && entity.getStyle() == LiuSwordVfxEntity.TARGET) {
			stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
			LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * 0.78F,
					entity.getLength() * 0.42F, 3.0F, entity.getSecondaryColor(), alpha(118.0F * fade));
		}
	}

	private void renderDash(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		orientForward(entity, stack);
		stack.translate(0.0D, 0.0D, -entity.getLength() * 0.42D);
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale(), entity.getLength() * 0.52F,
				8.0F, entity.getPrimaryColor(), alpha(145.0F * fade));
		if (entity.isDual()) {
			stack.mulPose(Axis.ZP.rotationDegrees(18.0F));
			LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * 0.85F,
					entity.getLength() * 0.48F, 8.0F, entity.getSecondaryColor(), alpha(106.0F * fade));
		}
	}

	private void renderCounter(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float expansion = 1.0F + entity.getProgress(0.0F) * 0.45F;
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * expansion,
				entity.getLength() * 0.5F * expansion, 7.0F, entity.getPrimaryColor(), alpha(218.0F * fade));
		stack.mulPose(Axis.ZP.rotationDegrees(45.0F));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * 0.72F,
				entity.getLength() * 0.36F, 7.0F, entity.getSecondaryColor(), alpha(125.0F * fade));
	}

	private void renderDomain(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		float radius = entity.getScale() * (0.94F + 0.04F * (float) Math.sin(entity.tickCount * 0.15F));
		LiuSwordBeamRenderer.drawHorizontalRing(out, stack.last(), radius, radius * 0.69F,
				5.0F, entity.getPrimaryColor(), alpha(82.0F * fade), 28);
	}

	private void renderDetonation(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float growth = 0.42F + entity.getProgress(0.0F) * 0.85F;
		for (int i = 0; i < (entity.isDual() ? 4 : 3); i++) {
			stack.pushPose();
			stack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() + i * (entity.isDual() ? 45.0F : 60.0F)));
			LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * growth,
					entity.getLength() * 0.18F, 4.0F,
					i % 2 == 0 ? entity.getPrimaryColor() : entity.getSecondaryColor(),
					alpha((205.0F - i * 24.0F) * fade));
			stack.popPose();
		}
	}

	private void renderExecutionExplosion(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out,
			float fade, float partialTick) {
		float progress = entity.getProgress(partialTick);
		float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0D);
		float growth = 0.2F + eased * 0.96F;
		float ringFade = Math.max(0.0F, 1.0F - progress * 1.12F);
		int orange = mixColor(entity.getPrimaryColor(), entity.getSecondaryColor(), 0.48F);

		stack.pushPose();
		float ringRadius = entity.getScale() * (0.3F + eased * 1.12F);
		LiuSwordBeamRenderer.drawHorizontal(out, stack.last(), ringRadius, 3.0F,
				orange, alpha(126.0F * fade * ringFade));
		stack.popPose();

		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float width = entity.getScale() * growth;
		float height = entity.getLength() * growth;
		stack.pushPose();
		stack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), width, height, 9.0F,
				entity.getPrimaryColor(), alpha(104.0F * fade));
		stack.popPose();

		stack.pushPose();
		stack.translate(0.0D, 0.0D, 0.012D);
		stack.mulPose(Axis.ZP.rotationDegrees(-entity.getRoll() * 0.37F));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), width * 0.73F, height * 0.76F, 9.0F,
				orange, alpha(158.0F * fade));
		stack.popPose();

		float ignitionFlash = Math.max(0.0F, 1.0F - progress * 2.7F);
		stack.pushPose();
		stack.translate(0.0D, 0.0D, 0.024D);
		stack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() * 0.19F));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), width * 0.43F, height * 0.48F, 9.0F,
				entity.getSecondaryColor(), alpha((188.0F + ignitionFlash * 48.0F) * fade));
		stack.popPose();
	}

	private void renderExecutionLink(LiuSwordVfxEntity entity, PoseStack stack, VertexConsumer out,
			float fade, float partialTick) {
		orientForward(entity, stack);
		float pulse = 0.92F + 0.08F * (float) Math.sin((entity.tickCount + partialTick) * 0.86F);
		float halfLength = entity.getLength() * 0.5F;
		for (int plane = 0; plane < 2; plane++) {
			stack.pushPose();
			stack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() + plane * 90.0F));
			stack.mulPose(Axis.XP.rotationDegrees(90.0F));
			LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * 1.65F * pulse,
					halfLength, 10.0F, entity.getPrimaryColor(), alpha(72.0F * fade));
			LiuSwordBeamRenderer.drawPlane(out, stack.last(), entity.getScale() * 0.72F * pulse,
					halfLength, 10.0F, entity.getSecondaryColor(), alpha(184.0F * fade));
			stack.popPose();
		}
	}

	private void orientForward(LiuSwordVfxEntity entity, PoseStack stack) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
	}

	private static void drawRolled(PoseStack stack, VertexConsumer out, float width, float height,
			float roll, float z, int color, float fade) {
		stack.pushPose();
		stack.translate(0.0D, 0.0D, z);
		stack.mulPose(Axis.ZP.rotationDegrees(roll));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), width * 1.13F, height * 1.38F,
				1.0F, color, alpha(104.0F * fade));
		LiuSwordBeamRenderer.drawPlane(out, stack.last(), width, height,
				0.0F, color, alpha(228.0F * fade));
		stack.popPose();
	}

	private static int alpha(float value) {
		return Math.max(0, Math.min(255, Math.round(value)));
	}

	private static int mixColor(int first, int second, float amount) {
		float blend = Math.max(0.0F, Math.min(1.0F, amount));
		int red = Math.round(((first >> 16) & 255) + (((second >> 16) & 255) - ((first >> 16) & 255)) * blend);
		int green = Math.round(((first >> 8) & 255) + (((second >> 8) & 255) - ((first >> 8) & 255)) * blend);
		int blue = Math.round((first & 255) + ((second & 255) - (first & 255)) * blend);
		return (red << 16) | (green << 8) | blue;
	}

	@Override
	public ResourceLocation getTextureLocation(LiuSwordVfxEntity entity) {
		return FALLBACK;
	}
}
