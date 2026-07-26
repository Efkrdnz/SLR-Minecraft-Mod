package net.solocraft.client.renderer;

import net.solocraft.entity.ManaArrowEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Lightweight code-native mana-arrow renderer shared visually with the held
 * Mana Quiver arrow. It intentionally has no ribbon, history, or particle
 * trail.
 */
public class ManaArrowRenderer extends EntityRenderer<ManaArrowEntity> {
	public ManaArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(ManaArrowEntity entity, float entityYaw, float partialTicks,
			PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(
				Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(
				Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

		int stage = Math.max(1, entity.getRangerStage());
		float pulse = 0.92F + Mth.sin((entity.tickCount + partialTicks) * 0.26F) * 0.08F;
		ManaArrowVisual.render(poseStack, buffers,
				new Vec3(-0.48D, 0.0D, 0.0D), new Vec3(0.52D, 0.0D, 0.0D),
				stage, stage >= 3, pulse,
				0.018D, 0.18D, 0.065D, 0.20D, 0.060D);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTicks, poseStack, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(ManaArrowEntity entity) {
		return ManaArrowVisual.WHITE_TEXTURE;
	}
}
