package net.solocraft.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.entity.DemonEntity;

import net.minecraft.resources.ResourceLocation;

public class DemonModel extends GeoModel<DemonEntity> {
	private static final ResourceLocation BROAD_ANIMATION = new ResourceLocation("sololeveling", "animations/el_demon.animation.json");
	private static final ResourceLocation THIN_ANIMATION = new ResourceLocation("sololeveling", "animations/el_demon_thin.animation.json");
	private static final ResourceLocation BROAD_MODEL = new ResourceLocation("sololeveling", "geo/el_demon.geo.json");
	private static final ResourceLocation THIN_MODEL = new ResourceLocation("sololeveling", "geo/el_demon_thin.geo.json");

	@Override
	public ResourceLocation getAnimationResource(DemonEntity entity) {
		return entity.isThinVariant() ? THIN_ANIMATION : BROAD_ANIMATION;
	}

	@Override
	public ResourceLocation getModelResource(DemonEntity entity) {
		return entity.isThinVariant() ? THIN_MODEL : BROAD_MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(DemonEntity entity) {
		String body = entity.isThinVariant() ? "el_demon_thin_texture" : "el_demon_texture";
		String suffix = entity.getTextureVariant() == 0 ? "" : Integer.toString(entity.getTextureVariant());
		return new ResourceLocation("sololeveling", "textures/entities/" + body + suffix + ".png");
	}

}
