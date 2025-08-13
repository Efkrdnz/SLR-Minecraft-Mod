package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.FireTornadoEntity;

import net.minecraft.resources.ResourceLocation;

public class FireTornadoModel extends GeoModel<FireTornadoEntity> {
	@Override
	public ResourceLocation getAnimationResource(FireTornadoEntity entity) {
		return new ResourceLocation("sololeveling", "animations/flame_tornado.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FireTornadoEntity entity) {
		return new ResourceLocation("sololeveling", "geo/flame_tornado.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FireTornadoEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
