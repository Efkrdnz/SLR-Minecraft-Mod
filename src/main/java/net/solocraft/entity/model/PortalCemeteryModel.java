package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PortalCemeteryEntity;

import net.minecraft.resources.ResourceLocation;

public class PortalCemeteryModel extends GeoModel<PortalCemeteryEntity> {
	@Override
	public ResourceLocation getAnimationResource(PortalCemeteryEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PortalCemeteryEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PortalCemeteryEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
