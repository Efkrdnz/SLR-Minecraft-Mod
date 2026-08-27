package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ShadowIgrisEntity;

import net.minecraft.resources.ResourceLocation;

public class ShadowIgrisModel extends GeoModel<ShadowIgrisEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShadowIgrisEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/igris_prev.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShadowIgrisEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/igris_prev.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShadowIgrisEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
