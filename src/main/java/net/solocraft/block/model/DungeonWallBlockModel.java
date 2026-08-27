package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.entity.DungeonWallTileEntity;

import net.minecraft.resources.ResourceLocation;

public class DungeonWallBlockModel extends GeoModel<DungeonWallTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(DungeonWallTileEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/dungeon_wall.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DungeonWallTileEntity animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/dungeon_wall.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DungeonWallTileEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/block/dungeon_wall.png");
	}
}
