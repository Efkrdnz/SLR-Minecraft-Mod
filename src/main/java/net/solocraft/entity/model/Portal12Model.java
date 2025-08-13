package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.Portal12Entity;

import net.minecraft.resources.ResourceLocation;

public class Portal12Model extends GeoModel<Portal12Entity> {
	@Override
	public ResourceLocation getAnimationResource(Portal12Entity entity) {
		return new ResourceLocation("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(Portal12Entity entity) {
		return new ResourceLocation("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(Portal12Entity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
