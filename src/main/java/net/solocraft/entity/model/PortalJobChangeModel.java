package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PortalJobChangeEntity;

import net.minecraft.resources.ResourceLocation;

public class PortalJobChangeModel extends GeoModel<PortalJobChangeEntity> {
	@Override
	public ResourceLocation getAnimationResource(PortalJobChangeEntity entity) {
		return new ResourceLocation("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PortalJobChangeEntity entity) {
		return new ResourceLocation("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PortalJobChangeEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
