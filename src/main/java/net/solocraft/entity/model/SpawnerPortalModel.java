package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.SpawnerPortalEntity;

import net.minecraft.resources.ResourceLocation;

public class SpawnerPortalModel extends GeoModel<SpawnerPortalEntity> {
	@Override
	public ResourceLocation getAnimationResource(SpawnerPortalEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/spawnerportal.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SpawnerPortalEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/spawnerportal.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SpawnerPortalEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
