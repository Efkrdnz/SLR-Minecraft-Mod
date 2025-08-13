package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.AfterImage2Entity;

import net.minecraft.resources.ResourceLocation;

public class AfterImage2Model extends GeoModel<AfterImage2Entity> {
	@Override
	public ResourceLocation getAnimationResource(AfterImage2Entity entity) {
		return new ResourceLocation("sololeveling", "animations/afterimage2.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AfterImage2Entity entity) {
		return new ResourceLocation("sololeveling", "geo/afterimage2.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AfterImage2Entity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
