package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.IceChunkEntity;

import net.minecraft.resources.ResourceLocation;

public class IceChunkModel extends GeoModel<IceChunkEntity> {
	@Override
	public ResourceLocation getAnimationResource(IceChunkEntity entity) {
		return new ResourceLocation("sololeveling", "animations/icechunks.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(IceChunkEntity entity) {
		return new ResourceLocation("sololeveling", "geo/icechunks.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IceChunkEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
