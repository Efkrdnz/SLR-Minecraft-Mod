package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.CursedChainsEntity;

import net.minecraft.resources.ResourceLocation;

public class CursedChainsModel extends GeoModel<CursedChainsEntity> {
	@Override
	public ResourceLocation getAnimationResource(CursedChainsEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/curse_chains.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(CursedChainsEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/curse_chains.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(CursedChainsEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
