package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SkeletonBruteEntity;

import net.minecraft.resources.ResourceLocation;

public class SkeletonBruteModel extends GeoModel<SkeletonBruteEntity> {
	@Override
	public ResourceLocation getAnimationResource(SkeletonBruteEntity entity) {
		return new ResourceLocation("sololeveling", "animations/nocsy_skeleton_brute1.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SkeletonBruteEntity entity) {
		return new ResourceLocation("sololeveling", "geo/nocsy_skeleton_brute1.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonBruteEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
