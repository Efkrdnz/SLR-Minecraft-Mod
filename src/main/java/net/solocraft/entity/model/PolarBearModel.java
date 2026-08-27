package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PolarBearEntity;

import net.minecraft.resources.ResourceLocation;

public class PolarBearModel extends GeoModel<PolarBearEntity> {
	@Override
	public ResourceLocation getAnimationResource(PolarBearEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/polarbear.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PolarBearEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/polarbear.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PolarBearEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
