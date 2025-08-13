package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.display.InstanceCoverDisplayItem;

import net.minecraft.resources.ResourceLocation;

public class InstanceCoverDisplayModel extends GeoModel<InstanceCoverDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(InstanceCoverDisplayItem animatable) {
		return new ResourceLocation("sololeveling", "animations/instancecover.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(InstanceCoverDisplayItem animatable) {
		return new ResourceLocation("sololeveling", "geo/instancecover.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(InstanceCoverDisplayItem entity) {
		return new ResourceLocation("sololeveling", "textures/block/instancecover1.png");
	}
}
