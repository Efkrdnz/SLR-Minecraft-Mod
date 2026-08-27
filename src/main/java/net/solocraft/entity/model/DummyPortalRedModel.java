package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DummyPortalRedEntity;

import net.minecraft.resources.ResourceLocation;

public class DummyPortalRedModel extends GeoModel<DummyPortalRedEntity> {
	@Override
	public ResourceLocation getAnimationResource(DummyPortalRedEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DummyPortalRedEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DummyPortalRedEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
