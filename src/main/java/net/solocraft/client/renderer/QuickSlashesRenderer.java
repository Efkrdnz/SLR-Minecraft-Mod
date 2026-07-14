package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.QuickSlashesRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.QuickSlashesEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class QuickSlashesRenderer extends EntityRenderer<QuickSlashesEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashgood1.png");
	private static final int SLASH_COUNT = 8;

	public QuickSlashesRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(QuickSlashesEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		float reveal = smoothstep(0.0F, 1.0F, entity.getReveal(partialTick));
		float age = entity.tickCount + partialTick;
		float scale = entity.getScale() * (1.0F + (1.0F - fade) * 0.16F);
		int seed = entity.getSeed();
		RenderType renderType = QuickSlashesRenderTypes.slashes(TEXTURE);
		VertexConsumer vertexConsumer = DeferredWorldShaderRenderer.buffer(bufferSource, renderType, false);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYaw()));
		for (int i = 0; i < SLASH_COUNT; i++) {
			float start = i / (float) SLASH_COUNT;
			float appear = smoothstep(start * 0.5F, start * 0.5F + 0.34F, reveal);
			if (appear <= 0.0F)
				continue;
			float linger = 1.0F - smoothstep(0.56F + start * 0.18F, 1.0F, reveal) * 0.3F;
			float side = (rand(seed, i, 23) - 0.5F) * 1.15F * scale;
			float up = (rand(seed, i, 31) - 0.5F) * 1.45F * scale;
			float depth = (rand(seed, i, 37) - 0.5F) * 0.7F * scale;
			float roll = (i % 2 == 0 ? -1.0F : 1.0F) * (30.0F + rand(seed, i, 43) * 82.0F) + age * 2.5F;
			float halfWidth = (1.15F + rand(seed, i, 61) * 0.72F) * scale;
			float halfHeight = (0.08F + rand(seed, i, 67) * 0.055F) * scale;
			int alpha = Math.round((145.0F + rand(seed, i, 71) * 95.0F) * fade * appear * linger);
			int red = Math.round(118.0F + rand(seed, i, 83) * 62.0F);
			int green = Math.round(188.0F + rand(seed, i, 89) * 58.0F);
			int blue = 255;
			poseStack.pushPose();
			poseStack.translate(side, up, depth);
			poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
			poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
			drawQuad(vertexConsumer, poseStack.last(), halfWidth * 1.16F, halfHeight * 1.8F, Math.round(red * 0.65F), Math.round(green * 0.72F), blue, Math.round(alpha * 0.28F));
			drawQuad(vertexConsumer, poseStack.last(), halfWidth, halfHeight, red, green, blue, alpha);
			poseStack.popPose();
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static float rand(int seed, int index, int salt) {
		int value = seed ^ (index * 73428767) ^ (salt * 912271);
		value ^= value << 13;
		value ^= value >>> 17;
		value ^= value << 5;
		return (value & 0x7fffffff) / (float) 0x7fffffff;
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
		return t * t * (3.0F - 2.0F * t);
	}

	private static void drawQuad(VertexConsumer vertexConsumer, PoseStack.Pose pose, float halfWidth, float halfHeight, int red, int green, int blue, int alpha) {
		vertex(vertexConsumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, red, green, blue, alpha);
		vertex(vertexConsumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, red, green, blue, alpha);
		vertex(vertexConsumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, red, green, blue, alpha);
		vertex(vertexConsumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, red, green, blue, alpha);
	}

	private static void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int red, int green, int blue, int alpha) {
		vertexConsumer.vertex(pose.pose(), x, y, z).color(red, green, blue, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(QuickSlashesEntity entity) {
		return TEXTURE;
	}
}
