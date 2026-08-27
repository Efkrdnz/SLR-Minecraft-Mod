package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.BellOfHealingEntity;

import net.minecraft.resources.ResourceLocation;

public class BellOfHealingModel extends GeoModel<BellOfHealingEntity> {
	@Override
	public ResourceLocation getAnimationResource(BellOfHealingEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/bellofhealing.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BellOfHealingEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/bellofhealing.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BellOfHealingEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
