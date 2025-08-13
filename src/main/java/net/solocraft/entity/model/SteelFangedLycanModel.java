package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SteelFangedLycanEntity;

import net.minecraft.resources.ResourceLocation;

public class SteelFangedLycanModel extends GeoModel<SteelFangedLycanEntity> {
	@Override
	public ResourceLocation getAnimationResource(SteelFangedLycanEntity entity) {
		return new ResourceLocation("sololeveling", "animations/lycan_normal.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SteelFangedLycanEntity entity) {
		return new ResourceLocation("sololeveling", "geo/lycan_normal.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SteelFangedLycanEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
