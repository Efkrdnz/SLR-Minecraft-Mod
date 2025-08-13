package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SteelFangWolfEntity;

import net.minecraft.resources.ResourceLocation;

public class SteelFangWolfModel extends GeoModel<SteelFangWolfEntity> {
	@Override
	public ResourceLocation getAnimationResource(SteelFangWolfEntity entity) {
		return new ResourceLocation("sololeveling", "animations/lycan.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SteelFangWolfEntity entity) {
		return new ResourceLocation("sololeveling", "geo/lycan.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SteelFangWolfEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
