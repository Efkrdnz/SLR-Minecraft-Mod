package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.BeruShadowEntity;

import net.minecraft.resources.ResourceLocation;

public class BeruShadowModel extends GeoModel<BeruShadowEntity> {
	@Override
	public ResourceLocation getAnimationResource(BeruShadowEntity entity) {
		return new ResourceLocation("sololeveling", "animations/beru_lucid.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BeruShadowEntity entity) {
		return new ResourceLocation("sololeveling", "geo/beru_lucid.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BeruShadowEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
