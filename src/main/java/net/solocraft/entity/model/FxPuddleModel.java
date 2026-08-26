package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FxPuddleEntity;

import net.minecraft.resources.ResourceLocation;

public class FxPuddleModel extends GeoModel<FxPuddleEntity> {
	@Override
	public ResourceLocation getAnimationResource(FxPuddleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/fx_puddle.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FxPuddleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/fx_puddle.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FxPuddleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
