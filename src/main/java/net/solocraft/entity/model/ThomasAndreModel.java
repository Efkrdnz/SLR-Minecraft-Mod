package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ThomasAndreEntity;

import net.minecraft.resources.ResourceLocation;

public class ThomasAndreModel extends GeoModel<ThomasAndreEntity> {
	@Override
	public ResourceLocation getAnimationResource(ThomasAndreEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/thomass.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ThomasAndreEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/thomass.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ThomasAndreEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
