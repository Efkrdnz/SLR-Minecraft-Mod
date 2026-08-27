package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.ElderBeastEntity;

import net.minecraft.resources.ResourceLocation;

public class ElderBeastModel extends GeoModel<ElderBeastEntity> {
	@Override
	public ResourceLocation getAnimationResource(ElderBeastEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/elder_beast.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ElderBeastEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/elder_beast.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ElderBeastEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
