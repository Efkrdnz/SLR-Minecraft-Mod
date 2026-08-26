package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.StatueswordEntity;

import net.minecraft.resources.ResourceLocation;

public class StatueswordModel extends GeoModel<StatueswordEntity> {
	@Override
	public ResourceLocation getAnimationResource(StatueswordEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/statue_sword.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(StatueswordEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/statue_sword.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StatueswordEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
