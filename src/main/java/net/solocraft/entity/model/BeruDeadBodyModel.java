package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.BeruDeadBodyEntity;

import net.minecraft.resources.ResourceLocation;

public class BeruDeadBodyModel extends GeoModel<BeruDeadBodyEntity> {
	@Override
	public ResourceLocation getAnimationResource(BeruDeadBodyEntity entity) {
		return new ResourceLocation("sololeveling", "animations/beru_final.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BeruDeadBodyEntity entity) {
		return new ResourceLocation("sololeveling", "geo/beru_final.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BeruDeadBodyEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
