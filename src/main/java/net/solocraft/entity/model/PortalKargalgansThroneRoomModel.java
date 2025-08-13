package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.PortalKargalgansThroneRoomEntity;

import net.minecraft.resources.ResourceLocation;

public class PortalKargalgansThroneRoomModel extends GeoModel<PortalKargalgansThroneRoomEntity> {
	@Override
	public ResourceLocation getAnimationResource(PortalKargalgansThroneRoomEntity entity) {
		return new ResourceLocation("sololeveling", "animations/portalgate.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PortalKargalgansThroneRoomEntity entity) {
		return new ResourceLocation("sololeveling", "geo/portalgate.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PortalKargalgansThroneRoomEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
