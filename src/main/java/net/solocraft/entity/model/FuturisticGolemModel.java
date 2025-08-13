package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FuturisticGolemEntity;

import net.minecraft.resources.ResourceLocation;

public class FuturisticGolemModel extends GeoModel<FuturisticGolemEntity> {
	@Override
	public ResourceLocation getAnimationResource(FuturisticGolemEntity entity) {
		return new ResourceLocation("sololeveling", "animations/futuristicgolem.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FuturisticGolemEntity entity) {
		return new ResourceLocation("sololeveling", "geo/futuristicgolem.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FuturisticGolemEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
