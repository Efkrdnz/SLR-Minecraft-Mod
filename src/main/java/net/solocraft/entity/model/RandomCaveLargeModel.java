package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.RandomCaveLargeEntity;

import net.minecraft.resources.ResourceLocation;

public class RandomCaveLargeModel extends GeoModel<RandomCaveLargeEntity> {
	@Override
	public ResourceLocation getAnimationResource(RandomCaveLargeEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RandomCaveLargeEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RandomCaveLargeEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
