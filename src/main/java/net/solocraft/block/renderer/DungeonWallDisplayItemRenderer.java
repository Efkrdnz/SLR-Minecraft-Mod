package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.solocraft.block.model.DungeonWallDisplayModel;
import net.solocraft.block.display.DungeonWallDisplayItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class DungeonWallDisplayItemRenderer extends GeoItemRenderer<DungeonWallDisplayItem> {
	public DungeonWallDisplayItemRenderer() {
		super(new DungeonWallDisplayModel());
	}

	@Override
	public RenderType getRenderType(DungeonWallDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
