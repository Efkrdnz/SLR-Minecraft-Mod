package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.CrossStrikeRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.CrossStrikeEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class CrossStrikeRenderer extends EntityRenderer<CrossStrikeEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashgood1.png");

	public CrossStrikeRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(CrossStrikeEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		float second = entity.getSecondSlashProgress(partialTick);
		float scale = entity.getScale();
		float halfWidth = 4.35F * scale;
		float halfHeight = 0.24F * scale;
		RenderType renderType = CrossStrikeRenderTypes.cross(TEXTURE);
		VertexConsumer vertexConsumer = DeferredWorldShaderRenderer.buffer(bufferSource, renderType, false);
		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		drawSlash(vertexConsumer, poseStack, -43.0F, halfWidth, halfHeight, Math.round(235.0F * fade));
		if (second > 0.0F) {
			drawSlash(vertexConsumer, poseStack, 43.0F, halfWidth * (0.88F + second * 0.12F), halfHeight, Math.round(235.0F * fade * second));
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void drawSlash(VertexConsumer vertexConsumer, PoseStack poseStack, float roll, float halfWidth, float halfHeight, int alpha) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
		var pose = poseStack.last();
		vertex(vertexConsumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha);
		vertex(vertexConsumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha);
		vertex(vertexConsumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha);
		vertex(vertexConsumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha);
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int alpha) {
		vertexConsumer.vertex(pose.pose(), x, y, z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(CrossStrikeEntity entity) {
		return TEXTURE;
	}
}
