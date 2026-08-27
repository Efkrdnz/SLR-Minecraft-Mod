package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.display.DungeonWallDisplayItem;

import net.minecraft.resources.ResourceLocation;

public class DungeonWallDisplayModel extends GeoModel<DungeonWallDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(DungeonWallDisplayItem animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/dungeon_wall.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(DungeonWallDisplayItem animatable) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/dungeon_wall.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DungeonWallDisplayItem entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/block/dungeon_wall.png");
	}
}
