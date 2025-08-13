package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FxspikEntity;

import net.minecraft.resources.ResourceLocation;

public class FxspikModel extends GeoModel<FxspikEntity> {
	@Override
	public ResourceLocation getAnimationResource(FxspikEntity entity) {
		return new ResourceLocation("sololeveling", "animations/fx_spike.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FxspikEntity entity) {
		return new ResourceLocation("sololeveling", "geo/fx_spike.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FxspikEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
