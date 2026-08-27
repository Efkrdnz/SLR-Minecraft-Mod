package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.display.InstanceCoverDisplayItem;

import net.minecraft.resources.ResourceLocation;

public class InstanceCoverDisplayModel extends GeoModel<InstanceCoverDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(InstanceCoverDisplayItem animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/instancecover.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(InstanceCoverDisplayItem animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/instancecover.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(InstanceCoverDisplayItem entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/block/instancecover1.png");
	}
}
