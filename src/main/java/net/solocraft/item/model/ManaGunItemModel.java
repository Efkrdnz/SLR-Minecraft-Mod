package net.solocraft.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.item.ManaGunItem;

import net.minecraft.resources.ResourceLocation;

public class ManaGunItemModel extends GeoModel<ManaGunItem> {
	@Override
	public ResourceLocation getAnimationResource(ManaGunItem animatable) {
		return new ResourceLocation("sololeveling", "animations/magicgun.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(ManaGunItem animatable) {
		return new ResourceLocation("sololeveling", "geo/magicgun.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(ManaGunItem animatable) {
		return new ResourceLocation("sololeveling", "textures/item/managuntex.png");
	}
}
