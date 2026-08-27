package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.entity.InstanceDungeonKeyLoggerTileEntity;

import net.minecraft.resources.ResourceLocation;

public class InstanceDungeonKeyLoggerBlockModel extends GeoModel<InstanceDungeonKeyLoggerTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(InstanceDungeonKeyLoggerTileEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/keyhole.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(InstanceDungeonKeyLoggerTileEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/keyhole.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(InstanceDungeonKeyLoggerTileEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/block/instancecover1.png");
	}
}
