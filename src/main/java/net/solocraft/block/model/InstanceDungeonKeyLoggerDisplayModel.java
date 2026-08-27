package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.display.InstanceDungeonKeyLoggerDisplayItem;

import net.minecraft.resources.ResourceLocation;

public class InstanceDungeonKeyLoggerDisplayModel extends GeoModel<InstanceDungeonKeyLoggerDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(InstanceDungeonKeyLoggerDisplayItem animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/keyhole.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(InstanceDungeonKeyLoggerDisplayItem animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/keyhole.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(InstanceDungeonKeyLoggerDisplayItem entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/block/instancecover1.png");
	}
}
