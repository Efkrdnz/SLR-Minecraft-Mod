package net.solocraft.client.renderer;

import net.solocraft.network.SungIlHwanVfxEventMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client-thread-only, bounded timeline store for Sung Il-Hwan presentation.
 *
 * <p>Entries contain no GPU objects and survive shader reloads. Authoritative
 * gameplay state is never inferred from this cache.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SungIlHwanVfxClientState {
	/**
	 * Enough room for the 96-target server safety ceiling, its authoritative
	 * sphere snapshot, and a small bounded set of concurrent stage/feedback
	 * timelines. Rendering applies a separate per-frame vertex budget.
	 */
	public static final int MAX_EXECUTION_TARGET_MARKS = 96;
	public static final int MAX_EVENTS = 160;
	private static final List<ActiveEvent> EVENTS =
			new ArrayList<>(MAX_EVENTS);
	private static long nextSequence;

	private SungIlHwanVfxClientState() {
	}

	public static void enqueue(SungIlHwanVfxEventMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> enqueue(message));
			return;
		}
		if (minecraft.level == null || minecraft.player == null
				|| message == null
				|| !SungIlHwanVfxEventMessage.isKnownEventType(
						message.eventType))
			return;
		if (message.privateToCaster()
				&& message.casterEntityId != minecraft.player.getId())
			return;

		long now = minecraft.level.getGameTime();
		prune(now);
		long elapsed = now - message.serverStartTick;
		if (elapsed >= message.duration
				|| elapsed < -SungIlHwanVfxEventMessage.MAX_FUTURE_START_TICKS)
			return;
		if (isDuplicate(message))
			return;

		applyTransition(message);
		if (message.eventType == SungIlHwanVfxEventMessage.STAGE_END) {
			addBounded(new ActiveEvent(message, nextSequence++));
			return;
		}
		if (message.eventType
				== SungIlHwanVfxEventMessage.EXECUTION_CANCEL) {
			addBounded(new ActiveEvent(message, nextSequence++));
			return;
		}
		addBounded(new ActiveEvent(message, nextSequence++));
	}

	public static List<ActiveEvent> snapshot(long now) {
		prune(now);
		return List.copyOf(EVENTS);
	}

	public static OverlayState overlay(float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return OverlayState.EMPTY;
		long now = minecraft.level.getGameTime();
		prune(now);

		ActiveEvent targeting = newestExecutionSphereForLocal();
		ActiveEvent exhaustion = newestForLocal(
				SungIlHwanVfxEventMessage.EXHAUSTION);
		ActiveEvent risk = newestForLocal(
				SungIlHwanVfxEventMessage.RISK_FEEDBACK);
		return new OverlayState(frame(targeting, now, partialTick),
				frame(exhaustion, now, partialTick),
				frame(risk, now, partialTick));
	}

	public static void clear() {
		EVENTS.clear();
		nextSequence = 0L;
	}

	public static void onResourceReload() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(SungIlHwanVfxClientState::onResourceReload);
			return;
		}
		if (minecraft.level == null)
			clear();
		else
			prune(minecraft.level.getGameTime());
	}

	private static void applyTransition(
			SungIlHwanVfxEventMessage message) {
		if (message.eventType == SungIlHwanVfxEventMessage.STAGE_ONE
				|| message.eventType == SungIlHwanVfxEventMessage.STAGE_TWO
				|| message.eventType == SungIlHwanVfxEventMessage.STAGE_END) {
			removeForCaster(message.casterEntityId,
					SungIlHwanVfxEventMessage.STAGE_ONE,
					SungIlHwanVfxEventMessage.STAGE_TWO,
					SungIlHwanVfxEventMessage.STAGE_END);
		}
		if (message.eventType
				== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET) {
			if (message.variant < 2) {
				// The server sends the sphere first, followed on the same ordered
				// channel by a complete authoritative mark snapshot. Clearing the
				// previous snapshot here prevents dead or departed targets from
				// lingering without accumulating duplicate timelines.
				EVENTS.removeIf(event ->
						event.message.casterEntityId == message.casterEntityId
								&& event.message.eventType
										== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET);
			} else {
				// Each marked target owns one bounded timeline. A refresh replaces
				// only that target rather than every mark in the sphere.
				EVENTS.removeIf(event ->
						event.message.casterEntityId == message.casterEntityId
								&& event.message.eventType
										== SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET
								&& event.message.variant >= 2
								&& event.message.targetEntityId
										== message.targetEntityId);
			}
		}
		if (message.eventType
				== SungIlHwanVfxEventMessage.EXECUTION_PUBLIC_CHARGE) {
			removeForCaster(message.casterEntityId,
					SungIlHwanVfxEventMessage.EXECUTION_PUBLIC_CHARGE);
		}
		if (message.eventType == SungIlHwanVfxEventMessage.EXECUTION_RELEASE
				|| message.eventType
						== SungIlHwanVfxEventMessage.EXECUTION_CANCEL) {
			removeForCaster(message.casterEntityId,
					SungIlHwanVfxEventMessage.EXECUTION_PUBLIC_CHARGE,
					SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET);
		}
		if (message.eventType == SungIlHwanVfxEventMessage.EXHAUSTION)
			removeForCaster(message.casterEntityId,
					SungIlHwanVfxEventMessage.EXHAUSTION);
		if (message.eventType == SungIlHwanVfxEventMessage.RISK_FEEDBACK)
			removeForCaster(message.casterEntityId,
					SungIlHwanVfxEventMessage.RISK_FEEDBACK);
	}

	private static void removeForCaster(int casterId, byte... types) {
		EVENTS.removeIf(event -> {
			if (event.message.casterEntityId != casterId)
				return false;
			for (byte type : types)
				if (event.message.eventType == type)
					return true;
			return false;
		});
	}

	private static boolean isDuplicate(
			SungIlHwanVfxEventMessage message) {
		return EVENTS.stream().anyMatch(event ->
				event.message.eventType == message.eventType
						&& event.message.casterEntityId
								== message.casterEntityId
						&& event.message.targetEntityId
								== message.targetEntityId
						&& event.message.serverStartTick
								== message.serverStartTick
						&& event.message.seed == message.seed
						&& event.message.variant == message.variant);
	}

	private static void addBounded(ActiveEvent incoming) {
		if (EVENTS.size() >= MAX_EVENTS) {
			ActiveEvent candidate = EVENTS.stream()
					.filter(event -> !event.essential())
					.min(Comparator.comparingLong(ActiveEvent::sequence))
					.orElse(EVENTS.get(0));
			EVENTS.remove(candidate);
		}
		EVENTS.add(incoming);
	}

	private static void prune(long now) {
		Minecraft minecraft = Minecraft.getInstance();
		int localId = minecraft.player == null ? -1
				: minecraft.player.getId();
		EVENTS.removeIf(event -> {
			long elapsed = now - event.message.serverStartTick;
			if (elapsed >= event.message.duration
					|| elapsed < -SungIlHwanVfxEventMessage.MAX_FUTURE_START_TICKS)
				return true;
			return event.message.privateToCaster()
					&& event.message.casterEntityId != localId;
		});
		while (EVENTS.size() > MAX_EVENTS)
			EVENTS.remove(0);
	}

	private static ActiveEvent newestForLocal(byte type) {
		Minecraft minecraft = Minecraft.getInstance();
		int localId = minecraft.player == null ? -1
				: minecraft.player.getId();
		ActiveEvent newest = null;
		for (ActiveEvent event : EVENTS) {
			if (event.message.eventType != type
					|| event.message.casterEntityId != localId)
				continue;
			if (newest == null || event.sequence > newest.sequence)
				newest = event;
		}
		return newest;
	}

	private static ActiveEvent newestExecutionSphereForLocal() {
		Minecraft minecraft = Minecraft.getInstance();
		int localId = minecraft.player == null ? -1
				: minecraft.player.getId();
		ActiveEvent newest = null;
		for (ActiveEvent event : EVENTS) {
			if (event.message.eventType
						!= SungIlHwanVfxEventMessage.EXECUTION_PRIVATE_TARGET
					|| event.message.casterEntityId != localId
					|| event.message.variant >= 2)
				continue;
			if (newest == null || event.sequence > newest.sequence)
				newest = event;
		}
		return newest;
	}

	private static OverlayFrame frame(ActiveEvent event, long now,
			float partialTick) {
		if (event == null)
			return OverlayFrame.EMPTY;
		float elapsed = event.elapsed(now, partialTick);
		if (elapsed < 0.0F || elapsed >= event.message.duration)
			return OverlayFrame.EMPTY;
		float progress = Mth.clamp(elapsed
				/ Math.max(1.0F, event.message.duration), 0.0F, 1.0F);
		return new OverlayFrame(true, progress,
				event.message.intensity / 255.0F,
				event.message.variant, event.message.seed,
				event.message.focus(), event.message.radius);
	}

	public static final class ActiveEvent {
		private final SungIlHwanVfxEventMessage message;
		private final long sequence;

		private ActiveEvent(SungIlHwanVfxEventMessage message,
				long sequence) {
			this.message = message;
			this.sequence = sequence;
		}

		public SungIlHwanVfxEventMessage message() {
			return message;
		}

		public long sequence() {
			return sequence;
		}

		public float elapsed(long now, float partialTick) {
			return now - message.serverStartTick + partialTick;
		}

		public float progress(long now, float partialTick) {
			return Mth.clamp(elapsed(now, partialTick)
					/ Math.max(1.0F, message.duration), 0.0F, 1.0F);
		}

		public boolean essential() {
			return message.hasFlag(
					SungIlHwanVfxEventMessage.FLAG_ESSENTIAL);
		}
	}

	public record OverlayFrame(boolean active, float progress,
			float intensity, int variant, int seed, Vec3 focus,
			float radius) {
		private static final OverlayFrame EMPTY = new OverlayFrame(false,
				0.0F, 0.0F, 0, 0, Vec3.ZERO, 1.0F);
	}

	public record OverlayState(OverlayFrame targeting,
			OverlayFrame exhaustion, OverlayFrame risk) {
		private static final OverlayState EMPTY = new OverlayState(
				OverlayFrame.EMPTY, OverlayFrame.EMPTY, OverlayFrame.EMPTY);

		public boolean active() {
			return targeting.active || exhaustion.active || risk.active;
		}
	}
}
