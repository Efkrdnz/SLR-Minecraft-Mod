package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FireFlyEntity;

import net.minecraft.resources.ResourceLocation;

public class FireFlyModel extends GeoModel<FireFlyEntity> {
	@Override
	public ResourceLocation getAnimationResource(FireFlyEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/fireflies.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FireFlyEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/fireflies.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FireFlyEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
