package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.StatueaxeEntity;

import net.minecraft.resources.ResourceLocation;

public class StatueaxeModel extends GeoModel<StatueaxeEntity> {
	@Override
	public ResourceLocation getAnimationResource(StatueaxeEntity entity) {
		return new ResourceLocation("sololeveling", "animations/statue_axe.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(StatueaxeEntity entity) {
		return new ResourceLocation("sololeveling", "geo/statue_axe.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StatueaxeEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

}
