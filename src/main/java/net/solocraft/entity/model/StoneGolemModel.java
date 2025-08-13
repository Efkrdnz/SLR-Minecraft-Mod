package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.StoneGolemEntity;

import net.minecraft.resources.ResourceLocation;

public class StoneGolemModel extends GeoModel<StoneGolemEntity> {
	@Override
	public ResourceLocation getAnimationResource(StoneGolemEntity entity) {
		return new ResourceLocation("sololeveling", "animations/bossgolem.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(StoneGolemEntity entity) {
		return new ResourceLocation("sololeveling", "geo/bossgolem.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StoneGolemEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
