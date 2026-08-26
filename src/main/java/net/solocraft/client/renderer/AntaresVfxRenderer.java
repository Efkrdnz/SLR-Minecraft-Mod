package net.solocraft.client.renderer;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.AntaresVfxClientState.ActiveEvent;
import net.solocraft.client.renderer.shader.AntaresVfxRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.client.renderer.shader.IrisCompat;
import net.solocraft.network.AntaresVfxEventMessage;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Bounded, packet-driven world presentation for Antares. Geometry is authored
 * in code so the kit remains readable with vanilla rendering, while the custom
 * shader adds flowing destruction, hot edges, membrane veins, and fractures.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AntaresVfxRenderer {
	private static final int MATERIAL_TELEGRAPH = 0;
	private static final int MATERIAL_VOID = 1;
	private static final int MATERIAL_FLOW = 2;
	private static final int MATERIAL_HOT = 3;
	private static final int MATERIAL_FRACTURE = 4;
	private static final int MATERIAL_SMOKE = 5;
	private static final int MATERIAL_MEMBRANE = 6;
	private static final int MATERIAL_SIGIL = 7;

	private static final int OBSIDIAN = 0x09050A;
	private static final int VOID_RED = 0x29030A;
	private static final int BLOOD_DARK = 0x5B0711;
	private static final int CRIMSON = 0xC31328;
	private static final int DESTRUCTION = 0xF23832;
	private static final int EMBER = 0xFF7041;
	private static final int HOT = 0xFFD1A3;
	private static final Vec3 UP = new Vec3(0.0D, 1.0D, 0.0D);

	private AntaresVfxRenderer() {
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		AntaresVfxClientState.clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ClientLevel)
			AntaresVfxClientState.clear();
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
				&& event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;
		long now = minecraft.level.getGameTime();
		List<ActiveEvent> active = AntaresVfxClientState.snapshot(now);
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

		RenderType surfaceType = AntaresVfxRenderTypes.surface();
		VertexConsumer surface = DeferredWorldShaderRenderer.buffer(buffers,
				surfaceType, true);
		for (VisibleEvent visual : visible)
			render(frame, visual, surface, budget, Pass.SURFACE);
		buffers.endBatch(surfaceType);

		if (quality != Quality.MINIMAL && budget.remaining >= 4) {
			RenderType emissiveType = AntaresVfxRenderTypes.emissive();
			VertexConsumer emissive = DeferredWorldShaderRenderer.buffer(buffers,
					emissiveType, true);
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
			AntaresVfxEventMessage message = timeline.message();
			float elapsed = timeline.elapsed(now, partialTick);
			if (elapsed < 0.0F || elapsed >= message.duration)
				continue;
			if (quality == Quality.MINIMAL && !timeline.essential())
				continue;

			Anchors anchors = resolveAnchors(minecraft, message, partialTick);
			AABB bounds = bounds(message, anchors);
			double distanceSqr = distanceToBoundsSqr(camera, bounds);
			if (distanceSqr > quality.renderDistance * quality.renderDistance
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
			AntaresVfxEventMessage message, float partialTick) {
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
			yaw = Mth.rotLerp(partialTick, caster.yRotO, caster.getYRot());
			pitch = Mth.lerp(partialTick, caster.xRotO, caster.getXRot());
			firstPerson = minecraft.player.getId() == caster.getId()
					&& minecraft.options.getCameraType().isFirstPerson();
		}
		if (message.eventType == AntaresVfxEventMessage.OVERAWED_MARK
				&& message.targetEntityId >= 0) {
			Entity target = minecraft.level.getEntity(message.targetEntityId);
			if (target != null && !target.isRemoved()) {
				focus = interpolated(target, partialTick).add(0.0D,
						Math.max(0.8D, target.getBbHeight() * 0.68D), 0.0D);
				anchor = focus;
			}
		}
		return new Anchors(anchor, focus, yaw, pitch, firstPerson);
	}

	private static boolean followsCaster(byte type) {
		return type == AntaresVfxEventMessage.BREATH_CHARGE
				|| type == AntaresVfxEventMessage.DESCENT_LAUNCH
				|| type == AntaresVfxEventMessage.ROAR_CHARGE
				|| type == AntaresVfxEventMessage.MANIFESTATION_START
				|| type == AntaresVfxEventMessage.MANIFESTATION_END;
	}

	private static Vec3 interpolated(Entity entity, float partialTick) {
		return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
				Mth.lerp(partialTick, entity.yo, entity.getY()),
				Mth.lerp(partialTick, entity.zo, entity.getZ()));
	}

	private static AABB bounds(AntaresVfxEventMessage message,
			Anchors anchors) {
		double padding = message.radius + 2.5D;
		return new AABB(Math.min(anchors.anchor.x, anchors.focus.x) - padding,
				Math.min(anchors.anchor.y, anchors.focus.y) - padding,
				Math.min(anchors.anchor.z, anchors.focus.z) - padding,
				Math.max(anchors.anchor.x, anchors.focus.x) + padding,
				Math.max(anchors.anchor.y, anchors.focus.y) + padding + 2.5D,
				Math.max(anchors.anchor.z, anchors.focus.z) + padding);
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
			VertexConsumer out, FrameBudget budget, Pass pass) {
		if (budget.remaining < 4)
			return;
		frame.stack.pushPose();
		frame.stack.translate(visual.anchor.x - frame.camera.x,
				visual.anchor.y - frame.camera.y,
				visual.anchor.z - frame.camera.z);
		switch (visual.timeline.message().eventType) {
			case AntaresVfxEventMessage.CLAW ->
					renderClaw(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.BREATH_CHARGE ->
					renderBreathCharge(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.BREATH_STREAM ->
					renderBreathStream(frame, visual, out, budget, pass, false);
			case AntaresVfxEventMessage.BREATH_END ->
					renderBreathEnd(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.DESCENT_LAUNCH ->
					renderDescentLaunch(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.DESCENT_IMPACT ->
					renderDescentImpact(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.ROAR_CHARGE ->
					renderRoarCharge(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.ROAR_RELEASE ->
					renderRoarRelease(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.OVERAWED_MARK ->
					renderOverawedMark(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.EXTINCTION_CHARGE ->
					renderExtinctionCharge(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.EXTINCTION_PULSE ->
					renderBreathStream(frame, visual, out, budget, pass, true);
			case AntaresVfxEventMessage.EXTINCTION_AFTERMATH ->
					renderExtinctionAftermath(frame, visual, out, budget, pass);
			case AntaresVfxEventMessage.MANIFESTATION_START,
					AntaresVfxEventMessage.MANIFESTATION_END ->
					renderManifestationTransition(frame, visual, out, budget, pass);
			default -> {
			}
		}
		frame.stack.popPose();
	}

	private static void renderClaw(RenderFrame frame, VisibleEvent visual,
			VertexConsumer out, FrameBudget budget, Pass pass) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = Mth.sin(progress * Mth.PI);
		float reveal = Mth.clamp(progress * 3.2F, 0.0F, 1.0F);
		boolean finisher = event.hasFlag(AntaresVfxEventMessage.FLAG_FINISHER);
		Vec3 end = visual.focus.subtract(visual.anchor);
		int count = finisher ? 4 : 3;
		for (int index = 0; index < count; index++) {
			float spread = (index - (count - 1) * 0.5F) * (finisher ? 0.48F : 0.34F);
			int color = pass == Pass.SURFACE ? OBSIDIAN
					: (index == count / 2 ? HOT : DESTRUCTION);
			int material = pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_HOT;
			drawCurvedSlash(frame, out, budget, end, spread,
					(finisher ? 0.17F : 0.11F) * (1.0F - progress * 0.35F),
					color, alpha((pass == Pass.SURFACE ? 205.0F : 230.0F) * fade),
					material, visual.light, reveal,
					frame.quality.curveSegments);
		}
		if (finisher && progress > 0.18F) {
			float radius = event.radius * Mth.clamp((progress - 0.18F) * 2.2F,
					0.0F, 1.0F);
			drawOrientedRing(out, frame.stack.last(), budget, end,
					safeDirection(end), radius, 0.07F,
					pass == Pass.SURFACE ? BLOOD_DARK : EMBER,
					alpha((pass == Pass.SURFACE ? 120.0F : 190.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_TELEGRAPH : MATERIAL_HOT,
					passLight(pass, visual.light), frame.quality.ringSegments);
		}
	}

	private static void renderBreathCharge(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = fadeInOut(visual.elapsed, event.duration, 2.0F, 2.5F);
		Vec3 direction = lookDirection(visual.yaw, visual.pitch);
		Vec3 mouth = new Vec3(0.0D, 1.48D, 0.0D).add(direction.scale(0.55D));
		for (int ring = 0; ring < 3; ring++) {
			float phase = Mth.clamp(progress * 1.35F - ring * 0.13F, 0.0F, 1.0F);
			float radius = (1.05F - phase * 0.78F) * (1.0F + ring * 0.12F);
			drawOrientedRing(out, frame.stack.last(), budget,
					mouth.add(direction.scale(0.35D + ring * 0.28D)), direction,
					radius, 0.045F, pass == Pass.SURFACE ? BLOOD_DARK : CRIMSON,
					alpha((pass == Pass.SURFACE ? 105.0F : 185.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_TELEGRAPH : MATERIAL_FLOW,
					passLight(pass, visual.light), frame.quality.ringSegments);
		}
		if (!visual.firstPersonCaster)
			drawDragonHead(frame, out, budget,
					new Vec3(0.0D, 1.35D, 0.0D), direction,
					1.15F + progress * 0.28F, fade,
					pass, visual.light, event.seed);
		drawBillboard(frame, out, budget, mouth, 0.18F + progress * 0.12F,
				pass == Pass.SURFACE ? VOID_RED : HOT,
				alpha((pass == Pass.SURFACE ? 155.0F : 235.0F) * fade),
				pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_SIGIL,
				passLight(pass, visual.light));
	}

	private static void renderBreathStream(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass,
			boolean extinction) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = fadeInOut(visual.elapsed, event.duration, 1.4F,
				extinction ? 6.0F : 3.0F);
		Vec3 end = visual.focus.subtract(visual.anchor);
		Vec3 direction = safeDirection(end);
		float pulse = extinction ? 1.0F + event.variant * 0.075F : 1.0F;
		float radius = event.radius * pulse
				* (0.82F + 0.12F * Mth.sin(visual.elapsed * 0.75F));
		int sides = extinction ? frame.quality.extinctionSides
				: frame.quality.beamSides;
		// Layers counter-swirl at different rates so the column churns instead of
		// sliding as one rigid cone.
		int segments = extinction ? frame.quality.extinctionSides + 6
				: frame.quality.beamSides + 4;
		float breathTime = visual.elapsed;
		if (pass == Pass.SURFACE) {
			drawFlowTube(out, frame.stack.last(), budget, Vec3.ZERO, end,
					radius * 0.55F, radius * 1.35F, OBSIDIAN,
					alpha((extinction ? 220.0F : 188.0F) * fade), MATERIAL_VOID,
					visual.light, sides, segments, 1.15F, 0.10F, breathTime);
			drawFlowTube(out, frame.stack.last(), budget,
					Vec3.ZERO.add(direction.scale(0.04D)), end,
					radius * 0.44F, radius * 1.05F, BLOOD_DARK,
					alpha((extinction ? 178.0F : 145.0F) * fade), MATERIAL_FLOW,
					visual.light, sides, segments, -1.75F, 0.16F, breathTime);
		} else {
			drawFlowTube(out, frame.stack.last(), budget,
					Vec3.ZERO.add(direction.scale(0.08D)), end,
					radius * 0.26F, radius * 0.62F,
					extinction ? HOT : DESTRUCTION,
					alpha((extinction ? 245.0F : 220.0F) * fade), MATERIAL_HOT,
					LightTexture.FULL_BRIGHT, sides, segments, 2.4F, 0.07F,
					breathTime);
			drawFlowTube(out, frame.stack.last(), budget,
					Vec3.ZERO.add(direction.scale(0.06D)), end,
					radius * 0.36F, radius * 0.9F, EMBER,
					alpha((extinction ? 165.0F : 135.0F) * fade), MATERIAL_FLOW,
					LightTexture.FULL_BRIGHT, sides, segments, -3.1F, 0.22F,
					breathTime);
			drawCrossedLine(out, frame.stack.last(), budget, Vec3.ZERO, end,
					radius * (extinction ? 0.2F : 0.15F), HOT,
					alpha(245.0F * fade), MATERIAL_HOT,
					LightTexture.FULL_BRIGHT);
		}
		drawOrientedRing(out, frame.stack.last(), budget, end, direction,
				radius * (1.04F + progress * 0.3F), radius * 0.08F,
				pass == Pass.SURFACE ? BLOOD_DARK : EMBER,
				alpha((pass == Pass.SURFACE ? 135.0F : 215.0F) * fade),
				pass == Pass.SURFACE ? MATERIAL_TELEGRAPH : MATERIAL_HOT,
				passLight(pass, visual.light), frame.quality.ringSegments);
		if (extinction && !visual.firstPersonCaster)
			drawDragonHead(frame, out, budget,
					direction.scale(-1.6D).add(0.0D, 1.1D, 0.0D), direction,
					2.35F, fade, pass, visual.light, event.seed);
	}

	private static void renderBreathEnd(RenderFrame frame, VisibleEvent visual,
			VertexConsumer out, FrameBudget budget, Pass pass) {
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = 1.0F - progress;
		Vec3 end = visual.focus.subtract(visual.anchor);
		if (pass == Pass.SURFACE)
			drawTube(out, frame.stack.last(), budget, Vec3.ZERO, end,
					0.75F * fade, 0.36F * fade, BLOOD_DARK,
					alpha(92.0F * fade), MATERIAL_SMOKE, visual.light,
					frame.quality.beamSides);
		int motes = pass == Pass.EMISSIVE ? frame.quality.motes
				: Math.max(2, frame.quality.motes / 2);
		Random random = new Random(visual.timeline.message().seed);
		for (int index = 0; index < motes; index++) {
			float along = random.nextFloat();
			Vec3 center = end.scale(along).add(
					(random.nextDouble() - 0.5D) * 0.7D,
					progress * (0.4D + random.nextDouble()),
					(random.nextDouble() - 0.5D) * 0.7D);
			drawBillboard(frame, out, budget, center,
					0.12F + random.nextFloat() * 0.2F,
					pass == Pass.SURFACE ? OBSIDIAN : CRIMSON,
					alpha((pass == Pass.SURFACE ? 70.0F : 105.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_SMOKE : MATERIAL_FLOW,
					passLight(pass, visual.light));
		}
	}

	private static void renderDescentLaunch(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		float fade = fadeInOut(visual.elapsed,
				visual.timeline.message().duration, 3.0F, 6.0F);
		float flap = Mth.sin(visual.elapsed * 0.31F) * 0.16F;
		if (!visual.firstPersonCaster)
			drawWingPair(out, frame.stack.last(), budget, visual.yaw,
					1.0F + flap, fade, pass, visual.light);
		Vec3 direction = lookDirection(visual.yaw, visual.pitch);
		Vec3 trailEnd = direction.scale(-3.2D).add(0.0D, 1.15D, 0.0D);
		drawCrossedLine(out, frame.stack.last(), budget,
				new Vec3(0.0D, 1.15D, 0.0D), trailEnd,
				pass == Pass.SURFACE ? 0.36F : 0.11F,
				pass == Pass.SURFACE ? OBSIDIAN : DESTRUCTION,
				alpha((pass == Pass.SURFACE ? 115.0F : 190.0F) * fade),
				pass == Pass.SURFACE ? MATERIAL_SMOKE : MATERIAL_HOT,
				passLight(pass, visual.light));
	}

	private static void renderDescentImpact(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = 1.0F - Mth.clamp((progress - 0.62F) / 0.38F, 0.0F, 1.0F);
		float radius = event.radius * Mth.sqrt(progress);
		for (int ring = 0; ring < 2; ring++)
			drawHorizontalRing(out, frame.stack.last(), budget,
					0.05D + ring * 0.07D,
					radius * (1.0F - ring * 0.18F), 0.08F,
					pass == Pass.SURFACE ? BLOOD_DARK : DESTRUCTION,
					alpha((pass == Pass.SURFACE ? 125.0F : 210.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_TELEGRAPH : MATERIAL_HOT,
					passLight(pass, visual.light), frame.quality.ringSegments);
		drawFractures(out, frame.stack.last(), budget, event.seed, radius,
				fade, pass, visual.light, frame.quality.fractures);
		int spires = Math.max(4, frame.quality.fractures / 2);
		for (int index = 0; index < spires; index++) {
			float angle = index * Mth.TWO_PI / spires + event.seed * 0.013F;
			float distance = radius * (0.24F + 0.52F * ((index % 3) / 2.0F));
			Vec3 base = new Vec3(Mth.cos(angle) * distance, 0.04D,
					Mth.sin(angle) * distance);
			Vec3 tip = base.add(Mth.cos(angle) * 0.3D,
					(1.1D + (index % 3) * 0.5D) * fade,
					Mth.sin(angle) * 0.3D);
			drawCrossedLine(out, frame.stack.last(), budget, base, tip,
					pass == Pass.SURFACE ? 0.12F : 0.045F,
					pass == Pass.SURFACE ? OBSIDIAN : EMBER,
					alpha((pass == Pass.SURFACE ? 145.0F : 215.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_HOT,
					passLight(pass, visual.light));
		}
	}

	private static void renderRoarCharge(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = Mth.sin(progress * Mth.PI * 0.82F);
		Vec3 direction = lookDirection(visual.yaw, 0.0F);
		if (!visual.firstPersonCaster)
			drawDragonHead(frame, out, budget,
					new Vec3(0.0D, 1.05D, 0.0D).add(direction.scale(-0.72D)),
					direction, 1.7F + progress * 0.55F, fade,
					pass, visual.light, visual.timeline.message().seed);
		for (int ring = 0; ring < 2; ring++)
			drawHorizontalRing(out, frame.stack.last(), budget,
					0.08D + ring * 0.05D,
					0.85F + progress * (1.1F + ring * 0.35F), 0.045F,
					pass == Pass.SURFACE ? BLOOD_DARK : CRIMSON,
					alpha((pass == Pass.SURFACE ? 95.0F : 155.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_TELEGRAPH : MATERIAL_FLOW,
					passLight(pass, visual.light), frame.quality.ringSegments);
	}

	private static void renderRoarRelease(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = 1.0F - Mth.clamp((progress - 0.72F) / 0.28F, 0.0F, 1.0F);
		float radius = event.radius * Mth.sqrt(progress);
		for (int layer = 0; layer < 3; layer++) {
			float layerProgress = Mth.clamp(progress * 1.25F - layer * 0.11F,
					0.0F, 1.0F);
			drawHorizontalRing(out, frame.stack.last(), budget,
					0.22D + layer * 0.34D,
					event.radius * Mth.sqrt(layerProgress),
					0.075F + layer * 0.02F,
					pass == Pass.SURFACE ? OBSIDIAN : (layer == 0 ? HOT : CRIMSON),
					alpha((pass == Pass.SURFACE ? 118.0F : 190.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_FLOW,
					passLight(pass, visual.light), frame.quality.ringSegments);
		}
		if (pass == Pass.EMISSIVE) {
			int rays = frame.quality.motes;
			for (int index = 0; index < rays; index++) {
				float angle = index * Mth.TWO_PI / rays + event.seed * 0.019F;
				Vec3 end = new Vec3(Mth.cos(angle) * radius,
						0.45D + (index % 3) * 0.22D,
						Mth.sin(angle) * radius);
				drawCrossedLine(out, frame.stack.last(), budget,
						new Vec3(0.0D, 1.0D, 0.0D), end, 0.035F,
						index % 4 == 0 ? HOT : DESTRUCTION,
						alpha(135.0F * fade), MATERIAL_HOT,
						LightTexture.FULL_BRIGHT);
			}
		}
	}

	private static void renderOverawedMark(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		float fade = fadeInOut(visual.elapsed,
				visual.timeline.message().duration, 4.0F, 8.0F);
		float pulse = 0.92F + Mth.sin(visual.elapsed * 0.42F) * 0.08F;
		drawBillboard(frame, out, budget, new Vec3(0.0D, 0.55D, 0.0D),
				0.32F * pulse, pass == Pass.SURFACE ? OBSIDIAN : DESTRUCTION,
				alpha((pass == Pass.SURFACE ? 185.0F : 220.0F) * fade),
				pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_SIGIL,
				passLight(pass, visual.light));
		for (int side = -1; side <= 1; side += 2)
			drawCrossedLine(out, frame.stack.last(), budget,
					new Vec3(side * 0.08D, 0.2D, 0.0D),
					new Vec3(side * 0.24D, 0.95D, 0.0D), 0.025F,
					pass == Pass.SURFACE ? BLOOD_DARK : EMBER,
					alpha((pass == Pass.SURFACE ? 130.0F : 190.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_TELEGRAPH : MATERIAL_HOT,
					passLight(pass, visual.light));
	}

	private static void renderExtinctionCharge(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = fadeInOut(visual.elapsed, event.duration, 3.0F, 2.0F);
		Vec3 end = visual.focus.subtract(visual.anchor);
		Vec3 direction = safeDirection(end);
		if (pass == Pass.SURFACE) {
			drawTube(out, frame.stack.last(), budget, Vec3.ZERO, end,
					event.radius * (0.74F - progress * 0.25F),
					event.radius * (0.54F - progress * 0.18F), BLOOD_DARK,
					alpha(82.0F * fade), MATERIAL_TELEGRAPH, visual.light,
					frame.quality.beamSides);
		}
		for (int ring = 0; ring < 4; ring++) {
			float along = 0.08F + ring * 0.18F;
			float radius = event.radius * (1.25F - progress * 0.72F)
					* (1.0F - ring * 0.08F);
			drawOrientedRing(out, frame.stack.last(), budget, end.scale(along),
					direction, radius, 0.055F,
					pass == Pass.SURFACE ? OBSIDIAN : (ring == 0 ? HOT : CRIMSON),
					alpha((pass == Pass.SURFACE ? 132.0F : 208.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_HOT,
					passLight(pass, visual.light), frame.quality.ringSegments);
		}
		if (!visual.firstPersonCaster)
			drawDragonHead(frame, out, budget,
					direction.scale(-2.0D).add(0.0D, 1.2D, 0.0D), direction,
					2.0F + progress * 1.05F, fade, pass, visual.light, event.seed);
	}

	private static void renderExtinctionAftermath(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		AntaresVfxEventMessage event = visual.timeline.message();
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float fade = 1.0F - progress;
		Vec3 end = visual.focus.subtract(visual.anchor);
		Vec3 direction = safeDirection(end);
		Vec3 side = basisRight(direction);
		int scars = frame.quality.aftermathScars;
		Random random = new Random(event.seed ^ 0x6A09E667);
		for (int index = 0; index < scars; index++) {
			float along = (index + 0.5F) / scars;
			Vec3 center = end.scale(along).add(side.scale(
					(random.nextDouble() - 0.5D) * event.radius * 1.4D));
			Vec3 scarEnd = center.add(direction.scale(0.7D + random.nextDouble() * 1.8D))
					.add(side.scale((random.nextDouble() - 0.5D) * 0.8D));
			drawLineRibbon(out, frame.stack.last(), budget,
					center.add(0.0D, 0.035D, 0.0D),
					scarEnd.add(0.0D, 0.035D, 0.0D), side.scale(0.08D),
					pass == Pass.SURFACE ? OBSIDIAN : CRIMSON,
					alpha((pass == Pass.SURFACE ? 120.0F : 150.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_FRACTURE : MATERIAL_HOT,
					passLight(pass, visual.light));
			if (index % 2 == 0)
				drawBillboard(frame, out, budget,
						center.add(0.0D, 0.3D + progress * 0.9D, 0.0D),
						0.35F + progress * 0.35F,
						pass == Pass.SURFACE ? OBSIDIAN : BLOOD_DARK,
						alpha((pass == Pass.SURFACE ? 75.0F : 55.0F) * fade),
						MATERIAL_SMOKE, passLight(pass, visual.light));
		}
	}

	private static void renderManifestationTransition(RenderFrame frame,
			VisibleEvent visual, VertexConsumer out, FrameBudget budget, Pass pass) {
		boolean starting = visual.timeline.message().eventType
				== AntaresVfxEventMessage.MANIFESTATION_START;
		float progress = visual.timeline.progress(gameTime(), frame.partialTick);
		float envelope = starting ? smoothOut(progress) : 1.0F - progress;
		float fade = starting ? fadeInOut(visual.elapsed,
				visual.timeline.message().duration, 3.0F, 5.0F) : 1.0F - progress;
		if (!visual.firstPersonCaster)
			drawWingPair(out, frame.stack.last(), budget, visual.yaw,
					envelope, fade, pass, visual.light);
		for (int ring = 0; ring < 3; ring++) {
			float radius = (0.8F + ring * 0.56F)
					* (starting ? 0.45F + envelope * 1.2F : 1.0F + progress);
			drawHorizontalRing(out, frame.stack.last(), budget,
					0.05D + ring * 0.13D, radius, 0.06F,
					pass == Pass.SURFACE ? OBSIDIAN : (ring == 0 ? HOT : CRIMSON),
					alpha((pass == Pass.SURFACE ? 140.0F : 205.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_FLOW,
					passLight(pass, visual.light), frame.quality.ringSegments);
		}
	}

	private static void drawCurvedSlash(RenderFrame frame, VertexConsumer out,
			FrameBudget budget, Vec3 end, float spread, float width, int color,
			int alpha, int material, int light, float reveal, int segments) {
		Vec3 direction = safeDirection(end);
		Vec3 side = basisRight(direction);
		Vec3 lift = safeDirection(side.cross(direction));
		Vec3 previous = side.scale(spread * 0.55D).add(lift.scale(-0.18D));
		int count = Math.max(3, segments);
		for (int segment = 1; segment <= count; segment++) {
			float t = segment / (float) count;
			if (t > reveal)
				break;
			float curve = Mth.sin(t * Mth.PI);
			Vec3 point = end.scale(t)
					.add(side.scale(spread * (0.55D - t) + curve * spread * 0.52D))
					.add(lift.scale(curve * (0.36D + Math.abs(spread) * 0.3D) - 0.18D));
			float taper = Mth.sin(Mth.PI * Mth.clamp(t, 0.03F, 0.97F));
			drawLineRibbon(out, frame.stack.last(), budget, previous, point,
					side.scale(Math.max(0.012D, width * taper)), color, alpha,
					material, light);
			previous = point;
		}
	}

	private static void drawDragonHead(RenderFrame frame, VertexConsumer out,
			FrameBudget budget, Vec3 center, Vec3 forward, float scale,
			float fade, Pass pass, int light, int seed) {
		Vec3 direction = safeDirection(forward);
		Vec3 right = basisRight(direction);
		Vec3 up = safeDirection(right.cross(direction));
		Vec3 back = center.add(direction.scale(-0.62D * scale));
		Vec3 brow = center.add(up.scale(0.24D * scale));
		Vec3 snout = center.add(direction.scale(0.72D * scale))
				.add(up.scale(-0.08D * scale));
		int color = pass == Pass.SURFACE ? OBSIDIAN : CRIMSON;
		int material = pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_FLOW;
		int alpha = alpha((pass == Pass.SURFACE ? 170.0F : 185.0F) * fade);
		drawQuad(out, frame.stack.last(), budget,
				back.add(right.scale(-0.48D * scale)),
				back.add(right.scale(0.48D * scale)),
				snout.add(right.scale(0.19D * scale)),
				snout.add(right.scale(-0.19D * scale)), color, alpha,
				material, passLight(pass, light));
		drawQuad(out, frame.stack.last(), budget,
				back.add(up.scale(-0.35D * scale)),
				brow.add(up.scale(0.36D * scale)), snout.add(up.scale(0.09D * scale)),
				snout.add(up.scale(-0.2D * scale)), color, alpha,
				material, passLight(pass, light));
		for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
			Vec3 hornRoot = brow.add(right.scale(sideSign * 0.32D * scale));
			Vec3 hornTip = back.add(right.scale(sideSign * 0.9D * scale))
					.add(up.scale(0.72D * scale));
			drawCrossedLine(out, frame.stack.last(), budget, hornRoot, hornTip,
					0.045F * scale, pass == Pass.SURFACE ? VOID_RED : EMBER,
					alpha((pass == Pass.SURFACE ? 195.0F : 225.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_HOT,
					passLight(pass, light));
			Vec3 eye = center.add(direction.scale(0.16D * scale))
					.add(right.scale(sideSign * 0.27D * scale))
					.add(up.scale(0.14D * scale));
			drawBillboard(frame, out, budget, eye, 0.085F * scale,
					pass == Pass.SURFACE ? BLOOD_DARK : HOT,
					alpha((pass == Pass.SURFACE ? 170.0F : 245.0F) * fade),
					MATERIAL_SIGIL, passLight(pass, light));
		}
	}

	/** Shared by the transient and persistent manifestation renderers. */
	public static void drawWingPair(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, float yaw, float spread, float fade, Pass pass,
			int light) {
		float radians = yaw * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
		Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
		Vec3 back = forward.scale(-1.0D);
		for (int sign = -1; sign <= 1; sign += 2) {
			Vec3 root = new Vec3(0.0D, 1.48D, 0.0D)
					.add(right.scale(sign * 0.18D));
			Vec3 elbow = root.add(right.scale(sign * 1.25D * spread))
					.add(0.0D, 0.78D * spread, 0.0D)
					.add(back.scale(0.44D));
			Vec3 tip = root.add(right.scale(sign * 3.15D * spread))
					.add(0.0D, 0.32D * spread, 0.0D)
					.add(back.scale(1.06D));
			Vec3 lower = root.add(right.scale(sign * 1.62D * spread))
					.add(0.0D, -1.0D * spread, 0.0D)
					.add(back.scale(0.82D));
			int membraneColor = pass == Pass.SURFACE ? OBSIDIAN : BLOOD_DARK;
			drawQuad(out, pose, budget, root, elbow, tip, lower,
					membraneColor,
					alpha((pass == Pass.SURFACE ? 188.0F : 105.0F) * fade),
					MATERIAL_MEMBRANE, passLight(pass, light));
			int boneColor = pass == Pass.SURFACE ? VOID_RED : DESTRUCTION;
			for (Vec3 destination : new Vec3[] {elbow, tip, lower})
				drawCrossedLine(out, pose, budget, root, destination,
						pass == Pass.SURFACE ? 0.055F : 0.026F,
						boneColor,
						alpha((pass == Pass.SURFACE ? 220.0F : 205.0F) * fade),
						pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_HOT,
						passLight(pass, light));
		}
		// A compact swept-back crown keeps the silhouette readable even when the
		// wings are viewed edge-on. It is deliberately attached to the player,
		// not a free-floating billboard.
		Vec3 crown = new Vec3(0.0D, 1.78D, 0.0D);
		for (int sign = -1; sign <= 1; sign += 2) {
			Vec3 root = crown.add(right.scale(sign * 0.13D));
			Vec3 tip = crown.add(right.scale(sign * 0.48D * spread))
					.add(0.0D, 0.62D * spread, 0.0D)
					.add(back.scale(0.35D));
			drawCrossedLine(out, pose, budget, root, tip,
					pass == Pass.SURFACE ? 0.045F : 0.021F,
					pass == Pass.SURFACE ? VOID_RED : EMBER,
					alpha((pass == Pass.SURFACE ? 225.0F : 215.0F) * fade),
					pass == Pass.SURFACE ? MATERIAL_VOID : MATERIAL_HOT,
					passLight(pass, light));
		}
	}

	private static void drawFractures(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, int seed, float radius, float fade, Pass pass,
			int light, int count) {
		Random random = new Random(seed ^ 0xBB67AE85);
		for (int index = 0; index < count; index++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			float reach = radius * (0.42F + random.nextFloat() * 0.58F);
			Vec3 side = new Vec3(-Mth.sin(angle), 0.0D, Mth.cos(angle));
			Vec3 start = new Vec3(Mth.cos(angle) * 0.2D, 0.035D,
					Mth.sin(angle) * 0.2D);
			Vec3 middle = new Vec3(Mth.cos(angle) * reach * 0.55D, 0.038D,
					Mth.sin(angle) * reach * 0.55D)
					.add(side.scale((random.nextDouble() - 0.5D) * reach * 0.22D));
			Vec3 end = new Vec3(Mth.cos(angle) * reach, 0.04D,
					Mth.sin(angle) * reach)
					.add(side.scale((random.nextDouble() - 0.5D) * reach * 0.14D));
			int color = pass == Pass.SURFACE ? OBSIDIAN : DESTRUCTION;
			int material = pass == Pass.SURFACE ? MATERIAL_FRACTURE : MATERIAL_HOT;
			int alpha = alpha((pass == Pass.SURFACE ? 175.0F : 210.0F) * fade);
			drawLineRibbon(out, pose, budget, start, middle,
					side.scale(0.07D), color, alpha, material,
					passLight(pass, light));
			drawLineRibbon(out, pose, budget, middle, end,
					side.scale(0.045D), color, alpha, material,
					passLight(pass, light));
		}
	}

	private static void drawTube(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 start, Vec3 end, float startRadius,
			float endRadius, int color, int alpha, int material, int light,
			int sides) {
		Vec3 axis = safeDirection(end.subtract(start));
		Vec3 right = basisRight(axis);
		Vec3 up = safeDirection(right.cross(axis));
		int count = Math.max(4, sides);
		for (int side = 0; side < count; side++) {
			float a0 = side * Mth.TWO_PI / count;
			float a1 = (side + 1) * Mth.TWO_PI / count;
			Vec3 r0 = right.scale(Mth.cos(a0)).add(up.scale(Mth.sin(a0)));
			Vec3 r1 = right.scale(Mth.cos(a1)).add(up.scale(Mth.sin(a1)));
			drawQuad(out, pose, budget,
					start.add(r0.scale(startRadius)),
					start.add(r1.scale(startRadius)),
					end.add(r1.scale(endRadius)),
					end.add(r0.scale(endRadius)), color, alpha, material, light);
		}
	}

	/**
	 * Segmented tube with a flare profile, a spine that writhes, and a twist
	 * around its axis. Its V coordinate advances along the length.
	 *
	 * <p>{@link #drawTube} is a single-segment cone, so it can neither bend nor
	 * flare, and {@link #drawQuad} gives every quad the whole 0..1 V range. The
	 * two together meant a twenty block breath carried exactly one flame cell,
	 * animating inside each quad instead of streaming down the beam. This draws
	 * rings along the axis and maps V across the whole run so the shader's flow
	 * and its cooling gradient finally travel the length of the effect.
	 */
	private static void drawFlowTube(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 start, Vec3 end, float startRadius,
			float endRadius, int color, int alpha, int material, int light,
			int sides, int segments, float swirl, float turbulence, float time) {
		Vec3 axis = safeDirection(end.subtract(start));
		Vec3 right = basisRight(axis);
		Vec3 up = safeDirection(right.cross(axis));
		int ringCount = Math.max(2, segments);
		int sideCount = Math.max(4, sides);
		Vec3 span = end.subtract(start);
		Vec3[] centers = new Vec3[ringCount + 1];
		float[] radii = new float[ringCount + 1];
		float[] twists = new float[ringCount + 1];
		for (int ring = 0; ring <= ringCount; ring++) {
			float t = ring / (float) ringCount;
			// Smoothstep flare reads as a mouth opening, not a straight cone.
			float eased = t * t * (3.0F - 2.0F * t);
			float radius = startRadius + (endRadius - startRadius) * eased;
			radius *= 1.0F + 0.16F * Mth.sin(t * 7.3F + time * 2.1F);
			float wobble = turbulence * radius;
			centers[ring] = start.add(span.scale(t))
					.add(right.scale(Mth.sin(t * 5.1F + time * 1.7F) * wobble))
					.add(up.scale(Mth.cos(t * 4.3F + time * 1.9F) * wobble));
			radii[ring] = radius;
			twists[ring] = swirl * t + time * 0.6F;
		}
		for (int side = 0; side < sideCount; side++) {
			float base0 = side * Mth.TWO_PI / sideCount;
			float base1 = (side + 1) * Mth.TWO_PI / sideCount;
			for (int ring = 0; ring < ringCount; ring++) {
				float v0 = ring / (float) ringCount;
				float v1 = (ring + 1) / (float) ringCount;
				drawQuadUv(out, pose, budget,
						centers[ring].add(radial(right, up, base0 + twists[ring], radii[ring])),
						centers[ring].add(radial(right, up, base1 + twists[ring], radii[ring])),
						centers[ring + 1].add(radial(right, up, base1 + twists[ring + 1], radii[ring + 1])),
						centers[ring + 1].add(radial(right, up, base0 + twists[ring + 1], radii[ring + 1])),
						color, alpha, material, light, v0, v1);
			}
		}
	}

	private static Vec3 radial(Vec3 right, Vec3 up, float angle, float radius) {
		return right.scale(Mth.cos(angle) * radius)
				.add(up.scale(Mth.sin(angle) * radius));
	}

	private static void drawHorizontalRing(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, double y, float radius,
			float width, int color, int alpha, int material, int light,
			int segments) {
		drawOrientedRing(out, pose, budget, new Vec3(0.0D, y, 0.0D), UP,
				radius, width, color, alpha, material, light, segments);
	}

	private static void drawOrientedRing(VertexConsumer out,
			PoseStack.Pose pose, FrameBudget budget, Vec3 center, Vec3 normal,
			float radius, float width, int color, int alpha, int material,
			int light, int segments) {
		Vec3 axis = safeDirection(normal);
		Vec3 right = basisRight(axis);
		Vec3 up = safeDirection(right.cross(axis));
		float inner = Math.max(0.01F, radius - width);
		float outer = radius + width;
		int count = Math.max(6, segments);
		for (int segment = 0; segment < count; segment++) {
			float a0 = segment * Mth.TWO_PI / count;
			float a1 = (segment + 1) * Mth.TWO_PI / count;
			Vec3 d0 = right.scale(Mth.cos(a0)).add(up.scale(Mth.sin(a0)));
			Vec3 d1 = right.scale(Mth.cos(a1)).add(up.scale(Mth.sin(a1)));
			drawQuad(out, pose, budget, center.add(d0.scale(inner)),
					center.add(d0.scale(outer)), center.add(d1.scale(outer)),
					center.add(d1.scale(inner)), color, alpha, material, light);
		}
	}

	private static void drawCrossedLine(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 start, Vec3 end, float width, int color,
			int alpha, int material, int light) {
		Vec3 axis = safeDirection(end.subtract(start));
		Vec3 side = basisRight(axis).scale(width);
		Vec3 second = safeDirection(axis.cross(side)).scale(width);
		drawLineRibbon(out, pose, budget, start, end, side, color, alpha,
				material, light);
		drawLineRibbon(out, pose, budget, start, end, second, color, alpha,
				material, light);
	}

	private static void drawLineRibbon(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 start, Vec3 end, Vec3 side, int color,
			int alpha, int material, int light) {
		drawQuad(out, pose, budget, start.subtract(side), start.add(side),
				end.add(side), end.subtract(side), color, alpha, material, light);
	}

	private static void drawBillboard(RenderFrame frame, VertexConsumer out,
			FrameBudget budget, Vec3 center, float size, int color, int alpha,
			int material, int light) {
		frame.stack.pushPose();
		frame.stack.translate(center.x, center.y, center.z);
		frame.stack.mulPose(frame.cameraOrientation);
		drawQuad(out, frame.stack.last(), budget,
				new Vec3(-size, -size, 0.0D), new Vec3(size, -size, 0.0D),
				new Vec3(size, size, 0.0D), new Vec3(-size, size, 0.0D),
				color, alpha, material, light);
		frame.stack.popPose();
	}

	private static void drawQuad(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color,
			int alpha, int material, int light) {
		if (!budget.take(4))
			return;
		Vec3 normal = b.subtract(a).cross(c.subtract(a));
		if (normal.lengthSqr() < 1.0E-8D)
			normal = UP;
		else
			normal = normal.normalize();
		vertex(out, pose, a, color, alpha, material + 0.01F, 0.01F,
				light, normal);
		vertex(out, pose, b, color, alpha, material + 0.99F, 0.01F,
				light, normal);
		vertex(out, pose, c, color, alpha, material + 0.99F, 0.99F,
				light, normal);
		vertex(out, pose, d, color, alpha, material + 0.01F, 0.99F,
				light, normal);
	}

	/** Quad with an explicit V span, so geometry can stretch the flow texture. */
	private static void drawQuadUv(VertexConsumer out, PoseStack.Pose pose,
			FrameBudget budget, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color,
			int alpha, int material, int light, float v0, float v1) {
		if (!budget.take(4))
			return;
		Vec3 normal = b.subtract(a).cross(c.subtract(a));
		if (normal.lengthSqr() < 1.0E-8D)
			normal = UP;
		else
			normal = normal.normalize();
		float low = Mth.clamp(v0, 0.01F, 0.99F);
		float high = Mth.clamp(v1, 0.01F, 0.99F);
		vertex(out, pose, a, color, alpha, material + 0.01F, low, light, normal);
		vertex(out, pose, b, color, alpha, material + 0.99F, low, light, normal);
		vertex(out, pose, c, color, alpha, material + 0.99F, high, light, normal);
		vertex(out, pose, d, color, alpha, material + 0.01F, high, light, normal);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose,
			Vec3 position, int color, int alpha, float u, float v, int light,
			Vec3 normal) {
		out.addVertex(pose, (float) position.x, (float) position.y,
				(float) position.z)
				.setColor((color >> 16) & 255, (color >> 8) & 255, color & 255,
						Mth.clamp(alpha, 0, 255))
				.setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(pose, (float) normal.x, (float) normal.y,
						(float) normal.z);
	}

	private static Vec3 basisRight(Vec3 direction) {
		Vec3 right = direction.cross(UP);
		if (right.lengthSqr() < 1.0E-6D)
			right = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
		return safeDirection(right);
	}

	private static Vec3 safeDirection(Vec3 value) {
		return value.lengthSqr() < 1.0E-8D
				? new Vec3(0.0D, 0.0D, 1.0D) : value.normalize();
	}

	private static Vec3 lookDirection(float yaw, float pitch) {
		return Vec3.directionFromRotation(pitch, yaw);
	}

	private static long gameTime() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level == null ? 0L : minecraft.level.getGameTime();
	}

	private static int passLight(Pass pass, int light) {
		return pass == Pass.EMISSIVE ? LightTexture.FULL_BRIGHT : light;
	}

	private static float fadeInOut(float elapsed, int duration,
			float inTicks, float outTicks) {
		return Mth.clamp(elapsed / Math.max(1.0F, inTicks), 0.0F, 1.0F)
				* Mth.clamp((duration - elapsed) / Math.max(1.0F, outTicks),
						0.0F, 1.0F);
	}

	private static float smoothOut(float value) {
		float clamped = Mth.clamp(value, 0.0F, 1.0F);
		return 1.0F - (1.0F - clamped) * (1.0F - clamped);
	}

	private static int alpha(float value) {
		return Mth.clamp(Math.round(value), 0, 255);
	}

	public enum Pass {
		SURFACE,
		EMISSIVE
	}

	private enum Quality {
		FULL(104.0D, 72, 30_000, 24, 12, 16, 12, 12, 10, 16),
		REDUCED(72.0D, 48, 13_000, 16, 8, 11, 8, 8, 7, 10),
		MINIMAL(44.0D, 28, 4_200, 10, 6, 7, 5, 5, 4, 6);

		private final double renderDistance;
		private final int maxVisible;
		private final int maxVertices;
		private final int ringSegments;
		private final int beamSides;
		private final int extinctionSides;
		private final int curveSegments;
		private final int motes;
		private final int fractures;
		private final int aftermathScars;

		Quality(double renderDistance, int maxVisible, int maxVertices,
				int ringSegments, int beamSides, int extinctionSides,
				int curveSegments, int motes, int fractures,
				int aftermathScars) {
			this.renderDistance = renderDistance;
			this.maxVisible = maxVisible;
			this.maxVertices = maxVertices;
			this.ringSegments = ringSegments;
			this.beamSides = beamSides;
			this.extinctionSides = extinctionSides;
			this.curveSegments = curveSegments;
			this.motes = motes;
			this.fractures = fractures;
			this.aftermathScars = aftermathScars;
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

	public static final class FrameBudget {
		private int remaining;

		public FrameBudget(int maximum) {
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

	private record VisibleEvent(ActiveEvent timeline, Vec3 anchor, Vec3 focus,
			float yaw, float pitch, float elapsed, double distanceSqr, int light,
			boolean firstPersonCaster) {
	}

	private record Anchors(Vec3 anchor, Vec3 focus, float yaw, float pitch,
			boolean firstPersonCaster) {
	}
}
