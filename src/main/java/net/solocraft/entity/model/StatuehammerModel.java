package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.StatuehammerEntity;

import net.minecraft.resources.ResourceLocation;

public class StatuehammerModel extends GeoModel<StatuehammerEntity> {
	@Override
	public ResourceLocation getAnimationResource(StatuehammerEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/statue_hammer.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(StatuehammerEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/statue_hammer.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StatuehammerEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
