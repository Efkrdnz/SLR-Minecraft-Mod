package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.AncientSamuraiEntity;

import net.minecraft.resources.ResourceLocation;

public class AncientSamuraiModel extends GeoModel<AncientSamuraiEntity> {
	@Override
	public ResourceLocation getAnimationResource(AncientSamuraiEntity entity) {
		return new ResourceLocation("sololeveling", "animations/ancientsamurai.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AncientSamuraiEntity entity) {
		return new ResourceLocation("sololeveling", "geo/ancientsamurai.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AncientSamuraiEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
