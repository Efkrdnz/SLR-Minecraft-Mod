package net.solocraft.entity.model;

import net.solocraft.entity.KaiselinEntity;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

public class KaiselinModel<T extends KaiselinEntity> extends GeoModel<T> {
	@Override
	public ResourceLocation getAnimationResource(T entity) {
		return new ResourceLocation("sololeveling", "animations/kaiselin.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(T entity) {
		return new ResourceLocation("sololeveling", "geo/kaiselin.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(T entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}
}
