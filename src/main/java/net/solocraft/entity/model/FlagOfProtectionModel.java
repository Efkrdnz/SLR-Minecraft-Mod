package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FlagOfProtectionEntity;

import net.minecraft.resources.ResourceLocation;

public class FlagOfProtectionModel extends GeoModel<FlagOfProtectionEntity> {
	@Override
	public ResourceLocation getAnimationResource(FlagOfProtectionEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/flagofprotection.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FlagOfProtectionEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/flagofprotection.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FlagOfProtectionEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
