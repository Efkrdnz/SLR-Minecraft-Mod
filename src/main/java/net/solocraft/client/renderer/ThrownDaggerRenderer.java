package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.client.renderer.shader.LiuSwordRenderTypes;
import net.solocraft.entity.ThrownDaggerEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/** Renders the real held item as a spinning blade plus a shader-driven violet/blue motion ribbon. */
public class ThrownDaggerRenderer extends EntityRenderer<ThrownDaggerEntity> {
	private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/particle/slashgood1.png");
	private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

	public ThrownDaggerRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(ThrownDaggerEntity entity, float yaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight) {
		Vec3 motion = entity.getDeltaMovement();
		if (motion.lengthSqr() > 0.002D) {
			poseStack.pushPose();
			float motionYaw = (float) Math.toDegrees(Math.atan2(motion.x, motion.z));
			float motionPitch = (float) -Math.toDegrees(Math.atan2(motion.y,
					Math.sqrt(motion.x * motion.x + motion.z * motion.z)));
			poseStack.mulPose(Axis.YP.rotationDegrees(motionYaw));
			poseStack.mulPose(Axis.XP.rotationDegrees(motionPitch));
			VertexConsumer trail = DeferredWorldShaderRenderer.buffer(buffers, LiuSwordRenderTypes.effect(TRAIL_TEXTURE));
			float length = entity.isSpectral() ? 3.8F : 2.8F;
			float width = entity.isSpectral() ? 0.34F : 0.25F;
			drawRibbon(trail, poseStack.last(), width, length, 0x765CFF, entity.isReturning() ? 155 : 205);
			poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
			drawRibbon(trail, poseStack.last(), width * 0.68F, length * 0.9F, 0x4DCEFF, entity.isReturning() ? 110 : 155);
			poseStack.popPose();
		}

		poseStack.pushPose();
		if (motion.lengthSqr() > 0.002D) {
			float motionYaw = (float) Math.toDegrees(Math.atan2(motion.x, motion.z));
			float motionPitch = (float) -Math.toDegrees(Math.atan2(motion.y,
					Math.sqrt(motion.x * motion.x + motion.z * motion.z)));
			poseStack.mulPose(Axis.YP.rotationDegrees(motionYaw));
			poseStack.mulPose(Axis.XP.rotationDegrees(motionPitch));
		}
		poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * (entity.isSpectral() ? 46.0F : 34.0F)));
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		poseStack.scale(1.15F, 1.15F, 1.15F);
		if (isLocalOwner(entity)) {
			VertexConsumer glow = DeferredWorldShaderRenderer.buffer(buffers, LiuSwordRenderTypes.effect(TRAIL_TEXTURE));
			drawOwnerGlow(glow, poseStack.last(), entity.tickCount + partialTick, entity.isSpectral());
		}
		itemRenderer.renderStatic(entity.getDaggerStack(), ItemDisplayContext.GROUND, 240,
				OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
	}

	private static boolean isLocalOwner(ThrownDaggerEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && entity.getOwnerId() != null
				&& minecraft.player.getUUID().equals(entity.getOwnerId());
	}

	private static void drawOwnerGlow(VertexConsumer out, PoseStack.Pose pose, float age, boolean spectral) {
		float pulse = 1.0F + (float) Math.sin(age * 0.24F) * 0.08F;
		float halfWidth = (spectral ? 0.72F : 0.58F) * pulse;
		float halfHeight = (spectral ? 0.26F : 0.22F) * pulse;
		int alpha = spectral ? 92 : 108;
		drawGlowQuad(out, pose, halfWidth, halfHeight, 0x7FE8FF, alpha);
		drawGlowQuad(out, pose, halfHeight * 0.76F, halfWidth * 0.76F, 0x8F5CFF, alpha * 2 / 3);
	}

	private static void drawGlowQuad(VertexConsumer out, PoseStack.Pose pose, float halfWidth, float halfHeight,
			int color, int alpha) {
		vertex(out, pose, -halfWidth, -halfHeight, -0.035F, 0.0F, 1.0F, color, alpha);
		vertex(out, pose, halfWidth, -halfHeight, -0.035F, 1.0F, 1.0F, color, alpha);
		vertex(out, pose, halfWidth, halfHeight, -0.035F, 1.0F, 0.0F, color, 0);
		vertex(out, pose, -halfWidth, halfHeight, -0.035F, 0.0F, 0.0F, color, 0);
	}

	private static void drawRibbon(VertexConsumer out, PoseStack.Pose pose, float halfWidth, float length,
			int color, int alpha) {
		vertex(out, pose, -halfWidth, 0.0F, 0.15F, 2.0F, 0.0F, color, alpha);
		vertex(out, pose, halfWidth, 0.0F, 0.15F, 3.0F, 0.0F, color, alpha);
		vertex(out, pose, halfWidth * 0.08F, 0.0F, -length, 3.0F, 1.0F, color, 0);
		vertex(out, pose, -halfWidth * 0.08F, 0.0F, -length, 2.0F, 1.0F, color, 0);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, int color, int alpha) {
		out.addVertex(pose, x, y, z)
				.setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
				.setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(240)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(ThrownDaggerEntity entity) {
		return TRAIL_TEXTURE;
	}
}
