package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SlasheffectswordEntity;

import net.minecraft.resources.ResourceLocation;

public class SlasheffectswordModel extends GeoModel<SlasheffectswordEntity> {
	@Override
	public ResourceLocation getAnimationResource(SlasheffectswordEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/slasheffectlatest.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SlasheffectswordEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/slasheffectlatest.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SlasheffectswordEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
