package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.IgrisDeadBodyEntity;

import net.minecraft.resources.ResourceLocation;

public class IgrisDeadBodyModel extends GeoModel<IgrisDeadBodyEntity> {
	@Override
	public ResourceLocation getAnimationResource(IgrisDeadBodyEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/igrismarcus.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(IgrisDeadBodyEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/igrismarcus.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IgrisDeadBodyEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
