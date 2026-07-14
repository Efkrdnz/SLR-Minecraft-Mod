package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.BasicAttackSlashRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.BasicAttackSlashEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class BasicAttackSlashRenderer extends EntityRenderer<BasicAttackSlashEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashgood1.png");

	public BasicAttackSlashRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BasicAttackSlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		int style = entity.getStyle();
		float agePush = 1.0F + (1.0F - fade) * 0.18F;
		float scale = entity.getScale() * agePush;
		float halfWidth = widthFor(style) * scale;
		float halfHeight = heightFor(style) * scale;
		int alpha = Math.round(alphaFor(style) * fade);

		poseStack.pushPose();
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
		RenderType renderType = BasicAttackSlashRenderTypes.slash(style, TEXTURE);
		VertexConsumer vertexConsumer = DeferredWorldShaderRenderer.buffer(bufferSource, renderType, false);
		var pose = poseStack.last();
		if (style == BasicAttackSlashEntity.STYLE_DUAL_DAGGER) {
			drawQuad(vertexConsumer, pose, halfWidth, halfHeight, -26.0F, alpha);
			drawQuad(vertexConsumer, pose, halfWidth * 0.96F, halfHeight, 26.0F, alpha);
		} else {
			drawQuad(vertexConsumer, pose, halfWidth, halfHeight, 0.0F, alpha);
		}
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static float widthFor(int style) {
		return switch (style) {
			case BasicAttackSlashEntity.STYLE_FIST -> 0.82F;
			case BasicAttackSlashEntity.STYLE_DAGGER -> 1.75F;
			case BasicAttackSlashEntity.STYLE_DUAL_DAGGER -> 1.95F;
			default -> 2.65F;
		};
	}

	private static float heightFor(int style) {
		return switch (style) {
			case BasicAttackSlashEntity.STYLE_FIST -> 0.82F;
			case BasicAttackSlashEntity.STYLE_DAGGER -> 0.28F;
			case BasicAttackSlashEntity.STYLE_DUAL_DAGGER -> 0.26F;
			default -> 0.42F;
		};
	}

	private static int alphaFor(int style) {
		return style == BasicAttackSlashEntity.STYLE_FIST ? 210 : 235;
	}

	private static void drawQuad(VertexConsumer vertexConsumer, PoseStack.Pose pose, float halfWidth, float halfHeight, float roll, int alpha) {
		float sin = (float) Math.sin(Math.toRadians(roll));
		float cos = (float) Math.cos(Math.toRadians(roll));
		vertex(vertexConsumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha, sin, cos);
		vertex(vertexConsumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha, sin, cos);
		vertex(vertexConsumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha, sin, cos);
		vertex(vertexConsumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha, sin, cos);
	}

	private static void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int alpha, float sin, float cos) {
		float rx = x * cos - y * sin;
		float ry = x * sin + y * cos;
		vertexConsumer.vertex(pose.pose(), rx, ry, z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(BasicAttackSlashEntity entity) {
		return TEXTURE;
	}
}
