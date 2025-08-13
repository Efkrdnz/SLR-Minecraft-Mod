package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FlameVortexEntity;

import net.minecraft.resources.ResourceLocation;

public class FlameVortexModel extends GeoModel<FlameVortexEntity> {
	@Override
	public ResourceLocation getAnimationResource(FlameVortexEntity entity) {
		return new ResourceLocation("sololeveling", "animations/flamefield.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FlameVortexEntity entity) {
		return new ResourceLocation("sololeveling", "geo/flamefield.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FlameVortexEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
