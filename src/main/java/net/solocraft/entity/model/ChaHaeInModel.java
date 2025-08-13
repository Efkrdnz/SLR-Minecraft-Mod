package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ChaHaeInEntity;

import net.minecraft.resources.ResourceLocation;

public class ChaHaeInModel extends GeoModel<ChaHaeInEntity> {
	@Override
	public ResourceLocation getAnimationResource(ChaHaeInEntity entity) {
		return new ResourceLocation("sololeveling", "animations/chahaeinmodel1.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ChaHaeInEntity entity) {
		return new ResourceLocation("sololeveling", "geo/chahaeinmodel1.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ChaHaeInEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
