package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DaggerSlashEntity;

import net.minecraft.resources.ResourceLocation;

public class DaggerSlashModel extends GeoModel<DaggerSlashEntity> {
	@Override
	public ResourceLocation getAnimationResource(DaggerSlashEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/daggerslash.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DaggerSlashEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/daggerslash.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DaggerSlashEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
