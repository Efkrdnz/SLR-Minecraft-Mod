package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.GemGolemEntity;

import net.minecraft.resources.ResourceLocation;

public class GemGolemModel extends GeoModel<GemGolemEntity> {
	@Override
	public ResourceLocation getAnimationResource(GemGolemEntity entity) {
		return new ResourceLocation("sololeveling", "animations/gemgolem.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GemGolemEntity entity) {
		return new ResourceLocation("sololeveling", "geo/gemgolem.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GemGolemEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
