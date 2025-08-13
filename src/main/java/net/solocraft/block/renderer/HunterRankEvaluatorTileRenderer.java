package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.solocraft.block.model.HunterRankEvaluatorBlockModel;
import net.solocraft.block.entity.HunterRankEvaluatorTileEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class HunterRankEvaluatorTileRenderer extends GeoBlockRenderer<HunterRankEvaluatorTileEntity> {
	public HunterRankEvaluatorTileRenderer() {
		super(new HunterRankEvaluatorBlockModel());
	}

	@Override
	public RenderType getRenderType(HunterRankEvaluatorTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
