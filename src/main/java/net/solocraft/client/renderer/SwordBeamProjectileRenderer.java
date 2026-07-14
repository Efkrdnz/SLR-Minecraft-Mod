package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.SwordBeamProjectileRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.SwordBeamProjectileEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class SwordBeamProjectileRenderer extends EntityRenderer<SwordBeamProjectileEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/slashgood1.png");

	public SwordBeamProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SwordBeamProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.0F)
			return;
		float age = entity.tickCount + partialTick;
		float pulse = 1.0F + Mth.sin(age * 0.55F) * 0.045F;
		float halfWidth = 2.75F * pulse;
		float halfHeight = 0.34F * pulse;
		int alpha = Math.round(245.0F * fade);
		RenderType renderType = SwordBeamProjectileRenderTypes.projectile(TEXTURE);
		VertexConsumer vertexConsumer = DeferredWorldShaderRenderer.buffer(bufferSource, renderType);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F + Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll() + Mth.sin(age * 0.27F) * 3.0F));
		var pose = poseStack.last();
		vertex(vertexConsumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha);
		vertex(vertexConsumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha);
		vertex(vertexConsumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha);
		vertex(vertexConsumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int alpha) {
		vertexConsumer.vertex(pose.pose(), x, y, z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(SwordBeamProjectileEntity entity) {
		return TEXTURE;
	}
}
