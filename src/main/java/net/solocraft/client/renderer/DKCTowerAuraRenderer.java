package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.DKCTowerAuraRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.DKCTowerAuraEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DKCTowerAuraRenderer extends EntityRenderer<DKCTowerAuraEntity> {
	private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("sololeveling:textures/particle/mana_red.png");
	private static final int SEGMENTS = 48;
	private static final int BANDS = 18;

	public DKCTowerAuraRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(DKCTowerAuraEntity aura, float entityYaw, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight) {
		float intensity = aura.getIntensity();
		if (intensity <= 0.001F)
			return;

		float time = (aura.level().getGameTime() % 240000L) + partialTick;
		RenderType renderType = DKCTowerAuraRenderTypes.towerAura();
		VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(bufferSource, renderType, true);
		PoseStack.Pose pose = poseStack.last();

		drawShell(vertices, pose, aura.getAuraRadius(), aura.getAuraHeight(), time,
				1.0F, Mth.clamp(intensity, 0.0F, 2.0F), 0.0F);
		drawShell(vertices, pose, aura.getAuraRadius(), aura.getAuraHeight(), time,
				1.055F, Mth.clamp(intensity * 0.42F, 0.0F, 1.0F), 0.173F);
		super.render(aura, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	private static void drawShell(VertexConsumer vertices, PoseStack.Pose pose, float baseRadius,
			float height, float time, float radiusScale, float alphaScale, float uvPhase) {
		for (int band = 0; band < BANDS; band++) {
			for (int segment = 0; segment < SEGMENTS; segment++) {
				vertex(vertices, pose, baseRadius, height, time, segment, band,
						radiusScale, alphaScale, uvPhase);
				vertex(vertices, pose, baseRadius, height, time, segment, band + 1,
						radiusScale, alphaScale, uvPhase);
				vertex(vertices, pose, baseRadius, height, time, segment + 1, band + 1,
						radiusScale, alphaScale, uvPhase);
				vertex(vertices, pose, baseRadius, height, time, segment + 1, band,
						radiusScale, alphaScale, uvPhase);
			}
		}
	}

	private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float baseRadius,
			float height, float time, int segment, int band, float radiusScale,
			float alphaScale, float uvPhase) {
		float angle = Mth.TWO_PI * segment / SEGMENTS;
		float heightFraction = band / (float) BANDS;
		float taper = 1.0F - 0.055F * smoothstep(0.68F, 1.0F, heightFraction);
		float flux = 1.0F
				+ 0.041F * Mth.sin(time * 0.025F + angle * 3.0F + heightFraction * 12.0F)
				+ 0.030F * Mth.sin(time * 0.016F - angle * 5.0F + heightFraction * 18.0F + 1.7F)
				+ 0.020F * Mth.sin(time * 0.041F + angle * 9.0F - heightFraction * 27.0F);
		float radius = baseRadius * radiusScale * taper * flux;
		float x = Mth.cos(angle) * radius;
		float z = Mth.sin(angle) * radius;
		float y = heightFraction * height;
		float u = segment / (float) SEGMENTS + uvPhase;
		int alpha = Mth.clamp(Math.round(alphaProfile(heightFraction) * alphaScale * 255.0F), 0, 255);

		Matrix4f matrix = pose.pose();
		Matrix3f normal = pose.normal();
		vertices.vertex(matrix, x, y, z).color(255, 255, 255, alpha).uv(u, heightFraction)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
				.normal(normal, Mth.cos(angle), 0.08F, Mth.sin(angle)).endVertex();
	}

	private static float alphaProfile(float heightFraction) {
		float bottom = smoothstep(0.0F, 0.045F, heightFraction);
		float crown = 1.0F - smoothstep(0.70F, 1.0F, heightFraction);
		return bottom * (0.28F + 0.72F * crown * crown);
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	@Override
	public ResourceLocation getTextureLocation(DKCTowerAuraEntity entity) {
		return FALLBACK_TEXTURE;
	}
}
