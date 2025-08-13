package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.AfterImage1Entity;

import net.minecraft.resources.ResourceLocation;

public class AfterImage1Model extends GeoModel<AfterImage1Entity> {
	@Override
	public ResourceLocation getAnimationResource(AfterImage1Entity entity) {
		return new ResourceLocation("sololeveling", "animations/afterimage1.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AfterImage1Entity entity) {
		return new ResourceLocation("sololeveling", "geo/afterimage1.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AfterImage1Entity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
