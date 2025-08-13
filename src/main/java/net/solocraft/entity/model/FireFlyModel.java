package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FireFlyEntity;

import net.minecraft.resources.ResourceLocation;

public class FireFlyModel extends GeoModel<FireFlyEntity> {
	@Override
	public ResourceLocation getAnimationResource(FireFlyEntity entity) {
		return new ResourceLocation("sololeveling", "animations/fireflies.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FireFlyEntity entity) {
		return new ResourceLocation("sololeveling", "geo/fireflies.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FireFlyEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
