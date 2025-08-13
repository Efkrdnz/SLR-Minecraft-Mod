package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FangedKasakaEntity;

import net.minecraft.resources.ResourceLocation;

public class FangedKasakaModel extends GeoModel<FangedKasakaEntity> {
	@Override
	public ResourceLocation getAnimationResource(FangedKasakaEntity entity) {
		return new ResourceLocation("sololeveling", "animations/fanged_kasaka.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FangedKasakaEntity entity) {
		return new ResourceLocation("sololeveling", "geo/fanged_kasaka.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FangedKasakaEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
