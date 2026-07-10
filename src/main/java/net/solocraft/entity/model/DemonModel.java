package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DemonEntity;

import net.minecraft.resources.ResourceLocation;

public class DemonModel extends GeoModel<DemonEntity> {
	@Override
	public ResourceLocation getAnimationResource(DemonEntity entity) {
		return new ResourceLocation("sololeveling", "animations/dkc_demon.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DemonEntity entity) {
		return new ResourceLocation("sololeveling", "geo/dkc_demon.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DemonEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
