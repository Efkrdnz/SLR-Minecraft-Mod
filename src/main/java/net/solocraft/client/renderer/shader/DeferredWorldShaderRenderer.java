package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Replays custom world quad shaders after an Iris shader pack finishes
 * its world pipeline. GUI, HUD, tooltip, and fullscreen shaders do not use this
 * path.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DeferredWorldShaderRenderer {
	private static final int MAX_BATCHES = 4096;
	private static final int MAX_VERTICES = 1_000_000;
	private static final int MAX_POOLED_BATCHES = 128;
	private static final int INITIAL_VERTEX_CAPACITY = 32;
	private static final int MAX_RETAINED_VERTEX_CAPACITY = 16_384;
	private static final List<CapturedBatch> CAPTURED_BATCHES = new ArrayList<>();
	private static final Deque<CapturedBatch> BATCH_POOL = new ArrayDeque<>();
	private static final VertexConsumer DISCARDING_CONSUMER = new DiscardingVertexConsumer();

	private static Matrix4f worldModelView;
	private static Matrix4f previousProjection;
	private static VertexSorting previousVertexSorting;
	private static boolean modelViewPushed;
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

	/**
	 * Compatibility hook retained for existing stage renderers. Iris 1.21.1 uses
	 * Minecraft's main depth attachment directly, so no depth copy is needed.
	 */
	public static void requestDepthAtStage(RenderLevelStageEvent event,
			RenderLevelStageEvent.Stage normalStage) {
	}

	/** Selects a normal NeoForge stage or AFTER_LEVEL while a shader pack is active. */
	public static boolean isRenderStage(RenderLevelStageEvent event,
			RenderLevelStageEvent.Stage normalStage) {
		RenderLevelStageEvent.Stage expected = IrisCompat.isShaderPackInUse()
				? RenderLevelStageEvent.Stage.AFTER_LEVEL
				: normalStage;
		return event.getStage() == expected;
	}

	/**
	 * Starts a direct late world pass for stage-owned geometry. Returns false only
	 * when rendering should be skipped, such as during an Iris shadow pass.
	 */
	public static boolean beginWorldPass(RenderLevelStageEvent event) {
		if (!IrisCompat.isShaderPackInUse())
			return true;
		if (IrisCompat.isRenderingShadowPass() || modelViewPushed)
			return false;

		bindFinalTarget();
		previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
		previousVertexSorting = RenderSystem.getVertexSorting();
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelViewPushed = true;
		try {
			modelView.identity();
			modelView.mul(worldModelView != null
					? worldModelView : event.getModelViewMatrix());
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
	 * NeoForge 1.21.1 supplies an identity pose at AFTER_LEVEL. That is the same
	 * pose convention used by ordinary stage renderers: camera rotation belongs in
	 * RenderSystem's model-view stack and must not also be baked into vertices.
	 */
	public static PoseStack worldPoseStack(RenderLevelStageEvent event) {
		if (!IrisCompat.isShaderPackInUse()
				|| event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return event.getPoseStack();
		return new PoseStack();
	}

	public static void endWorldPass() {
		if (!modelViewPushed)
			return;
		if (previousProjection != null && previousVertexSorting != null)
			RenderSystem.setProjectionMatrix(previousProjection, previousVertexSorting);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.popMatrix();
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
		worldModelView = new Matrix4f(event.getModelViewMatrix());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void flushCapturedWorldQuads(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return;
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
	}

	@SubscribeEvent
	public static void clearWhenWorldCloses(ClientTickEvent.Post event) {
		if (Minecraft.getInstance().level == null || !IrisCompat.isShaderPackInUse()) {
			clearCapturedBatches();
			worldModelView = null;
		}
	}

	private static void bindFinalTarget() {
		var mainTarget = Minecraft.getInstance().getMainRenderTarget();
		if (mainTarget == null)
			return;
		mainTarget.bindWrite(false);
	}

	private static void clearCapturedBatches() {
		for (CapturedBatch batch : CAPTURED_BATCHES) {
			batch.clear();
			if (BATCH_POOL.size() < MAX_POOLED_BATCHES)
				BATCH_POOL.addLast(batch);
		}
		CAPTURED_BATCHES.clear();
		capturedVertexCount = 0;
		if (Minecraft.getInstance().level == null || !IrisCompat.isShaderPackInUse())
			worldModelView = null;
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
				output.addVertex(positions[positionOffset], positions[positionOffset + 1],
						positions[positionOffset + 2])
						.setColor((color >>> 24) & 0xFF, (color >>> 16) & 0xFF,
								(color >>> 8) & 0xFF, color & 0xFF)
						.setUv(uvs[uvOffset], uvs[uvOffset + 1])
						.setUv1(unpackLow(overlay), unpackHigh(overlay))
						.setUv2(unpackLow(light), unpackHigh(light))
						.setNormal(normals[positionOffset], normals[positionOffset + 1],
								normals[positionOffset + 2]);
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

		private CapturingVertexConsumer(CapturedBatch batch) {
			this.batch = batch;
		}

		private void prepareForWrite() {
			currentIndex = -1;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			if (capturedVertexCount >= MAX_VERTICES) {
				currentIndex = -1;
				return this;
			}
			currentIndex = batch.add(x, y, z, packColor(255, 255, 255, 255));
			capturedVertexCount++;
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			if (currentIndex >= 0)
				batch.colors[currentIndex] = packColor(red, green, blue, alpha);
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			if (currentIndex >= 0) {
				int offset = currentIndex * 2;
				batch.uvs[offset] = u;
				batch.uvs[offset + 1] = v;
			}
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			if (currentIndex >= 0)
				batch.overlays[currentIndex] = packPair(u, v);
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			if (currentIndex >= 0)
				batch.lights[currentIndex] = packPair(u, v);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			if (currentIndex >= 0) {
				int offset = currentIndex * 3;
				batch.normals[offset] = x;
				batch.normals[offset + 1] = y;
				batch.normals[offset + 2] = z;
			}
			return this;
		}
	}

	private static final class DiscardingVertexConsumer implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
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
