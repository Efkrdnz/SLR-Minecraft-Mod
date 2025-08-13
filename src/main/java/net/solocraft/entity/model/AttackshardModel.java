package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.AttackshardEntity;

import net.minecraft.resources.ResourceLocation;

public class AttackshardModel extends GeoModel<AttackshardEntity> {
	@Override
	public ResourceLocation getAnimationResource(AttackshardEntity entity) {
		return new ResourceLocation("sololeveling", "animations/attackcrystal.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AttackshardEntity entity) {
		return new ResourceLocation("sololeveling", "geo/attackcrystal.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AttackshardEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
