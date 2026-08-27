package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SkeletonSummonerEntity;

import net.minecraft.resources.ResourceLocation;

public class SkeletonSummonerModel extends GeoModel<SkeletonSummonerEntity> {
	@Override
	public ResourceLocation getAnimationResource(SkeletonSummonerEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/nocsy_skeleton_necromancer.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SkeletonSummonerEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/nocsy_skeleton_necromancer.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonSummonerEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
