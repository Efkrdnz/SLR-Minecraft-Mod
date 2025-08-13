package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.MiniGemGolemEntity;

import net.minecraft.resources.ResourceLocation;

public class MiniGemGolemModel extends GeoModel<MiniGemGolemEntity> {
	@Override
	public ResourceLocation getAnimationResource(MiniGemGolemEntity entity) {
		return new ResourceLocation("sololeveling", "animations/dungeonmons1.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MiniGemGolemEntity entity) {
		return new ResourceLocation("sololeveling", "geo/dungeonmons1.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MiniGemGolemEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
