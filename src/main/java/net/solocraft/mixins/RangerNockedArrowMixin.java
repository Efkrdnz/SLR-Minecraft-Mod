package net.solocraft.mixins;

import net.solocraft.client.renderer.RangerManaQuiverHandRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks after the actual item model has been submitted but before its resolved
 * pose is popped, so custom/resource-pack bow transforms and depth are present.
 */
@Mixin(ItemRenderer.class)
public abstract class RangerNockedArrowMixin {
	@Inject(method = "render",
			at = @At(value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
					ordinal = 0, shift = At.Shift.BEFORE))
	private void sololeveling$renderNockedManaArrow(ItemStack stack, ItemDisplayContext context,
			boolean leftHand, PoseStack poseStack, MultiBufferSource buffers,
			int packedLight, int packedOverlay, BakedModel model, CallbackInfo callback) {
		RangerManaQuiverHandRenderer.renderNockedArrow(
				stack, context, leftHand, poseStack, buffers);
	}
}
