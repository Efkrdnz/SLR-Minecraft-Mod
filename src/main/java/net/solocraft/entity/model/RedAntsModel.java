package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.RedAntsEntity;

import net.minecraft.resources.ResourceLocation;

public class RedAntsModel extends GeoModel<RedAntsEntity> {
	@Override
	public ResourceLocation getAnimationResource(RedAntsEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/jejuantnormal.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RedAntsEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/jejuantnormal.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RedAntsEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
