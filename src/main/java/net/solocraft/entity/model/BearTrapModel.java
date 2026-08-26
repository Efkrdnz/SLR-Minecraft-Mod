package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.BearTrapEntity;

import net.minecraft.resources.ResourceLocation;

public class BearTrapModel extends GeoModel<BearTrapEntity> {
	@Override
	public ResourceLocation getAnimationResource(BearTrapEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/beartrap.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BearTrapEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/beartrap.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BearTrapEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
