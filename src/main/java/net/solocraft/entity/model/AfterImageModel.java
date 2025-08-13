package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.AfterImageEntity;

import net.minecraft.resources.ResourceLocation;

public class AfterImageModel extends GeoModel<AfterImageEntity> {
	@Override
	public ResourceLocation getAnimationResource(AfterImageEntity entity) {
		return new ResourceLocation("sololeveling", "animations/afterimage.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AfterImageEntity entity) {
		return new ResourceLocation("sololeveling", "geo/afterimage.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AfterImageEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
