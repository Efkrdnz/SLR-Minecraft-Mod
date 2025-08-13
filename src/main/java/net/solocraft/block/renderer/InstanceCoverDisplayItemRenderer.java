package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.solocraft.block.model.InstanceCoverDisplayModel;
import net.solocraft.block.display.InstanceCoverDisplayItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class InstanceCoverDisplayItemRenderer extends GeoItemRenderer<InstanceCoverDisplayItem> {
	public InstanceCoverDisplayItemRenderer() {
		super(new InstanceCoverDisplayModel());
	}

	@Override
	public RenderType getRenderType(InstanceCoverDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
