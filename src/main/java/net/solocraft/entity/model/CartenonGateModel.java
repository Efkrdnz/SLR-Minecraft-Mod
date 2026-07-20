package net.solocraft.entity.model;

import net.solocraft.entity.CartenonGateEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class CartenonGateModel extends GeoModel<CartenonGateEntity> {
	private static final ResourceLocation MODEL = new ResourceLocation("sololeveling", "geo/portalgate.geo.json");
	private static final ResourceLocation ANIMATION = new ResourceLocation("sololeveling", "animations/portalgate.animation.json");
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling", "textures/entities/portalgate2.png");

	@Override
	public ResourceLocation getModelResource(CartenonGateEntity entity) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(CartenonGateEntity entity) {
		return TEXTURE;
	}

	@Override
	public ResourceLocation getAnimationResource(CartenonGateEntity entity) {
		return ANIMATION;
	}
}
