
package net.solocraft.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.solocraft.entity.model.OrcShadowModel;
import net.solocraft.entity.layer.OrcShadowLayer;
import net.solocraft.entity.OrcShadowEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class OrcShadowRenderer extends GeoEntityRenderer<OrcShadowEntity> {
	public OrcShadowRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new OrcShadowModel());
		this.shadowRadius = 0.7f;
		this.addRenderLayer(new OrcShadowLayer(this));
	}

	@Override
	public RenderType getRenderType(OrcShadowEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}

	@Override
	public void preRender(PoseStack poseStack, OrcShadowEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green,
			float blue, float alpha) {
		float scale = 1.1f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
