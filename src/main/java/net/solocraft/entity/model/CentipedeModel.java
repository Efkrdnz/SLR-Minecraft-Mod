package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.CentipedeEntity;

import net.minecraft.resources.ResourceLocation;

public class CentipedeModel extends GeoModel<CentipedeEntity> {
	@Override
	public ResourceLocation getAnimationResource(CentipedeEntity entity) {
		return new ResourceLocation("sololeveling", "animations/centipede.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(CentipedeEntity entity) {
		return new ResourceLocation("sololeveling", "geo/centipede.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(CentipedeEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
