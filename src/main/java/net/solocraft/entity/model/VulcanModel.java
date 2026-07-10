package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.VulcanEntity;

import net.minecraft.resources.ResourceLocation;

public class VulcanModel extends GeoModel<VulcanEntity> {
	@Override
	public ResourceLocation getAnimationResource(VulcanEntity entity) {
		return new ResourceLocation("sololeveling", "animations/vulcan.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(VulcanEntity entity) {
		return new ResourceLocation("sololeveling", "geo/vulcan.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(VulcanEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
