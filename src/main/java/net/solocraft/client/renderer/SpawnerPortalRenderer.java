
package net.solocraft.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.solocraft.entity.model.SpawnerPortalModel;
import net.solocraft.entity.SpawnerPortalEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class SpawnerPortalRenderer extends GeoEntityRenderer<SpawnerPortalEntity> {
	public SpawnerPortalRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new SpawnerPortalModel());
		this.shadowRadius = 0.5f;
	}

	@Override
	public RenderType getRenderType(SpawnerPortalEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucentEmissive(getTextureLocation(animatable));
	}

	@Override
	public void preRender(PoseStack poseStack, SpawnerPortalEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green,
			float blue, float alpha) {
		float scale = 2.4f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
