package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Replays custom world quad shaders after an Iris/Oculus shader pack finishes
 * its world pipeline. GUI, HUD, tooltip, and fullscreen shaders do not use this
 * path.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeferredWorldShaderRenderer {
	private static final int MAX_BATCHES = 4096;
	private static final int MAX_VERTICES = 1_000_000;
	private static final int MAX_POOLED_BATCHES = 128;
	private static final int INITIAL_VERTEX_CAPACITY = 32;
	private static final int MAX_RETAINED_VERTEX_CAPACITY = 16_384;
	private static final List<CapturedBatch> CAPTURED_BATCHES = new ArrayList<>();
	private static final Deque<CapturedBatch> BATCH_POOL = new ArrayDeque<>();
	private static final VertexConsumer DISCARDING_CONSUMER = new DiscardingVertexConsumer();

	private static TextureTarget finalDepthSnapshot;
	private static Matrix4f worldModelView;
	private static Matrix4f worldPose;
	private static Matrix3f worldNormal;
	private static Matrix4f previousProjection;
	private static VertexSorting previousVertexSorting;
	private static boolean modelViewPushed;
	private static boolean depthRequested;
	private static boolean depthCapturedThisFrame;
	private static boolean depthRestoredThisFrame;
	private static int capturedVertexCount;

	private DeferredWorldShaderRenderer() {
	}

	/**
	 * Returns the ordinary buffer without shader packs. With a pack active, the
	 * completed vertices are captured for replay during AFTER_LEVEL.
	 */
	public static VertexConsumer buffer(MultiBufferSource originalBuffers, RenderType renderType) {
		return buffer(originalBuffers, renderType, null, true);
	}

	/** Captures an effect and only preserves world depth when it actually uses it. */
	public static VertexConsumer buffer(MultiBufferSource originalBuffers, RenderType renderType,
			boolean requiresDepth) {
		return buffer(originalBuffers, renderType, null, requiresDepth);
	}

	public static VertexConsumer buffer(MultiBufferSource originalBuffers, RenderType renderType,
			Runnable setupUniforms) {
		return buffer(originalBuffers, renderType, setupUniforms, true);
	}

	public static VertexConsumer buffer(MultiBufferSource originalBuffers, RenderType renderType,
			Runnable setupUniforms, boolean requiresDepth) {
		if (!IrisCompat.isShaderPackInUse())
			return originalBuffers.getBuffer(renderType);
		if (IrisCompat.isRenderingShadowPass())
			return DISCARDING_CONSUMER;
		if (CAPTURED_BATCHES.size() >= MAX_BATCHES || capturedVertexCount >= MAX_VERTICES)
			return DISCARDING_CONSUMER;

		CapturedBatch batch = lastCompatibleBatch(renderType, setupUniforms);
		if (batch == null) {
			batch = BATCH_POOL.pollFirst();
			if (batch == null)
				batch = new CapturedBatch();
			batch.prepare(renderType, setupUniforms);
			CAPTURED_BATCHES.add(batch);
		} else {
			batch.consumer.prepareForWrite();
		}
		if (requiresDepth)
			depthRequested = true;
		return batch.consumer;
	}

	private static CapturedBatch lastCompatibleBatch(RenderType renderType,
			Runnable setupUniforms) {
		if (CAPTURED_BATCHES.isEmpty())
			return null;
		CapturedBatch last = CAPTURED_BATCHES.get(CAPTURED_BATCHES.size() - 1);
		return last.renderType == renderType && last.setupUniforms == setupUniforms
				? last
				: null;
	}

	/** Requests final world depth for a stage-owned effect that will render late. */
	public static void requestDepthAtStage(RenderLevelStageEvent event,
			RenderLevelStageEvent.Stage normalStage) {
		if (event.getStage() == normalStage && IrisCompat.isShaderPackInUse()
				&& !IrisCompat.isRenderingShadowPass())
			depthRequested = true;
	}

	/** Selects a normal Forge stage or AFTER_LEVEL while a shader pack is active. */
	public static boolean isRenderStage(RenderLevelStageEvent event,
			RenderLevelStageEvent.Stage normalStage) {
		RenderLevelStageEvent.Stage expected = IrisCompat.isShaderPackInUse()
				? RenderLevelStageEvent.Stage.AFTER_LEVEL
				: normalStage;
		return event.getStage() == expected;
	}

	/**
	 * Starts a direct late world pass for stage-owned geometry. Returns false only
	 * when rendering should be skipped, such as during an Oculus shadow pass.
	 */
	public static boolean beginWorldPass(RenderLevelStageEvent event) {
		if (!IrisCompat.isShaderPackInUse())
			return true;
		if (IrisCompat.isRenderingShadowPass() || modelViewPushed)
			return false;

		bindFinalTarget();
		previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
		previousVertexSorting = RenderSystem.getVertexSorting();
		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelViewPushed = true;
		try {
			modelView.setIdentity();
			if (worldModelView != null)
				modelView.mulPoseMatrix(worldModelView);
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(new Matrix4f(event.getProjectionMatrix()),
					VertexSorting.DISTANCE_TO_ORIGIN);
			return true;
		} catch (RuntimeException exception) {
			endWorldPass();
			throw exception;
		}
	}

	/**
	 * Forge 1.20.1 supplies its projection PoseStack to AFTER_LEVEL, unlike the
	 * ordinary in-world stages. Recreate the camera/world stack captured earlier
	 * in the same frame so direct late effects keep their normal orientation.
	 */
	public static PoseStack worldPoseStack(RenderLevelStageEvent event) {
		if (!IrisCompat.isShaderPackInUse()
				|| event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
				|| worldPose == null)
			return event.getPoseStack();
		PoseStack copy = new PoseStack();
		copy.last().pose().set(worldPose);
		if (worldNormal != null)
			copy.last().normal().set(worldNormal);
		return copy;
	}

	public static void endWorldPass() {
		if (!modelViewPushed)
			return;
		if (previousProjection != null && previousVertexSorting != null)
			RenderSystem.setProjectionMatrix(previousProjection, previousVertexSorting);
		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		previousProjection = null;
		previousVertexSorting = null;
		modelViewPushed = false;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void captureMainCameraMatrices(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
				|| !IrisCompat.isShaderPackInUse() || IrisCompat.isRenderingShadowPass())
			return;
		worldModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
		worldPose = new Matrix4f(event.getPoseStack().last().pose());
		worldNormal = new Matrix3f(event.getPoseStack().last().normal());
		depthCapturedThisFrame = false;
		depthRestoredThisFrame = false;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void captureFinalDepth(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
				|| !depthRequested || !IrisCompat.isShaderPackInUse()
				|| IrisCompat.isRenderingShadowPass())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget mainTarget = minecraft.getMainRenderTarget();
		if (mainTarget == null || !mainTarget.useDepth)
			return;
		if (finalDepthSnapshot == null || finalDepthSnapshot.width != mainTarget.width
				|| finalDepthSnapshot.height != mainTarget.height) {
			releaseDepthSnapshot();
			finalDepthSnapshot = new TextureTarget(mainTarget.width, mainTarget.height, true,
					Minecraft.ON_OSX);
		}
		finalDepthSnapshot.copyDepthFrom(mainTarget);
		mainTarget.bindWrite(false);
		depthCapturedThisFrame = true;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void flushCapturedWorldQuads(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return;
		try {
			if (!IrisCompat.isShaderPackInUse()) {
				clearCapturedBatches();
				return;
			}
			if (CAPTURED_BATCHES.isEmpty() || IrisCompat.isRenderingShadowPass())
				return;
			if (!beginWorldPass(event)) {
				clearCapturedBatches();
				return;
			}
			MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
			try {
				for (CapturedBatch batch : CAPTURED_BATCHES) {
					if (batch.setupUniforms != null)
						batch.setupUniforms.run();
					VertexConsumer output = buffers.getBuffer(batch.renderType);
					batch.replay(output);
					buffers.endBatch(batch.renderType);
				}
			} finally {
				clearCapturedBatches();
				endWorldPass();
			}
		} finally {
			depthRequested = false;
			depthCapturedThisFrame = false;
			depthRestoredThisFrame = false;
		}
	}

	@SubscribeEvent
	public static void clearWhenWorldCloses(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		if (Minecraft.getInstance().level == null || !IrisCompat.isShaderPackInUse()) {
			clearCapturedBatches();
			releaseDepthSnapshot();
			depthRequested = false;
			depthCapturedThisFrame = false;
			depthRestoredThisFrame = false;
			worldModelView = null;
			worldPose = null;
			worldNormal = null;
		}
	}

	private static void bindFinalTarget() {
		RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
		if (mainTarget == null)
			return;
		if (depthCapturedThisFrame && !depthRestoredThisFrame
				&& finalDepthSnapshot != null
				&& finalDepthSnapshot.width == mainTarget.width
				&& finalDepthSnapshot.height == mainTarget.height) {
			mainTarget.copyDepthFrom(finalDepthSnapshot);
			depthRestoredThisFrame = true;
		}
		mainTarget.bindWrite(false);
	}

	private static void releaseDepthSnapshot() {
		if (finalDepthSnapshot != null) {
			finalDepthSnapshot.destroyBuffers();
			finalDepthSnapshot = null;
		}
	}

	private static void clearCapturedBatches() {
		for (CapturedBatch batch : CAPTURED_BATCHES) {
			batch.clear();
			if (BATCH_POOL.size() < MAX_POOLED_BATCHES)
				BATCH_POOL.addLast(batch);
		}
		CAPTURED_BATCHES.clear();
		capturedVertexCount = 0;
		if (Minecraft.getInstance().level == null || !IrisCompat.isShaderPackInUse()) {
			worldModelView = null;
			worldPose = null;
			worldNormal = null;
		}
	}

	private static final class CapturedBatch {
		private RenderType renderType;
		private Runnable setupUniforms;
		private int vertexCount;
		private float[] positions = new float[INITIAL_VERTEX_CAPACITY * 3];
		private int[] colors = new int[INITIAL_VERTEX_CAPACITY];
		private float[] uvs = new float[INITIAL_VERTEX_CAPACITY * 2];
		private int[] overlays = new int[INITIAL_VERTEX_CAPACITY];
		private int[] lights = new int[INITIAL_VERTEX_CAPACITY];
		private float[] normals = new float[INITIAL_VERTEX_CAPACITY * 3];
		private final CapturingVertexConsumer consumer = new CapturingVertexConsumer(this);

		private void prepare(RenderType renderType, Runnable setupUniforms) {
			this.renderType = renderType;
			this.setupUniforms = setupUniforms;
			this.vertexCount = 0;
			consumer.prepareForWrite();
		}

		private int add(float x, float y, float z, int color) {
			ensureCapacity(vertexCount + 1);
			int index = vertexCount++;
			int positionOffset = index * 3;
			positions[positionOffset] = x;
			positions[positionOffset + 1] = y;
			positions[positionOffset + 2] = z;
			colors[index] = color;
			int uvOffset = index * 2;
			uvs[uvOffset] = 0.0F;
			uvs[uvOffset + 1] = 0.0F;
			overlays[index] = 0;
			lights[index] = packPair(240, 240);
			normals[positionOffset] = 0.0F;
			normals[positionOffset + 1] = 1.0F;
			normals[positionOffset + 2] = 0.0F;
			return index;
		}

		private void ensureCapacity(int required) {
			if (required <= colors.length)
				return;
			int capacity = Math.max(required, colors.length * 2);
			positions = Arrays.copyOf(positions, capacity * 3);
			colors = Arrays.copyOf(colors, capacity);
			uvs = Arrays.copyOf(uvs, capacity * 2);
			overlays = Arrays.copyOf(overlays, capacity);
			lights = Arrays.copyOf(lights, capacity);
			normals = Arrays.copyOf(normals, capacity * 3);
		}

		private void replay(VertexConsumer output) {
			for (int index = 0; index < vertexCount; index++) {
				int positionOffset = index * 3;
				int uvOffset = index * 2;
				int color = colors[index];
				int overlay = overlays[index];
				int light = lights[index];
				output.vertex(positions[positionOffset], positions[positionOffset + 1],
						positions[positionOffset + 2])
						.color((color >>> 24) & 0xFF, (color >>> 16) & 0xFF,
								(color >>> 8) & 0xFF, color & 0xFF)
						.uv(uvs[uvOffset], uvs[uvOffset + 1])
						.overlayCoords(unpackLow(overlay), unpackHigh(overlay))
						.uv2(unpackLow(light), unpackHigh(light))
						.normal(normals[positionOffset], normals[positionOffset + 1],
								normals[positionOffset + 2])
						.endVertex();
			}
		}

		private void clear() {
			renderType = null;
			setupUniforms = null;
			vertexCount = 0;
			consumer.prepareForWrite();
			if (colors.length > MAX_RETAINED_VERTEX_CAPACITY) {
				positions = new float[INITIAL_VERTEX_CAPACITY * 3];
				colors = new int[INITIAL_VERTEX_CAPACITY];
				uvs = new float[INITIAL_VERTEX_CAPACITY * 2];
				overlays = new int[INITIAL_VERTEX_CAPACITY];
				lights = new int[INITIAL_VERTEX_CAPACITY];
				normals = new float[INITIAL_VERTEX_CAPACITY * 3];
			}
		}
	}

	private static final class CapturingVertexConsumer implements VertexConsumer {
		private final CapturedBatch batch;
		private int currentIndex = -1;
		private boolean hasDefaultColor;
		private int defaultRed = 255;
		private int defaultGreen = 255;
		private int defaultBlue = 255;
		private int defaultAlpha = 255;

		private CapturingVertexConsumer(CapturedBatch batch) {
			this.batch = batch;
		}

		private void prepareForWrite() {
			currentIndex = -1;
			hasDefaultColor = false;
			defaultRed = 255;
			defaultGreen = 255;
			defaultBlue = 255;
			defaultAlpha = 255;
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			if (capturedVertexCount >= MAX_VERTICES) {
				currentIndex = -1;
				return this;
			}
			int color = packColor(
					hasDefaultColor ? defaultRed : 255,
					hasDefaultColor ? defaultGreen : 255,
					hasDefaultColor ? defaultBlue : 255,
					hasDefaultColor ? defaultAlpha : 255);
			currentIndex = batch.add((float) x, (float) y, (float) z, color);
			capturedVertexCount++;
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			if (currentIndex >= 0)
				batch.colors[currentIndex] = packColor(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			if (currentIndex >= 0) {
				int offset = currentIndex * 2;
				batch.uvs[offset] = u;
				batch.uvs[offset + 1] = v;
			}
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			if (currentIndex >= 0)
				batch.overlays[currentIndex] = packPair(u, v);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			if (currentIndex >= 0)
				batch.lights[currentIndex] = packPair(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			if (currentIndex >= 0) {
				int offset = currentIndex * 3;
				batch.normals[offset] = x;
				batch.normals[offset + 1] = y;
				batch.normals[offset + 2] = z;
			}
			return this;
		}

		@Override
		public void endVertex() {
			currentIndex = -1;
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			hasDefaultColor = true;
			defaultRed = red;
			defaultGreen = green;
			defaultBlue = blue;
			defaultAlpha = alpha;
		}

		@Override
		public void unsetDefaultColor() {
			hasDefaultColor = false;
		}
	}

	private static final class DiscardingVertexConsumer implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		@Override
		public void endVertex() {
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
		}

		@Override
		public void unsetDefaultColor() {
		}
	}

	private static int packColor(int red, int green, int blue, int alpha) {
		return ((red & 0xFF) << 24) | ((green & 0xFF) << 16)
				| ((blue & 0xFF) << 8) | (alpha & 0xFF);
	}

	private static int packPair(int low, int high) {
		return (low & 0xFFFF) | ((high & 0xFFFF) << 16);
	}

	private static int unpackLow(int packed) {
		return packed & 0xFFFF;
	}

	private static int unpackHigh(int packed) {
		return packed >>> 16 & 0xFFFF;
	}
}
