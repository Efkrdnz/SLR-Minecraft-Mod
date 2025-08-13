
package net.solocraft.client.renderer;

import net.solocraft.entity.ShadowSoulEntity;
import net.solocraft.client.model.Modelshadowsoul;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.mojang.blaze3d.vertex.PoseStack;

public class ShadowSoulRenderer extends MobRenderer<ShadowSoulEntity, Modelshadowsoul<ShadowSoulEntity>> {
	public ShadowSoulRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelshadowsoul(context.bakeLayer(Modelshadowsoul.LAYER_LOCATION)), 0.5f);
	}

	@Override
	protected void scale(ShadowSoulEntity entity, PoseStack poseStack, float f) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(ShadowSoulEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/soultext.png");
	}

	@Override
	protected boolean isBodyVisible(ShadowSoulEntity entity) {
		return false;
	}

	@Override
	protected boolean isShaking(ShadowSoulEntity entity) {
		return true;
	}
}
