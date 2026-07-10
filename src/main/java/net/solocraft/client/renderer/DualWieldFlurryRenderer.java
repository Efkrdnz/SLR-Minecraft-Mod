package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.DualWieldFlurryRenderTypes;
import net.solocraft.entity.DualWieldFlurryEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DualWieldFlurryRenderer extends EntityRenderer<DualWieldFlurryEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashgood1.png");
	private static final int SLASH_COUNT = 19;
	private static final float TELEPORT_DELAY_TICKS = 12.0F;

	public DualWieldFlurryRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(DualWieldFlurryEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		RenderType renderType = DualWieldFlurryRenderTypes.flurry(TEXTURE);
		VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
		int seed = entity.getSeed();
		float age = entity.tickCount + partialTick;
		float reveal = clamp(age / TELEPORT_DELAY_TICKS, 0.0F, 1.0F);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYaw()));
		for (int i = 0; i < SLASH_COUNT; i++) {
			float r1 = rand(seed, i, 11);
			float r2 = rand(seed, i, 23);
			float r3 = rand(seed, i, 37);
			float start = i / (float) SLASH_COUNT;
			float appear = smoothstep(start, start + 0.12F, reveal);
			if (appear <= 0.0F)
				continue;
			float linger = 1.0F - smoothstep(start + 0.42F, 1.0F, reveal) * 0.28F;
			float forward = -4.25F + i * 0.48F + (r1 - 0.5F) * 0.2F;
			float side = (r2 - 0.5F) * (2.65F + rand(seed, i, 31) * 0.7F);
			boolean nearVertical = rand(seed, i, 43) > 0.24F;
			boolean fortyFive = !nearVertical && rand(seed, i, 79) > 0.38F;
			float direction = rand(seed, i, 37) > 0.5F ? 1.0F : -1.0F;
			float roll = nearVertical ? direction * (58.0F + rand(seed, i, 53) * 22.0F) : fortyFive ? direction * (38.0F + rand(seed, i, 67) * 13.0F) : -22.0F + r3 * 44.0F;
			float width = nearVertical ? 1.35F + rand(seed, i, 61) * 1.0F : 1.9F + rand(seed, i, 61) * 1.2F;
			float height = 0.035F + rand(seed, i, 73) * 0.035F;
			float verticalReach = (float) (Math.abs(Math.sin(Math.toRadians(roll))) * width + Math.abs(Math.cos(Math.toRadians(roll))) * height);
			float up = nearVertical ? verticalReach - 1.12F + rand(seed, i, 47) * 0.22F : -0.05F + rand(seed, i, 47) * 1.45F;
			int alpha = Math.round((150.0F + rand(seed, i, 89) * 90.0F) * fade * appear * linger);
			poseStack.pushPose();
			poseStack.translate(side, up, forward);
			poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
			drawQuad(vertexConsumer, poseStack.last().pose(), poseStack.last().normal(), width, height, alpha);
			poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
			drawQuad(vertexConsumer, poseStack.last().pose(), poseStack.last().normal(), width * 0.7F, height * 0.9F, Math.round(alpha * 0.55F));
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
		float t = clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static void drawQuad(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, float halfWidth, float halfHeight, int alpha) {
		vertex(vertexConsumer, pose, normal, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha);
		vertex(vertexConsumer, pose, normal, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha);
		vertex(vertexConsumer, pose, normal, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha);
		vertex(vertexConsumer, pose, normal, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha);
	}

	private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v, int alpha) {
		vertexConsumer.vertex(pose, x, y, z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(DualWieldFlurryEntity entity) {
		return TEXTURE;
	}
}
