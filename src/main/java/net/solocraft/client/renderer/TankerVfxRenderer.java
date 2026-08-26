package net.solocraft.client.renderer;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.client.renderer.shader.IrisCompat;
import net.solocraft.client.renderer.shader.TankerVfxRenderTypes;
import net.solocraft.network.TankerVfxEventMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stage-owned, packet-driven presentation for Tank Leap, Taunt, Shield Bash,
 * Reinforcement, Willpower, and Protection Mark.
 *
 * <p>No visual entity or per-tick network packet is created. Every timeline is
 * reconstructed from {@code serverStartTick}, seed, and bounded event facts.</p>
 */
@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.GAME,
		value = Dist.CLIENT
)
public final class TankerVfxRenderer {
	public static final int MAX_QUEUED_EVENTS = 96;
	public static final int FULL_MAX_VISIBLE_EVENTS = 32;
	public static final int LOW_MAX_VISIBLE_EVENTS = 16;
	public static final int OFF_MAX_VISIBLE_EVENTS = 8;
	public static final int FULL_MAX_VERTICES = 24_000;
	public static final int LOW_MAX_VERTICES = 8_000;
	public static final int OFF_MAX_VERTICES = 2_000;
	public static final int FULL_RING_SEGMENTS = 32;
	public static final int LOW_RING_SEGMENTS = 12;
	public static final int OFF_RING_SEGMENTS = 8;
	public static final int FULL_MAX_BURST_PARTICLES = 24;
	public static final int LOW_MAX_BURST_PARTICLES = 8;
	public static final int OFF_MAX_BURST_PARTICLES = 0;
	public static final int FULL_MAX_PERSISTENT_PARTICLES_PER_TICK = 2;
	public static final int FULL_MAX_PERSISTENT_PARTICLES_PER_EVENT = 96;

	private static final int MATERIAL_STEEL = 0;
	private static final int MATERIAL_GOLD = 1;
	private static final int MATERIAL_EMBER = 2;
	private static final int MATERIAL_CRACK = 3;
	private static final int MATERIAL_DUST = 4;

	private static final int STEEL_DARK = 0x252B2E;
	private static final int STEEL_MID = 0x465158;
	private static final int STEEL_EDGE = 0x667078;
	private static final int GOLD_PALE = 0xE7CD82;
	private static final int GOLD_HOT = 0xFFE4A1;
	private static final int EMBER = 0xE76C2D;
	private static final int EMBER_HOT = 0xFFB05A;
	private static final int DUST = 0xB8AA91;

	private static final List<ActiveEvent> EVENTS = new ArrayList<>(MAX_QUEUED_EVENTS);
	private static long nextSequence;

	private TankerVfxRenderer() {
	}

	/** Called on the client thread by {@link TankerVfxEventMessage}. */
	public static void enqueue(TankerVfxEventMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> enqueue(message));
			return;
		}
		if (minecraft.level == null)
			return;

		long now = minecraft.level.getGameTime();
		pruneInvalidAndExpired(now);
		if (!TankerVfxEventMessage.isKnownEventType(message.eventType))
			return;
		if (now - message.serverStartTick >= message.duration)
			return;
		if (isDuplicate(message)) {
			// A retransmitted authoritative landing is still terminal for any
			// stale travel timeline that survived packet reordering.
			if (message.eventType == TankerVfxEventMessage.LEAP_LAND)
				removeTypes(message.ownerEntityId, TankerVfxEventMessage.LEAP_START);
			return;
		}

		applyStateTransition(message);
		ActiveEvent incoming = new ActiveEvent(message, nextSequence++, now);
		if (EVENTS.size() >= MAX_QUEUED_EVENTS && !evictFor(incoming, minecraft))
			return;
		EVENTS.add(incoming);
		trimQueueToLimit();

		double elapsed = Math.max(0.0D, now - message.serverStartTick);
		if (elapsed <= 6.0D && !message.hasFlag(TankerVfxEventMessage.FLAG_REPLAY)
				&& !message.hasFlag(TankerVfxEventMessage.FLAG_SILENT))
			playInitialSound(minecraft, message);
	}

	public static void clear() {
		EVENTS.clear();
		nextSequence = 0L;
	}

	/**
	 * Render-type reload hook. Tanker events contain no GPU handles, so valid
	 * authoritative timelines survive shader replacement while expired or invalid
	 * entries are discarded. The render types independently use vanilla fallbacks
	 * until their replacement ShaderInstances finish loading.
	 */
	public static void onResourceReload() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(TankerVfxRenderer::onResourceReload);
			return;
		}
		if (minecraft.level == null) {
			clear();
			return;
		}
		pruneInvalidAndExpired(minecraft.level.getGameTime());
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ClientLevel)
			clear();
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
				&& event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return;
		if (IrisCompat.isRenderingShadowPass())
			return;

		boolean renderStage = DeferredWorldShaderRenderer.isRenderStage(event,
				RenderLevelStageEvent.Stage.AFTER_PARTICLES);
		if (!renderStage && event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null || EVENTS.isEmpty())
			return;

		long now = minecraft.level.getGameTime();
		pruneInvalidAndExpired(now);
		if (EVENTS.isEmpty())
			return;

		DeferredWorldShaderRenderer.requestDepthAtStage(event,
				RenderLevelStageEvent.Stage.AFTER_PARTICLES);
		if (!renderStage)
			return;

		Quality quality = Quality.current(minecraft);
		List<VisibleEvent> visible = collectVisible(event, minecraft, quality, now);
		if (visible.isEmpty())
			return;
		playAmbientCues(minecraft, visible, quality);

		PoseStack poseStack = DeferredWorldShaderRenderer.worldPoseStack(event);
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		RenderFrame frame = new RenderFrame(poseStack, event.getCamera().getPosition(),
				minecraft.getEntityRenderDispatcher().cameraOrientation(),
				event.getPartialTick().getGameTimeDeltaPartialTick(false), quality);
		FrameBudget budget = new FrameBudget(quality.maxVertices);

		RenderType surfaceType = TankerVfxRenderTypes.surface();
		VertexConsumer surface = DeferredWorldShaderRenderer.buffer(buffers, surfaceType, true);
		for (VisibleEvent visual : visible)
			renderEvent(frame, visual, surface, budget, Pass.SURFACE);
		buffers.endBatch(surfaceType);

		boolean hasGlow = quality != Quality.OFF
				&& visible.stream().anyMatch(visual -> quality.allowsGlow(visual.event));
		if (hasGlow && budget.remaining() >= 4) {
			RenderType emissiveType = TankerVfxRenderTypes.emissive();
			VertexConsumer emissive = DeferredWorldShaderRenderer.buffer(buffers,
					emissiveType, true);
			for (VisibleEvent visual : visible) {
				if (quality.allowsGlow(visual.event))
					renderEvent(frame, visual, emissive, budget, Pass.EMISSIVE);
			}
			buffers.endBatch(emissiveType);
		}
	}

	private static List<VisibleEvent> collectVisible(RenderLevelStageEvent renderEvent,
			Minecraft minecraft, Quality quality, long now) {
		Vec3 camera = renderEvent.getCamera().getPosition();
		float partialTick = renderEvent.getPartialTick()
				.getGameTimeDeltaPartialTick(false);
		List<VisibleEvent> candidates = new ArrayList<>(EVENTS.size());

		for (ActiveEvent event : EVENTS) {
			float elapsed = event.elapsed(now, partialTick);
			if (elapsed < 0.0F || elapsed >= event.duration())
				continue;
			if (!quality.allowsEvent(event))
				continue;
			if (quality == Quality.OFF
					&& event.type() == TankerVfxEventMessage.TAUNT_RING
					&& elapsed >= 12.0F)
				continue;

			Anchor anchor = resolveAnchor(minecraft.level, minecraft, event, partialTick);
			AABB bounds = eventBounds(event.type(), anchor.position);
			double distanceSqr = distanceToBoundsSqr(camera, bounds);
			if (distanceSqr > quality.renderDistance * quality.renderDistance)
				continue;
			if (!renderEvent.getFrustum().isVisible(bounds))
				continue;

			int packedLight = LevelRenderer.getLightColor(minecraft.level,
					BlockPos.containing(anchor.position));
			candidates.add(new VisibleEvent(event, anchor.position, anchor.yaw,
					anchor.pitch, elapsed, distanceSqr, packedLight,
					anchor.firstPersonOwner));
		}

		candidates.sort(Comparator
				.comparing((VisibleEvent visual) -> !visual.event.essential())
				.thenComparingDouble(visual -> visual.distanceSqr)
				.thenComparingLong(visual -> visual.event.sequence));
		if (candidates.size() > quality.maxVisible)
			return new ArrayList<>(candidates.subList(0, quality.maxVisible));
		return candidates;
	}

	private static Anchor resolveAnchor(ClientLevel level, Minecraft minecraft,
			ActiveEvent event, float partialTick) {
		Vec3 position = event.origin();
		float yaw = event.yaw();
		float pitch = event.pitch();
		boolean firstPersonOwner = false;

		if (followsOwner(event.type())) {
			Entity owner = level.getEntity(event.ownerId());
			if (owner != null && !owner.isRemoved()) {
				position = new Vec3(
						Mth.lerp(partialTick, owner.xo, owner.getX()),
						Mth.lerp(partialTick, owner.yo, owner.getY()),
						Mth.lerp(partialTick, owner.zo, owner.getZ()));
				yaw = Mth.rotLerp(partialTick, owner.yRotO, owner.getYRot());
				pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
				firstPersonOwner = minecraft.player != null
						&& owner.getId() == minecraft.player.getId()
						&& minecraft.options.getCameraType().isFirstPerson();
			}
		}
		return new Anchor(position, yaw, pitch, firstPersonOwner);
	}

	private static AABB eventBounds(byte type, Vec3 anchor) {
		double radius;
		double below = 0.35D;
		double above;
		switch (type) {
			case TankerVfxEventMessage.TAUNT_RING -> {
				radius = TankerVfxEventMessage.TAUNT_RADIUS + 0.75D;
				above = 2.5D;
			}
			case TankerVfxEventMessage.MARK_DEPLOY,
					TankerVfxEventMessage.MARK_INTEGRITY_THRESHOLD,
					TankerVfxEventMessage.MARK_BREAK,
					TankerVfxEventMessage.MARK_CANCEL -> {
				radius = TankerVfxEventMessage.MARK_RADIUS + 0.75D;
				above = 3.0D;
			}
			case TankerVfxEventMessage.LEAP_LAND -> {
				radius = TankerVfxEventMessage.LEAP_LAND_RADIUS + 1.0D;
				above = 2.8D;
			}
			case TankerVfxEventMessage.BASH_SWEEP -> {
				radius = TankerVfxEventMessage.BASH_REACH + 1.2D;
				above = 3.0D;
			}
			default -> {
				radius = 3.25D;
				above = 3.75D;
			}
		}
		return new AABB(anchor.x - radius, anchor.y - below, anchor.z - radius,
				anchor.x + radius, anchor.y + above, anchor.z + radius);
	}

	private static double distanceToBoundsSqr(Vec3 point, AABB bounds) {
		double dx = Math.max(Math.max(bounds.minX - point.x, 0.0D),
				point.x - bounds.maxX);
		double dy = Math.max(Math.max(bounds.minY - point.y, 0.0D),
				point.y - bounds.maxY);
		double dz = Math.max(Math.max(bounds.minZ - point.z, 0.0D),
				point.z - bounds.maxZ);
		return dx * dx + dy * dy + dz * dz;
	}

	private static void renderEvent(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		PoseStack stack = frame.poseStack;
		stack.pushPose();
		stack.translate(visual.position.x - frame.cameraPosition.x,
				visual.position.y - frame.cameraPosition.y,
				visual.position.z - frame.cameraPosition.z);
		stack.mulPose(Axis.YP.rotationDegrees(-visual.yaw));

		switch (visual.event.type()) {
			case TankerVfxEventMessage.LEAP_START ->
					renderLeapStart(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.LEAP_LAND ->
					renderLeapLand(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.TAUNT_RING ->
					renderTaunt(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.BASH_SWEEP ->
					renderBashSweep(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.BASH_HIT ->
					renderBashHit(frame, visual, vertices, budget, pass, false);
			case TankerVfxEventMessage.BASH_STRAIN_RELIEF ->
					renderBashHit(frame, visual, vertices, budget, pass, true);
			case TankerVfxEventMessage.REINFORCEMENT_BRACE_START ->
					renderReinforcementBrace(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.REINFORCEMENT_BRACE_HIT ->
					renderReinforcementHit(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.REINFORCEMENT_STANCE_START ->
					renderReinforcementStance(frame, visual, vertices, budget, pass, false);
			case TankerVfxEventMessage.REINFORCEMENT_STANCE_END ->
					renderReinforcementStance(frame, visual, vertices, budget, pass, true);
			case TankerVfxEventMessage.WILLPOWER_START ->
					renderWillpower(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.WILLPOWER_STRAIN_THRESHOLD ->
					renderWillpowerThreshold(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.WILLPOWER_SETTLE ->
					renderWillpowerSettle(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.WILLPOWER_BREAK ->
					renderWillpowerBreak(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.MARK_DEPLOY ->
					renderMark(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.MARK_INTEGRITY_THRESHOLD ->
					renderMarkThreshold(frame, visual, vertices, budget, pass);
			case TankerVfxEventMessage.MARK_BREAK ->
					renderMarkEnd(frame, visual, vertices, budget, pass, true);
			case TankerVfxEventMessage.MARK_CANCEL ->
					renderMarkEnd(frame, visual, vertices, budget, pass, false);
			default -> {
			}
		}
		stack.popPose();
	}

	// Timeline renderers are kept together below so every visual phase is auditable.

	/** Tank Leap: 0-4t launch compression, then a bounded owner-following travel read. */
	private static void renderLeapStart(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		float elapsed = visual.elapsed;
		float launch = Mth.clamp(elapsed / 4.0F, 0.0F, 1.0F);
		float release = elapsed < 4.0F ? Mth.sin(launch * Mth.PI) : 0.0F;
		float fade = fadeOut(elapsed, visual.event.duration(), 3.0F);
		PoseStack stack = frame.poseStack;
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;

		if (pass == Pass.SURFACE) {
			float plateAlpha = frame.quality == Quality.OFF ? 255.0F : 180.0F;
			for (int side = -1; side <= 1; side += 2) {
				stack.pushPose();
				stack.translate(side * 0.27D, 0.07D - release * 0.055D, 0.08D);
				stack.mulPose(Axis.YP.rotationDegrees(side * 5.0F));
				drawBox(vertices, stack.last(), budget, -0.19F, 0.0F, -0.35F,
						0.19F, 0.1F + release * 0.055F, 0.32F,
						STEEL_DARK, alpha(plateAlpha * fade), MATERIAL_STEEL, light);
				stack.popPose();
			}
			return;
		}

		if (elapsed <= 5.0F) {
			for (int side = -1; side <= 1; side += 2) {
				stack.pushPose();
				stack.translate(side * 0.27D, 0.18D, 0.1D);
				drawPlate(vertices, stack.last(), budget, 0.29F, 0.055F, 0.0F,
						1.0F, GOLD_PALE, alpha(150.0F * fade), MATERIAL_GOLD, light);
				stack.popPose();
			}
		}
		if (frame.quality != Quality.OFF && elapsed >= 3.0F) {
			int motes = frame.quality == Quality.FULL ? 6 : 4;
			drawTrailDust(frame, visual, vertices, budget,
					Math.min(motes, frame.quality.burstParticles), fade);
		}
	}

	/** Tank Leap: exact five-block landing telegraph authored only by LEAP_LAND. */
	private static void renderLeapLand(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		float progress = easeOut(Mth.clamp(visual.elapsed / 10.0F, 0.0F, 1.0F));
		float radius = Math.max(0.45F,
				(float) TankerVfxEventMessage.LEAP_LAND_RADIUS * progress);
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 3.2F);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;

		if (pass == Pass.SURFACE) {
			int slabs = frame.quality == Quality.FULL ? 16 : 8;
			int surfaceAlpha = frame.quality == Quality.OFF
					? alpha(255.0F * fade) : alpha(190.0F * fade);
			for (int index = 0; index < slabs; index++) {
				float center = index * 360.0F / slabs + hash01(visual.event.seed(),
						index, 17) * 4.0F;
				float halfWidth = frame.quality == Quality.FULL ? 7.0F : 13.0F;
				float inner = 0.36F + hash01(visual.event.seed(), index, 23) * 0.4F;
				drawFloorWedge(vertices, frame.poseStack.last(), budget, inner, radius,
						center - halfWidth, center + halfWidth, 0.025F,
						index % 3 == 0 ? STEEL_MID : STEEL_DARK, surfaceAlpha,
						MATERIAL_STEEL, light);
			}
			return;
		}

		drawGroundRing(vertices, frame.poseStack.last(), budget,
				Math.max(0.15F, radius - 0.12F), radius,
				frame.quality.ringSegments, 0.038F, GOLD_PALE,
				alpha(165.0F * fade), MATERIAL_GOLD, light);
		drawGroundRing(vertices, frame.poseStack.last(), budget, 0.1F,
				0.72F + progress * 0.34F, Math.min(12, frame.quality.ringSegments),
				0.044F, EMBER_HOT, alpha(185.0F * fade),
				MATERIAL_EMBER, light);
		int sparks = frame.quality == Quality.FULL ? 18 : 8;
		drawRadialSparks(frame, visual, vertices, budget,
				Math.min(sparks, frame.quality.burstParticles), radius * 0.72F,
				0.08F, 0.75F, EMBER, fade);
	}

	/** Taunt: 0-4t plant, 4-12t expansion, exact 12-block held pressure ring. */
	private static void renderTaunt(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		float elapsed = visual.elapsed;
		float plant = easeOut(Mth.clamp(elapsed / 4.0F, 0.0F, 1.0F));
		float expansion = easeOut(Mth.clamp((elapsed - 4.0F) / 8.0F, 0.0F, 1.0F));
		float plantedRadius = 1.3F;
		float radius = elapsed < 4.0F ? 0.65F + plant * 0.65F
				: plantedRadius + ((float) TankerVfxEventMessage.TAUNT_RADIUS
						- plantedRadius) * expansion;
		if (elapsed >= 12.0F)
			radius = (float) TankerVfxEventMessage.TAUNT_RADIUS;
		float heldPulse = elapsed < 12.0F ? 1.0F
				: 0.62F + 0.08F * Mth.sin(elapsed * 0.22F);
		float fade = fadeOut(elapsed, visual.event.duration(), 6.0F);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;

		if (pass == Pass.SURFACE) {
			int surfaceAlpha = frame.quality == Quality.OFF ? 255
					: alpha((elapsed < 12.0F ? 192.0F : 96.0F) * fade);
			drawGroundRing(vertices, frame.poseStack.last(), budget,
					Math.max(0.0F, radius - 0.34F), radius,
					frame.quality.ringSegments, 0.026F, STEEL_MID,
					surfaceAlpha, MATERIAL_STEEL, light);

			float plateRadius = Math.max(0.72F, radius * 0.72F);
			for (int index = 0; index < 4; index++) {
				frame.poseStack.pushPose();
				frame.poseStack.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
				frame.poseStack.translate(0.0D, 0.11D, plateRadius);
				frame.poseStack.mulPose(Axis.XP.rotationDegrees(-54.0F));
				drawPlate(vertices, frame.poseStack.last(), budget,
						1.35F * Math.max(0.35F, expansion), 0.68F, 0.0F,
						0.72F, STEEL_DARK, surfaceAlpha, MATERIAL_STEEL, light);
				frame.poseStack.popPose();
			}

			if (elapsed < 5.0F) {
				frame.poseStack.pushPose();
				frame.poseStack.translate(0.0D, 0.08D, 0.0D);
				drawCrest(vertices, frame.poseStack.last(), budget, 0.68F,
						0.9F * plant, STEEL_DARK, surfaceAlpha,
						MATERIAL_STEEL, light, 0);
				frame.poseStack.popPose();
			}
			return;
		}

		drawGroundRing(vertices, frame.poseStack.last(), budget,
				Math.max(0.0F, radius - 0.095F), radius,
				frame.quality.ringSegments, 0.04F, GOLD_PALE,
				alpha(132.0F * heldPulse * fade), MATERIAL_GOLD, light);
		if (elapsed < 9.0F) {
			int sparks = frame.quality == Quality.FULL ? 10 : 4;
			drawRadialSparks(frame, visual, vertices, budget,
					Math.min(sparks, frame.quality.burstParticles),
					Math.max(0.7F, radius * 0.78F), 0.05F, 0.42F,
					EMBER, fade);
		}
	}

	/** Shield Bash: 0-2t load, 2-6t four-tick commitment to 3.6 blocks. */
	private static void renderBashSweep(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		float elapsed = visual.elapsed;
		float load = easeOut(Mth.clamp(elapsed / 2.0F, 0.0F, 1.0F));
		float lunge = easeOut(Mth.clamp((elapsed - 2.0F) / 4.0F, 0.0F, 1.0F));
		float reach = 0.72F + ((float) TankerVfxEventMessage.BASH_REACH - 0.72F)
				* lunge;
		float fade = fadeOut(elapsed, visual.event.duration(), 1.4F);
		float localScale = visual.firstPersonOwner ? 0.72F : 1.0F;
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;

		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, visual.firstPersonOwner ? 0.2D : 0.0D,
				visual.firstPersonOwner ? 0.45D : 0.0D);
		frame.poseStack.scale(localScale, localScale, localScale);
		frame.poseStack.mulPose(Axis.XP.rotationDegrees(visual.pitch * 0.18F));
		if (pass == Pass.SURFACE) {
			drawShieldArc(vertices, frame.poseStack.last(), budget, 0.98F,
					0.62F, Math.min(10, frame.quality.ringSegments), reach,
					1.04F, STEEL_DARK,
					frame.quality == Quality.OFF ? alpha(255.0F * fade)
							: alpha(208.0F * fade),
					MATERIAL_STEEL, light);
			if (frame.quality == Quality.FULL && lunge > 0.0F) {
				drawFloorWedge(vertices, frame.poseStack.last(), budget, 0.45F,
						Math.max(0.62F, reach), -10.0F, 10.0F, 0.08F,
						STEEL_MID, alpha(92.0F * fade), MATERIAL_STEEL, light);
			}
		} else {
			drawShieldArc(vertices, frame.poseStack.last(), budget, 1.02F,
					0.89F, Math.min(10, frame.quality.ringSegments),
					reach + 0.012F, 1.04F, GOLD_HOT,
					alpha((118.0F + load * 54.0F) * fade),
					MATERIAL_GOLD, light);
			if (frame.quality == Quality.FULL && lunge > 0.05F)
				drawBashTrailSparks(frame, visual, vertices, budget,
						Math.min(6, frame.quality.burstParticles), reach, fade);
		}
		frame.poseStack.popPose();
	}

	/** Shield Bash confirmed-hit slab or named Strain-relief seam. */
	private static void renderBashHit(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass,
			boolean strainRelief) {
		if (frame.quality == Quality.OFF && visual.elapsed > 1.0F)
			return;
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 5.0F);
		float snap = 0.84F + easeOut(Mth.clamp(visual.elapsed / 2.0F, 0.0F, 1.0F))
				* 0.22F;
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, strainRelief ? 0.82D : 0.72D,
				strainRelief ? 0.58D : 0.0D);
		frame.poseStack.scale(snap, snap, snap);
		if (pass == Pass.SURFACE) {
			if (strainRelief) {
				drawBand(vertices, frame.poseStack.last(), budget, 0.72F,
						-0.08F, 0.08F, -92.0F, 92.0F, 8,
						STEEL_MID, alpha(165.0F * fade),
						MATERIAL_STEEL, light);
			} else {
				drawImpactSlab(vertices, frame.poseStack.last(), budget, 1.16F,
						0.82F, 0.18F, STEEL_MID,
						frame.quality == Quality.OFF ? alpha(255.0F * fade)
								: alpha(208.0F * fade),
						MATERIAL_STEEL, light);
			}
		} else if (strainRelief) {
			drawBand(vertices, frame.poseStack.last(), budget, 0.74F,
					-0.035F, 0.035F, -88.0F, 88.0F, 8,
					GOLD_HOT, alpha(176.0F * fade), MATERIAL_GOLD, light);
		} else {
			drawPlate(vertices, frame.poseStack.last(), budget, 0.82F, 0.55F,
					0.105F, 0.66F, EMBER_HOT, alpha(208.0F * fade),
					MATERIAL_EMBER, light);
			if (frame.quality == Quality.FULL)
				drawRadialSparks(frame, visual, vertices, budget,
						Math.min(10, frame.quality.burstParticles), 0.8F,
						0.1F, 0.9F, EMBER, fade);
		}
		frame.poseStack.popPose();
	}

	/** Reinforcement: closed, interlocking 0-12t perfect-brace silhouette. */
	private static void renderReinforcementBrace(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass) {
		float lock = easeOut(Mth.clamp(visual.elapsed / 4.0F, 0.0F, 1.0F));
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 2.0F);
		float firstPersonScale = visual.firstPersonOwner ? 0.68F : 1.0F;
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, visual.firstPersonOwner ? 0.1D : 0.0D,
				visual.firstPersonOwner ? 1.0D : 0.62D);
		frame.poseStack.scale(firstPersonScale, firstPersonScale, firstPersonScale);

		if (pass == Pass.SURFACE) {
			if (frame.quality == Quality.OFF) {
				drawPlate(vertices, frame.poseStack.last(), budget, 1.38F,
						1.64F, 0.0F, 0.78F, STEEL_DARK,
						alpha(255.0F * fade),
						MATERIAL_STEEL, light);
			} else {
				int plates = frame.quality == Quality.FULL ? 5 : 3;
				for (int index = 0; index < plates; index++) {
					float centered = index - (plates - 1) * 0.5F;
					frame.poseStack.pushPose();
					frame.poseStack.translate(centered * 0.34F * lock,
							0.72D + Math.abs(centered) * 0.06D,
							-Math.abs(centered) * 0.025D);
					frame.poseStack.mulPose(Axis.ZP.rotationDegrees(centered * -7.0F));
					drawPlate(vertices, frame.poseStack.last(), budget, 0.66F,
							1.38F - Math.abs(centered) * 0.08F, 0.0F,
							0.76F, index % 2 == 0 ? STEEL_DARK : STEEL_MID,
							alpha(214.0F * fade), MATERIAL_STEEL, light);
					frame.poseStack.popPose();
				}
			}
		} else {
			int seams = frame.quality == Quality.FULL ? 4 : 2;
			for (int index = 0; index < seams; index++) {
				float x = (index - (seams - 1) * 0.5F) * 0.32F;
				frame.poseStack.pushPose();
				frame.poseStack.translate(x * lock, 0.72D, 0.018D);
				drawPlate(vertices, frame.poseStack.last(), budget, 0.045F,
						1.18F, 0.0F, 1.0F, GOLD_PALE,
						alpha(148.0F * fade), MATERIAL_GOLD, light);
				frame.poseStack.popPose();
			}
		}
		frame.poseStack.popPose();
	}

	private static void renderReinforcementHit(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass) {
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 4.0F);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, 1.05D, 0.0D);
		if (pass == Pass.SURFACE) {
			drawImpactSlab(vertices, frame.poseStack.last(), budget, 1.35F,
					1.22F, 0.16F, STEEL_EDGE, alpha(218.0F * fade),
					MATERIAL_STEEL, light);
		} else {
			drawPlate(vertices, frame.poseStack.last(), budget, 0.42F, 0.42F,
					0.1F, 0.72F, EMBER_HOT, alpha(196.0F * fade),
					MATERIAL_EMBER, light);
			if (frame.quality == Quality.FULL)
				drawRadialSparks(frame, visual, vertices, budget, 1, 0.2F,
						0.85F, 0.95F, EMBER_HOT, fade);
		}
		frame.poseStack.popPose();
	}

	/** Reinforcement: visibly smaller, open 80t stance and its 0-6t release. */
	private static void renderReinforcementStance(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass, boolean ending) {
		float progress = ending
				? Mth.clamp(visual.elapsed / 6.0F, 0.0F, 1.0F)
				: easeOut(Mth.clamp(visual.elapsed / 6.0F, 0.0F, 1.0F));
		float fade = ending ? 1.0F - progress
				: fadeOut(visual.elapsed, visual.event.duration(), 5.0F);
		float open = ending ? 1.0F + progress * 0.28F : 1.12F - progress * 0.12F;
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, 0.9D,
				visual.firstPersonOwner ? 0.55D : 0.0D);
		frame.poseStack.scale(open, 1.0F, open);

		if (pass == Pass.SURFACE) {
			if (frame.quality == Quality.OFF) {
				for (int side = -1; side <= 1; side += 2) {
					frame.poseStack.pushPose();
					frame.poseStack.translate(side * 0.28D, 0.0D, 0.52D);
					drawPlate(vertices, frame.poseStack.last(), budget, 0.42F,
							0.54F, 0.0F, 0.78F, STEEL_DARK,
							alpha(255.0F * fade), MATERIAL_STEEL, light);
					frame.poseStack.popPose();
				}
			} else {
				drawBand(vertices, frame.poseStack.last(), budget, 0.64F,
						-0.32F, -0.13F, -68.0F, 68.0F, 8,
						STEEL_DARK, alpha(132.0F * fade),
						MATERIAL_STEEL, light);
				drawBand(vertices, frame.poseStack.last(), budget, 0.57F,
						0.05F, 0.2F, -58.0F, 58.0F, 8,
						STEEL_MID, alpha(112.0F * fade),
						MATERIAL_STEEL, light);
			}
		} else {
			drawBand(vertices, frame.poseStack.last(), budget, 0.655F,
					-0.335F, -0.29F, -68.0F, 68.0F, 8,
					GOLD_PALE, alpha(78.0F * fade), MATERIAL_GOLD, light);
			drawBand(vertices, frame.poseStack.last(), budget, 0.585F,
					0.18F, 0.225F, -58.0F, 58.0F, 8,
					GOLD_PALE, alpha(64.0F * fade), MATERIAL_GOLD, light);
		}
		frame.poseStack.popPose();
	}

	/** Willpower: 0-8t bind, then three deterministic bands through tick 160. */
	private static void renderWillpower(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		float bind = easeOut(Mth.clamp(visual.elapsed / 8.0F, 0.0F, 1.0F));
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 7.0F);
		float radius = 0.86F - bind * 0.28F;
		int threshold = Math.min(4, (visual.event.intensity + 63) / 64);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, 0.68D, 0.0D);

		if (pass == Pass.SURFACE) {
			if (frame.quality == Quality.OFF) {
				drawBand(vertices, frame.poseStack.last(), budget, radius,
						-0.08F, 0.1F, 0.0F, 360.0F, 8,
						threshold >= 3 ? STEEL_EDGE : STEEL_DARK,
						alpha(255.0F * fade), MATERIAL_STEEL, light);
			} else {
				for (int band = 0; band < 3; band++) {
					float y = band * 0.38F;
					drawBand(vertices, frame.poseStack.last(), budget,
							radius - band * 0.035F, y - 0.07F, y + 0.07F,
							0.0F, 360.0F, frame.quality.ringSegments,
							band == 1 ? STEEL_MID : STEEL_DARK,
							alpha((132.0F + threshold * 8.0F) * fade),
							MATERIAL_STEEL, light);
				}
			}
		} else if (frame.quality == Quality.LOW) {
			int tint = threshold >= 3 ? EMBER : GOLD_PALE;
			int material = threshold >= 3 ? MATERIAL_EMBER : MATERIAL_GOLD;
			drawBand(vertices, frame.poseStack.last(), budget, radius + 0.012F,
					0.305F, 0.345F, 0.0F, 360.0F, 12,
					tint, alpha((54.0F + threshold * 16.0F) * fade),
					material, light);
		} else {
			for (int crack = 0; crack < threshold; crack++) {
				float x = -0.34F + crack * 0.22F;
				float y = 0.08F + (crack & 1) * 0.34F;
				drawEmberCrack(vertices, frame.poseStack.last(), budget,
						x, y, radius + 0.018F, 0.18F + crack * 0.03F,
						visual.event.seed() + crack * 37, EMBER_HOT,
						alpha((126.0F + threshold * 18.0F) * fade), light);
			}
			drawBand(vertices, frame.poseStack.last(), budget, radius + 0.014F,
					-0.09F, -0.045F, 0.0F, 360.0F,
					frame.quality.ringSegments, GOLD_PALE,
					alpha(58.0F * fade), MATERIAL_GOLD, light);
		}
		frame.poseStack.popPose();
	}

	private static void renderWillpowerThreshold(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass) {
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 6.0F);
		int threshold = Math.max(1, Math.min(4,
				(visual.event.intensity + 63) / 64));
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, 0.98D, 0.0D);
		if (pass == Pass.SURFACE) {
			drawBand(vertices, frame.poseStack.last(), budget, 0.62F,
					-0.07F, 0.07F, 0.0F, 360.0F, 12,
					STEEL_EDGE, alpha(92.0F * fade), MATERIAL_STEEL, light);
		} else if (frame.quality == Quality.FULL) {
			for (int crack = 0; crack < threshold; crack++) {
				drawEmberCrack(vertices, frame.poseStack.last(), budget,
						-0.28F + crack * 0.18F, -0.22F + (crack & 1) * 0.2F,
						0.64F, 0.28F, visual.event.seed() + crack * 29,
						EMBER_HOT, alpha(188.0F * fade), light);
			}
		}
		frame.poseStack.popPose();
	}

	/** Each authoritative settlement packet contracts exactly one ten-tick ring. */
	private static void renderWillpowerSettle(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass) {
		float progress = easeOut(Mth.clamp(visual.elapsed / 10.0F, 0.0F, 1.0F));
		float radius = 2.15F - progress * 1.55F;
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 3.0F);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		frame.poseStack.pushPose();
		frame.poseStack.translate(0.0D, 0.72D, 0.0D);
		if (pass == Pass.SURFACE) {
			drawBand(vertices, frame.poseStack.last(), budget, radius,
					-0.055F, 0.055F, 0.0F, 360.0F,
					frame.quality.ringSegments,
					frame.quality == Quality.OFF ? STEEL_EDGE : STEEL_MID,
					frame.quality == Quality.OFF ? alpha(255.0F * fade)
							: alpha(158.0F * fade),
					MATERIAL_STEEL, light);
		} else {
			int color = visual.event.intensity >= TankerVfxEventMessage.INTENSITY_75
					? EMBER_HOT : GOLD_HOT;
			int material = visual.event.intensity >= TankerVfxEventMessage.INTENSITY_75
					? MATERIAL_EMBER : MATERIAL_GOLD;
			drawBand(vertices, frame.poseStack.last(), budget, radius + 0.012F,
					-0.025F, 0.025F, 0.0F, 360.0F,
					frame.quality.ringSegments, color,
					alpha(152.0F * fade), material, light);
		}
		frame.poseStack.popPose();
	}

	private static void renderWillpowerBreak(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass) {
		float progress = easeOut(Mth.clamp(visual.elapsed / 8.0F, 0.0F, 1.0F));
		float fade = 1.0F - Mth.clamp(visual.elapsed / 8.0F, 0.0F, 1.0F);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		for (int fragment = 0; fragment < 6; fragment++) {
			float angle = fragment * 60.0F + hash01(visual.event.seed(), fragment, 73) * 14.0F;
			float radius = 0.56F + progress * (0.72F
					+ hash01(visual.event.seed(), fragment, 79) * 0.65F);
			frame.poseStack.pushPose();
			frame.poseStack.mulPose(Axis.YP.rotationDegrees(angle));
			frame.poseStack.translate(0.0D, 0.58D + (fragment % 3) * 0.31D,
					radius);
			frame.poseStack.mulPose(Axis.ZP.rotationDegrees(angle + progress * 34.0F));
			if (pass == Pass.SURFACE) {
				drawPlate(vertices, frame.poseStack.last(), budget, 0.34F,
						0.16F, 0.0F, 0.55F, STEEL_MID,
						alpha(186.0F * fade), MATERIAL_STEEL, light);
			} else {
				drawPlate(vertices, frame.poseStack.last(), budget, 0.26F,
						0.045F, 0.012F, 1.0F, EMBER_HOT,
						alpha(158.0F * fade), MATERIAL_CRACK, light);
			}
			frame.poseStack.popPose();
		}
	}

	/** Protection Mark: 0-10t deploy, exact six-block rim, crest, and eight tabs. */
	private static void renderMark(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		float deploy = easeOut(Mth.clamp(visual.elapsed / 10.0F, 0.0F, 1.0F));
		float radius = (float) TankerVfxEventMessage.MARK_RADIUS * deploy;
		if (visual.elapsed >= 10.0F)
			radius = (float) TankerVfxEventMessage.MARK_RADIUS;
		float fade = fadeOut(visual.elapsed, visual.event.duration(), 8.0F);
		float held = visual.elapsed < 10.0F ? 1.0F
				: 0.88F + 0.04F * Mth.sin(visual.elapsed * 0.16F);
		int fracture = Math.min(3, (visual.event.intensity + 63) / 64);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;

		if (pass == Pass.SURFACE) {
			drawGroundRing(vertices, frame.poseStack.last(), budget,
					Math.max(0.0F, radius - 0.3F), radius,
					frame.quality.ringSegments, 0.028F,
					fracture >= 2 ? STEEL_EDGE : STEEL_MID,
					frame.quality == Quality.OFF ? alpha(255.0F * fade)
							: alpha(174.0F * held * fade),
					MATERIAL_STEEL, light);
			if (frame.quality != Quality.OFF) {
				for (int tab = 0; tab < 8; tab++) {
					float angle = tab * 45.0F;
					drawFloorWedge(vertices, frame.poseStack.last(), budget,
							Math.max(0.0F, radius - 1.12F), radius - 0.36F,
							angle - 3.7F, angle + 3.7F, 0.034F,
							STEEL_DARK, alpha(188.0F * fade),
							MATERIAL_STEEL, light);
				}
			}
			frame.poseStack.pushPose();
			frame.poseStack.translate(0.0D, 0.04D, 0.0D);
			drawCrest(vertices, frame.poseStack.last(), budget,
					frame.quality == Quality.OFF ? 0.82F : 0.92F,
					1.35F * Math.max(0.25F, deploy), STEEL_DARK,
					frame.quality == Quality.OFF ? alpha(255.0F * fade)
							: alpha(216.0F * fade),
					MATERIAL_STEEL, light, fracture);
			frame.poseStack.popPose();
			return;
		}

		drawGroundRing(vertices, frame.poseStack.last(), budget,
				Math.max(0.0F, radius - 0.085F), radius,
				frame.quality.ringSegments, 0.044F, GOLD_PALE,
				alpha(112.0F * held * fade), MATERIAL_GOLD, light);
		for (int crack = 0; crack < fracture; crack++) {
			float angle = 35.0F + crack * 97.0F;
			frame.poseStack.pushPose();
			frame.poseStack.mulPose(Axis.YP.rotationDegrees(angle));
			frame.poseStack.translate(0.0D, 0.055D, Math.max(0.4F, radius - 0.22F));
			frame.poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
			drawEmberCrack(vertices, frame.poseStack.last(), budget,
					0.0F, 0.0F, 0.012F, 0.42F, visual.event.seed() + crack * 61,
					EMBER_HOT, alpha(148.0F * fade), light);
			frame.poseStack.popPose();
		}
	}

	private static void renderMarkThreshold(RenderFrame frame,
			VisibleEvent visual, VertexConsumer vertices, FrameBudget budget,
			Pass pass) {
		float progress = easeOut(Mth.clamp(visual.elapsed
				/ visual.event.duration(), 0.0F, 1.0F));
		float fade = 1.0F - progress;
		float radius = (float) TankerVfxEventMessage.MARK_RADIUS
				- progress * 0.42F;
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		if (pass == Pass.SURFACE) {
			drawGroundRing(vertices, frame.poseStack.last(), budget, radius - 0.2F,
					radius, frame.quality.ringSegments, 0.052F,
					STEEL_EDGE, alpha(112.0F * fade), MATERIAL_STEEL, light);
		} else if (frame.quality == Quality.FULL) {
			drawGroundRing(vertices, frame.poseStack.last(), budget, radius - 0.08F,
					radius, frame.quality.ringSegments, 0.056F,
					EMBER, alpha(138.0F * fade), MATERIAL_EMBER, light);
		}
	}

	private static void renderMarkEnd(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass,
			boolean broken) {
		float progress = easeOut(Mth.clamp(visual.elapsed / 8.0F, 0.0F, 1.0F));
		float radius = broken
				? (float) TankerVfxEventMessage.MARK_RADIUS + progress * 0.65F
				: (float) TankerVfxEventMessage.MARK_RADIUS * (1.0F - progress);
		float fade = 1.0F - Mth.clamp(visual.elapsed / 8.0F, 0.0F, 1.0F);
		int light = pass == Pass.SURFACE ? visual.packedLight : LightTexture.FULL_BRIGHT;
		int segments = frame.quality.ringSegments;

		if (pass == Pass.SURFACE) {
			for (int index = 0; index < segments; index++) {
				if (broken && (index + visual.event.seed()) % 3 == 0)
					continue;
				float center = index * 360.0F / segments;
				float half = 150.0F / segments;
				drawFloorWedge(vertices, frame.poseStack.last(), budget,
						Math.max(0.0F, radius - 0.28F), radius,
						center - half, center + half, 0.035F,
						broken ? STEEL_EDGE : STEEL_MID,
						frame.quality == Quality.OFF ? alpha(238.0F * fade)
								: alpha(184.0F * fade),
						MATERIAL_STEEL, light);
			}
			frame.poseStack.pushPose();
			frame.poseStack.translate((broken ? progress * 0.22F : 0.0F),
					0.04D, 0.0D);
			drawCrest(vertices, frame.poseStack.last(), budget, 0.88F,
					1.3F * fade, STEEL_DARK, alpha(218.0F * fade),
					MATERIAL_STEEL, light, broken ? 3 : 1);
			frame.poseStack.popPose();
		} else {
			int color = broken ? EMBER_HOT : GOLD_PALE;
			int material = broken ? MATERIAL_EMBER : MATERIAL_GOLD;
			drawGroundRing(vertices, frame.poseStack.last(), budget,
					Math.max(0.0F, radius - 0.08F), radius, segments,
					0.052F, color, alpha(142.0F * fade), material, light);
			if (broken && frame.quality == Quality.FULL)
				drawRadialSparks(frame, visual, vertices, budget,
						Math.min(12, frame.quality.burstParticles),
						(float) TankerVfxEventMessage.MARK_RADIUS,
						0.06F, 0.7F, EMBER, fade);
		}
	}

	// Reusable bounded primitive set: plate, wedge, shield arc, impact slab,
	// ground ring, crest, ember crack, and camera-facing dust spark.

	private static void drawPlate(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float width, float height, float z,
			float topScale, int color, int alpha, int material, int packedLight) {
		float halfWidth = width * 0.5F;
		float topHalfWidth = halfWidth * topScale;
		float halfHeight = height * 0.5F;
		quad(vertices, pose, budget,
				new Vec3(-halfWidth, -halfHeight, z),
				new Vec3(halfWidth, -halfHeight, z),
				new Vec3(topHalfWidth, halfHeight, z),
				new Vec3(-topHalfWidth, halfHeight, z),
				color, alpha, material, packedLight);
	}

	private static void drawFloorWedge(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float innerRadius, float outerRadius,
			float startDegrees, float endDegrees, float y, int color, int alpha,
			int material, int packedLight) {
		double start = Math.toRadians(startDegrees);
		double end = Math.toRadians(endDegrees);
		Vec3 innerStart = polar(innerRadius, start, y);
		Vec3 outerStart = polar(outerRadius, start, y);
		Vec3 outerEnd = polar(outerRadius, end, y);
		Vec3 innerEnd = polar(innerRadius, end, y);
		quad(vertices, pose, budget, innerStart, outerStart, outerEnd, innerEnd,
				color, alpha, material, packedLight);
	}

	private static void drawGroundRing(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float innerRadius, float outerRadius, int segments,
			float y, int color, int alpha, int material, int packedLight) {
		int boundedSegments = Mth.clamp(segments, 3, 32);
		for (int index = 0; index < boundedSegments; index++) {
			float start = index * 360.0F / boundedSegments;
			float end = (index + 1) * 360.0F / boundedSegments;
			drawFloorWedge(vertices, pose, budget, innerRadius, outerRadius,
					start, end, y, color, alpha, material, packedLight);
		}
	}

	private static void drawShieldArc(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float outerRadius, float innerRadius, int segments,
			float forward, float centerY, int color, int alpha, int material,
			int packedLight) {
		int boundedSegments = Mth.clamp(segments, 4, 16);
		for (int index = 0; index < boundedSegments; index++) {
			double start = Math.toRadians(-112.0F + index * 224.0F / boundedSegments);
			double end = Math.toRadians(-112.0F
					+ (index + 1) * 224.0F / boundedSegments);
			Vec3 outerStart = new Vec3(Math.sin(start) * outerRadius,
					centerY + Math.cos(start) * outerRadius, forward);
			Vec3 outerEnd = new Vec3(Math.sin(end) * outerRadius,
					centerY + Math.cos(end) * outerRadius, forward);
			Vec3 innerEnd = new Vec3(Math.sin(end) * innerRadius,
					centerY + Math.cos(end) * innerRadius, forward + 0.012F);
			Vec3 innerStart = new Vec3(Math.sin(start) * innerRadius,
					centerY + Math.cos(start) * innerRadius, forward + 0.012F);
			quad(vertices, pose, budget, innerStart, outerStart, outerEnd, innerEnd,
					color, alpha, material, packedLight);
		}
	}

	private static void drawBand(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float radius, float bottomY, float topY,
			float startDegrees, float endDegrees, int segments, int color,
			int alpha, int material, int packedLight) {
		int boundedSegments = Mth.clamp(segments, 3, 32);
		float span = endDegrees - startDegrees;
		for (int index = 0; index < boundedSegments; index++) {
			double start = Math.toRadians(startDegrees + span * index / boundedSegments);
			double end = Math.toRadians(startDegrees
					+ span * (index + 1) / boundedSegments);
			Vec3 lowerStart = new Vec3(Math.sin(start) * radius, bottomY,
					Math.cos(start) * radius);
			Vec3 lowerEnd = new Vec3(Math.sin(end) * radius, bottomY,
					Math.cos(end) * radius);
			Vec3 upperEnd = new Vec3(Math.sin(end) * radius, topY,
					Math.cos(end) * radius);
			Vec3 upperStart = new Vec3(Math.sin(start) * radius, topY,
					Math.cos(start) * radius);
			quad(vertices, pose, budget, lowerStart, lowerEnd, upperEnd, upperStart,
					color, alpha, material, packedLight);
		}
	}

	private static void drawImpactSlab(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float width, float height, float depth,
			int color, int alpha, int material, int packedLight) {
		drawBox(vertices, pose, budget, -width * 0.5F, -height * 0.5F,
				-depth * 0.5F, width * 0.5F, height * 0.5F, depth * 0.5F,
				color, alpha, material, packedLight);
	}

	private static void drawBox(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float minX, float minY, float minZ,
			float maxX, float maxY, float maxZ, int color, int alpha,
			int material, int packedLight) {
		if (budget.remaining() < 24)
			return;
		Vec3 lbf = new Vec3(minX, minY, minZ);
		Vec3 rbf = new Vec3(maxX, minY, minZ);
		Vec3 rtf = new Vec3(maxX, maxY, minZ);
		Vec3 ltf = new Vec3(minX, maxY, minZ);
		Vec3 lbb = new Vec3(minX, minY, maxZ);
		Vec3 rbb = new Vec3(maxX, minY, maxZ);
		Vec3 rtb = new Vec3(maxX, maxY, maxZ);
		Vec3 ltb = new Vec3(minX, maxY, maxZ);

		quad(vertices, pose, budget, lbf, rbf, rtf, ltf,
				color, alpha, material, packedLight);
		quad(vertices, pose, budget, rbb, lbb, ltb, rtb,
				color, alpha, material, packedLight);
		quad(vertices, pose, budget, lbb, lbf, ltf, ltb,
				color, alpha, material, packedLight);
		quad(vertices, pose, budget, rbf, rbb, rtb, rtf,
				color, alpha, material, packedLight);
		quad(vertices, pose, budget, ltf, rtf, rtb, ltb,
				color, alpha, material, packedLight);
		quad(vertices, pose, budget, lbb, rbb, rbf, lbf,
				color, alpha, material, packedLight);
	}

	private static void drawCrest(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float width, float height, int color, int alpha,
			int material, int packedLight, int fractureStage) {
		float half = width * 0.5F;
		float shoulderY = height * 0.58F;
		float topY = height;
		float split = fractureStage >= 3 ? width * 0.055F : 0.0F;

		quad(vertices, pose, budget,
				new Vec3(-half - split, shoulderY, 0.0D),
				new Vec3(-split, shoulderY, 0.0D),
				new Vec3(-split, topY, 0.0D),
				new Vec3(-half * 0.78F - split, topY, 0.0D),
				color, alpha, material, packedLight);
		quad(vertices, pose, budget,
				new Vec3(split, shoulderY, 0.0D),
				new Vec3(half + split, shoulderY, 0.0D),
				new Vec3(half * 0.78F + split, topY, 0.0D),
				new Vec3(split, topY, 0.0D),
				color, alpha, material, packedLight);
		quad(vertices, pose, budget,
				new Vec3(-half - split, shoulderY, 0.0D),
				new Vec3(-split, shoulderY, 0.0D),
				new Vec3(-split, 0.0D, 0.0D),
				new Vec3(-split, 0.0D, 0.0D),
				color, alpha, material, packedLight);
		quad(vertices, pose, budget,
				new Vec3(split, shoulderY, 0.0D),
				new Vec3(half + split, shoulderY, 0.0D),
				new Vec3(split, 0.0D, 0.0D),
				new Vec3(split, 0.0D, 0.0D),
				color, alpha, material, packedLight);

		int cracks = Mth.clamp(fractureStage, 0, 3);
		for (int crack = 0; crack < cracks; crack++) {
			float x = -width * 0.23F + crack * width * 0.21F;
			drawEmberCrack(vertices, pose, budget, x, height * 0.3F,
					0.012F, height * (0.2F + crack * 0.035F),
					0x51A7 + crack * 97, STEEL_EDGE,
					Math.min(240, alpha + 18), packedLight);
		}
	}

	private static void drawEmberCrack(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float startX, float startY, float z,
			float length, int seed, int color, int alpha, int packedLight) {
		float x = startX;
		float y = startY;
		for (int segment = 0; segment < 3; segment++) {
			float nextX = x + (hash01(seed, segment, 101) - 0.5F)
					* length * 0.48F;
			float nextY = y + length / 3.0F;
			drawRibbon2d(vertices, pose, budget, x, y, nextX, nextY,
					z, 0.018F + segment * 0.003F, color, alpha,
					MATERIAL_CRACK, packedLight);
			x = nextX;
			y = nextY;
		}
	}

	private static void drawRibbon2d(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, float startX, float startY, float endX,
			float endY, float z, float halfWidth, int color, int alpha,
			int material, int packedLight) {
		float dx = endX - startX;
		float dy = endY - startY;
		float length = Mth.sqrt(dx * dx + dy * dy);
		if (length < 0.0001F)
			return;
		float sideX = -dy / length * halfWidth;
		float sideY = dx / length * halfWidth;
		quad(vertices, pose, budget,
				new Vec3(startX + sideX, startY + sideY, z),
				new Vec3(endX + sideX, endY + sideY, z),
				new Vec3(endX - sideX, endY - sideY, z),
				new Vec3(startX - sideX, startY - sideY, z),
				color, alpha, material, packedLight);
	}

	private static void drawRadialSparks(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, int count, float radius,
			float baseY, float height, int color, float fade) {
		for (int index = 0; index < count; index++) {
			float angle = hash01(visual.event.seed(), index, 131) * Mth.TWO_PI;
			float distance = radius * (0.28F
					+ hash01(visual.event.seed(), index, 137) * 0.72F);
			float x = Mth.cos(angle) * distance;
			float z = Mth.sin(angle) * distance;
			float y = baseY + hash01(visual.event.seed(), index, 139) * height;
			float size = 0.045F + hash01(visual.event.seed(), index, 149) * 0.075F;
			drawSparkBillboard(frame, visual, vertices, budget, x, y, z, size,
					color, alpha((108.0F + size * 540.0F) * fade));
		}
	}

	private static void drawTrailDust(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, int count, float fade) {
		for (int index = 0; index < count; index++) {
			float progress = (index + 1.0F) / (count + 1.0F);
			float x = (hash01(visual.event.seed(), index, 157) - 0.5F) * 0.7F;
			float y = 0.08F + hash01(visual.event.seed(), index, 163) * 0.42F;
			float z = -0.25F - progress * 1.65F;
			float size = 0.065F + hash01(visual.event.seed(), index, 167) * 0.06F;
			drawSparkBillboard(frame, visual, vertices, budget, x, y, z, size,
					DUST, alpha(92.0F * fade));
		}
	}

	private static void drawBashTrailSparks(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, int count, float reach,
			float fade) {
		for (int index = 0; index < count; index++) {
			float progress = (index + 1.0F) / (count + 1.0F);
			float x = (hash01(visual.event.seed(), index, 173) - 0.5F) * 0.54F;
			float y = 0.32F + hash01(visual.event.seed(), index, 179) * 0.8F;
			float z = 0.45F + progress * Math.max(0.2F, reach - 0.45F);
			drawSparkBillboard(frame, visual, vertices, budget, x, y, z,
					0.05F, GOLD_PALE, alpha(76.0F * fade));
		}
	}

	private static void drawSparkBillboard(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, float x, float y,
			float z, float size, int color, int alpha) {
		frame.poseStack.pushPose();
		frame.poseStack.translate(x, y, z);
		// Undo the event-facing rotation before applying the absolute camera
		// quaternion so sparks remain camera-facing without changing their origin.
		frame.poseStack.mulPose(Axis.YP.rotationDegrees(visual.yaw));
		frame.poseStack.mulPose(frame.billboard);
		drawPlate(vertices, frame.poseStack.last(), budget, size, size, 0.0F,
				1.0F, color, alpha, MATERIAL_DUST, LightTexture.FULL_BRIGHT);
		frame.poseStack.popPose();
	}

	private static Vec3 polar(float radius, double angle, float y) {
		return new Vec3(Math.sin(angle) * radius, y, Math.cos(angle) * radius);
	}

	private static void quad(VertexConsumer vertices, PoseStack.Pose pose,
			FrameBudget budget, Vec3 first, Vec3 second, Vec3 third, Vec3 fourth,
			int color, int alpha, int material, int packedLight) {
		if (alpha <= 0 || !budget.reserve(4))
			return;
		Vec3 normal = second.subtract(first).cross(third.subtract(first));
		if (normal.lengthSqr() < 0.0000001D)
			normal = new Vec3(0.0D, 0.0D, 1.0D);
		else
			normal = normal.normalize();
		vertex(vertices, pose, first, normal, 0.0F, 1.0F,
				color, alpha, material, packedLight);
		vertex(vertices, pose, second, normal, 1.0F, 1.0F,
				color, alpha, material, packedLight);
		vertex(vertices, pose, third, normal, 1.0F, 0.0F,
				color, alpha, material, packedLight);
		vertex(vertices, pose, fourth, normal, 0.0F, 0.0F,
				color, alpha, material, packedLight);
	}

	private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
			Vec3 point, Vec3 normal, float u, float v,
			int color, int alpha, int material, int packedLight) {
		int red = color >> 16 & 0xFF;
		int green = color >> 8 & 0xFF;
		int blue = color & 0xFF;
		float materialU = material + Mth.clamp(u, 0.0F, 0.999F);
		vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
				.setColor(red, green, blue, Mth.clamp(alpha, 0, 255))
				.setUv(materialU, Mth.clamp(v, 0.0F, 1.0F))
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setNormal(pose, (float) normal.x, (float) normal.y,
						(float) normal.z);
	}

	private static float easeOut(float value) {
		float clamped = Mth.clamp(value, 0.0F, 1.0F);
		float inverse = 1.0F - clamped;
		return 1.0F - inverse * inverse * inverse;
	}

	private static float fadeOut(float elapsed, int duration, float fadeTicks) {
		return Mth.clamp((duration - elapsed) / Math.max(0.001F, fadeTicks),
				0.0F, 1.0F);
	}

	private static int alpha(float value) {
		return Mth.clamp(Math.round(value), 0, 255);
	}

	private static float hash01(int seed, int index, int salt) {
		int value = seed ^ index * 0x9E3779B9 ^ salt * 0x85EBCA6B;
		value ^= value >>> 16;
		value *= 0x7FEB352D;
		value ^= value >>> 15;
		value *= 0x846CA68B;
		value ^= value >>> 16;
		return (value & 0x00FFFFFF) / 16777215.0F;
	}

	private static boolean followsOwner(byte type) {
		return switch (type) {
			case TankerVfxEventMessage.LEAP_START,
					TankerVfxEventMessage.BASH_SWEEP,
					TankerVfxEventMessage.BASH_STRAIN_RELIEF,
					TankerVfxEventMessage.REINFORCEMENT_BRACE_START,
					TankerVfxEventMessage.REINFORCEMENT_STANCE_START,
					TankerVfxEventMessage.REINFORCEMENT_STANCE_END,
					TankerVfxEventMessage.WILLPOWER_START,
					TankerVfxEventMessage.WILLPOWER_STRAIN_THRESHOLD,
					TankerVfxEventMessage.WILLPOWER_SETTLE,
					TankerVfxEventMessage.WILLPOWER_BREAK -> true;
			default -> false;
		};
	}

	private static void applyStateTransition(TankerVfxEventMessage message) {
		int ownerId = message.ownerEntityId;
		switch (message.eventType) {
			case TankerVfxEventMessage.LEAP_LAND ->
					removeTypes(ownerId, TankerVfxEventMessage.LEAP_START);
			case TankerVfxEventMessage.REINFORCEMENT_BRACE_START ->
					removeTypes(ownerId, TankerVfxEventMessage.REINFORCEMENT_BRACE_START,
							TankerVfxEventMessage.REINFORCEMENT_STANCE_START);
			case TankerVfxEventMessage.REINFORCEMENT_STANCE_START -> {
				removeTypes(ownerId, TankerVfxEventMessage.REINFORCEMENT_BRACE_START,
						TankerVfxEventMessage.REINFORCEMENT_STANCE_START);
			}
			case TankerVfxEventMessage.REINFORCEMENT_STANCE_END ->
					removeTypes(ownerId, TankerVfxEventMessage.REINFORCEMENT_STANCE_START);
			case TankerVfxEventMessage.WILLPOWER_START ->
					removeTypes(ownerId, TankerVfxEventMessage.WILLPOWER_START);
			case TankerVfxEventMessage.WILLPOWER_STRAIN_THRESHOLD ->
					updateIntensity(ownerId, TankerVfxEventMessage.WILLPOWER_START,
							message.intensity);
			case TankerVfxEventMessage.BASH_STRAIN_RELIEF ->
					updateIntensity(ownerId, TankerVfxEventMessage.WILLPOWER_START,
							message.intensity);
			case TankerVfxEventMessage.WILLPOWER_SETTLE,
					TankerVfxEventMessage.WILLPOWER_BREAK ->
					removeTypes(ownerId, TankerVfxEventMessage.WILLPOWER_START);
			case TankerVfxEventMessage.MARK_DEPLOY ->
					removeTypes(ownerId, TankerVfxEventMessage.MARK_DEPLOY);
			case TankerVfxEventMessage.MARK_INTEGRITY_THRESHOLD ->
					updateIntensity(ownerId, TankerVfxEventMessage.MARK_DEPLOY,
							message.intensity);
			case TankerVfxEventMessage.MARK_BREAK,
					TankerVfxEventMessage.MARK_CANCEL ->
					removeTypes(ownerId, TankerVfxEventMessage.MARK_DEPLOY);
			default -> {
			}
		}
	}

	private static void updateIntensity(int ownerId, byte type, int intensity) {
		for (int index = EVENTS.size() - 1; index >= 0; index--) {
			ActiveEvent event = EVENTS.get(index);
			if (event.ownerId() == ownerId && event.type() == type) {
				event.intensity = Mth.clamp(intensity, 0, 255);
				return;
			}
		}
	}

	private static void removeTypes(int ownerId, byte... types) {
		EVENTS.removeIf(event -> {
			if (event.ownerId() != ownerId)
				return false;
			for (byte type : types) {
				if (event.type() == type)
					return true;
			}
			return false;
		});
	}

	private static boolean isDuplicate(TankerVfxEventMessage message) {
		for (ActiveEvent event : EVENTS) {
			if (event.type() == message.eventType
					&& event.ownerId() == message.ownerEntityId
					&& event.startTick() == message.serverStartTick
					&& event.seed() == message.seed)
				return true;
		}
		return false;
	}

	private static void pruneInvalidAndExpired(long now) {
		EVENTS.removeIf(event -> !TankerVfxEventMessage.isKnownEventType(event.type())
				|| event.expired(now));
		trimQueueToLimit();
		if (EVENTS.isEmpty())
			nextSequence = 0L;
	}

	/** Defensive hard cap in addition to the normal admission-time eviction. */
	private static void trimQueueToLimit() {
		while (EVENTS.size() > MAX_QUEUED_EVENTS) {
			int candidate = oldestMatching(false, false);
			if (candidate < 0)
				candidate = oldestMatching(true, false);
			if (candidate < 0)
				candidate = oldestIndex();
			if (candidate < 0)
				break;
			EVENTS.remove(candidate);
		}
	}

	private static boolean evictFor(ActiveEvent incoming, Minecraft minecraft) {
		Vec3 reference = minecraft.player == null
				? incoming.origin()
				: minecraft.player.position();
		int candidate = farthestMatching(reference, false, false);
		if (candidate >= 0) {
			EVENTS.remove(candidate);
			return true;
		}
		if (!incoming.essential())
			return false;

		candidate = farthestMatching(reference, true, false);
		if (candidate < 0)
			candidate = oldestIndex();
		if (candidate >= 0) {
			EVENTS.remove(candidate);
			return true;
		}
		return false;
	}

	private static int farthestMatching(Vec3 reference, boolean allowEssential,
			boolean allowProtectedBoundary) {
		int candidate = -1;
		double farthest = -1.0D;
		for (int index = 0; index < EVENTS.size(); index++) {
			ActiveEvent event = EVENTS.get(index);
			if (!allowEssential && event.essential())
				continue;
			if (!allowProtectedBoundary && event.protectedBoundary())
				continue;
			double distance = event.origin().distanceToSqr(reference);
			if (distance > farthest) {
				farthest = distance;
				candidate = index;
			}
		}
		return candidate;
	}

	private static int oldestIndex() {
		int candidate = -1;
		long oldest = Long.MAX_VALUE;
		for (int index = 0; index < EVENTS.size(); index++) {
			ActiveEvent event = EVENTS.get(index);
			if (event.sequence < oldest) {
				oldest = event.sequence;
				candidate = index;
			}
		}
		return candidate;
	}

	private static int oldestMatching(boolean allowEssential,
			boolean allowProtectedBoundary) {
		int candidate = -1;
		long oldest = Long.MAX_VALUE;
		for (int index = 0; index < EVENTS.size(); index++) {
			ActiveEvent event = EVENTS.get(index);
			if (!allowEssential && event.essential())
				continue;
			if (!allowProtectedBoundary && event.protectedBoundary())
				continue;
			if (event.sequence < oldest) {
				oldest = event.sequence;
				candidate = index;
			}
		}
		return candidate;
	}

	private static void playInitialSound(Minecraft minecraft,
			TankerVfxEventMessage message) {
		SoundProfile profile = soundFor(message);
		if (profile == null || minecraft.level == null)
			return;
		minecraft.level.playLocalSound(message.originX, message.originY, message.originZ,
				profile.sound, SoundSource.PLAYERS, profile.volume, profile.pitch, false);
	}

	private static SoundProfile soundFor(TankerVfxEventMessage message) {
		return switch (message.eventType) {
			case TankerVfxEventMessage.LEAP_START ->
					new SoundProfile(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.58F, 0.72F);
			case TankerVfxEventMessage.LEAP_LAND ->
					new SoundProfile(SoundEvents.ANVIL_LAND, 0.95F, 0.68F);
			case TankerVfxEventMessage.TAUNT_RING ->
					new SoundProfile(SoundEvents.ANVIL_PLACE, 0.72F, 0.74F);
			case TankerVfxEventMessage.BASH_SWEEP ->
					new SoundProfile(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.52F, 0.84F);
			case TankerVfxEventMessage.BASH_HIT ->
					new SoundProfile(SoundEvents.SHIELD_BLOCK, 0.88F, 0.78F);
			case TankerVfxEventMessage.BASH_STRAIN_RELIEF ->
					new SoundProfile(SoundEvents.IRON_GOLEM_REPAIR, 0.48F, 1.32F);
			case TankerVfxEventMessage.REINFORCEMENT_BRACE_START ->
					new SoundProfile(SoundEvents.IRON_DOOR_CLOSE, 0.68F, 0.72F);
			case TankerVfxEventMessage.REINFORCEMENT_BRACE_HIT ->
					new SoundProfile(SoundEvents.ANVIL_USE, 0.78F, 1.08F);
			case TankerVfxEventMessage.REINFORCEMENT_STANCE_START ->
					new SoundProfile(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.48F, 0.74F);
			case TankerVfxEventMessage.REINFORCEMENT_STANCE_END ->
					new SoundProfile(SoundEvents.IRON_DOOR_OPEN, 0.4F, 0.92F);
			case TankerVfxEventMessage.WILLPOWER_START ->
					new SoundProfile(SoundEvents.CHAIN_PLACE, 0.52F, 0.72F);
			case TankerVfxEventMessage.WILLPOWER_STRAIN_THRESHOLD ->
					new SoundProfile(SoundEvents.IRON_GOLEM_DAMAGE, 0.42F,
							0.78F + message.intensity / 1024.0F);
			case TankerVfxEventMessage.WILLPOWER_SETTLE ->
					new SoundProfile(SoundEvents.ANVIL_LAND, 0.46F,
							0.7F + message.intensity / 850.0F);
			case TankerVfxEventMessage.WILLPOWER_BREAK ->
					new SoundProfile(SoundEvents.ANVIL_DESTROY, 0.82F, 0.74F);
			case TankerVfxEventMessage.MARK_DEPLOY ->
					new SoundProfile(SoundEvents.BEACON_ACTIVATE, 0.68F, 0.72F);
			case TankerVfxEventMessage.MARK_INTEGRITY_THRESHOLD ->
					new SoundProfile(SoundEvents.SHIELD_BLOCK, 0.34F, 1.08F);
			case TankerVfxEventMessage.MARK_BREAK ->
					new SoundProfile(SoundEvents.ANVIL_DESTROY, 0.78F, 0.68F);
			case TankerVfxEventMessage.MARK_CANCEL ->
					new SoundProfile(SoundEvents.BEACON_DEACTIVATE, 0.5F, 0.88F);
			default -> null;
		};
	}

	private static void playAmbientCues(Minecraft minecraft, List<VisibleEvent> visible,
			Quality quality) {
		if (quality == Quality.OFF || minecraft.level == null)
			return;
		for (VisibleEvent visual : visible) {
			ActiveEvent event = visual.event;
			if (event.type() != TankerVfxEventMessage.WILLPOWER_START
					|| event.hasFlag(TankerVfxEventMessage.FLAG_SILENT))
				continue;
			float elapsed = event.elapsed(minecraft.level.getGameTime(), 0.0F);
			int cue = elapsed < 30.0F ? -1 : Math.min(3, (int) ((elapsed - 30.0F) / 40.0F));
			if (cue <= event.lastAmbientCue)
				continue;
			event.lastAmbientCue = cue;
			minecraft.level.playLocalSound(visual.position.x, visual.position.y + 0.8D,
					visual.position.z, SoundEvents.WARDEN_HEARTBEAT,
					SoundSource.PLAYERS, 0.16F, 0.82F + cue * 0.035F, false);
		}
	}

	private enum Pass {
		SURFACE,
		EMISSIVE
	}

	private enum Quality {
		FULL(64.0D, FULL_MAX_VISIBLE_EVENTS, FULL_MAX_VERTICES,
				FULL_RING_SEGMENTS, FULL_MAX_BURST_PARTICLES),
		LOW(40.0D, LOW_MAX_VISIBLE_EVENTS, LOW_MAX_VERTICES,
				LOW_RING_SEGMENTS, LOW_MAX_BURST_PARTICLES),
		OFF(32.0D, OFF_MAX_VISIBLE_EVENTS, OFF_MAX_VERTICES,
				OFF_RING_SEGMENTS, OFF_MAX_BURST_PARTICLES);

		private final double renderDistance;
		private final int maxVisible;
		private final int maxVertices;
		private final int ringSegments;
		private final int burstParticles;

		Quality(double renderDistance, int maxVisible, int maxVertices,
				int ringSegments, int burstParticles) {
			this.renderDistance = renderDistance;
			this.maxVisible = maxVisible;
			this.maxVertices = maxVertices;
			this.ringSegments = ringSegments;
			this.burstParticles = burstParticles;
		}

		private static Quality current(Minecraft minecraft) {
			ParticleStatus status = minecraft.options.particles().get();
			if (status == ParticleStatus.MINIMAL)
				return OFF;
			if (status == ParticleStatus.DECREASED)
				return LOW;
			return FULL;
		}

		private boolean allowsEvent(ActiveEvent event) {
			return this != OFF || event.essential();
		}

		private boolean allowsGlow(ActiveEvent event) {
			return this == FULL || this == LOW && event.essential();
		}
	}

	private static final class ActiveEvent {
		private final TankerVfxEventMessage message;
		private final long sequence;
		private int intensity;
		private int lastAmbientCue;

		private ActiveEvent(TankerVfxEventMessage message, long sequence, long now) {
			this.message = message;
			this.sequence = sequence;
			this.intensity = message.intensity;
			float elapsed = Math.max(0.0F, now - message.serverStartTick);
			this.lastAmbientCue = message.hasFlag(TankerVfxEventMessage.FLAG_REPLAY)
					&& elapsed >= 30.0F
					? Math.min(3, (int) ((elapsed - 30.0F) / 40.0F))
					: -1;
		}

		private byte type() {
			return message.eventType;
		}

		private int ownerId() {
			return message.ownerEntityId;
		}

		private Vec3 origin() {
			return message.origin();
		}

		private float yaw() {
			return message.yawDegrees();
		}

		private float pitch() {
			return message.pitchDegrees();
		}

		private long startTick() {
			return message.serverStartTick;
		}

		private int duration() {
			return message.duration;
		}

		private int seed() {
			return message.seed;
		}

		private boolean hasFlag(int flag) {
			return message.hasFlag(flag);
		}

		private float elapsed(long now, float partialTick) {
			return now - message.serverStartTick + partialTick;
		}

		private boolean expired(long now) {
			return now - message.serverStartTick >= message.duration;
		}

		private boolean essential() {
			if (message.hasFlag(TankerVfxEventMessage.FLAG_ESSENTIAL))
				return true;
			return switch (message.eventType) {
				case TankerVfxEventMessage.LEAP_START,
						TankerVfxEventMessage.LEAP_LAND,
						TankerVfxEventMessage.TAUNT_RING,
						TankerVfxEventMessage.BASH_SWEEP,
						TankerVfxEventMessage.BASH_HIT,
						TankerVfxEventMessage.REINFORCEMENT_BRACE_START,
						TankerVfxEventMessage.REINFORCEMENT_BRACE_HIT,
						TankerVfxEventMessage.REINFORCEMENT_STANCE_START,
						TankerVfxEventMessage.REINFORCEMENT_STANCE_END,
						TankerVfxEventMessage.WILLPOWER_START,
						TankerVfxEventMessage.WILLPOWER_SETTLE,
						TankerVfxEventMessage.WILLPOWER_BREAK,
						TankerVfxEventMessage.MARK_DEPLOY,
						TankerVfxEventMessage.MARK_BREAK,
						TankerVfxEventMessage.MARK_CANCEL -> true;
				default -> false;
			};
		}

		private boolean protectedBoundary() {
			return switch (message.eventType) {
				case TankerVfxEventMessage.TAUNT_RING,
						TankerVfxEventMessage.REINFORCEMENT_BRACE_START,
						TankerVfxEventMessage.REINFORCEMENT_STANCE_START,
						TankerVfxEventMessage.WILLPOWER_START,
						TankerVfxEventMessage.MARK_DEPLOY -> true;
				default -> false;
			};
		}
	}

	private record Anchor(Vec3 position, float yaw, float pitch,
			boolean firstPersonOwner) {
	}

	private record VisibleEvent(ActiveEvent event, Vec3 position, float yaw,
			float pitch, float elapsed, double distanceSqr, int packedLight,
			boolean firstPersonOwner) {
	}

	private record RenderFrame(PoseStack poseStack, Vec3 cameraPosition,
			Quaternionf billboard, float partialTick, Quality quality) {
	}

	private record SoundProfile(SoundEvent sound, float volume, float pitch) {
	}

	private static final class FrameBudget {
		private final int limit;
		private int used;

		private FrameBudget(int limit) {
			this.limit = limit;
		}

		private boolean reserve(int vertices) {
			if (vertices <= 0 || used + vertices > limit)
				return false;
			used += vertices;
			return true;
		}

		private int remaining() {
			return limit - used;
		}
	}
}
