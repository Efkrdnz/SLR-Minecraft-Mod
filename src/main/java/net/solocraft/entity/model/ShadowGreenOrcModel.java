package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ShadowGreenOrcEntity;

import net.minecraft.resources.ResourceLocation;

public class ShadowGreenOrcModel extends GeoModel<ShadowGreenOrcEntity> {
	@Override
	public ResourceLocation getAnimationResource(ShadowGreenOrcEntity entity) {
		return new ResourceLocation("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ShadowGreenOrcEntity entity) {
		return new ResourceLocation("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ShadowGreenOrcEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
