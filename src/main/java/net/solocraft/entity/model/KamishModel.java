package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.KamishEntity;

import net.minecraft.resources.ResourceLocation;

public class KamishModel extends GeoModel<KamishEntity> {
	@Override
	public ResourceLocation getAnimationResource(KamishEntity entity) {
		return new ResourceLocation("sololeveling", "animations/dragon.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KamishEntity entity) {
		return new ResourceLocation("sololeveling", "geo/dragon.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KamishEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
