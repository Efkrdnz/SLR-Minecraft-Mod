package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.entity.InstanceCoverTileEntity;

import net.minecraft.resources.ResourceLocation;

public class InstanceCoverBlockModel extends GeoModel<InstanceCoverTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(InstanceCoverTileEntity animatable) {
		return new ResourceLocation("sololeveling", "animations/instancecover.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(InstanceCoverTileEntity animatable) {
		return new ResourceLocation("sololeveling", "geo/instancecover.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(InstanceCoverTileEntity entity) {
		return new ResourceLocation("sololeveling", "textures/block/instancecover1.png");
	}
}
