package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ShadowHighOrcEntity;

import net.minecraft.resources.ResourceLocation;

public class ShadowHighOrcModel extends GeoModel<ShadowHighOrcEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShadowHighOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShadowHighOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShadowHighOrcEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
