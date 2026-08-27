package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DragonheadEntity;

import net.minecraft.resources.ResourceLocation;

public class DragonheadModel extends GeoModel<DragonheadEntity> {
	@Override
	public ResourceLocation getAnimationResource(DragonheadEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/dragonhead.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DragonheadEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/dragonhead.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DragonheadEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
