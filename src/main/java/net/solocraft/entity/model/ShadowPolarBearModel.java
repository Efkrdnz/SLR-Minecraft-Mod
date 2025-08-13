package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ShadowPolarBearEntity;

import net.minecraft.resources.ResourceLocation;

public class ShadowPolarBearModel extends GeoModel<ShadowPolarBearEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShadowPolarBearEntity entity) {
		return new ResourceLocation("sololeveling", "animations/polarbear.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShadowPolarBearEntity entity) {
		return new ResourceLocation("sololeveling", "geo/polarbear.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShadowPolarBearEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
