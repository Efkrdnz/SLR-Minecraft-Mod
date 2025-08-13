package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SteelFangWolfShadowEntity;

import net.minecraft.resources.ResourceLocation;

public class SteelFangWolfShadowModel extends GeoModel<SteelFangWolfShadowEntity> {
	@Override
	public ResourceLocation getAnimationResource(SteelFangWolfShadowEntity entity) {
		return new ResourceLocation("sololeveling", "animations/lycan.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SteelFangWolfShadowEntity entity) {
		return new ResourceLocation("sololeveling", "geo/lycan.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SteelFangWolfShadowEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
