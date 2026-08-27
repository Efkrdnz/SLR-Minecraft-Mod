package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.Portal1Entity;

import net.minecraft.resources.ResourceLocation;

public class Portal1Model extends GeoModel<Portal1Entity> {
	@Override
	public ResourceLocation getAnimationResource(Portal1Entity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(Portal1Entity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(Portal1Entity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
