package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PortalSEntity;

import net.minecraft.resources.ResourceLocation;

public class PortalSModel extends GeoModel<PortalSEntity> {
	@Override
	public ResourceLocation getAnimationResource(PortalSEntity entity) {
		return new ResourceLocation("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PortalSEntity entity) {
		return new ResourceLocation("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PortalSEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
