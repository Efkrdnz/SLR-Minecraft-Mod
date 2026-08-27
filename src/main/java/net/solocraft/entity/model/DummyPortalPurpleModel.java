package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DummyPortalPurpleEntity;

import net.minecraft.resources.ResourceLocation;

public class DummyPortalPurpleModel extends GeoModel<DummyPortalPurpleEntity> {
	@Override
	public ResourceLocation getAnimationResource(DummyPortalPurpleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DummyPortalPurpleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DummyPortalPurpleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
