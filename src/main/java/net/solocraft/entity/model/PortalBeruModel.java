package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PortalBeruEntity;

import net.minecraft.resources.ResourceLocation;

public class PortalBeruModel extends GeoModel<PortalBeruEntity> {
	@Override
	public ResourceLocation getAnimationResource(PortalBeruEntity entity) {
		return new ResourceLocation("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PortalBeruEntity entity) {
		return new ResourceLocation("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PortalBeruEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
