package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.HighOrcEntity;

import net.minecraft.resources.ResourceLocation;

public class HighOrcModel extends GeoModel<HighOrcEntity> {
	@Override
	public ResourceLocation getAnimationResource(HighOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(HighOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(HighOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
