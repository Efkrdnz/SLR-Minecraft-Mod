package net.solocraft.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.item.KangsDaggerItem;

import net.minecraft.resources.ResourceLocation;

public class KangsDaggerItemModel extends GeoModel<KangsDaggerItem> {
	@Override
	public ResourceLocation getAnimationResource(KangsDaggerItem animatable) {
		return new ResourceLocation("sololeveling", "animations/taeshikdagger_-_converted.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KangsDaggerItem animatable) {
		return new ResourceLocation("sololeveling", "geo/taeshikdagger_-_converted.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KangsDaggerItem animatable) {
		return new ResourceLocation("sololeveling", "textures/item/taeshikdagger.png");
	}
}
