package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.solocraft.block.model.HunterRankEvaluatorDisplayModel;
import net.solocraft.block.display.HunterRankEvaluatorDisplayItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class HunterRankEvaluatorDisplayItemRenderer extends GeoItemRenderer<HunterRankEvaluatorDisplayItem> {
	public HunterRankEvaluatorDisplayItemRenderer() {
		super(new HunterRankEvaluatorDisplayModel());
	}

	@Override
	public RenderType getRenderType(HunterRankEvaluatorDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
