package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.GreenOrcEntity;

import net.minecraft.resources.ResourceLocation;

public class GreenOrcModel extends GeoModel<GreenOrcEntity> {
	@Override
	public ResourceLocation getAnimationResource(GreenOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GreenOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GreenOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
