package net.solocraft.client.aura;

import net.solocraft.SololevelingMod;
import net.solocraft.client.aura.AuraSmokeField.Puff;
import net.solocraft.client.renderer.shader.AuraSmokeRenderTypes;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/**
 * Draws the {@link AuraSmokeField} as soft, camera-facing billboards in world
 * space. Rendering here (rather than in {@code RenderPlayerEvent}) is what lets
 * the smoke lag behind and drift independently of the player's rigid pose.
 */
@Mod.EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = Mod.EventBusSubscriber.Bus.FORGE,
		value = Dist.CLIENT
)
public final class AuraSmokeRenderer {
	private AuraSmokeRenderer() {
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			return;
		}

		Map<Integer, List<Puff>> all = AuraSmokeField.puffs();
		if (all.isEmpty()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}

		Camera camera = event.getCamera();
		Vec3 cameraPosition = camera.getPosition();
		Quaternionf billboard = minecraft.getEntityRenderDispatcher().cameraOrientation();
		float partialTick = event.getPartialTick();
		boolean custom = AuraSmokeRenderTypes.usesCustomShader();

		int selfId = minecraft.player.getId();
		boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

		// Two passes keep each render type's buffer open instead of flushing per
		// puff, and draw the translucent smoke before the additive embers so the
		// glow reads on top.
		for (int pass = 0; pass < 2; pass++) {
			boolean brightPass = pass == 1;
			for (Map.Entry<Integer, List<Puff>> entry : all.entrySet()) {
				if (firstPerson && entry.getKey() == selfId) {
					continue;
				}
				for (Puff puff : entry.getValue()) {
					if (puff.bright != brightPass) {
						continue;
					}
					renderPuff(poseStack, buffers, puff, cameraPosition, billboard, partialTick, custom);
				}
			}
		}

		buffers.endBatch();
	}

	private static void renderPuff(
			PoseStack poseStack,
			MultiBufferSource buffers,
			Puff puff,
			Vec3 cameraPosition,
			Quaternionf billboard,
			float partialTick,
			boolean custom
	) {
		double life = puff.age / puff.maxAge;
		if (life >= 1.0D) {
			return;
		}

		float lifeFraction = (float) life;
		float fadeIn = Mth.clamp(lifeFraction / 0.14F, 0.0F, 1.0F);
		float fadeOut = puff.bright
				? 1.0F - Mth.clamp((lifeFraction - 0.35F) / 0.65F, 0.0F, 1.0F)
				: 1.0F - Mth.clamp((lifeFraction - 0.5F) / 0.5F, 0.0F, 1.0F);
		float alpha = puff.baseAlpha * fadeIn * fadeOut;
		if (alpha < 0.01F) {
			return;
		}

		double x = Mth.lerp(partialTick, puff.ppx, puff.px) - cameraPosition.x;
		double y = Mth.lerp(partialTick, puff.ppy, puff.py) - cameraPosition.y;
		double z = Mth.lerp(partialTick, puff.ppz, puff.pz) - cameraPosition.z;

		float size = puff.size * (0.55F + 0.95F * lifeFraction);

		RenderType type = puff.bright
				? AuraSmokeRenderTypes.ember(puff.texture)
				: AuraSmokeRenderTypes.smoke(puff.texture);
		VertexConsumer vertices = buffers.getBuffer(type);

		int red = custom ? (puff.color >> 16) & 0xFF : 255;
		int green = custom ? (puff.color >> 8) & 0xFF : 255;
		int blue = custom ? puff.color & 0xFF : 255;
		int packedAlpha = (int) (alpha * 255.0F);
		float uBase = custom ? (float) puff.seed : 0.0F;

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(billboard);

		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();

		vertex(vertices, pose, normal, -size, -size, uBase, 1.0F, red, green, blue, packedAlpha);
		vertex(vertices, pose, normal, size, -size, uBase + 1.0F, 1.0F, red, green, blue, packedAlpha);
		vertex(vertices, pose, normal, size, size, uBase + 1.0F, 0.0F, red, green, blue, packedAlpha);
		vertex(vertices, pose, normal, -size, size, uBase, 0.0F, red, green, blue, packedAlpha);

		poseStack.popPose();
	}

	private static void vertex(
			VertexConsumer vertices,
			Matrix4f pose,
			Matrix3f normal,
			float x,
			float y,
			float u,
			float v,
			int red,
			int green,
			int blue,
			int alpha
	) {
		vertices.vertex(pose, x, y, 0.0F)
				.color(red, green, blue, alpha)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(240)
				.normal(normal, 0.0F, 0.0F, 1.0F)
				.endVertex();
	}
}
