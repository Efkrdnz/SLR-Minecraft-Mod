package net.solocraft.client.renderer;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.SungIlHwanVfxClientState.ActiveEvent;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.client.renderer.shader.IrisCompat;
import net.solocraft.client.renderer.shader.SungIlHwanVfxRenderTypes;
import net.solocraft.network.SungIlHwanVfxEventMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Packet-driven world presentation for Sung Il-Hwan.
 *
 * <p>No visual entities or per-frame packets are created. A hard event,
 * visibility, distance, segment, and vertex budget bounds every frame. Rendering
 * uses depth-tested RenderTypes and the shared deferred depth handoff, avoiding
 * direct OpenGL state mutation.</p>
 */
@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.GAME,
		value = Dist.CLIENT
)
public final class SungIlHwanVfxRenderer {
	private static final int MATERIAL_VOID = 0;
	private static final int MATERIAL_SILVER = 1;
	private static final int MATERIAL_GOLD = 2;
	private static final int MATERIAL_FRACTURE = 3;
	private static final int MATERIAL_SOFT = 4;
	private static final int MATERIAL_SLASH = 5;

	private static final int VOID_BLACK = 0x07090D;
	private static final int VOID_EDGE = 0x151A22;
	private static final int SILVER_DARK = 0x737C86;
	private static final int GOLD_DARK = 0x8F5205;
	private static final int GOLD_AMBER = 0xE5A11B;
	private static final int GOLD_PALE = 0xF3CE66;
	private static final int GOLD_HOT = 0xFFF1AC;
	private static final int GOLD_WHITE = 0xFFF9D8;

	private SungIlHwanVfxRenderer() {
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		SungIlHwanVfxClientState.clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ClientLevel)
			SungIlHwanVfxClientState.clear();
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
		if (!renderStage
				&& event.getStage()
						!= RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;
		long now = minecraft.level.getGameTime();
		List<ActiveEvent> active = SungIlHwanVfxClientState.snapshot(now);
		if (active.isEmpty())
			return;

		DeferredWorldShaderRenderer.requestDepthAtStage(event,
				RenderLevelStageEvent.Stage.AFTER_PARTICLES);
		if (!renderStage)
			return;

		Quality quality = Quality.current(minecraft);
		List<VisibleEvent> visible = collectVisible(event, minecraft, active,
				quality, now);
		if (visible.isEmpty())
			return;

		PoseStack stack = DeferredWorldShaderRenderer.worldPoseStack(event);
		MultiBufferSource.BufferSource buffers =
				minecraft.renderBuffers().bufferSource();
		RenderFrame frame = new RenderFrame(stack,
				event.getCamera().getPosition(),
				minecraft.getEntityRenderDispatcher().cameraOrientation(),
				event.getPartialTick().getGameTimeDeltaPartialTick(false), quality);
		FrameBudget budget = new FrameBudget(quality.maxVertices);

		RenderType surfaceType = SungIlHwanVfxRenderTypes.surface();
		VertexConsumer surface = DeferredWorldShaderRenderer.buffer(buffers,
				surfaceType, true);
		for (VisibleEvent visual : visible)
			render(frame, visual, surface, budget, Pass.SURFACE);
		buffers.endBatch(surfaceType);

		if (quality != Quality.MINIMAL && budget.remaining >= 4) {
			RenderType emissiveType = SungIlHwanVfxRenderTypes.emissive();
			VertexConsumer emissive = DeferredWorldShaderRenderer.buffer(
					buffers, emissiveType, true);
			for (VisibleEvent visual : visible)
				render(frame, visual, emissive, budget, Pass.EMISSIVE);
			buffers.endBatch(emissiveType);
		}
	}

	private static List<VisibleEvent> collectVisible(
			RenderLevelStageEvent event, Minecraft minecraft,
			List<ActiveEvent> active, Quality quality, long now) {
		Vec3 camera = event.getCamera().getPosition();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		List<VisibleEvent> visible = new ArrayList<>(active.size());
		for (ActiveEvent timeline : active) {
			SungIlHwanVfxEventMessage message = timeline.message();
			float elapsed = timeline.elapsed(now, partialTick);
			if (elapsed < 0.0F || elapsed >= message.duration)
				continue;
			if (quality == Quality.MINIMAL && !timeline.essential())
				continue;

			Anchors anchors = resolveAnchors(minecraft, message, partialTick);
			AABB bounds = bounds(message, anchors);
			double distanceSqr = distanceToBoundsSqr(camera, bounds);
			if (distanceSqr > quality.renderDistance
					* quality.renderDistance
					|| !event.getFrustum().isVisible(bounds))
				continue;
			int light = LevelRenderer.getLightColor(minecraft.level,
					BlockPos.containing(anchors.anchor));
			visible.add(new VisibleEvent(timeline, anchors.anchor,
					anchors.focus, anchors.yaw, anchors.pitch, elapsed,
					distanceSqr, light, anchors.firstPersonCaster));
		}
		visible.sort(Comparator
				.comparing((VisibleEvent item) -> !item.timeline.essential())
				.thenComparingDouble(item -> item.distanceSqr)
				.thenComparingLong(item -> item.timeline.sequence()));
		if (visible.size() > quality.maxVisible)
			return new ArrayList<>(visible.subList(0, quality.maxVisible));
		return visible;
	}

	private static Anchors resolveAnchors(Minecraft minecraft,
			SungIlHwanVfxEventMessage message, float partialTick) {
		Vec3 anchor = message.origin();
		Vec3 focus = message.focus();
		float yaw = message.yawDegrees();
		float pitch = message.pitchDegrees();
		boolean firstPerson = false;

		Entity caster = minecraft.level.getEntity(message.casterEntityId);
		if (caster != null && !caster.isRemoved()) {
			Vec3 live = interpolated(caster, partialTick);
			if (followsCaster(message.eventType)) {
				Vec3 offset = focus.subtract(anchor);
				anchor = live;
				focus = live.add(offset);
			}
			yaw = Mth.rotLerp(partialTick, caster.yRotO,
					caster.getYRot());
			pitch = Mth.lerp(partialTick, caster.xRotO,
					caster.getXRot());
			firstPerson = minecraft.player.getId() == caster.getId()
					&& minecraft.options.getCameraType().isFirstPerson();
		}

		if ((message.eventType == SungIlHwanVfxEventMessage.FEAR_MARK
				|| message.eventType
						== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET)
				&& message.targetEntityId >= 0) {
			Entity target = minecraft.level.getEntity(message.targetEntityId);
			if (target != null && !target.isRemoved())
				focus = interpolated(target, partialTick).add(0.0D,
						Math.max(0.7D, target.getBbHeight() * 0.68D),
						0.0D);
		}
		if (message.eventType == SungIlHwanVfxEventMessage.FEAR_MARK
				|| (message.eventType
						== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET
						&& message.variant >= 2))
			anchor = focus;
		return new Anchors(anchor, focus, yaw, pitch, firstPerson);
	}

	private static Vec3 interpolated(Entity entity, float partialTick) {
		return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY()),
				Mth.lerp(partialTick, entity.zo, entity.getZ()));
	}

	private static boolean followsCaster(byte type) {
		return type == SungIlHwanVfxEventMessage.STAGE_ONE
				|| type == SungIlHwanVfxEventMessage.STAGE_TWO
				|| type == SungIlHwanVfxEventMessage.STAGE_END
				|| type
						== SungIlHwanVfxEventMessage.EXECUTION_PUBLIC_CHARGE
				|| type
						== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET
				|| type == SungIlHwanVfxEventMessage.EXECUTION_CANCEL
				|| type == SungIlHwanVfxEventMessage.EXHAUSTION
				|| type == SungIlHwanVfxEventMessage.RISK_FEEDBACK;
	}

	private static AABB bounds(SungIlHwanVfxEventMessage message,
			Anchors anchors) {
		double radius = message.eventType
						== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET
						&& message.variant >= 2
				? 2.25D : message.radius + 1.5D;
		double minX = Math.min(anchors.anchor.x, anchors.focus.x) - radius;
		double minY = Math.min(anchors.anchor.y, anchors.focus.y) - radius;
		double minZ = Math.min(anchors.anchor.z, anchors.focus.z) - radius;
		double maxX = Math.max(anchors.anchor.x, anchors.focus.x) + radius;
		double maxY = Math.max(anchors.anchor.y, anchors.focus.y)
				+ radius + 2.5D;
		double maxZ = Math.max(anchors.anchor.z, anchors.focus.z) + radius;
		return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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

	private static void render(RenderFrame frame, VisibleEvent visual,
			VertexConsumer vertices, FrameBudget budget, Pass pass) {
		if (budget.remaining < 4)
			return;
		PoseStack stack = frame.stack;
		stack.pushPose();
		stack.translate(visual.anchor.x - frame.camera.x,
				visual.anchor.y - frame.camera.y,
				visual.anchor.z - frame.camera.z);
		switch (visual.timeline.message().eventType) {
			case SungIlHwanVfxEventMessage.STAGE_ONE ->
					renderStage(frame, visual, vertices, budget, pass, false);
			case SungIlHwanVfxEventMessage.STAGE_TWO ->
					renderStage(frame, visual, vertices, budget, pass, true);
			case SungIlHwanVfxEventMessage.STAGE_END ->
					renderStageEnd(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.FEAR_PULSE ->
					renderFearPulse(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.FEAR_MARK ->
					renderFearMark(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.SPATIAL_SLASH ->
					renderSpatialSlash(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.EXECUTION_PUBLIC_CHARGE ->
					renderPublicCharge(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET ->
					renderPrivateTarget(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.EXECUTION_RELEASE ->
					renderExecutionRelease(frame, visual, vertices, budget,
							pass);
			case SungIlHwanVfxEventMessage.EXECUTION_FRACTURE ->
					renderFracture(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.EXECUTION_CANCEL ->
					renderCancel(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.EXHAUSTION ->
					renderExhaustion(frame, visual, vertices, budget, pass);
			case SungIlHwanVfxEventMessage.RISK_FEEDBACK ->
					renderRisk(frame, visual, vertices, budget, pass);
			default -> {
			}
		}
		stack.popPose();
	}

	private static void renderStage(RenderFrame frame, VisibleEvent visual,
			VertexConsumer out, FrameBudget budget, Pass pass,
			boolean stageTwo) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float fade = fadeInOut(visual.elapsed, event.duration,
				stageTwo ? 10.0F : 7.0F, 12.0F);
		float time = visual.elapsed;
		float strength = event.intensity / 255.0F;
		float radius = event.radius * (0.88F
				+ Mth.sin(time * 0.12F) * 0.035F);
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;

		// The reusable PlayerAuraSystem supplies the full-body liquid ruler
		// flame. The secondary presentation deliberately has no flat seal or
		// platform: broken roots and three-dimensional flame tongues grow out of
		// the ground and visually join the shared body aura.
		renderSpiritualGroundEnergy(frame, visual, out, budget, pass,
				stageTwo, radius, fade, strength, time, light);

		if (pass == Pass.EMISSIVE && !visual.firstPersonCaster) {
			int motes = stageTwo ? frame.quality.stageTwoMotes
					: frame.quality.stageOneMotes;
			for (int index = 0; index < motes; index++) {
				float phase = index * Mth.TWO_PI / Math.max(1, motes)
						+ event.seed * 0.0071F
						+ time * (stageTwo ? 0.19F : 0.11F);
				float y = 0.26F + index * 1.9F / Math.max(1, motes - 1);
				float orbit = (stageTwo ? 0.8F : 0.6F)
						+ 0.08F * Mth.sin(time * 0.17F + index);
				Vec3 mote = new Vec3(Mth.cos(phase) * orbit, y,
						Mth.sin(phase) * orbit);
				drawDiamondBillboard(frame, out, budget, mote,
						stageTwo ? 0.11F : 0.075F,
						index % 3 == 0 ? GOLD_WHITE : GOLD_PALE,
						alpha((stageTwo ? 170.0F : 120.0F) * fade),
						MATERIAL_GOLD, light);
			}
			if (stageTwo) {
				// A restrained forked crown distinguishes the committed stage
				// without obscuring the shared full-body aura.
				for (int side = -1; side <= 1; side += 2) {
					Vec3 start = new Vec3(side * 0.18D, 1.28D, 0.05D);
					Vec3 end = new Vec3(side * 1.05D, 2.28D, 0.18D);
					drawLine(out, frame.stack.last(), budget, start, end,
							0.052F, GOLD_HOT, alpha(175.0F * fade),
							MATERIAL_GOLD, light);
				}
			}
		}
	}

	private static void renderStageEnd(RenderFrame frame, VisibleEvent visual,
			VertexConsumer out, FrameBudget budget, Pass pass) {
		float progress = visual.timeline.progress(
				Minecraft.getInstance().level.getGameTime(),
				frame.partialTick);
		float fade = 1.0F - progress;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		int strands = pass == Pass.EMISSIVE
				? frame.quality.stageOneWisps + 2
				: Math.max(4, frame.quality.stageOneWisps - 1);
		Random random = new Random(visual.timeline.message().seed
				^ 0x65A90F21L);
		for (int index = 0; index < strands; index++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			float distance = 0.18F + random.nextFloat() * 0.62F;
			Vec3 start = new Vec3(Mth.cos(angle) * distance,
					0.05D + random.nextFloat() * 0.16D,
					Mth.sin(angle) * distance);
			float reach = 0.75F + progress * 1.9F
					+ random.nextFloat() * 0.5F;
			Vec3 end = new Vec3(Mth.cos(angle) * reach,
					0.12D + progress * (0.35D + random.nextDouble()),
					Mth.sin(angle) * reach);
			drawEnergyStrand(out, frame.stack.last(), budget, start, end,
					0.05F * (1.0F - progress * 0.55F),
					pass == Pass.SURFACE ? VOID_EDGE
							: (index % 4 == 0 ? GOLD_HOT : GOLD_AMBER),
					alpha((pass == Pass.SURFACE ? 72.0F : 142.0F)
							* fade),
					pass == Pass.SURFACE ? MATERIAL_SOFT : MATERIAL_GOLD,
					light, Math.max(2, frame.quality.slashSegments / 2),
					index * 31 + visual.timeline.message().seed);
		}
	}

	private static void renderFearPulse(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float progress = Mth.clamp(visual.elapsed / event.duration,
				0.0F, 1.0F);
		float eased = Mth.sqrt(progress);
		float fade = 1.0F - Mth.clamp((progress - 0.62F) / 0.38F,
				0.0F, 1.0F);
		float radius = 0.72F + Math.max(0.2F, event.radius - 0.72F)
				* eased;
		float bodyEnvelope = 1.0F - Mth.clamp(progress / 0.48F,
				0.0F, 1.0F);
		float shellEnvelope = Mth.sin(Mth.clamp(progress, 0.0F, 1.0F)
				* Mth.PI) * fade;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;

		// Presence erupts from the caster's entire silhouette, then drives a
		// genuinely three-dimensional shock shell across the battlefield. The
		// intentionally irregular strands keep it from reading as a flat ring.
		renderPresenceBodySurge(frame, visual, out, budget, pass,
				bodyEnvelope, progress, light);
		renderPresenceShockShell(frame, visual, out, budget, pass,
				radius, shellEnvelope, progress, light);
	}

	private static void renderFearMark(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		float pulse = 0.94F + 0.08F
				* Mth.sin(visual.elapsed * 0.48F);
		float fade = fadeInOut(visual.elapsed,
				visual.timeline.message().duration, 5.0F, 10.0F);
		float size = visual.timeline.message().radius * pulse;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		frame.stack.pushPose();
		frame.stack.mulPose(frame.cameraOrientation);
		if (pass == Pass.SURFACE) {
			drawDiamondPlane(out, frame.stack.last(), budget,
					size * 0.76F, size, VOID_BLACK,
					alpha(120.0F * fade), MATERIAL_VOID, light);
		} else {
			drawDiamondOutline(out, frame.stack.last(), budget,
					size * 0.72F, size * 0.94F, 0.045F, GOLD_HOT,
					alpha(225.0F * fade), MATERIAL_GOLD, light);
			drawLine(out, frame.stack.last(), budget,
					new Vec3(-size * 0.38F, 0.0D, 0.01D),
					new Vec3(size * 0.38F, 0.0D, 0.01D), 0.032F,
					GOLD_PALE, alpha(188.0F * fade), MATERIAL_GOLD,
					light);
		}
		frame.stack.popPose();
	}

	private static void renderSpatialSlash(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float progress = Mth.clamp(visual.elapsed / event.duration,
				0.0F, 1.0F);
		float reveal = Mth.clamp(progress / 0.14F, 0.0F, 1.0F);
		float fade = 1.0F - Mth.clamp((progress - 0.34F) / 0.66F,
				0.0F, 1.0F);
		Vec3 authored = visual.focus.subtract(visual.anchor);
		Vec3 direction = authored.lengthSqr() > 1.0E-4D
				? authored.normalize()
				: lookDirection(event.yawDegrees(), event.pitchDegrees());
		double reach = Math.max(8.0D + event.variant * 0.32D,
				authored.length());
		Vec3 upReference = Math.abs(direction.y) > 0.92D
				? new Vec3(1.0D, 0.0D, 0.0D)
				: new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 lateral = direction.cross(upReference).normalize();
		Vec3 vertical = lateral.cross(direction).normalize();
		float halfLength = 4.15F + Math.min(1.55F,
				event.variant * 0.2F);
		float diagonal = switch (event.variant % 4) {
			case 0 -> 0.52F;
			case 1 -> -0.52F;
			case 2 -> 0.2F;
			default -> -0.72F;
		};
		Vec3 center = direction.scale(Math.min(6.2D,
				2.8D + reach * 0.38D));
		Vec3 cutStart = center.add(lateral.scale(-halfLength))
				.add(vertical.scale(-halfLength * diagonal));
		Vec3 cutEnd = center.add(lateral.scale(halfLength))
				.add(vertical.scale(halfLength * diagonal));
		Vec3 revealedStart = center.add(cutStart.subtract(center)
				.scale(reveal));
		Vec3 revealedEnd = center.add(cutEnd.subtract(center)
				.scale(reveal));
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		if (pass == Pass.SURFACE) {
			drawSharpBlade(out, frame.stack.last(), budget, revealedStart,
					revealedEnd, 0.22F + event.radius * 0.08F,
					0.3F, VOID_BLACK, alpha(178.0F * fade),
					MATERIAL_SLASH, light, frame.quality.slashSegments);
			return;
		}

		// Pointed, curved blade ribbons replace the old uniform-width quads.
		// The displaced amber echo gives the cut a fast draw-through, while the
		// pale needle core keeps the silhouette razor sharp with or without
		// bloom and with the vanilla shader fallback.
		Vec3 echoOffset = vertical.scale(-0.18D)
				.add(lateral.scale(-0.08D))
				.add(direction.scale(-0.06D));
		drawSharpBlade(out, frame.stack.last(), budget,
				revealedStart.add(echoOffset), revealedEnd.add(echoOffset),
				0.13F, 0.24F, GOLD_DARK, alpha(92.0F * fade),
				MATERIAL_SLASH, light, frame.quality.slashSegments);
		drawSharpBlade(out, frame.stack.last(), budget, revealedStart,
				revealedEnd, 0.105F + event.radius * 0.018F,
				0.2F, GOLD_AMBER, alpha(225.0F * fade),
				MATERIAL_SLASH, light, frame.quality.slashSegments);
		drawSharpBlade(out, frame.stack.last(), budget, revealedStart,
				revealedEnd, 0.025F, 0.08F, GOLD_WHITE,
				alpha(255.0F * fade), MATERIAL_SLASH, light,
				frame.quality.slashSegments);

		if (event.variant >= 3) {
			Vec3 crossStart = center.add(lateral.scale(-halfLength * 0.9D))
					.add(vertical.scale(halfLength * diagonal * 0.92D));
			Vec3 crossEnd = center.add(lateral.scale(halfLength * 0.9D))
					.add(vertical.scale(-halfLength * diagonal * 0.92D));
			float secondaryReveal = Mth.clamp((reveal - 0.12F) / 0.88F,
					0.0F, 1.0F);
			crossStart = center.add(crossStart.subtract(center)
					.scale(secondaryReveal));
			crossEnd = center.add(crossEnd.subtract(center)
					.scale(secondaryReveal));
			drawSharpBlade(out, frame.stack.last(), budget, crossStart,
					crossEnd, 0.082F, -0.2F, GOLD_PALE,
					alpha(210.0F * fade), MATERIAL_SLASH, light,
					frame.quality.slashSegments);
			drawSharpBlade(out, frame.stack.last(), budget,
					revealedStart.add(vertical.scale(0.3D)),
					revealedEnd.add(vertical.scale(0.3D)), 0.019F,
					0.08F, GOLD_HOT, alpha(175.0F * fade),
					MATERIAL_SLASH, light,
					Math.max(5, frame.quality.slashSegments - 2));
		}
	}

	private static void renderPublicCharge(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float progress = Mth.clamp(visual.elapsed / event.duration,
				0.0F, 1.0F);
		float pulse = 0.92F + 0.08F
				* Mth.sin(visual.elapsed * (0.28F + progress * 0.22F));
		float fade = fadeInOut(visual.elapsed, event.duration, 5.0F, 5.0F);
		float radius = event.radius * pulse;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		if (pass == Pass.SURFACE) {
			drawHorizontalRing(out, frame.stack.last(), budget, radius,
					radius - 0.34F, 0.04F, VOID_BLACK,
					alpha(118.0F * fade), MATERIAL_VOID, light,
					frame.quality.ringSegments);
			if (!visual.firstPersonCaster) {
				int blades = frame.quality.chargeBlades;
				for (int index = 0; index < blades; index++) {
					frame.stack.pushPose();
					frame.stack.mulPose(Axis.YP.rotationDegrees(
							index * 360.0F / blades
									- visual.elapsed * (3.0F + progress * 4.0F)));
					frame.stack.translate(0.0D, 0.0D,
							0.55D + index % 2 * 0.22D);
					drawTaperedBlade(out, frame.stack.last(), budget,
							0.18F, 1.45F + progress * 0.9F,
							VOID_EDGE, alpha(76.0F * fade),
							MATERIAL_SOFT, light);
					frame.stack.popPose();
				}
			}
			return;
		}
		drawHorizontalRing(out, frame.stack.last(), budget,
				radius * (1.0F - progress * 0.32F),
				radius * (1.0F - progress * 0.32F) - 0.055F,
				0.08F + progress * 0.8F, GOLD_HOT,
				alpha(190.0F * fade), MATERIAL_GOLD, light,
				frame.quality.ringSegments);
		drawHorizontalRing(out, frame.stack.last(), budget,
				radius * (0.54F + progress * 0.18F),
				radius * (0.5F + progress * 0.18F),
				1.25F + progress * 0.7F, GOLD_PALE,
				alpha((95.0F + progress * 90.0F) * fade),
				MATERIAL_GOLD, light, frame.quality.ringSegments);
		if (!visual.firstPersonCaster) {
			drawLine(out, frame.stack.last(), budget,
					new Vec3(0.0D, 0.25D, 0.0D),
					new Vec3(0.0D, 2.35D + progress * 0.55D, 0.0D),
					0.055F, GOLD_HOT,
					alpha((85.0F + progress * 110.0F) * fade),
					MATERIAL_GOLD, light);
		}
	}

	private static void renderPrivateTarget(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		if (event.variant >= 2) {
			renderExecutionTargetMark(frame, visual, out, budget, pass);
			return;
		}
		float progress = Mth.clamp(visual.elapsed / event.duration,
				0.0F, 1.0F);
		float authoredProgress = event.intensity / 255.0F;
		float charge = Mth.clamp(Math.max(progress, authoredProgress),
				0.0F, 1.0F);
		float pulse = 0.98F + 0.025F
				* Mth.sin(visual.elapsed * 0.42F);
		// Variant zero carries the one-shot maximum and animates locally until
		// the first server snapshot. Variant one already carries the exact
		// authoritative current radius and must not be scaled a second time.
		float radius = event.variant == 0
				? event.radius * (0.16F + 0.84F * charge) * pulse
				: event.radius * pulse;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		int segments = frame.quality.sphereSegments;
		frame.stack.pushPose();
		frame.stack.translate(0.0D, 1.0D, 0.0D);
		if (pass == Pass.SURFACE) {
			drawSphereBands(out, frame.stack.last(), budget, radius,
					0.035F, VOID_EDGE, 42, MATERIAL_VOID, light,
					segments, false);
			frame.stack.popPose();
			return;
		}
		drawSphereBands(out, frame.stack.last(), budget, radius,
				0.028F, GOLD_PALE,
				alpha(115.0F + charge * 70.0F), MATERIAL_GOLD,
				light, segments, true);
		drawHorizontalRing(out, frame.stack.last(), budget,
				radius * (0.42F + 0.08F
						* Mth.sin(visual.elapsed * 0.3F)),
				radius * 0.38F, 0.0F, GOLD_HOT,
				alpha(175.0F), MATERIAL_GOLD, light, segments);

		frame.stack.pushPose();
		frame.stack.mulPose(frame.cameraOrientation);
		drawDiamondOutline(out, frame.stack.last(), budget,
				Math.min(0.9F, radius * 0.24F),
				Math.min(1.15F, radius * 0.32F), 0.03F,
				GOLD_WHITE, alpha(225.0F), MATERIAL_GOLD, light);
		frame.stack.popPose();
		frame.stack.popPose();
	}

	private static void renderExecutionTargetMark(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float fade = fadeInOut(visual.elapsed, event.duration, 2.0F, 6.0F);
		float pulse = 0.92F + 0.1F
				* Mth.sin(visual.elapsed * 0.62F + event.seed * 0.01F);
		float size = 0.72F * pulse;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		frame.stack.pushPose();
		frame.stack.mulPose(frame.cameraOrientation);
		if (pass == Pass.SURFACE) {
			drawDiamondPlane(out, frame.stack.last(), budget,
					size * 0.72F, size, VOID_BLACK,
					alpha(112.0F * fade), MATERIAL_VOID, light);
		} else {
			drawDiamondOutline(out, frame.stack.last(), budget,
					size * 0.76F, size, 0.052F, GOLD_HOT,
					alpha(238.0F * fade), MATERIAL_GOLD, light);
			drawLine(out, frame.stack.last(), budget,
					new Vec3(-size * 0.94D, 0.0D, 0.01D),
					new Vec3(size * 0.94D, 0.0D, 0.01D),
					0.026F, GOLD_WHITE, alpha(220.0F * fade),
					MATERIAL_GOLD, light);
		}
		frame.stack.popPose();
	}

	private static void renderExecutionRelease(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float progress = Mth.clamp(visual.elapsed / event.duration,
				0.0F, 1.0F);
		float fade = 1.0F - Mth.clamp((progress - 0.58F) / 0.42F,
				0.0F, 1.0F);
		float radius = event.radius;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		Vec3 sphereCenter = new Vec3(0.0D, 1.0D, 0.0D);

		frame.stack.pushPose();
		frame.stack.translate(sphereCenter.x, sphereCenter.y,
				sphereCenter.z);
		if (pass == Pass.SURFACE) {
			drawSphereBands(out, frame.stack.last(), budget,
					radius, 0.06F, VOID_BLACK,
					alpha(74.0F * fade), MATERIAL_VOID, light,
					frame.quality.sphereSegments, true);
		} else {
			drawSphereBands(out, frame.stack.last(), budget, radius,
					0.025F, GOLD_AMBER, alpha(92.0F * fade),
					MATERIAL_GOLD, light, frame.quality.sphereSegments,
					true);
		}
		frame.stack.popPose();

		// Original, code-authored Judgment-Cut-style field: deterministic cuts
		// appear throughout the whole volume while the caster's afterimages
		// imply rapid traversal. Geometry remains strictly quality-bounded.
		Random random = new Random(event.seed ^ 0x4A554447);
		int cuts = frame.quality.executionCuts;
		for (int index = 0; index < cuts; index++) {
			Vec3 center = sphereCenter.add(randomPointInSphere(random,
					radius * 0.76F));
			Vec3 direction = randomUnit(random);
			double length = radius * (0.28D + random.nextDouble() * 0.42D);
			Vec3 start = center.subtract(direction.scale(length * 0.5D));
			Vec3 end = center.add(direction.scale(length * 0.5D));
			float activation = index / (float) Math.max(1, cuts) * 0.44F;
			float localReveal = Mth.clamp((progress - activation) / 0.055F,
					0.0F, 1.0F);
			float localFade = 1.0F - Mth.clamp(
					(progress - activation - 0.16F) / 0.32F,
					0.0F, 1.0F);
			if (localReveal <= 0.0F || localFade <= 0.0F)
				continue;
			Vec3 midpoint = start.add(end).scale(0.5D);
			start = midpoint.add(start.subtract(midpoint)
					.scale(localReveal));
			end = midpoint.add(end.subtract(midpoint)
					.scale(localReveal));
			int fieldSegments = Math.max(4,
					(frame.quality.slashSegments + 1) / 2);
			float bend = index % 2 == 0 ? 0.22F : -0.22F;
			if (pass == Pass.SURFACE) {
				drawSharpBlade(out, frame.stack.last(), budget, start, end,
						0.105F, bend, VOID_BLACK,
						alpha(152.0F * localFade * fade),
						MATERIAL_SLASH, light, fieldSegments);
			} else {
				drawSharpBlade(out, frame.stack.last(), budget, start, end,
						0.062F, bend, index % 4 == 0
								? GOLD_WHITE : GOLD_HOT,
						alpha(238.0F * localFade * fade),
						MATERIAL_SLASH, light, fieldSegments);
				drawSharpBlade(out, frame.stack.last(), budget, start, end,
						0.018F, bend * 0.35F, GOLD_WHITE,
						alpha(248.0F * localFade * fade),
						MATERIAL_SLASH, light, fieldSegments);
			}
		}

		Random echoRandom = new Random(event.seed ^ 0x53494C48);
		Vec3 previous = sphereCenter;
		for (int echo = 0; echo < frame.quality.afterimages; echo++) {
			Vec3 position = sphereCenter.add(randomPointInSphere(echoRandom,
					radius * 0.68F));
			position = new Vec3(position.x,
					Math.max(0.0D, position.y - 0.55D), position.z);
			float echoWindow = Mth.clamp(
					1.0F - Math.abs(progress
							- (0.08F + echo * 0.09F)) / 0.18F,
					0.0F, 1.0F);
			drawHumanoidAfterimage(frame, out, budget, position,
					0.82F, pass == Pass.SURFACE ? VOID_EDGE : GOLD_PALE,
					alpha((pass == Pass.SURFACE ? 76.0F : 142.0F)
							* echoWindow * fade),
					pass == Pass.SURFACE ? MATERIAL_SOFT : MATERIAL_GOLD,
					light);
			if (pass == Pass.EMISSIVE)
				drawLine(out, frame.stack.last(), budget, previous,
						position.add(0.0D, 0.75D, 0.0D), 0.026F,
						GOLD_AMBER, alpha(92.0F * echoWindow * fade),
						MATERIAL_GOLD, light);
			previous = position.add(0.0D, 0.75D, 0.0D);
		}
	}

	private static void drawHumanoidAfterimage(RenderFrame frame,
			VertexConsumer out, FrameBudget budget, Vec3 position,
			float scale, int color, int alpha, int material, int light) {
		frame.stack.pushPose();
		frame.stack.translate(position.x, position.y, position.z);
		frame.stack.mulPose(frame.cameraOrientation);
		drawPlane(out, frame.stack.last(), budget, -0.22F * scale,
				0.45F * scale, 0.22F * scale, 1.43F * scale,
				color, alpha, material, light);
		drawDiamondPlane(out, frame.stack.last(), budget,
				0.18F * scale, 0.22F * scale, color,
				alpha, material, light);
		frame.stack.translate(0.0D, 1.62D * scale, 0.0D);
		drawDiamondPlane(out, frame.stack.last(), budget,
				0.17F * scale, 0.21F * scale, color,
				alpha, material, light);
		frame.stack.popPose();
	}

	private static void renderFracture(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float progress = Mth.clamp(visual.elapsed / event.duration,
				0.0F, 1.0F);
		float reveal = Mth.clamp(progress / 0.16F, 0.0F, 1.0F);
		float fade = 1.0F - Mth.clamp((progress - 0.48F) / 0.52F,
				0.0F, 1.0F);
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		// This packet's origin is already the captured executionCenter, unlike
		// the release event which is anchored at the caster's feet.
		Vec3 sphereCenter = Vec3.ZERO;
		frame.stack.pushPose();
		frame.stack.translate(sphereCenter.x, sphereCenter.y,
				sphereCenter.z);
		if (pass == Pass.SURFACE) {
			drawSphereBands(out, frame.stack.last(), budget,
					event.radius * (0.88F + progress * 0.2F),
					0.11F, VOID_BLACK, alpha(152.0F * fade),
					MATERIAL_FRACTURE, light,
					frame.quality.sphereSegments, true);
		} else {
			drawSphereBands(out, frame.stack.last(), budget,
					event.radius * (0.88F + progress * 0.2F),
					0.045F, GOLD_WHITE, alpha(245.0F * fade),
					MATERIAL_GOLD, light,
					frame.quality.sphereSegments, true);
			drawHorizontalRing(out, frame.stack.last(), budget,
					event.radius * (0.65F + progress * 0.62F),
					event.radius * (0.65F + progress * 0.62F) - 0.08F,
					0.0F, GOLD_HOT, alpha(220.0F * fade),
					MATERIAL_GOLD, light, frame.quality.ringSegments);
		}
		frame.stack.popPose();

		Random random = new Random(event.seed ^ 0x46524143);
		int cuts = frame.quality.executionCuts
				+ frame.quality.fractureRays;
		for (int index = 0; index < cuts; index++) {
			Vec3 center = sphereCenter.add(randomPointInSphere(random,
					event.radius * 0.8F));
			Vec3 direction = randomUnit(random);
			double length = event.radius
					* (0.30D + random.nextDouble() * 0.52D);
			Vec3 start = center.subtract(direction.scale(length * 0.5D
					* reveal));
			Vec3 end = center.add(direction.scale(length * 0.5D
					* reveal));
			int fieldSegments = Math.max(4,
					(frame.quality.slashSegments + 1) / 2);
			float bend = index % 2 == 0 ? 0.28F : -0.28F;
			if (pass == Pass.SURFACE) {
				drawSharpBlade(out, frame.stack.last(), budget, start, end,
						0.135F, bend, VOID_BLACK,
						alpha(184.0F * fade), MATERIAL_SLASH, light,
						fieldSegments);
			} else {
				drawSharpBlade(out, frame.stack.last(), budget, start, end,
						0.068F, bend, index % 5 == 0
								? GOLD_WHITE : GOLD_HOT,
						alpha(252.0F * fade), MATERIAL_SLASH, light,
						fieldSegments);
				drawSharpBlade(out, frame.stack.last(), budget, start, end,
						0.019F, bend * 0.32F, GOLD_WHITE,
						alpha(255.0F * fade), MATERIAL_SLASH, light,
						fieldSegments);
			}
		}
	}

	private static void renderCancel(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		float progress = visual.timeline.progress(
				Minecraft.getInstance().level.getGameTime(),
				frame.partialTick);
		float radius = visual.timeline.message().radius
				* (1.0F - progress * 0.72F);
		float fade = 1.0F - progress;
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		drawHorizontalRing(out, frame.stack.last(), budget, radius,
				Math.max(0.0F, radius - (pass == Pass.SURFACE
						? 0.25F : 0.045F)), 0.25F + progress,
				pass == Pass.SURFACE ? VOID_BLACK : GOLD_AMBER,
				alpha((pass == Pass.SURFACE ? 90.0F : 125.0F) * fade),
				pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_GOLD,
				light, frame.quality.ringSegments);
	}

	private static void renderExhaustion(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float severity = event.intensity / 255.0F;
		float fade = fadeInOut(visual.elapsed, event.duration, 3.0F, 12.0F);
		int light = pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT
				: visual.light;
		float radius = event.radius * (0.85F
				+ 0.08F * Mth.sin(visual.elapsed * 0.42F));
		drawHorizontalRing(out, frame.stack.last(), budget, radius,
				Math.max(0.0F, radius - (pass == Pass.SURFACE
						? 0.26F : 0.035F)), 0.055F,
				pass == Pass.SURFACE ? VOID_BLACK : SILVER_DARK,
				alpha((pass == Pass.SURFACE ? 105.0F : 120.0F)
						* fade * (0.6F + severity * 0.4F)),
				pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_SILVER,
				light, frame.quality.ringSegments);
		if (!visual.firstPersonCaster) {
			int shards = frame.quality == Quality.FULL ? 6 : 3;
			for (int index = 0; index < shards; index++) {
				float angle = index * Mth.TWO_PI / shards
						+ event.seed * 0.01F;
				Vec3 start = new Vec3(Mth.cos(angle) * 0.52D,
						1.7D + index % 2 * 0.18D,
						Mth.sin(angle) * 0.52D);
				Vec3 end = start.add(0.0D,
						-0.5D - severity * 0.55D, 0.0D);
				drawLine(out, frame.stack.last(), budget, start, end,
						pass == Pass.SURFACE ? 0.07F : 0.025F,
						pass == Pass.SURFACE ? VOID_EDGE : GOLD_PALE,
						alpha((pass == Pass.SURFACE ? 95.0F : 145.0F)
								* fade),
						pass == Pass.SURFACE ? MATERIAL_SOFT
								: MATERIAL_GOLD, light);
			}
		}
	}

	private static void renderRisk(RenderFrame frame, VisibleEvent visual,
			VertexConsumer out, FrameBudget budget, Pass pass) {
		if (pass == Pass.SURFACE)
			return;
		float fade = fadeInOut(visual.elapsed,
				visual.timeline.message().duration, 2.0F, 8.0F);
		float severity = visual.timeline.message().intensity / 255.0F;
		float radius = 0.72F + severity * 0.42F;
		drawHorizontalRing(out, frame.stack.last(), budget, radius,
				radius - 0.035F, 0.12F, GOLD_HOT,
				alpha(110.0F * fade * severity), MATERIAL_GOLD,
				LightTexture.FULL_BRIGHT,
				frame.quality.ringSegments);
	}

	private static void renderSpiritualGroundEnergy(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass, boolean stageTwo, float radius, float fade,
			float strength, float time, int light) {
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		int wisps = stageTwo ? frame.quality.stageTwoWisps
				: frame.quality.stageOneWisps;
		Random random = new Random(event.seed ^ 0x4D7A2C19L);
		for (int index = 0; index < wisps; index++) {
			float baseAngle = random.nextFloat() * Mth.TWO_PI;
			float drift = Mth.sin(time * (0.045F
					+ random.nextFloat() * 0.025F) + index * 1.73F)
					* (stageTwo ? 0.18F : 0.11F);
			float angle = baseAngle + drift;
			float baseRadius = radius * (0.16F
					+ random.nextFloat() * 0.34F);
			Vec3 root = new Vec3(Mth.cos(angle) * baseRadius,
					0.035D,
					Mth.sin(angle) * baseRadius);

			float rootReach = radius * (0.72F
					+ random.nextFloat() * 0.34F);
			Vec3 outerRoot = new Vec3(Mth.cos(angle) * rootReach,
					0.045D + random.nextDouble() * 0.07D,
					Mth.sin(angle) * rootReach);
			float flicker = 0.76F + 0.24F
					* Mth.sin(time * 0.26F + index * 2.17F);
			int rootColor = pass == Pass.SURFACE ? VOID_EDGE
					: (index % 4 == 0 ? GOLD_HOT : GOLD_AMBER);
			drawEnergyStrand(out, frame.stack.last(), budget, root,
					outerRoot,
					(pass == Pass.SURFACE ? 0.075F : 0.032F)
							* (stageTwo ? 1.16F : 1.0F),
					rootColor,
					alpha((pass == Pass.SURFACE ? 76.0F : 158.0F)
							* fade * flicker * strength),
					pass == Pass.SURFACE ? MATERIAL_SOFT : MATERIAL_GOLD,
					light, Math.max(2, frame.quality.slashSegments / 2),
					event.seed + index * 43);

			// Each tongue bends independently and is crossed in depth, so the
			// spiritual ground energy reads as flame rather than a vertical card.
			float height = (stageTwo ? 1.65F : 1.08F)
					* (0.72F + random.nextFloat() * 0.62F) * flicker;
			Vec3 flameEnd = root.add(
					Mth.cos(angle + Mth.HALF_PI)
							* (0.16F + random.nextFloat() * 0.25F),
					height,
					Mth.sin(angle + Mth.HALF_PI)
							* (0.16F + random.nextFloat() * 0.25F));
			int flameColor = pass == Pass.SURFACE ? GOLD_DARK
					: (index % 3 == 0 ? GOLD_WHITE : GOLD_PALE);
			drawEnergyStrand(out, frame.stack.last(), budget, root,
					flameEnd,
					(pass == Pass.SURFACE ? 0.095F : 0.047F)
							* (stageTwo ? 1.2F : 1.0F),
					flameColor,
					alpha((pass == Pass.SURFACE ? 70.0F : 184.0F)
							* fade * flicker * strength),
					MATERIAL_SOFT, light,
					Math.max(3, frame.quality.slashSegments / 2),
					event.seed ^ index * 101);
		}

		if (pass != Pass.EMISSIVE)
			return;
		int streaks = Math.max(4, wisps - 1);
		for (int index = 0; index < streaks; index++) {
			float angle = index * Mth.TWO_PI / streaks
					+ event.seed * 0.0037F
					+ Mth.sin(time * 0.07F + index) * 0.1F;
			float pulse = 0.72F + 0.28F
					* Mth.sin(time * 0.32F + index * 1.31F);
			Vec3 start = new Vec3(Mth.cos(angle) * radius * 0.12F,
					0.065D,
					Mth.sin(angle) * radius * 0.12F);
			Vec3 end = new Vec3(Mth.cos(angle) * radius
					* (0.62F + 0.24F * pulse),
					0.07D + 0.06D * pulse,
					Mth.sin(angle) * radius
					* (0.62F + 0.24F * pulse));
			drawEnergyStrand(out, frame.stack.last(), budget, start, end,
					stageTwo ? 0.03F : 0.023F,
					index % 3 == 0 ? GOLD_WHITE : GOLD_AMBER,
					alpha((stageTwo ? 152.0F : 112.0F) * fade * pulse),
					MATERIAL_GOLD, light,
					Math.max(2, frame.quality.slashSegments / 3),
					event.seed - index * 59);
		}
	}

	private static void renderPresenceBodySurge(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass, float envelope, float progress, int light) {
		if (envelope <= 0.001F)
			return;
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		int strands = Math.max(8, frame.quality.stageTwoWisps * 2);
		Random random = new Random(event.seed ^ 0x5E30A7D4L);
		float firstPersonScale = visual.firstPersonCaster ? 0.48F : 1.0F;
		for (int index = 0; index < strands; index++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			float startY = 0.08F + random.nextFloat() * 1.82F;
			float startRadius = 0.12F + random.nextFloat() * 0.33F;
			Vec3 start = new Vec3(Mth.cos(angle) * startRadius,
					startY, Mth.sin(angle) * startRadius);
			float reach = 0.75F + random.nextFloat() * 1.7F
					+ progress * 1.45F;
			float rise = (random.nextFloat() - 0.16F)
					* (1.25F + reach * 0.38F);
			Vec3 end = start.add(Mth.cos(angle) * reach, rise,
					Mth.sin(angle) * reach);
			float flicker = 0.74F + 0.26F * Mth.sin(
					visual.elapsed * 0.42F + index * 1.91F);
			int color = pass == Pass.SURFACE
					? (index % 4 == 0 ? GOLD_DARK : VOID_EDGE)
					: (index % 5 == 0 ? GOLD_WHITE
							: index % 2 == 0 ? GOLD_HOT : GOLD_AMBER);
			drawEnergyStrand(out, frame.stack.last(), budget, start, end,
					(pass == Pass.SURFACE ? 0.13F : 0.056F)
							* (0.82F + random.nextFloat() * 0.42F),
					color,
					alpha((pass == Pass.SURFACE ? 118.0F : 232.0F)
							* envelope * flicker * firstPersonScale),
					MATERIAL_SOFT, light,
					Math.max(3, frame.quality.slashSegments / 2),
					event.seed + index * 79);
		}

		// A white-hot central lift makes the source unmistakably the caster's
		// whole body instead of another mark drawn on the ground.
		int columns = Math.max(3, frame.quality.stageOneWisps / 2);
		for (int index = 0; index < columns; index++) {
			float angle = index * Mth.TWO_PI / columns
					+ event.seed * 0.005F;
			Vec3 start = new Vec3(Mth.cos(angle) * 0.22F, 0.03D,
					Mth.sin(angle) * 0.22F);
			Vec3 end = new Vec3(Mth.cos(angle + 0.7F) * 0.46F,
					2.55D + index * 0.18D,
					Mth.sin(angle + 0.7F) * 0.46F);
			drawEnergyStrand(out, frame.stack.last(), budget, start, end,
					pass == Pass.SURFACE ? 0.16F : 0.067F,
					pass == Pass.SURFACE ? GOLD_DARK : GOLD_WHITE,
					alpha((pass == Pass.SURFACE ? 100.0F : 245.0F)
							* envelope * firstPersonScale),
					MATERIAL_SOFT, light,
					Math.max(4, frame.quality.slashSegments / 2),
					event.seed ^ index * 181);
		}
	}

	private static void renderPresenceShockShell(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget,
			Pass pass, float radius, float envelope, float progress,
			int light) {
		if (envelope <= 0.001F)
			return;
		SungIlHwanVfxEventMessage event = visual.timeline.message();
		float verticalRadius = Math.max(1.05F, radius * 0.64F);
		int longitudeSegments = Math.max(8,
				frame.quality.sphereSegments + 4);
		int latitudeSegments = Math.max(4,
				frame.quality.sphereSegments / 2 + 1);
		frame.stack.pushPose();
		frame.stack.translate(0.0D, 1.0D, 0.0D);
		drawEnergyShell(out, frame.stack.last(), budget, radius,
				verticalRadius,
				pass == Pass.SURFACE ? GOLD_DARK : GOLD_HOT,
				alpha((pass == Pass.SURFACE ? 42.0F : 88.0F)
						* envelope),
				MATERIAL_SOFT, light, longitudeSegments, latitudeSegments,
				event.seed, progress, pass == Pass.EMISSIVE);
		frame.stack.popPose();

		int rays = Math.max(8, frame.quality.stageTwoWisps * 2);
		Random random = new Random(event.seed ^ 0x29B741C3L);
		Vec3 center = new Vec3(0.0D, 1.0D, 0.0D);
		for (int index = 0; index < rays; index++) {
			Vec3 direction = randomUnit(random);
			// Slightly flatten the expansion so the front surrounds a standing
			// player without becoming a perfect synthetic bubble.
			direction = new Vec3(direction.x, direction.y * 0.72D,
					direction.z).normalize();
			float jitter = 0.8F + random.nextFloat() * 0.18F;
			Vec3 start = center.add(direction.scale(radius * jitter));
			Vec3 end = center.add(direction.scale(radius
					* (1.02F + random.nextFloat() * 0.09F)
					+ 0.25F + random.nextFloat() * 0.6F));
			int color = pass == Pass.SURFACE ? GOLD_DARK
					: (index % 5 == 0 ? GOLD_WHITE : GOLD_PALE);
			drawEnergyStrand(out, frame.stack.last(), budget, start, end,
					pass == Pass.SURFACE ? 0.075F : 0.035F,
					color,
					alpha((pass == Pass.SURFACE ? 78.0F : 196.0F)
							* envelope),
					pass == Pass.SURFACE ? MATERIAL_SOFT : MATERIAL_GOLD,
					light, Math.max(2, frame.quality.slashSegments / 3),
					event.seed + index * 137);
		}
	}

	private static void drawEnergyShell(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, float horizontalRadius,
			float verticalRadius, int color, int alpha, int material,
			int light, int longitudeSegments, int latitudeSegments,
			int seed, float time, boolean broken) {
		if (alpha <= 0 || horizontalRadius <= 0.0F
				|| verticalRadius <= 0.0F)
			return;
		for (int latitude = 0; latitude < latitudeSegments; latitude++) {
			float firstV = latitude / (float) latitudeSegments;
			float secondV = (latitude + 1.0F) / latitudeSegments;
			float firstLatitude = -Mth.HALF_PI + firstV * Mth.PI;
			float secondLatitude = -Mth.HALF_PI + secondV * Mth.PI;
			for (int longitude = 0; longitude < longitudeSegments;
					longitude++) {
				int hash = seed + longitude * 7349 + latitude * 1933;
				if (broken && (hash & 7) == 0)
					continue;
				if (!budget.take(4))
					return;
				float firstU = longitude / (float) longitudeSegments;
				float secondU = (longitude + 1.0F)
						/ longitudeSegments;
				float firstLongitude = firstU * Mth.TWO_PI;
				float secondLongitude = secondU * Mth.TWO_PI;
				float ripple = 1.0F + 0.025F * Mth.sin(
						firstLongitude * 5.0F + firstLatitude * 3.0F
								+ time * 14.0F + seed * 0.001F);
				Vec3 a = ellipsoidPoint(firstLatitude, firstLongitude,
						horizontalRadius * ripple, verticalRadius * ripple);
				Vec3 b = ellipsoidPoint(firstLatitude, secondLongitude,
						horizontalRadius * ripple, verticalRadius * ripple);
				Vec3 c = ellipsoidPoint(secondLatitude, secondLongitude,
						horizontalRadius * ripple, verticalRadius * ripple);
				Vec3 d = ellipsoidPoint(secondLatitude, firstLongitude,
						horizontalRadius * ripple, verticalRadius * ripple);
				int panelAlpha = alpha((float) alpha
						* (0.78F + 0.22F * ((hash >>> 3) & 3) / 3.0F));
				shellVertex(out, pose, a, color, panelAlpha,
						material + 0.02F, 0.02F, light,
						horizontalRadius, verticalRadius);
				shellVertex(out, pose, b, color, panelAlpha,
						material + 0.98F, 0.02F, light,
						horizontalRadius, verticalRadius);
				shellVertex(out, pose, c, color, panelAlpha,
						material + 0.98F, 0.98F, light,
						horizontalRadius, verticalRadius);
				shellVertex(out, pose, d, color, panelAlpha,
						material + 0.02F, 0.98F, light,
						horizontalRadius, verticalRadius);
			}
		}
	}

	private static Vec3 ellipsoidPoint(float latitude, float longitude,
			float horizontalRadius, float verticalRadius) {
		float horizontal = Mth.cos(latitude) * horizontalRadius;
		return new Vec3(Mth.cos(longitude) * horizontal,
				Mth.sin(latitude) * verticalRadius,
				Mth.sin(longitude) * horizontal);
	}

	private static void shellVertex(VertexConsumer out, PoseStack.Pose pose,
			Vec3 point, int color, int alpha, float u, float v, int light,
			float horizontalRadius, float verticalRadius) {
		Vec3 normal = new Vec3(
				point.x / Math.max(0.001D,
						horizontalRadius * horizontalRadius),
				point.y / Math.max(0.001D,
						verticalRadius * verticalRadius),
				point.z / Math.max(0.001D,
						horizontalRadius * horizontalRadius));
		if (normal.lengthSqr() < 1.0E-6D)
			normal = new Vec3(0.0D, 1.0D, 0.0D);
		else
			normal = normal.normalize();
		vertex(out, pose, (float) point.x, (float) point.y,
				(float) point.z, color, alpha, u, v, light,
				(float) normal.x, (float) normal.y, (float) normal.z);
	}

	private static void drawEnergyStrand(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 start, Vec3 end,
			float width, int color, int alpha, int material, int light,
			int segments, int seed) {
		Vec3 delta = end.subtract(start);
		if (alpha <= 0 || width <= 0.0F || delta.lengthSqr() < 1.0E-6D)
			return;
		Vec3 direction = delta.normalize();
		Vec3 reference = Math.abs(direction.y) < 0.82D
				? new Vec3(0.0D, 1.0D, 0.0D)
				: new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 bend = direction.cross(reference);
		if (bend.lengthSqr() < 1.0E-6D)
			bend = new Vec3(0.0D, 0.0D, 1.0D);
		bend = bend.normalize().scale(
				0.05D + Math.min(0.48D, delta.length() * 0.09D));
		if ((seed & 1) == 0)
			bend = bend.scale(-1.0D);
		int safeSegments = Math.max(2, Math.min(8, segments));
		Vec3 previous = start;
		for (int index = 1; index <= safeSegments; index++) {
			float t = index / (float) safeSegments;
			float curve = Mth.sin(t * Mth.PI)
					* Mth.sin(seed * 0.013F + t * 5.7F);
			Vec3 next = start.add(delta.scale(t))
					.add(bend.scale(curve));
			float taper = 1.0F - t * 0.68F;
			int localAlpha = alpha((float) alpha
					* (0.96F - t * 0.38F));
			drawCrossedLine(out, pose, budget, previous, next,
					Math.max(0.006F, width * taper), color, localAlpha,
					material, light);
			previous = next;
		}
	}

	private static Vec3 lookDirection(float yawDegrees,
			float pitchDegrees) {
		float yaw = -yawDegrees * Mth.DEG_TO_RAD - Mth.PI;
		float pitch = -pitchDegrees * Mth.DEG_TO_RAD;
		float horizontal = -Mth.cos(pitch);
		Vec3 direction = new Vec3(Mth.sin(yaw) * horizontal,
				Mth.sin(pitch), Mth.cos(yaw) * horizontal);
		return direction.lengthSqr() < 1.0E-6D
				? new Vec3(0.0D, 0.0D, 1.0D)
				: direction.normalize();
	}

	private static Vec3 randomUnit(Random random) {
		double y = random.nextDouble() * 2.0D - 1.0D;
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
		return new Vec3(Math.cos(angle) * horizontal, y,
				Math.sin(angle) * horizontal);
	}

	private static Vec3 randomPointInSphere(Random random,
			double radius) {
		double distance = Math.cbrt(random.nextDouble())
				* Math.max(0.0D, radius);
		return randomUnit(random).scale(distance);
	}

	private static void drawCurvedTrail(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 end,
			float curve, float width, float reveal, int color, int alpha,
			int material, int light, int segments, int seed) {
		if (reveal <= 0.0F || end.lengthSqr() < 0.001D)
			return;
		Vec3 direction = end.normalize();
		Vec3 lateral = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (lateral.lengthSqr() < 0.001D)
			lateral = new Vec3(1.0D, 0.0D, 0.0D);
		lateral = lateral.normalize();
		Vec3 control = end.scale(0.5D)
				.add(lateral.scale(curve * Math.min(5.0D, end.length())))
				.add(0.0D, Math.abs(curve) * 0.75D, 0.0D);
		int visibleSegments = Math.max(1,
				Math.min(segments, Mth.ceil(segments * reveal)));
		Vec3 previous = Vec3.ZERO;
		for (int index = 1; index <= visibleSegments; index++) {
			float t = index / (float) segments;
			float inverse = 1.0F - t;
			Vec3 next = control.scale(2.0D * inverse * t)
					.add(end.scale(t * t));
			float localWidth = width * (0.42F
					+ Mth.sin(t * Mth.PI) * 0.72F);
			drawLine(out, pose, budget, previous, next, localWidth,
					color, alpha, material, light);
			previous = next;
		}
	}

	private static void drawSphereBands(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, float radius,
			float width, int color, int alpha, int material, int light,
			int segments, boolean full) {
		int latitudeCount = full ? 3 : 2;
		for (int latitude = 0; latitude < latitudeCount; latitude++) {
			float normalized = latitudeCount == 1 ? 0.0F
					: latitude / (float) (latitudeCount - 1);
			float y = (normalized - 0.5F) * radius * 1.2F;
			float ringRadius = Mth.sqrt(Math.max(0.01F,
					radius * radius - y * y));
			drawHorizontalRing(out, pose, budget, ringRadius,
					Math.max(0.0F, ringRadius - width), y, color, alpha,
					material, light, segments);
		}
		int longitudes = full ? 3 : 2;
		for (int longitude = 0; longitude < longitudes; longitude++) {
			float angle = longitude * Mth.PI / longitudes;
			Vec3[] points = new Vec3[segments + 1];
			for (int index = 0; index <= segments; index++) {
				float phase = index * Mth.TWO_PI / segments;
				float planar = Mth.cos(phase) * radius;
				points[index] = new Vec3(Mth.cos(angle) * planar,
						Mth.sin(phase) * radius,
						Mth.sin(angle) * planar);
			}
			drawPolyline(out, pose, budget, points, width, color, alpha,
					material, light);
		}
	}

	private static void drawPolyline(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3[] points, float width, int color,
			int alpha, int material, int light) {
		for (int index = 1; index < points.length; index++)
			drawLine(out, pose, budget, points[index - 1], points[index],
					width, color, alpha, material, light);
	}

	private static void drawHorizontalRing(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, float outer,
			float inner, float y, int color, int alpha, int material,
			int light, int segments) {
		if (outer <= 0.0F || alpha <= 0)
			return;
		float safeInner = Mth.clamp(inner, 0.0F, outer);
		for (int index = 0; index < segments; index++) {
			if (!budget.take(4))
				return;
			float first = index * Mth.TWO_PI / segments;
			float second = (index + 1) * Mth.TWO_PI / segments;
			float firstCos = Mth.cos(first);
			float firstSin = Mth.sin(first);
			float secondCos = Mth.cos(second);
			float secondSin = Mth.sin(second);
			vertex(out, pose, firstCos * safeInner, y,
					firstSin * safeInner, color, alpha, material + 0.02F,
					0.02F, light, 0.0F, 1.0F, 0.0F);
			vertex(out, pose, firstCos * outer, y, firstSin * outer,
					color, alpha, material + 0.98F, 0.02F, light,
					0.0F, 1.0F, 0.0F);
			vertex(out, pose, secondCos * outer, y, secondSin * outer,
					color, alpha, material + 0.98F, 0.98F, light,
					0.0F, 1.0F, 0.0F);
			vertex(out, pose, secondCos * safeInner, y,
					secondSin * safeInner, color, alpha,
					material + 0.02F, 0.98F, light,
					0.0F, 1.0F, 0.0F);
		}
	}

	private static void drawTaperedBlade(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, float halfWidth,
			float height, int color, int alpha, int material, int light) {
		if (!budget.take(4))
			return;
		vertex(out, pose, -halfWidth, 0.0F, 0.0F, color, 0,
				material + 0.02F, 0.98F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, halfWidth, 0.0F, 0.0F, color, 0,
				material + 0.98F, 0.98F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, halfWidth * 0.12F, height * 0.82F, 0.0F,
				color, alpha, material + 0.98F, 0.18F, light,
				0.0F, 0.0F, 1.0F);
		vertex(out, pose, 0.0F, height, 0.0F, color, 0,
				material + 0.5F, 0.02F, light, 0.0F, 0.0F, 1.0F);
	}

	private static void drawDiamondBillboard(RenderFrame frame,
			VertexConsumer out, FrameBudget budget, Vec3 center, float size,
			int color, int alpha, int material, int light) {
		frame.stack.pushPose();
		frame.stack.translate(center.x, center.y, center.z);
		frame.stack.mulPose(frame.cameraOrientation);
		drawDiamondPlane(out, frame.stack.last(), budget, size, size,
				color, alpha, material, light);
		frame.stack.popPose();
	}

	private static void drawDiamondPlane(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, float width,
			float height, int color, int alpha, int material, int light) {
		if (!budget.take(4))
			return;
		vertex(out, pose, 0.0F, -height, 0.0F, color, 0,
				material + 0.5F, 0.98F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, width, 0.0F, 0.0F, color, alpha,
				material + 0.98F, 0.5F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, 0.0F, height, 0.0F, color, alpha,
				material + 0.5F, 0.02F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, -width, 0.0F, 0.0F, color, alpha,
				material + 0.02F, 0.5F, light, 0.0F, 0.0F, 1.0F);
	}

	private static void drawPlane(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, float minX, float minY, float maxX,
			float maxY, int color, int alpha, int material, int light) {
		if (!budget.take(4))
			return;
		vertex(out, pose, minX, minY, 0.0F, color, alpha,
				material + 0.02F, 0.98F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, maxX, minY, 0.0F, color, alpha,
				material + 0.98F, 0.98F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, maxX, maxY, 0.0F, color, alpha,
				material + 0.98F, 0.02F, light, 0.0F, 0.0F, 1.0F);
		vertex(out, pose, minX, maxY, 0.0F, color, alpha,
				material + 0.02F, 0.02F, light, 0.0F, 0.0F, 1.0F);
	}

	private static void drawDiamondOutline(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, float width,
			float height, float lineWidth, int color, int alpha,
			int material, int light) {
		Vec3 top = new Vec3(0.0D, height, 0.0D);
		Vec3 right = new Vec3(width, 0.0D, 0.0D);
		Vec3 bottom = new Vec3(0.0D, -height, 0.0D);
		Vec3 left = new Vec3(-width, 0.0D, 0.0D);
		drawLine(out, pose, budget, top, right, lineWidth, color, alpha,
				material, light);
		drawLine(out, pose, budget, right, bottom, lineWidth, color, alpha,
				material, light);
		drawLine(out, pose, budget, bottom, left, lineWidth, color, alpha,
				material, light);
		drawLine(out, pose, budget, left, top, lineWidth, color, alpha,
				material, light);
	}

	/**
	 * Draws a pointed energy edge rather than a rectangular beam. The primary
	 * ribbon supplies the readable blade silhouette and the much thinner
	 * perpendicular ribbon keeps it visible from steep camera angles without
	 * making it look like a thick crossed plank.
	 */
	private static void drawSharpBlade(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 start, Vec3 end,
			float width, float bend, int color, int alpha, int material,
			int light, int segments) {
		Vec3 delta = end.subtract(start);
		if (delta.lengthSqr() < 1.0E-6D || width <= 0.0F || alpha <= 0)
			return;
		Vec3 direction = delta.normalize();
		Vec3 reference = Math.abs(direction.y) < 0.86D
				? new Vec3(0.0D, 1.0D, 0.0D)
				: new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 primary = direction.cross(reference);
		if (primary.lengthSqr() < 1.0E-6D)
			primary = new Vec3(0.0D, 0.0D, 1.0D);
		primary = primary.normalize();
		Vec3 secondary = direction.cross(primary).normalize();
		int boundedSegments = Mth.clamp(segments, 3, 16);
		drawTaperedSlashRibbon(out, pose, budget, start, end, primary,
				secondary.scale(width * bend * 2.25D), width, color,
				alpha, material, light, boundedSegments);
		drawTaperedSlashRibbon(out, pose, budget, start, end, secondary,
				primary.scale(-width * bend * 0.72D), width * 0.34F,
				color, alpha, material, light, boundedSegments);
	}

	private static void drawTaperedSlashRibbon(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 start, Vec3 end,
			Vec3 side, Vec3 curve, float width, int color, int alpha,
			int material, int light, int segments) {
		Vec3 normal = end.subtract(start).normalize().cross(side).normalize();
		for (int index = 0; index < segments; index++) {
			if (!budget.take(4))
				return;
			float first = index / (float) segments;
			float second = (index + 1) / (float) segments;
			float firstProfile = (float) Math.pow(
					Math.max(0.0D, Math.sin(Math.PI * first)), 0.62D);
			float secondProfile = (float) Math.pow(
					Math.max(0.0D, Math.sin(Math.PI * second)), 0.62D);
			// A slight asymmetric belly resembles a swept blade, while both
			// endpoints collapse to actual points in the fallback renderer.
			float firstWidth = width * firstProfile
					* (0.9F + 0.1F * Mth.sin(first * Mth.TWO_PI));
			float secondWidth = width * secondProfile
					* (0.9F + 0.1F * Mth.sin(second * Mth.TWO_PI));
			Vec3 firstCenter = start.lerp(end, first)
					.add(curve.scale(Mth.sin(first * Mth.PI)));
			Vec3 secondCenter = start.lerp(end, second)
					.add(curve.scale(Mth.sin(second * Mth.PI)));
			Vec3 a = firstCenter.subtract(side.scale(firstWidth));
			Vec3 b = firstCenter.add(side.scale(firstWidth));
			Vec3 c = secondCenter.add(side.scale(secondWidth));
			Vec3 d = secondCenter.subtract(side.scale(secondWidth));
			vertex(out, pose, (float) a.x, (float) a.y, (float) a.z,
					color, alpha, material + 0.02F, first, light,
					(float) normal.x, (float) normal.y, (float) normal.z);
			vertex(out, pose, (float) b.x, (float) b.y, (float) b.z,
					color, alpha, material + 0.98F, first, light,
					(float) normal.x, (float) normal.y, (float) normal.z);
			vertex(out, pose, (float) c.x, (float) c.y, (float) c.z,
					color, alpha, material + 0.98F, second, light,
					(float) normal.x, (float) normal.y, (float) normal.z);
			vertex(out, pose, (float) d.x, (float) d.y, (float) d.z,
					color, alpha, material + 0.02F, second, light,
					(float) normal.x, (float) normal.y, (float) normal.z);
		}
	}

	private static void drawCrossedLine(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 start, Vec3 end,
			float width, int color, int alpha, int material, int light) {
		drawLine(out, pose, budget, start, end, width, color, alpha,
				material, light);
		Vec3 delta = end.subtract(start);
		if (delta.lengthSqr() < 1.0E-6D)
			return;
		Vec3 direction = delta.normalize();
		Vec3 reference = Math.abs(direction.x) < 0.75D
				? new Vec3(1.0D, 0.0D, 0.0D)
				: new Vec3(0.0D, 0.0D, 1.0D);
		Vec3 side = direction.cross(reference);
		if (side.lengthSqr() < 1.0E-5D)
			return;
		drawLineRibbon(out, pose, budget, start, end,
				side.normalize().scale(width), color, alpha, material,
				light);
	}

	private static void drawLine(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 start, Vec3 end, float width,
			int color, int alpha, int material, int light) {
		Vec3 delta = end.subtract(start);
		if (delta.lengthSqr() < 1.0E-6D)
			return;
		Vec3 side = delta.normalize()
				.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (side.lengthSqr() < 1.0E-5D)
			side = new Vec3(1.0D, 0.0D, 0.0D);
		drawLineRibbon(out, pose, budget, start, end,
				side.normalize().scale(width), color, alpha, material,
				light);
	}

	private static void drawLineRibbon(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 start, Vec3 end,
			Vec3 side, int color, int alpha, int material, int light) {
		if (!budget.take(4))
			return;
		Vec3 a = start.subtract(side);
		Vec3 b = start.add(side);
		Vec3 c = end.add(side);
		Vec3 d = end.subtract(side);
		vertex(out, pose, a, color, alpha, material + 0.02F, 0.02F,
				light);
		vertex(out, pose, b, color, alpha, material + 0.98F, 0.02F,
				light);
		vertex(out, pose, c, color, alpha, material + 0.98F, 0.98F,
				light);
		vertex(out, pose, d, color, alpha, material + 0.02F, 0.98F,
				light);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose,
			Vec3 position, int color, int alpha, float u, float v,
			int light) {
		vertex(out, pose, (float) position.x, (float) position.y,
				(float) position.z, color, alpha, u, v, light,
				0.0F, 1.0F, 0.0F);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose,
			float x, float y, float z, int color, int alpha, float u,
			float v, int light, float normalX, float normalY,
			float normalZ) {
		out.addVertex(pose, x, y, z)
				.setColor((color >> 16) & 255, (color >> 8) & 255,
						color & 255, Mth.clamp(alpha, 0, 255))
				.setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, normalX, normalY, normalZ);
	}

	private static float fadeInOut(float elapsed, int duration,
			float inTicks, float outTicks) {
		float fadeIn = Mth.clamp(elapsed / Math.max(1.0F, inTicks),
				0.0F, 1.0F);
		float fadeOut = Mth.clamp((duration - elapsed)
				/ Math.max(1.0F, outTicks), 0.0F, 1.0F);
		return fadeIn * fadeOut;
	}

	private static int alpha(float value) {
		return Mth.clamp(Math.round(value), 0, 255);
	}

	private enum Pass {
		SURFACE,
		EMISSIVE
	}

	private enum Quality {
		// Visibility ceilings preserve a full 96-mark Execution snapshot. The
		// independent vertex ceilings remain authoritative for GPU work.
		FULL(88.0D, 112, 22_000, 32, 14, 7, 11, 8, 12, 8, 3, 10),
		REDUCED(56.0D, 104, 8_000, 16, 9, 4, 7, 5, 8, 5, 2, 6),
		MINIMAL(40.0D, 100, 2_800, 8, 6, 2, 4, 3, 5, 3, 1, 4);

		private final double renderDistance;
		private final int maxVisible;
		private final int maxVertices;
		private final int ringSegments;
		private final int sphereSegments;
		private final int stageOneWisps;
		private final int stageTwoWisps;
		private final int stageOneMotes;
		private final int stageTwoMotes;
		private final int chargeBlades;
		private final int afterimages;
		private final int releaseRays;
		private final int slashSegments;
		private final int fractureRays;
		private final int executionCuts;

		Quality(double renderDistance, int maxVisible, int maxVertices,
				int ringSegments, int sphereSegments,
				int stageOneWisps, int stageTwoWisps,
				int stageOneMotes, int stageTwoMotes,
				int chargeBlades, int afterimages, int releaseRays) {
			this.renderDistance = renderDistance;
			this.maxVisible = maxVisible;
			this.maxVertices = maxVertices;
			this.ringSegments = ringSegments;
			this.sphereSegments = sphereSegments;
			this.stageOneWisps = stageOneWisps;
			this.stageTwoWisps = stageTwoWisps;
			this.stageOneMotes = stageOneMotes;
			this.stageTwoMotes = stageTwoMotes;
			this.chargeBlades = chargeBlades;
			this.afterimages = afterimages;
			this.releaseRays = releaseRays;
			this.slashSegments = Math.max(5, sphereSegments - 2);
			this.fractureRays = Math.max(4, releaseRays - 1);
			this.executionCuts = Math.max(8, releaseRays * 3);
		}

		private static Quality current(Minecraft minecraft) {
			ParticleStatus status = minecraft.options.particles().get();
			if (status == ParticleStatus.MINIMAL)
				return MINIMAL;
			if (status == ParticleStatus.DECREASED)
				return REDUCED;
			return FULL;
		}
	}

	private static final class FrameBudget {
		private int remaining;

		private FrameBudget(int maximum) {
			remaining = Math.max(0, maximum);
		}

		private boolean take(int count) {
			if (count <= 0 || remaining < count)
				return false;
			remaining -= count;
			return true;
		}
	}

	private record RenderFrame(PoseStack stack, Vec3 camera,
			org.joml.Quaternionf cameraOrientation, float partialTick,
			Quality quality) {
	}

	private record VisibleEvent(ActiveEvent timeline, Vec3 anchor,
			Vec3 focus, float yaw, float pitch, float elapsed,
			double distanceSqr, int light, boolean firstPersonCaster) {
	}

	private record Anchors(Vec3 anchor, Vec3 focus, float yaw,
			float pitch, boolean firstPersonCaster) {
	}
}
