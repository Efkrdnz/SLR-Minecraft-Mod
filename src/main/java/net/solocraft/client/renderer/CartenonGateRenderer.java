package net.solocraft.client.renderer;

import net.solocraft.entity.CartenonGateEntity;
import net.solocraft.entity.model.CartenonGateModel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CartenonGateRenderer extends GeoEntityRenderer<CartenonGateEntity> {
	public CartenonGateRenderer(EntityRendererProvider.Context context) {
		super(context, new CartenonGateModel());
		shadowRadius = 0.0F;
	}

	@Override
	public RenderType getRenderType(CartenonGateEntity entity, ResourceLocation texture,
			MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucentEmissive(texture);
	}

	@Override
	public void preRender(PoseStack poseStack, CartenonGateEntity entity, BakedGeoModel model,
			MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
			int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		this.scaleWidth = 1.2F;
		this.scaleHeight = 1.2F;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick,
				packedLight, packedOverlay, red, green, blue, alpha);
	}
}
