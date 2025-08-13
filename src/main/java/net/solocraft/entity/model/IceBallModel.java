package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.IceBallEntity;

import net.minecraft.resources.ResourceLocation;

public class IceBallModel extends GeoModel<IceBallEntity> {
	@Override
	public ResourceLocation getAnimationResource(IceBallEntity entity) {
		return new ResourceLocation("sololeveling", "animations/iceball.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(IceBallEntity entity) {
		return new ResourceLocation("sololeveling", "geo/iceball.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IceBallEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
