package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.KasakaEntity;

import net.minecraft.resources.ResourceLocation;

public class KasakaModel extends GeoModel<KasakaEntity> {
	@Override
	public ResourceLocation getAnimationResource(KasakaEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/serpent.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KasakaEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/serpent.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KasakaEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
