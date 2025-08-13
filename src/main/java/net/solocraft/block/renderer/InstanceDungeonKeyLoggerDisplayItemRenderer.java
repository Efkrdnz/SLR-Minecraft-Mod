package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.solocraft.block.model.InstanceDungeonKeyLoggerDisplayModel;
import net.solocraft.block.display.InstanceDungeonKeyLoggerDisplayItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class InstanceDungeonKeyLoggerDisplayItemRenderer extends GeoItemRenderer<InstanceDungeonKeyLoggerDisplayItem> {
	public InstanceDungeonKeyLoggerDisplayItemRenderer() {
		super(new InstanceDungeonKeyLoggerDisplayModel());
	}

	@Override
	public RenderType getRenderType(InstanceDungeonKeyLoggerDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
