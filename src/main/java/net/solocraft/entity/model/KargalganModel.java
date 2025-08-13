package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.KargalganEntity;

import net.minecraft.resources.ResourceLocation;

public class KargalganModel extends GeoModel<KargalganEntity> {
	@Override
	public ResourceLocation getAnimationResource(KargalganEntity entity) {
		return new ResourceLocation("sololeveling", "animations/kardalgan_boss.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KargalganEntity entity) {
		return new ResourceLocation("sololeveling", "geo/kardalgan_boss.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KargalganEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
