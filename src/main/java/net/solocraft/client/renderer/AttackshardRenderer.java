
package net.solocraft.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.solocraft.entity.model.AttackshardModel;
import net.solocraft.entity.layer.AttackshardLayer;
import net.solocraft.entity.AttackshardEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class AttackshardRenderer extends GeoEntityRenderer<AttackshardEntity> {
	public AttackshardRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new AttackshardModel());
		this.shadowRadius = 0.5f;
		this.addRenderLayer(new AttackshardLayer(this));
	}

	@Override
	public RenderType getRenderType(AttackshardEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}

	@Override
	public void preRender(PoseStack poseStack, AttackshardEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green,
			float blue, float alpha) {
		float scale = 1.4f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
	}

	@Override
	protected float getDeathMaxRotation(AttackshardEntity entityLivingBaseIn) {
		return 0.0F;
	}
}
