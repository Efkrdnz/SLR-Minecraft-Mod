package net.solocraft.client.renderer;

import net.solocraft.entity.ShadowIronEntity;
import net.solocraft.entity.layer.ShadowIronEmissiveLayer;
import net.solocraft.entity.model.ShadowIronModel;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ShadowIronRenderer extends GeoEntityRenderer<ShadowIronEntity> {
	public ShadowIronRenderer(EntityRendererProvider.Context context) {
		super(context, new ShadowIronModel());
		this.shadowRadius = 0.62F;
		addRenderLayer(new ShadowIronEmissiveLayer(this));
	}

	@Override
	public RenderType getRenderType(ShadowIronEntity entity,
			ResourceLocation texture, MultiBufferSource bufferSource,
			float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(entity));
	}

	@Override
	public void preRender(PoseStack poseStack, ShadowIronEntity entity,
			BakedGeoModel model, MultiBufferSource bufferSource,
			VertexConsumer buffer, boolean isReRender, float partialTick,
			int packedLight, int packedOverlay, int colour) {
		this.scaleWidth = 1.0F;
		this.scaleHeight = 1.0F;
		super.preRender(poseStack, entity, model, bufferSource, buffer,
				isReRender, partialTick, packedLight, packedOverlay, colour);
	}
}
