package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.KamishShadowEntity;

import net.minecraft.resources.ResourceLocation;

public class KamishShadowModel extends GeoModel<KamishShadowEntity> {
	@Override
	public ResourceLocation getAnimationResource(KamishShadowEntity entity) {
		return new ResourceLocation("sololeveling", "animations/dragon.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KamishShadowEntity entity) {
		return new ResourceLocation("sololeveling", "geo/dragon.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KamishShadowEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
