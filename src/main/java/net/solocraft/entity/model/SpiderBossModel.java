package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SpiderBossEntity;

import net.minecraft.resources.ResourceLocation;

public class SpiderBossModel extends GeoModel<SpiderBossEntity> {
	@Override
	public ResourceLocation getAnimationResource(SpiderBossEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/spiderboss.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SpiderBossEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/spiderboss.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SpiderBossEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
