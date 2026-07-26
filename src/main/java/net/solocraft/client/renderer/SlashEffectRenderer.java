package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.SlashEffectRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.SlashEffectEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class SlashEffectRenderer extends EntityRenderer<SlashEffectEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashfury.png");
	private static final ResourceLocation ASSASSIN_TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashgood1.png");

	public SlashEffectRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SlashEffectEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		boolean assassin = entity.getVariant() >= 100;
		float scale = entity.getScale() * (1.0F + (1.0F - fade) * 0.25F);
		float distanceProgress = assassin ? 0.0F : Math.min(entity.getVariant(), 14) / 14.0F;
		float lengthScale = 1.18F + distanceProgress * 0.95F;
		float halfWidth = (assassin ? 1.75F : 2.35F) * scale * lengthScale;
		float halfHeight = (assassin ? 0.13F : 0.26F) * scale * (1.0F + distanceProgress * 0.05F);
		int variant = Math.floorMod(entity.getVariant(), 4);
		float tintR = assassin ? 225.0F : switch (variant) {
			case 1 -> 255.0F;
			case 2 -> 255.0F;
			case 3 -> 180.0F;
			default -> 255.0F;
		};
		float tintG = assassin ? (variant == 3 ? 205.0F : 242.0F) : switch (variant) {
			case 1 -> 128.0F;
			case 2 -> 42.0F;
			case 3 -> 48.0F;
			default -> 58.0F;
		};
		float tintB = assassin ? 255.0F : switch (variant) {
			case 1 -> 28.0F;
			case 2 -> 220.0F;
			case 3 -> 255.0F;
			default -> 36.0F;
		};

		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
		VertexConsumer vertexConsumer = DeferredWorldShaderRenderer.buffer(bufferSource,
				SlashEffectRenderTypes.slash(assassin ? ASSASSIN_TEXTURE : TEXTURE));
		var pose = poseStack.last();
		int glowAlpha = Math.round((assassin ? 30.0F : 48.0F) * fade);
		int coreAlpha = Math.round((assassin ? 255.0F : 235.0F) * fade);
		float glowWidth = assassin ? 1.04F : 1.08F;
		float glowHeight = assassin ? 1.55F : 1.28F;
		vertex(vertexConsumer, pose, -halfWidth * glowWidth, -halfHeight * glowHeight, -0.01F, 0.0F, 1.0F, tintR, tintG, tintB, glowAlpha);
		vertex(vertexConsumer, pose, halfWidth * glowWidth, -halfHeight * glowHeight, -0.01F, 1.0F, 1.0F, tintR, tintG, tintB, glowAlpha);
		vertex(vertexConsumer, pose, halfWidth * glowWidth, halfHeight * glowHeight, -0.01F, 1.0F, 0.0F, tintR, tintG, tintB, glowAlpha);
		vertex(vertexConsumer, pose, -halfWidth * glowWidth, halfHeight * glowHeight, -0.01F, 0.0F, 0.0F, tintR, tintG, tintB, glowAlpha);
		vertex(vertexConsumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, tintR, tintG, tintB, coreAlpha);
		vertex(vertexConsumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, tintR, tintG, tintB, coreAlpha);
		vertex(vertexConsumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, tintR, tintG, tintB, coreAlpha);
		vertex(vertexConsumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, tintR, tintG, tintB, coreAlpha);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float red, float green, float blue, int alpha) {
		vertexConsumer.vertex(pose.pose(), x, y, z).color((int) red, (int) green, (int) blue, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(SlashEffectEntity entity) {
		return TEXTURE;
	}
}
