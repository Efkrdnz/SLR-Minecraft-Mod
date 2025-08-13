
package net.solocraft.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.solocraft.entity.model.StatueOfGodModel;
import net.solocraft.entity.layer.StatueOfGodLayer;
import net.solocraft.entity.StatueOfGodEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class StatueOfGodRenderer extends GeoEntityRenderer<StatueOfGodEntity> {
	public StatueOfGodRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new StatueOfGodModel());
		this.shadowRadius = 1f;
		this.addRenderLayer(new StatueOfGodLayer(this));
	}

	@Override
	public RenderType getRenderType(StatueOfGodEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}

	@Override
	public void preRender(PoseStack poseStack, StatueOfGodEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green,
			float blue, float alpha) {
		float scale = 3f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	protected float getDeathMaxRotation(StatueOfGodEntity entityLivingBaseIn) {
		return 0.0F;
	}
}
