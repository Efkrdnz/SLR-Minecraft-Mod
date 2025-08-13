package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.solocraft.block.model.InstanceCoverBlockModel;
import net.solocraft.block.entity.InstanceCoverTileEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class InstanceCoverTileRenderer extends GeoBlockRenderer<InstanceCoverTileEntity> {
	public InstanceCoverTileRenderer() {
		super(new InstanceCoverBlockModel());
	}

	@Override
	public RenderType getRenderType(InstanceCoverTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
