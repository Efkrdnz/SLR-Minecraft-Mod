package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ShadowGreenOrcEntity;

import net.minecraft.resources.ResourceLocation;

public class ShadowGreenOrcModel extends GeoModel<ShadowGreenOrcEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShadowGreenOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShadowGreenOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShadowGreenOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
