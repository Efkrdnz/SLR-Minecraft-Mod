package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.entity.HunterRankEvaluatorTileEntity;

import net.minecraft.resources.ResourceLocation;

public class HunterRankEvaluatorBlockModel extends GeoModel<HunterRankEvaluatorTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(HunterRankEvaluatorTileEntity animatable) {
		return new ResourceLocation("sololeveling", "animations/evaluator.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(HunterRankEvaluatorTileEntity animatable) {
		return new ResourceLocation("sololeveling", "geo/evaluator.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(HunterRankEvaluatorTileEntity entity) {
		return new ResourceLocation("sololeveling", "textures/block/altartexture1.png");
	}
}
