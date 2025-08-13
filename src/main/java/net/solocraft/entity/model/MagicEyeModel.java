package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.MagicEyeEntity;

import net.minecraft.resources.ResourceLocation;

public class MagicEyeModel extends GeoModel<MagicEyeEntity> {
	@Override
	public ResourceLocation getAnimationResource(MagicEyeEntity entity) {
		return new ResourceLocation("sololeveling", "animations/detecteye.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MagicEyeEntity entity) {
		return new ResourceLocation("sololeveling", "geo/detecteye.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MagicEyeEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
