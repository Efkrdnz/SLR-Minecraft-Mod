package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.BarukaEntity;

import net.minecraft.resources.ResourceLocation;

public class BarukaModel extends GeoModel<BarukaEntity> {
	@Override
	public ResourceLocation getAnimationResource(BarukaEntity entity) {
		return new ResourceLocation("sololeveling", "animations/baruka.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BarukaEntity entity) {
		return new ResourceLocation("sololeveling", "geo/baruka.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BarukaEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
