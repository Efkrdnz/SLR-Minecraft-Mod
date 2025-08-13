package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FlagOfProtectionEntity;

import net.minecraft.resources.ResourceLocation;

public class FlagOfProtectionModel extends GeoModel<FlagOfProtectionEntity> {
	@Override
	public ResourceLocation getAnimationResource(FlagOfProtectionEntity entity) {
		return new ResourceLocation("sololeveling", "animations/flagofprotection.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FlagOfProtectionEntity entity) {
		return new ResourceLocation("sololeveling", "geo/flagofprotection.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FlagOfProtectionEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
