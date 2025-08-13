
package net.solocraft.client.renderer;

import net.solocraft.entity.IcecleEntity;
import net.solocraft.client.model.Modelicecle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.mojang.blaze3d.vertex.PoseStack;

public class IcecleRenderer extends MobRenderer<IcecleEntity, Modelicecle<IcecleEntity>> {
	public IcecleRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelicecle(context.bakeLayer(Modelicecle.LAYER_LOCATION)), 0.5f);
	}

	@Override
	protected void scale(IcecleEntity entity, PoseStack poseStack, float f) {
		poseStack.scale(2f, 2f, 2f);
	}

	@Override
	public ResourceLocation getTextureLocation(IcecleEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/icecle.png");
	}
}
