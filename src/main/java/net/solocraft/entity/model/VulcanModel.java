package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.VulcanEntity;

import net.minecraft.resources.ResourceLocation;

public class VulcanModel extends GeoModel<VulcanEntity> {
	@Override
	public ResourceLocation getAnimationResource(VulcanEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/vulcan.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(VulcanEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/vulcan.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(VulcanEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
