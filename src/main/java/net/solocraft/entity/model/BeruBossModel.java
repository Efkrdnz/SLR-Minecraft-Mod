package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.BeruBossEntity;

import net.minecraft.resources.ResourceLocation;

public class BeruBossModel extends GeoModel<BeruBossEntity> {
	@Override
	public ResourceLocation getAnimationResource(BeruBossEntity entity) {
		return new ResourceLocation("sololeveling", "animations/beru_lucid.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BeruBossEntity entity) {
		return new ResourceLocation("sololeveling", "geo/beru_lucid.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BeruBossEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
