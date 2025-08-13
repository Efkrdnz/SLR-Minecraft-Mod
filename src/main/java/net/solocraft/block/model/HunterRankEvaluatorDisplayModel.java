package net.solocraft.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.solocraft.block.display.HunterRankEvaluatorDisplayItem;

import net.minecraft.resources.ResourceLocation;

public class HunterRankEvaluatorDisplayModel extends GeoModel<HunterRankEvaluatorDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(HunterRankEvaluatorDisplayItem animatable) {
		return new ResourceLocation("sololeveling", "animations/evaluator.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(HunterRankEvaluatorDisplayItem animatable) {
		return new ResourceLocation("sololeveling", "geo/evaluator.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(HunterRankEvaluatorDisplayItem entity) {
		return new ResourceLocation("sololeveling", "textures/block/altartexture1.png");
	}
}
