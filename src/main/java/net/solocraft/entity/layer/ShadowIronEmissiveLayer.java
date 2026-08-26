package net.solocraft.entity.layer;

import net.solocraft.entity.ShadowIronEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ShadowIronEmissiveLayer extends GeoRenderLayer<ShadowIronEntity> {
	private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "textures/entities/iron_shadow_em.png");
	private static final ResourceLocation DOMAIN = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "textures/entities/iron_shadow_em_domain.png");

	public ShadowIronEmissiveLayer(GeoRenderer<ShadowIronEntity> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, ShadowIronEntity entity,
			BakedGeoModel bakedModel, RenderType renderType,
			MultiBufferSource bufferSource, VertexConsumer buffer,
			float partialTick, int packedLight, int packedOverlay) {
		ResourceLocation texture = entity.isDomainBoosted() ? DOMAIN : NORMAL;
		RenderType emissive = RenderType.eyes(texture);
		getRenderer().reRender(getDefaultBakedModel(entity), poseStack,
				bufferSource, entity, emissive, bufferSource.getBuffer(emissive),
				partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xEBFFFFFF);
	}
}
