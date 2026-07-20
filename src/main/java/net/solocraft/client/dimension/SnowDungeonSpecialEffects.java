package net.solocraft.client.dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Client presentation for the snow red-gate arena.
 *
 * <p>The geometry is intentionally texture-free so resource packs cannot leave
 * the dungeon with a missing sky. Encounter state can later tint the aurora
 * without replacing this baseline renderer.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SnowDungeonSpecialEffects extends DimensionSpecialEffects {
	private static final float SKY_RADIUS = 96.0F;
	private static final float SKY_TOP = 72.0F;
	private static final float SKY_BOTTOM = -42.0F;

	public SnowDungeonSpecialEffects() {
		super(Float.NaN, true, SkyType.NONE, false, false);
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
		double light = 0.88D + 0.08D * Mth.clamp(sunHeight, 0.0F, 1.0F);
		return new Vec3(0.12D * light, 0.18D * light, 0.29D * light);
	}

	@Override
	public boolean isFoggyAt(int x, int y) {
		return false;
	}

	@Override
	public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
			double camX, double camY, double camZ, Matrix4f projectionMatrix) {
		// The aurora supplies the high-altitude silhouette. Vanilla clouds look
		// too bright and are not useful inside this bounded encounter dimension.
		return true;
	}

	@Override
	public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
			Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
		if (isFoggy || camera.getFluidInCamera() != FogType.NONE) {
			return false;
		}

		setupFog.run();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Matrix4f matrix = poseStack.last().pose();
		renderTwilightBox(matrix);
		float animationTime = ticks + partialTick;
		renderAurora(matrix, animationTime);
		renderMoon(matrix);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		return true;
	}

	private static void renderTwilightBox(Matrix4f matrix) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		// Upper vault.
		vertex(buffer, matrix, -SKY_RADIUS, SKY_TOP, -SKY_RADIUS, 0.025F, 0.045F, 0.105F, 1.0F);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_TOP, SKY_RADIUS, 0.025F, 0.045F, 0.105F, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_TOP, SKY_RADIUS, 0.025F, 0.045F, 0.105F, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_TOP, -SKY_RADIUS, 0.025F, 0.045F, 0.105F, 1.0F);

		// Four sides use the same vertical gradient, so their corners meet cleanly.
		side(buffer, matrix, -SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS);
		side(buffer, matrix, SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, SKY_RADIUS);
		side(buffer, matrix, SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS);
		side(buffer, matrix, -SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, -SKY_RADIUS);

		// A dark lower cap prevents an unloaded horizon from flashing black.
		vertex(buffer, matrix, -SKY_RADIUS, SKY_BOTTOM, SKY_RADIUS, 0.055F, 0.10F, 0.17F, 1.0F);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_BOTTOM, -SKY_RADIUS, 0.055F, 0.10F, 0.17F, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_BOTTOM, -SKY_RADIUS, 0.055F, 0.10F, 0.17F, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_BOTTOM, SKY_RADIUS, 0.055F, 0.10F, 0.17F, 1.0F);

		BufferUploader.drawWithShader(buffer.end());
	}

	private static void side(BufferBuilder buffer, Matrix4f matrix, float x0, float z0, float x1, float z1) {
		vertex(buffer, matrix, x0, SKY_BOTTOM, z0, 0.055F, 0.10F, 0.17F, 1.0F);
		vertex(buffer, matrix, x1, SKY_BOTTOM, z1, 0.055F, 0.10F, 0.17F, 1.0F);
		vertex(buffer, matrix, x1, SKY_TOP, z1, 0.025F, 0.045F, 0.105F, 1.0F);
		vertex(buffer, matrix, x0, SKY_TOP, z0, 0.025F, 0.045F, 0.105F, 1.0F);
	}

	private static void renderAurora(Matrix4f matrix, float time) {
		renderAuroraBand(matrix, time * 0.010F, -86.0F, -64.0F, 17.0F, 0.20F, 0.76F, 0.78F, 0.20F);
		renderAuroraBand(matrix, time * 0.008F + 2.1F, -88.0F, -57.0F, 27.0F, 0.32F, 0.50F, 0.88F, 0.14F);
		renderAuroraBand(matrix, time * 0.012F + 4.3F, -90.0F, -70.0F, 8.0F, 0.48F, 0.32F, 0.82F, 0.10F);
	}

	private static void renderAuroraBand(Matrix4f matrix, float phase, float z, float startX, float baseY,
			float red, float green, float blue, float alpha) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= 16; i++) {
			float x = startX + i * 8.0F;
			float wave = Mth.sin(phase + i * 0.62F) * 5.5F + Mth.sin(phase * 0.61F + i * 0.27F) * 2.5F;
			float lowerY = baseY + wave;
			float upperY = lowerY + 24.0F + Mth.sin(phase + i * 0.38F) * 3.0F;
			vertex(buffer, matrix, x, lowerY, z, red, green, blue, 0.0F);
			vertex(buffer, matrix, x, upperY, z, red, green, blue, alpha);
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void renderMoon(Matrix4f matrix) {
		// Soft halo first, then the pale frozen moon.
		renderDisc(matrix, -37.0F, 45.0F, -91.0F, 14.0F, 0.55F, 0.72F, 1.0F, 0.18F, 0.0F);
		renderDisc(matrix, -37.0F, 45.0F, -90.5F, 8.5F, 0.78F, 0.88F, 1.0F, 0.92F, 0.72F);
	}

	private static void renderDisc(Matrix4f matrix, float centerX, float centerY, float z, float radius,
			float red, float green, float blue, float centerAlpha, float edgeAlpha) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		vertex(buffer, matrix, centerX, centerY, z, red, green, blue, centerAlpha);
		for (int i = 0; i <= 40; i++) {
			float angle = (float) (Math.PI * 2.0D * i / 40.0D);
			vertex(buffer, matrix, centerX + Mth.cos(angle) * radius, centerY + Mth.sin(angle) * radius, z,
					red, green, blue, edgeAlpha);
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
			float red, float green, float blue, float alpha) {
		buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
	}
}
