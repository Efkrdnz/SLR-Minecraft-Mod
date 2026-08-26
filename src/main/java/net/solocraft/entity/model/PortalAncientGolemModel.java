package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PortalAncientGolemEntity;

import net.minecraft.resources.ResourceLocation;

public class PortalAncientGolemModel extends GeoModel<PortalAncientGolemEntity> {
	@Override
	public ResourceLocation getAnimationResource(PortalAncientGolemEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PortalAncientGolemEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PortalAncientGolemEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
