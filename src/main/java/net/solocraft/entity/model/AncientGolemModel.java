package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.AncientGolemEntity;

import net.minecraft.resources.ResourceLocation;

public class AncientGolemModel extends GeoModel<AncientGolemEntity> {
	@Override
	public ResourceLocation getAnimationResource(AncientGolemEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/ancientgolem.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(AncientGolemEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/ancientgolem.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(AncientGolemEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
