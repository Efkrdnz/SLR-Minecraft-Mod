package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SkeletonWarriorEntity;

import net.minecraft.resources.ResourceLocation;

public class SkeletonWarriorModel extends GeoModel<SkeletonWarriorEntity> {
	@Override
	public ResourceLocation getAnimationResource(SkeletonWarriorEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/nocsy_skeleton_warrior.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SkeletonWarriorEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/nocsy_skeleton_warrior.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SkeletonWarriorEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
