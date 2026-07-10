package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.OrcEntity;

import net.minecraft.resources.ResourceLocation;

public class OrcModel extends GeoModel<OrcEntity> {
	@Override
	public ResourceLocation getAnimationResource(OrcEntity entity) {
		return new ResourceLocation("sololeveling", "animations/greenorc.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(OrcEntity entity) {
		return new ResourceLocation("sololeveling", "geo/greenorc.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(OrcEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
