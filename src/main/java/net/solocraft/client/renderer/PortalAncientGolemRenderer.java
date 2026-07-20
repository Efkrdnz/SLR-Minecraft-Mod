
package net.solocraft.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.solocraft.entity.model.PortalAncientGolemModel;
import net.solocraft.entity.PortalAncientGolemEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class PortalAncientGolemRenderer extends GeoEntityRenderer<PortalAncientGolemEntity> {
	public PortalAncientGolemRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new PortalAncientGolemModel());
		this.shadowRadius = 0.5f;
	}

	@Override
	public RenderType getRenderType(PortalAncientGolemEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucentEmissive(getTextureLocation(animatable));
	}

	@Override
	public void preRender(PoseStack poseStack, PortalAncientGolemEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red,
			float green, float blue, float alpha) {
		float scale = 1.5f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	protected float getDeathMaxRotation(PortalAncientGolemEntity entityLivingBaseIn) {
		return 0.0F;
	}
}
