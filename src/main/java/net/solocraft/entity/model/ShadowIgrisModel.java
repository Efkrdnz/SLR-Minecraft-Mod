package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ShadowIgrisEntity;

import net.minecraft.resources.ResourceLocation;

public class ShadowIgrisModel extends GeoModel<ShadowIgrisEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShadowIgrisEntity entity) {
		return new ResourceLocation("sololeveling", "animations/igris_prev.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShadowIgrisEntity entity) {
		return new ResourceLocation("sololeveling", "geo/igris_prev.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShadowIgrisEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
