package net.solocraft.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.solocraft.block.model.DungeonWallBlockModel;
import net.solocraft.block.entity.DungeonWallTileEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

public class DungeonWallTileRenderer extends GeoBlockRenderer<DungeonWallTileEntity> {
	public DungeonWallTileRenderer() {
		super(new DungeonWallBlockModel());
	}

	@Override
	public RenderType getRenderType(DungeonWallTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
