package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ArrowSplashEntity;

import net.minecraft.resources.ResourceLocation;

public class ArrowSplashModel extends GeoModel<ArrowSplashEntity> {
	@Override
	public ResourceLocation getAnimationResource(ArrowSplashEntity entity) {
		return new ResourceLocation("sololeveling", "animations/arrowsplash.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ArrowSplashEntity entity) {
		return new ResourceLocation("sololeveling", "geo/arrowsplash.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ArrowSplashEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
