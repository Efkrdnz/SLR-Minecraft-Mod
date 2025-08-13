package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.solocraft.block.model.InstanceDungeonKeyLoggerBlockModel;
import net.solocraft.block.entity.InstanceDungeonKeyLoggerTileEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class InstanceDungeonKeyLoggerTileRenderer extends GeoBlockRenderer<InstanceDungeonKeyLoggerTileEntity> {
	public InstanceDungeonKeyLoggerTileRenderer() {
		super(new InstanceDungeonKeyLoggerBlockModel());
	}

	@Override
	public RenderType getRenderType(InstanceDungeonKeyLoggerTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
