package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.BlessSigilRenderTypes;
import net.solocraft.client.renderer.shader.HealBeamRenderTypes;
import net.solocraft.client.renderer.shader.SanctuaryFieldRenderTypes;
import net.solocraft.entity.HealerVfxEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws the Healer effect quads.
 *
 * <p>Each variant is a small number of quads whose shader reads stage,
 * expansion and colour out of the vertex colour, so the renderer stays simple
 * and the visual logic lives in GLSL.</p>
 */
public class HealerVfxRenderer extends EntityRenderer<HealerVfxEntity> {
	private static final ResourceLocation TEXTURE = ResourceLocation
			.fromNamespaceAndPath("sololeveling", "textures/particle/aura_green.png");
	private static final int FULL_LIGHT = 15728880;

	public HealerVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public ResourceLocation getTextureLocation(HealerVfxEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(HealerVfxEntity entity, float yaw, float partialTick,
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		float progress = entity.progress(partialTick);
		// Fade in quickly, hold, then fade out over the last quarter.
		float fade = Math.min(1.0F, progress * 6.0F)
				* (1.0F - Mth.clamp((progress - 0.75F) * 4.0F, 0.0F, 1.0F));
		if (fade <= 0.01F)
			return;

		int color = entity.getColor();
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;

		poseStack.pushPose();
		switch (entity.getVariant()) {
			case HealerVfxEntity.BEAM -> renderBeam(entity, poseStack, bufferSource,
					red, green, blue, fade);
			case HealerVfxEntity.SIGIL -> renderFlat(entity, poseStack, bufferSource,
					BlessSigilRenderTypes.get(TEXTURE), entity.getScale(),
					red, green, blue, entity.getStage() / 5.0F, fade);
			case HealerVfxEntity.FIELD -> renderFlat(entity, poseStack, bufferSource,
					SanctuaryFieldRenderTypes.get(TEXTURE), entity.getScale(),
					red, green, blue, 0.92F, fade);
			case HealerVfxEntity.WAVE -> renderFlat(entity, poseStack, bufferSource,
					SanctuaryFieldRenderTypes.get(TEXTURE), entity.getScale(),
					red, green, blue, Math.max(0.08F, progress), fade);
			default -> {
			}
		}
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
	}

	/**
	 * A ribbon from this entity toward its stored endpoint. Stage 4 and above
	 * emit a second offset strand, which is exactly where Heal Beam gains its
	 * chain, so the strand count reads as the behaviour change.
	 */
	private void renderBeam(HealerVfxEntity entity, PoseStack poseStack,
			MultiBufferSource bufferSource, float red, float green, float blue, float fade) {
		Vec3 delta = entity.getEndOffset();
		double length = delta.length();
		if (length < 0.05D)
			return;

		VertexConsumer buffer = bufferSource.getBuffer(HealBeamRenderTypes.get(TEXTURE));
		float yaw = (float) Math.atan2(delta.z, delta.x);
		float pitch = (float) Math.asin(Mth.clamp(delta.y / length, -1.0D, 1.0D));
		int strands = entity.getStage() >= 4 ? 2 : 1;

		poseStack.mulPose(new org.joml.Quaternionf().rotateY(-yaw));
		poseStack.mulPose(new org.joml.Quaternionf().rotateZ(pitch));
		for (int strand = 0; strand < strands; strand++) {
			poseStack.pushPose();
			// The second strand rides slightly above and beside the first.
			if (strand == 1)
				poseStack.translate(0.0D, 0.28D, 0.18D);
			float width = 0.34F + entity.getStage() * 0.05F;
			quad(poseStack, buffer, (float) length, width, red, green, blue,
					entity.getStage() / 5.0F, fade * (strand == 0 ? 1.0F : 0.7F));
			poseStack.popPose();
		}
	}

	/** A ground-plane quad, used by sigils, fields and waves. */
	private void renderFlat(HealerVfxEntity entity, PoseStack poseStack,
			MultiBufferSource bufferSource, RenderType renderType, float radius,
			float red, float green, float blue, float parameter, float fade) {
		VertexConsumer buffer = bufferSource.getBuffer(renderType);
		poseStack.pushPose();
		poseStack.translate(0.0D, 0.06D, 0.0D);
		poseStack.mulPose(new org.joml.Quaternionf().rotateX((float) Math.toRadians(90.0D)));
		Matrix4f matrix = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		float size = Math.max(0.2F, radius);
		vertex(buffer, matrix, normal, -size, -size, 0.0F, 0.0F, red, green, blue, parameter, fade);
		vertex(buffer, matrix, normal, -size, size, 0.0F, 1.0F, red, green, blue, parameter, fade);
		vertex(buffer, matrix, normal, size, size, 1.0F, 1.0F, red, green, blue, parameter, fade);
		vertex(buffer, matrix, normal, size, -size, 1.0F, 0.0F, red, green, blue, parameter, fade);
		poseStack.popPose();
	}

	private void quad(PoseStack poseStack, VertexConsumer buffer, float length,
			float width, float red, float green, float blue, float parameter, float fade) {
		Matrix4f matrix = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		float half = width * 0.5F;
		vertex(buffer, matrix, normal, 0.0F, -half, 0.0F, 0.0F, red, green, blue, parameter, fade);
		vertex(buffer, matrix, normal, 0.0F, half, 0.0F, 1.0F, red, green, blue, parameter, fade);
		vertex(buffer, matrix, normal, length, half, 1.0F, 1.0F, red, green, blue, parameter, fade);
		vertex(buffer, matrix, normal, length, -half, 1.0F, 0.0F, red, green, blue, parameter, fade);
	}

	/**
	 * The blue channel carries the shader parameter — stage for sigils and
	 * beams, expansion for fields and waves — so one vertex format serves all
	 * three shaders.
	 */
	private void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
			float x, float y, float u, float v, float red, float green, float blue,
			float parameter, float alpha) {
		buffer.addVertex(matrix, x, y, 0.0F)
				.setColor(red, green, Mth.clamp(parameter, 0.0F, 1.0F), alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_LIGHT)
				.setNormal(0.0F, 1.0F, 0.0F);
	}
}
