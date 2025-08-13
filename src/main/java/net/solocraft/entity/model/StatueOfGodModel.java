package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.StatueOfGodEntity;

import net.minecraft.resources.ResourceLocation;

public class StatueOfGodModel extends GeoModel<StatueOfGodEntity> {
	@Override
	public ResourceLocation getAnimationResource(StatueOfGodEntity entity) {
		return new ResourceLocation("sololeveling", "animations/statueofgod.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(StatueOfGodEntity entity) {
		return new ResourceLocation("sololeveling", "geo/statueofgod.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StatueOfGodEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
