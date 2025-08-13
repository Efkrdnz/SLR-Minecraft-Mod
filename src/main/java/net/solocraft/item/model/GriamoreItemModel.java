package net.solocraft.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.item.GriamoreItem;

import net.minecraft.resources.ResourceLocation;

public class GriamoreItemModel extends GeoModel<GriamoreItem> {
	@Override
	public ResourceLocation getAnimationResource(GriamoreItem animatable) {
		return new ResourceLocation("sololeveling", "animations/griamore.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GriamoreItem animatable) {
		return new ResourceLocation("sololeveling", "geo/griamore.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GriamoreItem animatable) {
		return new ResourceLocation("sololeveling", "textures/item/griamore.png");
	}
}
