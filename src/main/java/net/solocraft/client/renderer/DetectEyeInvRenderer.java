
package net.solocraft.client.renderer;

import net.solocraft.entity.DetectEyeInvEntity;
import net.solocraft.client.model.Modelinv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.mojang.blaze3d.vertex.PoseStack;

public class DetectEyeInvRenderer extends MobRenderer<DetectEyeInvEntity, Modelinv<DetectEyeInvEntity>> {
	public DetectEyeInvRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelinv(context.bakeLayer(Modelinv.LAYER_LOCATION)), 0.1f);
	}

	@Override
	protected void scale(DetectEyeInvEntity entity, PoseStack poseStack, float f) {
		poseStack.scale(0.1f, 0.1f, 0.1f);
	}

	@Override
	public ResourceLocation getTextureLocation(DetectEyeInvEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/invistext.png");
	}
}
