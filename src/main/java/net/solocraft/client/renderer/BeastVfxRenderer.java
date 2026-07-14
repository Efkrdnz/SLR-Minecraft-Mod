package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.BeastVfxRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.BeastVfxEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public final class BeastVfxRenderer extends EntityRenderer<BeastVfxEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling", "textures/particle/slashgood1.png");

	public BeastVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BeastVfxEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		VertexConsumer surface = DeferredWorldShaderRenderer.buffer(buffers, BeastVfxRenderTypes.surface());
		VertexConsumer glow = DeferredWorldShaderRenderer.buffer(buffers, BeastVfxRenderTypes.additive());
		poseStack.pushPose();
		switch (entity.getStyle()) {
			case BeastVfxEntity.CLAW -> renderClaw(entity, poseStack, surface, glow, fade);
			case BeastVfxEntity.SCAR -> renderScar(entity, poseStack, surface, glow, fade);
			case BeastVfxEntity.INTERCEPT -> renderIntercept(entity, poseStack, surface, glow, fade);
			case BeastVfxEntity.QUARRY -> renderQuarry(entity, poseStack, surface, glow, fade);
			case BeastVfxEntity.OPENING -> renderOpening(entity, poseStack, surface, glow, fade);
			default -> renderClaw(entity, poseStack, surface, glow, fade);
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private void renderClaw(BeastVfxEntity entity, PoseStack stack, VertexConsumer surface,
			VertexConsumer glow, float fade) {
		orientForward(entity, stack);
		int count = entity.getVariant() >= 3 ? 2 : 1;
		for (int i = 0; i < count; i++) {
			stack.pushPose();
			float roll = entity.getRoll() + (count == 2 ? (i == 0 ? -18.0F : 18.0F) : 0.0F);
			stack.mulPose(Axis.ZP.rotationDegrees(roll));
			stack.translate(0.0D, (i - (count - 1) * 0.5D) * 0.16D, i * -0.012D);
			drawLayered(stack, surface, glow, entity.getScale(), entity.getLength() * 0.32F,
					0.0F, entity.getPrimaryColor(), entity.getSecondaryColor(), fade, 230, 96);
			stack.popPose();
		}
	}

	private void renderScar(BeastVfxEntity entity, PoseStack stack, VertexConsumer surface,
			VertexConsumer glow, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float expansion = 0.86F + entity.getProgress(0.0F) * 0.2F;
		for (int i = -1; i <= 1; i++) {
			stack.pushPose();
			stack.translate(i * entity.getScale() * 0.18D, i * -0.035D, i * 0.004D);
			stack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() + i * 4.5F));
			drawLayered(stack, surface, glow, entity.getScale() * 0.72F * expansion,
					entity.getLength() * 0.16F, 1.0F, entity.getPrimaryColor(),
					entity.getSecondaryColor(), fade, 205, 82);
			stack.popPose();
		}
	}

	private void renderIntercept(BeastVfxEntity entity, PoseStack stack, VertexConsumer surface,
			VertexConsumer glow, float fade) {
		orientForward(entity, stack);
		float pulse = 0.94F + 0.06F * (float) Math.sin(entity.tickCount * 1.25F);
		float width = entity.getScale() * pulse;
		float trail = entity.getLength() * 0.55F;
		for (int i = 0; i < 3; i++) {
			stack.pushPose();
			stack.translate(0.0D, (i - 1) * 0.22D, -trail * 0.82D - i * 0.18D);
			stack.mulPose(Axis.XP.rotationDegrees(90.0F));
			stack.mulPose(Axis.ZP.rotationDegrees((i - 1) * 13.0F));
			drawLayered(stack, surface, glow, width * (1.0F - i * 0.16F), trail,
					2.0F, entity.getPrimaryColor(), entity.getSecondaryColor(),
					fade * (1.0F - i * 0.18F), 174, 70);
			stack.popPose();
		}
	}

	private void renderQuarry(BeastVfxEntity entity, PoseStack stack, VertexConsumer surface,
			VertexConsumer glow, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		stack.translate(0.0D, entity.getLength() * 0.58D, 0.0D);
		float pulse = 0.96F + 0.08F * (float) Math.sin(entity.tickCount * 0.72F);
		drawLayered(stack, surface, glow, entity.getScale() * pulse,
				entity.getLength() * 0.55F * pulse, 3.0F, entity.getPrimaryColor(),
				entity.getSecondaryColor(), fade, 222, 98);
	}

	private void renderOpening(BeastVfxEntity entity, PoseStack stack, VertexConsumer surface,
			VertexConsumer glow, float fade) {
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float expansion = 0.82F + entity.getProgress(0.0F) * 0.42F;
		for (int i = 0; i < 2; i++) {
			stack.pushPose();
			stack.mulPose(Axis.ZP.rotationDegrees((i == 0 ? -34.0F : 34.0F) + entity.getRoll()));
			drawLayered(stack, surface, glow, entity.getScale() * expansion,
					entity.getLength() * 0.16F, 4.0F, entity.getPrimaryColor(),
					entity.getSecondaryColor(), fade, 228, 86);
			stack.popPose();
		}
	}

	private static void drawLayered(PoseStack stack, VertexConsumer surface, VertexConsumer glow,
			float width, float height, float kind, int primary, int secondary, float fade,
			int glowAlpha, int surfaceAlpha) {
		LiuSwordBeamRenderer.drawPlane(surface, stack.last(), width * 1.05F, height * 1.18F,
				kind, secondary, alpha(surfaceAlpha * fade));
		stack.pushPose();
		stack.translate(0.0D, 0.0D, 0.006D);
		LiuSwordBeamRenderer.drawPlane(glow, stack.last(), width, height,
				kind, primary, alpha(glowAlpha * fade));
		stack.popPose();
	}

	private static void orientForward(BeastVfxEntity entity, PoseStack stack) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
	}

	private static int alpha(float value) {
		return Math.max(0, Math.min(255, Math.round(value)));
	}

	@Override
	public ResourceLocation getTextureLocation(BeastVfxEntity entity) {
		return FALLBACK;
	}
}
