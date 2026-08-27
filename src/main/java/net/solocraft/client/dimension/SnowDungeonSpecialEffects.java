package net.solocraft.client.dimension;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.solocraft.dungeon.runtime.RedGateRealmLayout;
import net.solocraft.SololevelingMod;
import net.solocraft.world.dimension.rift.RiftTerritory;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;

/**
 * Client presentation for the shared Monarch Red Gate realm.
 *
 * <p>The geometry is intentionally texture-free so resource packs cannot leave
 * the dungeon with a missing sky. The encoded arena lane selects a local
 * territory palette without another dimension or synchronization packet.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = SololevelingMod.MODID, value = Dist.CLIENT)
public final class SnowDungeonSpecialEffects extends DimensionSpecialEffects {
	private static final ResourceLocation DIMENSION_ID =
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "dungeon_dimension_snow");
	private static final float TERRAIN_FOG_NEAR = 30.0F;
	private static final float TERRAIN_FOG_FAR = 88.0F;
	private static final float SKY_RADIUS = 96.0F;
	private static final float SKY_TOP = 72.0F;
	private static final float SKY_BOTTOM = -42.0F;
	private static final SkyPalette DESTRUCTION = palette(
			0.30F, 0.07F, 0.035F, 0.12F, 0.012F, 0.008F, 0.32F, 0.045F, 0.018F,
			1.0F, 0.20F, 0.06F, 0.72F, 0.04F, 0.02F, 1.0F, 0.45F, 0.18F);
	private static final SkyPalette FROST = palette(
			0.12F, 0.18F, 0.29F, 0.025F, 0.045F, 0.105F, 0.055F, 0.10F, 0.17F,
			0.20F, 0.76F, 0.78F, 0.48F, 0.32F, 0.82F, 0.78F, 0.88F, 1.0F);
	private static final SkyPalette FANGS = palette(
			0.08F, 0.15F, 0.09F, 0.01F, 0.05F, 0.025F, 0.07F, 0.20F, 0.09F,
			0.20F, 0.82F, 0.34F, 0.65F, 0.88F, 0.22F, 0.68F, 1.0F, 0.72F);
	private static final SkyPalette PLAGUES = palette(
			0.16F, 0.13F, 0.055F, 0.045F, 0.035F, 0.01F, 0.20F, 0.16F, 0.025F,
			0.62F, 0.72F, 0.08F, 0.36F, 0.52F, 0.04F, 0.82F, 0.88F, 0.35F);
	private static final SkyPalette IRON_BODY = palette(
			0.12F, 0.13F, 0.15F, 0.035F, 0.04F, 0.055F, 0.16F, 0.17F, 0.20F,
			0.48F, 0.55F, 0.65F, 0.75F, 0.32F, 0.15F, 0.84F, 0.86F, 0.92F);
	private static final SkyPalette WHITE_FLAMES = palette(
			0.12F, 0.16F, 0.22F, 0.018F, 0.035F, 0.07F, 0.06F, 0.18F, 0.28F,
			0.12F, 0.76F, 1.0F, 0.78F, 0.88F, 1.0F, 0.78F, 1.0F, 1.0F);
	private static final SkyPalette TRANSFIGURATION = palette(
			0.12F, 0.05F, 0.22F, 0.04F, 0.008F, 0.10F, 0.18F, 0.04F, 0.32F,
			0.72F, 0.18F, 1.0F, 0.10F, 0.85F, 0.90F, 0.90F, 0.52F, 1.0F);
	private static final SkyPalette BEGINNING = palette(
			0.025F, 0.03F, 0.06F, 0.003F, 0.004F, 0.012F, 0.035F, 0.02F, 0.075F,
			0.30F, 0.12F, 0.55F, 0.05F, 0.45F, 0.56F, 0.58F, 0.42F, 0.78F);

	public SnowDungeonSpecialEffects() {
		super(Float.NaN, true, SkyType.NONE, false, false);
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
		SkyPalette palette = paletteFor(currentTerritory());
		double light = 0.88D + 0.08D * Mth.clamp(sunHeight, 0.0F, 1.0F);
		return new Vec3(palette.fog().red() * light, palette.fog().green() * light,
				palette.fog().blue() * light);
	}

	@Override
	public boolean isFoggyAt(int x, int y) {
		return false;
	}

	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null
				|| !minecraft.level.dimension().location().equals(DIMENSION_ID)
				|| event.getType() != FogType.NONE)
			return;
		float far = Math.min(event.getFarPlaneDistance(), TERRAIN_FOG_FAR);
		if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
			event.setNearPlaneDistance(0.0F);
			event.setFarPlaneDistance(far * 0.90F);
		} else {
			event.setNearPlaneDistance(Math.min(TERRAIN_FOG_NEAR, far * 0.48F));
			event.setFarPlaneDistance(far);
		}
		event.setFogShape(FogShape.CYLINDER);
		event.setCanceled(true);
	}

	@Override
	public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
			double camX, double camY, double camZ, Matrix4f modelViewMatrix,
			Matrix4f projectionMatrix) {
		// The aurora supplies the high-altitude silhouette. Vanilla clouds look
		// too bright and are not useful inside this bounded encounter dimension.
		return true;
	}

	@Override
	public boolean renderSky(ClientLevel level, int ticks, float partialTick,
			Matrix4f modelViewMatrix, Camera camera,
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

		SkyPalette palette = paletteFor(RedGateRealmLayout.territoryAtX(camera.getPosition().x)
				.orElse(RiftTerritory.FROST));
		try {
			renderTwilightBox(modelViewMatrix, palette);
			float animationTime = ticks + partialTick;
			renderAurora(modelViewMatrix, animationTime, palette);
			renderMoon(modelViewMatrix, palette);
		} finally {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
		}
		return true;
	}

	private static void renderTwilightBox(Matrix4f matrix, SkyPalette palette) {
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		// Upper vault.
		vertex(buffer, matrix, -SKY_RADIUS, SKY_TOP, -SKY_RADIUS, palette.top(), 1.0F);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_TOP, SKY_RADIUS, palette.top(), 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_TOP, SKY_RADIUS, palette.top(), 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_TOP, -SKY_RADIUS, palette.top(), 1.0F);

		// Four sides use the same vertical gradient, so their corners meet cleanly.
		side(buffer, matrix, -SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, palette);
		side(buffer, matrix, SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, SKY_RADIUS, palette);
		side(buffer, matrix, SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, palette);
		side(buffer, matrix, -SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, -SKY_RADIUS, palette);

		// A dark lower cap prevents an unloaded horizon from flashing black.
		vertex(buffer, matrix, -SKY_RADIUS, SKY_BOTTOM, SKY_RADIUS, palette.bottom(), 1.0F);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_BOTTOM, -SKY_RADIUS, palette.bottom(), 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_BOTTOM, -SKY_RADIUS, palette.bottom(), 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_BOTTOM, SKY_RADIUS, palette.bottom(), 1.0F);

		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void side(BufferBuilder buffer, Matrix4f matrix, float x0, float z0, float x1, float z1,
			SkyPalette palette) {
		vertex(buffer, matrix, x0, SKY_BOTTOM, z0, palette.bottom(), 1.0F);
		vertex(buffer, matrix, x1, SKY_BOTTOM, z1, palette.bottom(), 1.0F);
		vertex(buffer, matrix, x1, SKY_TOP, z1, palette.top(), 1.0F);
		vertex(buffer, matrix, x0, SKY_TOP, z0, palette.top(), 1.0F);
	}

	private static void renderAurora(Matrix4f matrix, float time, SkyPalette palette) {
		Color first = palette.aurora();
		Color second = palette.accent();
		renderAuroraBand(matrix, time * 0.010F, -86.0F, -64.0F, 17.0F,
				first.red(), first.green(), first.blue(), 0.20F);
		renderAuroraBand(matrix, time * 0.008F + 2.1F, -88.0F, -57.0F, 27.0F,
				(first.red() + second.red()) * 0.5F, (first.green() + second.green()) * 0.5F,
				(first.blue() + second.blue()) * 0.5F, 0.14F);
		renderAuroraBand(matrix, time * 0.012F + 4.3F, -90.0F, -70.0F, 8.0F,
				second.red(), second.green(), second.blue(), 0.10F);
	}

	private static void renderAuroraBand(Matrix4f matrix, float phase, float z, float startX, float baseY,
			float red, float green, float blue, float alpha) {
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= 16; i++) {
			float x = startX + i * 8.0F;
			float wave = Mth.sin(phase + i * 0.62F) * 5.5F + Mth.sin(phase * 0.61F + i * 0.27F) * 2.5F;
			float lowerY = baseY + wave;
			float upperY = lowerY + 24.0F + Mth.sin(phase + i * 0.38F) * 3.0F;
			vertex(buffer, matrix, x, lowerY, z, red, green, blue, 0.0F);
			vertex(buffer, matrix, x, upperY, z, red, green, blue, alpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void renderMoon(Matrix4f matrix, SkyPalette palette) {
		Color moon = palette.moon();
		// Soft halo first, then the pale frozen moon.
		renderDisc(matrix, -37.0F, 45.0F, -91.0F, 14.0F,
				moon.red(), moon.green(), moon.blue(), 0.18F, 0.0F);
		renderDisc(matrix, -37.0F, 45.0F, -90.5F, 8.5F,
				moon.red(), moon.green(), moon.blue(), 0.92F, 0.72F);
	}

	private static void renderDisc(Matrix4f matrix, float centerX, float centerY, float z, float radius,
			float red, float green, float blue, float centerAlpha, float edgeAlpha) {
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		vertex(buffer, matrix, centerX, centerY, z, red, green, blue, centerAlpha);
		for (int i = 0; i <= 40; i++) {
			float angle = (float) (Math.PI * 2.0D * i / 40.0D);
			vertex(buffer, matrix, centerX + Mth.cos(angle) * radius, centerY + Mth.sin(angle) * radius, z,
					red, green, blue, edgeAlpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
			float red, float green, float blue, float alpha) {
		buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
			Color color, float alpha) {
		vertex(buffer, matrix, x, y, z, color.red(), color.green(), color.blue(), alpha);
	}

	private static RiftTerritory currentTerritory() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null)
			return RiftTerritory.FROST;
		return RedGateRealmLayout.territoryAtX(minecraft.player.getX()).orElse(RiftTerritory.FROST);
	}

	private static SkyPalette paletteFor(RiftTerritory territory) {
		return switch (territory) {
			case DESTRUCTION -> DESTRUCTION;
			case FROST -> FROST;
			case FANGS -> FANGS;
			case PLAGUES -> PLAGUES;
			case IRON_BODY -> IRON_BODY;
			case WHITE_FLAMES -> WHITE_FLAMES;
			case TRANSFIGURATION -> TRANSFIGURATION;
			case BEGINNING -> BEGINNING;
		};
	}

	private static SkyPalette palette(float fogRed, float fogGreen, float fogBlue,
			float topRed, float topGreen, float topBlue, float bottomRed, float bottomGreen, float bottomBlue,
			float auroraRed, float auroraGreen, float auroraBlue, float accentRed, float accentGreen, float accentBlue,
			float moonRed, float moonGreen, float moonBlue) {
		return new SkyPalette(new Color(fogRed, fogGreen, fogBlue), new Color(topRed, topGreen, topBlue),
				new Color(bottomRed, bottomGreen, bottomBlue), new Color(auroraRed, auroraGreen, auroraBlue),
				new Color(accentRed, accentGreen, accentBlue), new Color(moonRed, moonGreen, moonBlue));
	}

	private record Color(float red, float green, float blue) {
	}

	private record SkyPalette(Color fog, Color top, Color bottom, Color aurora, Color accent, Color moon) {
	}
}
