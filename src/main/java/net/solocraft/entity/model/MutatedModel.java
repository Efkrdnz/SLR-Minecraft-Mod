package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.MutatedEntity;

import net.minecraft.resources.ResourceLocation;

public class MutatedModel extends GeoModel<MutatedEntity> {
	@Override
	public ResourceLocation getAnimationResource(MutatedEntity entity) {
		return new ResourceLocation("sololeveling", "animations/mutated.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MutatedEntity entity) {
		return new ResourceLocation("sololeveling", "geo/mutated.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MutatedEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
