package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DummyPortalNormalEntity;

import net.minecraft.resources.ResourceLocation;

public class DummyPortalNormalModel extends GeoModel<DummyPortalNormalEntity> {
	@Override
	public ResourceLocation getAnimationResource(DummyPortalNormalEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DummyPortalNormalEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DummyPortalNormalEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
