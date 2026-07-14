package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.LiuSwordRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.LiuSwordBeamEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class LiuSwordBeamRenderer extends EntityRenderer<LiuSwordBeamEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling", "textures/particle/slashgood1.png");

	public LiuSwordBeamRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(LiuSwordBeamEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getRenderFade(partialTick);
		if (fade <= 0.001F)
			return;
		VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(buffers,
				LiuSwordRenderTypes.effect(FALLBACK));
		int tier = entity.getTier();
		float width = entity.getBeamWidth() * entity.getVisualScale();
		float height = width * (tier >= 3
				? (entity.isDual() ? 0.35F : 0.29F)
				: (entity.isDual() ? 0.44F : 0.34F));
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

		if (entity.isDual()) {
			drawBranch(poseStack, vertices, width, height, 18.0F, entity.getPrimaryColor(), fade, tier, 0.0F);
			drawBranch(poseStack, vertices, width, height, -18.0F, entity.getSecondaryColor(), fade, tier, -0.035F);
		} else {
			drawBranch(poseStack, vertices, width, height, 0.0F, entity.getPrimaryColor(), fade, tier, 0.0F);
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private static void drawBranch(PoseStack stack, VertexConsumer vertices, float width, float height,
			float roll, int color, float fade, int tier, float zOffset) {
		stack.pushPose();
		stack.translate(0.0D, 0.0D, zOffset);
		stack.mulPose(Axis.ZP.rotationDegrees(roll));
		int trailCount = tier >= 3 ? 1 : 2;
		for (int trail = trailCount; trail >= 1; trail--) {
			stack.pushPose();
			stack.translate(0.0D, 0.0D, -trail * (0.24D + tier * 0.055D));
			float trailScale = 1.0F + trail * 0.055F;
			drawPlane(vertices, stack.last(), width * trailScale, height * (1.0F + trail * 0.09F),
					2.0F, color, alpha((72.0F - trail * 10.0F) * fade));
			stack.popPose();
		}
		drawPlane(vertices, stack.last(), width * 1.13F, height * 1.45F, 1.0F,
				color, alpha(105.0F * fade));
		drawPlane(vertices, stack.last(), width, height, 0.0F, color, alpha(235.0F * fade));
		drawPlane(vertices, stack.last(), width * 0.88F, height * 0.46F, 0.0F,
				lighten(color, 0.72F), alpha(245.0F * fade));
		stack.popPose();
	}

	public static void drawPlane(VertexConsumer out, PoseStack.Pose pose, float halfWidth, float halfHeight,
			float kind, int color, int alpha) {
		vertex(out, pose, -halfWidth, -halfHeight, 0.0F, kind, 1.0F, color, alpha);
		vertex(out, pose, halfWidth, -halfHeight, 0.0F, kind + 1.0F, 1.0F, color, alpha);
		vertex(out, pose, halfWidth, halfHeight, 0.0F, kind + 1.0F, 0.0F, color, alpha);
		vertex(out, pose, -halfWidth, halfHeight, 0.0F, kind, 0.0F, color, alpha);
	}

	public static void drawHorizontal(VertexConsumer out, PoseStack.Pose pose, float radius, float kind,
			int color, int alpha) {
		vertex(out, pose, -radius, 0.02F, -radius, kind, 1.0F, color, alpha);
		vertex(out, pose, -radius, 0.02F, radius, kind + 1.0F, 0.0F, color, alpha);
		vertex(out, pose, radius, 0.02F, radius, kind + 1.0F, 1.0F, color, alpha);
		vertex(out, pose, radius, 0.02F, -radius, kind, 0.0F, color, alpha);
	}

	public static void drawHorizontalRing(VertexConsumer out, PoseStack.Pose pose, float outerRadius,
			float innerRadius, float kind, int color, int alpha, int segments) {
		int count = Math.max(12, segments);
		float inner = Math.max(0.0F, Math.min(outerRadius - 0.01F, innerRadius));
		for (int segment = 0; segment < count; segment++) {
			double angle0 = Math.PI * 2.0D * segment / count;
			double angle1 = Math.PI * 2.0D * (segment + 1) / count;
			float outerX0 = (float) Math.cos(angle0) * outerRadius;
			float outerZ0 = (float) Math.sin(angle0) * outerRadius;
			float outerX1 = (float) Math.cos(angle1) * outerRadius;
			float outerZ1 = (float) Math.sin(angle1) * outerRadius;
			float innerX0 = (float) Math.cos(angle0) * inner;
			float innerZ0 = (float) Math.sin(angle0) * inner;
			float innerX1 = (float) Math.cos(angle1) * inner;
			float innerZ1 = (float) Math.sin(angle1) * inner;
			ringVertex(out, pose, outerX0, outerZ0, outerRadius, kind, color, alpha);
			ringVertex(out, pose, outerX1, outerZ1, outerRadius, kind, color, alpha);
			ringVertex(out, pose, innerX1, innerZ1, outerRadius, kind, color, alpha);
			ringVertex(out, pose, innerX0, innerZ0, outerRadius, kind, color, alpha);
		}
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int color, int alpha) {
		out.vertex(pose.pose(), x, y, z)
				.color((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
				.uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
				.normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
	}

	private static void ringVertex(VertexConsumer out, PoseStack.Pose pose, float x, float z,
			float radius, float kind, int color, int alpha) {
		float u = kind + 0.5F + 0.499F * x / radius;
		float v = 0.5F + 0.499F * z / radius;
		out.vertex(pose.pose(), x, 0.02F, z)
				.color((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
				.uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
				.normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
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
		return Math.max(0, Math.min(255, Math.round(value)));
	}

	@Override
	public ResourceLocation getTextureLocation(LiuSwordBeamEntity entity) {
		return FALLBACK;
	}
}
