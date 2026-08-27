package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.IceElfEntity;

import net.minecraft.resources.ResourceLocation;

public class IceElfModel extends GeoModel<IceElfEntity> {
	@Override
	public ResourceLocation getAnimationResource(IceElfEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/iceelf.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(IceElfEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/iceelf.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IceElfEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
