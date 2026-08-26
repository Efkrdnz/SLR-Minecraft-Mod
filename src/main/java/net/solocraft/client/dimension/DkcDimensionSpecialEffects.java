package net.solocraft.client.dimension;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcSpatialLayout;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * One lightweight client renderer for every spatially separated DKC floor.
 *
 * <p>The server only needs one dimension shell. Floor identity comes from the
 * camera/player coordinates, so fog, light, sky and particles can still change
 * without keeping another {@code ServerLevel} alive. The sky is bounded to a few
 * hundred vertices and one vanilla moon-texture quad per frame.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = SololevelingMod.MODID, value = Dist.CLIENT)
public final class DkcDimensionSpecialEffects extends DimensionSpecialEffects {
	private static final float SKY_RADIUS = 128.0F;
	private static final float SKY_TOP = 92.0F;
	private static final float SKY_BOTTOM = -68.0F;
	private static final float BLOOD_MOON_DISTANCE = 112.0F;
	private static final float BLOOD_MOON_ELEVATION = (float) Math.toRadians(45.0D);
	/*
	 * Mojang's full-moon artwork occupies only part of its 16 x 16 atlas cell.
	 * The larger quad makes the visible lunar disc match the old 63-block-wide
	 * procedural body rather than making only the atlas cell large.
	 */
	private static final float BLOOD_MOON_HALF_SIZE = 128.0F;
	private static final ResourceLocation VANILLA_MOON_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/moon_phases.png");

	private static final int[] FOG_COLORS = {
			0,
			0x5A1515, 0x642019, 0x4C201A, 0x54251C, 0x6A2110,
			0x73260E, 0x591419, 0x4B1016, 0x6E1808, 0x7A2005,
			0x291421, 0x24142C, 0x20152F, 0x29143A, 0x321344,
			0x171C3D, 0x131E4A, 0x22194F, 0x351653, 0x46105D
	};
	private static final int[] SKY_COLORS = {
			0,
			0xB62D20, 0xC74625, 0x9B4E2D, 0xB45A32, 0xE05A19,
			0xF06A17, 0xD52E31, 0xB9202D, 0xF2470D, 0xFF6A08,
			0x71345D, 0x673C78, 0x594283, 0x6F3B94, 0x8738A8,
			0x465BB1, 0x3E6BD2, 0x6755D8, 0x9B45D5, 0xCA35E8
	};
	private static final float[] FOG_FAR = {
			0.0F,
			84.0F, 148.0F, 136.0F, 124.0F, 92.0F,
			116.0F, 108.0F, 100.0F, 94.0F, 78.0F,
			142.0F, 132.0F, 122.0F, 112.0F, 96.0F,
			92.0F, 88.0F, 84.0F, 78.0F, 72.0F
	};
	private static final Profile[] PROFILES = createProfiles();
	private static Profile cachedPlayerProfile = PROFILES[DkcFloorRegistry.FIRST_FLOOR];

	public DkcDimensionSpecialEffects() {
		// A custom renderer supplies all three sky families. NONE prevents a
		// second vanilla sky pass while retaining skylight for authored spaces.
		super(Float.NaN, true, SkyType.NONE, false, false);
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 ignored, float sunHeight) {
		Profile profile = activeProfile();
		float light = 0.76F + 0.12F * Mth.clamp(sunHeight, 0.0F, 1.0F);
		return colorVector(profile.fogColor()).scale(light);
	}

	@Override
	public boolean isFoggyAt(int x, int z) {
		return activeProfile().denseFog();
	}

	@Override
	public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
			double camX, double camY, double camZ, Matrix4f modelViewMatrix,
			Matrix4f projectionMatrix) {
		// The custom horizon already contains restrained smoke/storm layers.
		return true;
	}

	@Override
	public boolean renderSky(ClientLevel level, int ticks, float partialTick,
			Matrix4f modelViewMatrix, Camera camera,
			Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
		if (!isSharedDkc(level) || camera.getFluidInCamera() != FogType.NONE)
			return false;

		// Draw inside NeoForge's dimension-effects callback. Iris marks this exact
		// section as CUSTOM_SKY, so the geometry is routed through the active pack
		// instead of being submitted after Iris has left its sky phase.
		setupFog.run();
		renderSkyLayer(modelViewMatrix, camera, ticks, partialTick);
		return true;
	}

	private static void renderSkyLayer(Matrix4f matrix, Camera camera, int ticks,
			float partialTick) {
		Profile profile = profileAt(camera.getBlockPosition());
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		try {
			renderVault(matrix, profile);
			renderVanillaBloodMoon(matrix, profile);
			float time = ticks + partialTick;
			switch (profile.family()) {
				case EMBER -> renderEmberSky(matrix, profile, time);
				case FURNACE -> renderFurnaceSky(matrix, profile, time);
				case TEMPEST -> renderTempestSky(matrix, profile, time);
			}
		} finally {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
		}
	}

	@Override
	public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken,
			float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors) {
		if (!isSharedDkc(level))
			return;
		Profile profile = activeProfile();
		float skyContribution = pixelY / 15.0F;
		float strength = profile.lightTintStrength() * (0.72F + skyContribution * 0.28F);
		colors.mul(
				Mth.lerp(strength, 1.0F, profile.lightRed()),
				Mth.lerp(strength, 1.0F, profile.lightGreen()),
				Mth.lerp(strength, 1.0F, profile.lightBlue()));
		colors.set(Mth.clamp(colors.x(), 0.0F, 1.0F), Mth.clamp(colors.y(), 0.0F, 1.0F),
				Mth.clamp(colors.z(), 0.0F, 1.0F));
	}

	@SubscribeEvent
	public static void onFogColor(ViewportEvent.ComputeFogColor event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isSharedDkc(minecraft.level) || event.getCamera().getFluidInCamera() != FogType.NONE)
			return;
		int color = profileAt(event.getCamera().getBlockPosition()).fogColor();
		event.setRed(((color >> 16) & 0xFF) / 255.0F);
		event.setGreen(((color >> 8) & 0xFF) / 255.0F);
		event.setBlue((color & 0xFF) / 255.0F);
	}

	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isSharedDkc(minecraft.level) || event.getType() != FogType.NONE)
			return;
		Profile profile = profileAt(event.getCamera().getBlockPosition());
		float far = Math.min(event.getFarPlaneDistance(), profile.fogFar());
		if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
			event.setNearPlaneDistance(0.0F);
			event.setFarPlaneDistance(far * 0.86F);
		} else {
			event.setNearPlaneDistance(Math.min(profile.fogNear(), far * 0.48F));
			event.setFarPlaneDistance(far);
		}
		event.setFogShape(FogShape.CYLINDER);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (minecraft.player == null || !isSharedDkc(level)) {
			cachedPlayerProfile = PROFILES[DkcFloorRegistry.FIRST_FLOOR];
			return;
		}
		cachedPlayerProfile = profileAt(minecraft.player.blockPosition());
		if (minecraft.isPaused())
			return;

		Profile profile = cachedPlayerProfile;
		long gameTime = level.getGameTime();
		ParticleStatus status = minecraft.options.particles().get();
		int interval = profile.particleInterval();
		if (status == ParticleStatus.DECREASED)
			interval *= 2;
		else if (status == ParticleStatus.MINIMAL)
			interval *= 4;
		if (gameTime % interval != profile.floor() % interval)
			return;

		RandomSource random = level.random;
		int count = status == ParticleStatus.ALL && (gameTime + profile.floor()) % 3L == 0L ? 2 : 1;
		for (int i = 0; i < count; i++)
			spawnAmbientParticle(level, minecraft.player.position(), profile, random);
	}

	private static void spawnAmbientParticle(ClientLevel level, Vec3 center, Profile profile, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double distance = 5.0D + random.nextDouble() * 17.0D;
		double x = center.x + Math.cos(angle) * distance;
		double y = center.y - 1.5D + random.nextDouble() * 10.0D;
		double z = center.z + Math.sin(angle) * distance;
		double drift = profile.family() == SkyFamily.TEMPEST ? 0.018D : 0.008D;
		double velocityX = random.nextGaussian() * drift;
		double velocityY = profile.family() == SkyFamily.FURNACE ? 0.014D : -0.004D;
		double velocityZ = random.nextGaussian() * drift;
		level.addParticle(profile.particle(), x, y, z, velocityX, velocityY, velocityZ);
	}

	private static Profile activeProfile() {
		return cachedPlayerProfile;
	}

	private static Profile profileAt(BlockPos position) {
		int floor = DkcSpatialLayout.floorAt(position);
		if (floor < DkcFloorRegistry.FIRST_FLOOR || floor > DkcFloorRegistry.LAST_FLOOR)
			floor = DkcFloorRegistry.FIRST_FLOOR;
		return PROFILES[floor];
	}

	private static boolean isSharedDkc(ClientLevel level) {
		return level != null && DkcFloorRegistry.SHARED_DIMENSION.equals(level.dimension());
	}

	private static Profile[] createProfiles() {
		Profile[] profiles = new Profile[DkcFloorRegistry.LAST_FLOOR + 1];
		for (int floor = DkcFloorRegistry.FIRST_FLOOR; floor <= DkcFloorRegistry.LAST_FLOOR; floor++) {
			SkyFamily family = floor >= 16 ? SkyFamily.TEMPEST : floor >= 6 && floor <= 10
					? SkyFamily.FURNACE : SkyFamily.EMBER;
			boolean dense = floor == 1 || floor == 5 || floor == 10 || floor >= 16;
			float far = FOG_FAR[floor];
			float near = dense ? far * 0.14F : far * 0.31F;
			int horizon = mixColor(FOG_COLORS[floor], SKY_COLORS[floor], 0.52F);
			int zenith = scaleColor(SKY_COLORS[floor], family == SkyFamily.FURNACE ? 0.105F : 0.19F);
			float lightRed;
			float lightGreen;
			float lightBlue;
			float tintStrength;
			ParticleOptions particle;
			if (floor <= 5) {
				lightRed = 1.12F;
				lightGreen = 0.90F;
				lightBlue = 0.84F;
				tintStrength = 0.18F + floor * 0.008F;
				particle = ParticleTypes.ASH;
			} else if (floor <= 10) {
				lightRed = 1.16F;
				lightGreen = 0.84F;
				lightBlue = 0.72F;
				tintStrength = 0.20F + (floor - 5) * 0.01F;
				particle = ParticleTypes.ASH;
			} else if (floor <= 15) {
				lightRed = 1.03F;
				lightGreen = 0.88F;
				lightBlue = 1.10F;
				tintStrength = 0.20F + (floor - 10) * 0.008F;
				particle = ParticleTypes.WHITE_ASH;
			} else {
				lightRed = 0.93F;
				lightGreen = 0.92F;
				lightBlue = 1.18F;
				tintStrength = 0.22F + (floor - 15) * 0.01F;
				particle = ParticleTypes.CRIMSON_SPORE;
			}
			profiles[floor] = new Profile(floor, FOG_COLORS[floor], zenith, horizon, SKY_COLORS[floor],
					family, dense, near, far, lightRed, lightGreen, lightBlue, tintStrength, particle,
					Math.max(3, 7 - floor / 5));
		}
		return profiles;
	}

	private static void renderVault(Matrix4f matrix, Profile profile) {
		int top = profile.zenithColor();
		int horizon = profile.horizonColor();
		int bottom = scaleColor(profile.fogColor(), 0.30F);
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		vertex(buffer, matrix, -SKY_RADIUS, SKY_TOP, -SKY_RADIUS, top, 1.0F);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_TOP, SKY_RADIUS, top, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_TOP, SKY_RADIUS, top, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_TOP, -SKY_RADIUS, top, 1.0F);
		vaultSide(buffer, matrix, -SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, top, horizon, bottom);
		vaultSide(buffer, matrix, SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, SKY_RADIUS, top, horizon, bottom);
		vaultSide(buffer, matrix, SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, SKY_RADIUS, top, horizon, bottom);
		vaultSide(buffer, matrix, -SKY_RADIUS, SKY_RADIUS, -SKY_RADIUS, -SKY_RADIUS, top, horizon, bottom);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_BOTTOM, SKY_RADIUS, bottom, 1.0F);
		vertex(buffer, matrix, -SKY_RADIUS, SKY_BOTTOM, -SKY_RADIUS, bottom, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_BOTTOM, -SKY_RADIUS, bottom, 1.0F);
		vertex(buffer, matrix, SKY_RADIUS, SKY_BOTTOM, SKY_RADIUS, bottom, 1.0F);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void vaultSide(BufferBuilder buffer, Matrix4f matrix, float x0, float z0, float x1, float z1,
			int top, int horizon, int bottom) {
		vertex(buffer, matrix, x0, SKY_BOTTOM, z0, bottom, 1.0F);
		vertex(buffer, matrix, x1, SKY_BOTTOM, z1, bottom, 1.0F);
		vertex(buffer, matrix, x1, 11.0F, z1, horizon, 1.0F);
		vertex(buffer, matrix, x0, 11.0F, z0, horizon, 1.0F);
		vertex(buffer, matrix, x0, 11.0F, z0, horizon, 1.0F);
		vertex(buffer, matrix, x1, 11.0F, z1, horizon, 1.0F);
		vertex(buffer, matrix, x1, SKY_TOP, z1, top, 1.0F);
		vertex(buffer, matrix, x0, SKY_TOP, z0, top, 1.0F);
	}

	private static void renderEmberSky(Matrix4f matrix, Profile profile, float time) {
		renderRibbon(matrix, time * 0.006F, -116.0F, -88.0F, 4.0F, 13.0F,
				profile.accentColor(), 0.085F);
		renderSkySparks(matrix, profile, time, 22);
	}

	private static void renderFurnaceSky(Matrix4f matrix, Profile profile, float time) {
		renderRibbon(matrix, time * 0.004F, -119.0F, -92.0F, 5.0F, 27.0F,
				profile.accentColor(), 0.095F);
		renderRibbon(matrix, -time * 0.0032F + 2.3F, -117.0F, -98.0F, 31.0F, 21.0F,
				profile.fogColor(), 0.13F);
		renderRibbon(matrix, time * 0.0027F + 4.7F, -115.0F, -86.0F, 57.0F, 18.0F,
				profile.horizonColor(), 0.08F);
		renderSkySparks(matrix, profile, time * 0.62F, 14);
	}

	private static void renderTempestSky(Matrix4f matrix, Profile profile, float time) {
		renderRibbon(matrix, time * 0.009F, -119.0F, -96.0F, 12.0F, 25.0F,
				profile.accentColor(), 0.14F);
		renderRibbon(matrix, -time * 0.007F + 3.0F, -117.0F, -91.0F, 43.0F, 23.0F,
				profile.horizonColor(), 0.12F);
		float flashCycle = (time + profile.floor() * 37.0F) % 190.0F;
		if (flashCycle < 5.0F)
			renderLightning(matrix, profile.floor(), (1.0F - flashCycle / 5.0F) * 0.68F);
		renderSkySparks(matrix, profile, time * 1.2F, 18);
	}

	private static void renderRibbon(Matrix4f matrix, float phase, float z, float startX, float baseY,
			float height, int color, float alpha) {
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= 16; i++) {
			float x = startX + i * 12.0F;
			float wave = Mth.sin(phase + i * 0.58F) * 5.0F + Mth.sin(phase * 0.71F + i * 0.23F) * 2.4F;
			vertex(buffer, matrix, x, baseY + wave, z, color, 0.0F);
			vertex(buffer, matrix, x, baseY + wave + height, z, color, alpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void renderSkySparks(Matrix4f matrix, Profile profile, float time, int count) {
		float alpha = profile.family() == SkyFamily.FURNACE ? 0.38F : 0.52F;
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < count; i++) {
			int hash = i * 73428767 ^ profile.floor() * 912931;
			float x = -104.0F + Math.floorMod(hash, 209);
			float y = -8.0F + Math.floorMod(hash >>> 8, 87);
			float drift = Mth.sin(time * 0.012F + i * 1.73F) * 2.2F;
			float size = 0.25F + Math.floorMod(hash >>> 16, 5) * 0.11F;
			float z = -118.0F + Math.floorMod(hash >>> 22, 7);
			vertex(buffer, matrix, x - size, y + drift - size, z, profile.accentColor(), alpha);
			vertex(buffer, matrix, x + size, y + drift - size, z, profile.accentColor(), alpha);
			vertex(buffer, matrix, x + size, y + drift + size, z, profile.accentColor(), alpha);
			vertex(buffer, matrix, x - size, y + drift + size, z, profile.accentColor(), alpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void renderLightning(Matrix4f matrix, int floor, float alpha) {
		float x = -63.0F + (floor - 16) * 25.0F;
		float y = 77.0F;
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < 6; i++) {
			float nextX = x + (i % 2 == 0 ? 7.0F : -4.0F);
			float nextY = y - 17.0F;
			lightningSegment(buffer, matrix, x, y, nextX, nextY, -119.0F, 0.75F, alpha);
			x = nextX;
			y = nextY;
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void lightningSegment(BufferBuilder buffer, Matrix4f matrix, float x0, float y0,
			float x1, float y1, float z, float width, float alpha) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float length = Mth.sqrt(dx * dx + dy * dy);
		float ox = -dy / length * width;
		float oy = dx / length * width;
		vertex(buffer, matrix, x0 - ox, y0 - oy, z, 0.72F, 0.78F, 1.0F, alpha);
		vertex(buffer, matrix, x0 + ox, y0 + oy, z, 0.72F, 0.78F, 1.0F, alpha);
		vertex(buffer, matrix, x1 + ox, y1 + oy, z, 0.72F, 0.78F, 1.0F, alpha);
		vertex(buffer, matrix, x1 - ox, y1 - oy, z, 0.72F, 0.78F, 1.0F, alpha);
	}

	/**
	 * Minecraft's own full-moon texture, enlarged, blood-red, and fixed at an
	 * exact 45-degree elevation.
	 *
	 * Floor 15 faces the moon toward Radiru Castle on -Z; the climbing floors keep
	 * it over the +Z tower axis. Keeping the DKC-owned sky pass prevents the vanilla
	 * time rotation, sun, and white moon from being restored by renderer wrappers.
	 */
	private static void renderVanillaBloodMoon(Matrix4f matrix, Profile profile) {
		float sin = Mth.sin(BLOOD_MOON_ELEVATION);
		float cos = Mth.cos(BLOOD_MOON_ELEVATION);
		float directionZ = profile.floor() == 15 ? -1.0F : 1.0F;
		float centerY = BLOOD_MOON_DISTANCE * sin;
		float centerZ = BLOOD_MOON_DISTANCE * cos * directionZ;
		float verticalY = cos;
		float verticalZ = -sin * directionZ;

		// Retain only a restrained atmospheric halo; all visible surface detail
		// comes from the vanilla full-moon tile in moon_phases.png.
		renderMoonDisc(matrix, 0.0F, centerY, centerZ, verticalY, verticalZ,
				0.0F, 0.0F, 42.0F, 0xFF1820, 0.20F, 0.0F);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, VANILLA_MOON_TEXTURE);
		RenderSystem.setShaderColor(1.0F, 0.12F, 0.15F, 1.0F);
		// moon_phases.png has opaque black around each phase. Vanilla's celestial
		// additive blend makes black contribute nothing; ordinary alpha blending
		// would expose the atlas cell as a large black square.
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);

		float size = BLOOD_MOON_HALF_SIZE;
		float lowerY = centerY - size * verticalY;
		float lowerZ = centerZ - size * verticalZ;
		float upperY = centerY + size * verticalY;
		float upperZ = centerZ + size * verticalZ;
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.addVertex(matrix, -size, lowerY, lowerZ).setUv(0.0F, 0.5F);
		buffer.addVertex(matrix, size, lowerY, lowerZ).setUv(0.25F, 0.5F);
		buffer.addVertex(matrix, size, upperY, upperZ).setUv(0.25F, 0.0F);
		buffer.addVertex(matrix, -size, upperY, upperZ).setUv(0.0F, 0.0F);
		BufferUploader.drawWithShader(buffer.buildOrThrow());

		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
	}

	private static void renderMoonDisc(Matrix4f matrix, float baseX, float baseY, float baseZ,
			float verticalY, float verticalZ, float localX, float localY, float radius,
			int color, float centerAlpha, float edgeAlpha) {
		float centerX = baseX + localX;
		float centerY = baseY + localY * verticalY;
		float centerZ = baseZ + localY * verticalZ;
		BufferBuilder buffer = Tesselator.getInstance().begin(
				VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		vertex(buffer, matrix, centerX, centerY, centerZ, color, centerAlpha);
		for (int i = 0; i <= 40; i++) {
			float angle = (float) (Math.PI * 2.0D * i / 40.0D);
			float horizontal = Mth.cos(angle) * radius;
			float vertical = Mth.sin(angle) * radius;
			vertex(buffer, matrix, centerX + horizontal, centerY + vertical * verticalY,
					centerZ + vertical * verticalZ, color, edgeAlpha);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
			int color, float alpha) {
		vertex(buffer, matrix, x, y, z, ((color >> 16) & 0xFF) / 255.0F,
				((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
			float red, float green, float blue, float alpha) {
		buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
	}

	private static Vec3 colorVector(int rgb) {
		return new Vec3(((rgb >> 16) & 0xFF) / 255.0D, ((rgb >> 8) & 0xFF) / 255.0D,
				(rgb & 0xFF) / 255.0D);
	}

	private static int scaleColor(int rgb, float scale) {
		int red = Mth.clamp(Math.round(((rgb >> 16) & 0xFF) * scale), 0, 255);
		int green = Mth.clamp(Math.round(((rgb >> 8) & 0xFF) * scale), 0, 255);
		int blue = Mth.clamp(Math.round((rgb & 0xFF) * scale), 0, 255);
		return red << 16 | green << 8 | blue;
	}

	private static int mixColor(int first, int second, float secondWeight) {
		float firstWeight = 1.0F - secondWeight;
		int red = Math.round(((first >> 16) & 0xFF) * firstWeight + ((second >> 16) & 0xFF) * secondWeight);
		int green = Math.round(((first >> 8) & 0xFF) * firstWeight + ((second >> 8) & 0xFF) * secondWeight);
		int blue = Math.round((first & 0xFF) * firstWeight + (second & 0xFF) * secondWeight);
		return red << 16 | green << 8 | blue;
	}

	private enum SkyFamily {
		EMBER,
		FURNACE,
		TEMPEST
	}

	private record Profile(int floor, int fogColor, int zenithColor, int horizonColor, int accentColor,
			SkyFamily family, boolean denseFog, float fogNear, float fogFar, float lightRed, float lightGreen,
			float lightBlue, float lightTintStrength, ParticleOptions particle, int particleInterval) {
	}
}
