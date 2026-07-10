package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.OrcShadowEntity;

import net.minecraft.resources.ResourceLocation;

public class OrcShadowModel extends GeoModel<OrcShadowEntity> {
	@Override
	public ResourceLocation getAnimationResource(OrcShadowEntity entity) {
		return new ResourceLocation("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(OrcShadowEntity entity) {
		return new ResourceLocation("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(OrcShadowEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
