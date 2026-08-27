package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.IgrisShadowEntity;

import net.minecraft.resources.ResourceLocation;

public class IgrisShadowModel extends GeoModel<IgrisShadowEntity> {
	@Override
	public ResourceLocation getAnimationResource(IgrisShadowEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/igrismarcus.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(IgrisShadowEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/igrismarcus.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IgrisShadowEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
