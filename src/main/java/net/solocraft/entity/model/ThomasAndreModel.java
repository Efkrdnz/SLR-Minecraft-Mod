package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ThomasAndreEntity;

import net.minecraft.resources.ResourceLocation;

public class ThomasAndreModel extends GeoModel<ThomasAndreEntity> {
	@Override
	public ResourceLocation getAnimationResource(ThomasAndreEntity entity) {
		return new ResourceLocation("sololeveling", "animations/thomass.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ThomasAndreEntity entity) {
		return new ResourceLocation("sololeveling", "geo/thomass.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ThomasAndreEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
