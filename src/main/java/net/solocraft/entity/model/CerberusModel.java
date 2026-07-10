package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.CerberusEntity;

import net.minecraft.resources.ResourceLocation;

public class CerberusModel extends GeoModel<CerberusEntity> {
	@Override
	public ResourceLocation getAnimationResource(CerberusEntity entity) {
		return new ResourceLocation("sololeveling", "animations/cerberus.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(CerberusEntity entity) {
		return new ResourceLocation("sololeveling", "geo/cerberus.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(CerberusEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
