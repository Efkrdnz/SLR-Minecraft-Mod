package net.solocraft.client.renderer;

import net.solocraft.network.AntaresVfxEventMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded, client-thread-only presentation cache for Antares abilities.
 * Gameplay never reads this class; packets contain only visual facts and the
 * local Ruin display snapshot.
 */
@OnlyIn(Dist.CLIENT)
public final class AntaresVfxClientState {
	public static final int MAX_EVENTS = 128;
	private static final List<ActiveEvent> EVENTS =
			new ArrayList<>(MAX_EVENTS);
	private static long nextSequence;
	private static int ruinCharges;
	private static int ruinMaximum = 3;
	private static boolean manifested;
	private static long lastRuinChangeTick = Long.MIN_VALUE;

	private AntaresVfxClientState() {
	}

	public static void enqueue(AntaresVfxEventMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(() -> enqueue(message));
			return;
		}
		if (minecraft.level == null || minecraft.player == null
				|| message == null
				|| !AntaresVfxEventMessage.isKnownEventType(message.eventType))
			return;
		if (message.privateToCaster()
				&& message.casterEntityId != minecraft.player.getId())
			return;

		long now = minecraft.level.getGameTime();
		prune(now);
		if (message.eventType == AntaresVfxEventMessage.RUIN_SYNC) {
			int nextMaximum = Mth.clamp(Mth.floor(message.radius + 0.5F), 1, 31);
			int nextCharges = Mth.clamp(message.variant, 0, nextMaximum);
			if (nextCharges != ruinCharges || nextMaximum != ruinMaximum)
				lastRuinChangeTick = now;
			ruinMaximum = nextMaximum;
			ruinCharges = nextCharges;
			manifested = message.hasFlag(AntaresVfxEventMessage.FLAG_MANIFESTED);
			return;
		}

		long elapsed = now - message.serverStartTick;
		if (elapsed >= message.duration
				|| elapsed < -AntaresVfxEventMessage.MAX_FUTURE_START_TICKS
				|| duplicate(message))
			return;

		applyTransition(message);
		if (message.casterEntityId == minecraft.player.getId()) {
			if (message.eventType == AntaresVfxEventMessage.MANIFESTATION_START)
				manifested = true;
			else if (message.eventType == AntaresVfxEventMessage.MANIFESTATION_END)
				manifested = false;
		}
		addBounded(new ActiveEvent(message, nextSequence++));
	}

	public static List<ActiveEvent> snapshot(long now) {
		prune(now);
		return List.copyOf(EVENTS);
	}

	public static HudState hudState(float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return HudState.EMPTY;
		long now = minecraft.level.getGameTime();
		prune(now);
		float pulse = lastRuinChangeTick == Long.MIN_VALUE ? 0.0F
				: 1.0F - Mth.clamp((now - lastRuinChangeTick + partialTick)
						/ 12.0F, 0.0F, 1.0F);
		return new HudState(ruinCharges, ruinMaximum, manifested, pulse,
				newestLocalProgress(AntaresVfxEventMessage.EXTINCTION_CHARGE,
						now, partialTick));
	}

	public static void clear() {
		EVENTS.clear();
		nextSequence = 0L;
		ruinCharges = 0;
		ruinMaximum = 3;
		manifested = false;
		lastRuinChangeTick = Long.MIN_VALUE;
	}

	public static void onResourceReload() {
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.isSameThread()) {
			minecraft.execute(AntaresVfxClientState::onResourceReload);
			return;
		}
		if (minecraft.level == null)
			clear();
		else
			prune(minecraft.level.getGameTime());
	}

	private static void applyTransition(AntaresVfxEventMessage message) {
		byte type = message.eventType;
		if (type == AntaresVfxEventMessage.BREATH_CHARGE)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.BREATH_CHARGE,
					AntaresVfxEventMessage.BREATH_STREAM,
					AntaresVfxEventMessage.BREATH_END);
		else if (type == AntaresVfxEventMessage.BREATH_STREAM) {
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.BREATH_CHARGE,
					AntaresVfxEventMessage.BREATH_STREAM);
		} else if (type == AntaresVfxEventMessage.BREATH_END)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.BREATH_CHARGE,
					AntaresVfxEventMessage.BREATH_STREAM,
					AntaresVfxEventMessage.BREATH_END);
		else if (type == AntaresVfxEventMessage.DESCENT_LAUNCH)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.DESCENT_LAUNCH);
		else if (type == AntaresVfxEventMessage.DESCENT_IMPACT)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.DESCENT_LAUNCH);
		else if (type == AntaresVfxEventMessage.ROAR_CHARGE)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.ROAR_CHARGE);
		else if (type == AntaresVfxEventMessage.ROAR_RELEASE)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.ROAR_CHARGE);
		else if (type == AntaresVfxEventMessage.OVERAWED_MARK
				&& message.targetEntityId >= 0)
			EVENTS.removeIf(event -> event.message.eventType
					== AntaresVfxEventMessage.OVERAWED_MARK
					&& event.message.targetEntityId == message.targetEntityId);
		else if (type == AntaresVfxEventMessage.EXTINCTION_CHARGE)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.EXTINCTION_CHARGE,
					AntaresVfxEventMessage.EXTINCTION_PULSE,
					AntaresVfxEventMessage.EXTINCTION_AFTERMATH);
		else if (type == AntaresVfxEventMessage.EXTINCTION_AFTERMATH)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.EXTINCTION_CHARGE);
		else if (type == AntaresVfxEventMessage.MANIFESTATION_START
				|| type == AntaresVfxEventMessage.MANIFESTATION_END)
			removeForCaster(message.casterEntityId,
					AntaresVfxEventMessage.MANIFESTATION_START,
					AntaresVfxEventMessage.MANIFESTATION_END);
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

	private static boolean duplicate(AntaresVfxEventMessage message) {
		return EVENTS.stream().anyMatch(event ->
				event.message.eventType == message.eventType
						&& event.message.casterEntityId == message.casterEntityId
						&& event.message.targetEntityId == message.targetEntityId
						&& event.message.serverStartTick == message.serverStartTick
						&& event.message.seed == message.seed
						&& event.message.variant == message.variant);
	}

	private static void addBounded(ActiveEvent incoming) {
		if (EVENTS.size() >= MAX_EVENTS) {
			ActiveEvent oldest = EVENTS.stream()
					.filter(event -> !event.essential())
					.min(Comparator.comparingLong(ActiveEvent::sequence))
					.orElse(EVENTS.get(0));
			EVENTS.remove(oldest);
		}
		EVENTS.add(incoming);
	}

	private static void prune(long now) {
		Minecraft minecraft = Minecraft.getInstance();
		int localId = minecraft.player == null ? -1 : minecraft.player.getId();
		EVENTS.removeIf(event -> {
			long elapsed = now - event.message.serverStartTick;
			return elapsed >= event.message.duration
					|| elapsed < -AntaresVfxEventMessage.MAX_FUTURE_START_TICKS
					|| (event.message.privateToCaster()
							&& event.message.casterEntityId != localId);
		});
		while (EVENTS.size() > MAX_EVENTS)
			EVENTS.remove(0);
	}

	private static float newestLocalProgress(byte type, long now,
			float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		int localId = minecraft.player == null ? -1 : minecraft.player.getId();
		ActiveEvent newest = null;
		for (ActiveEvent event : EVENTS) {
			if (event.message.eventType == type
					&& event.message.casterEntityId == localId
					&& (newest == null || event.sequence > newest.sequence))
				newest = event;
		}
		return newest == null ? -1.0F : newest.progress(now, partialTick);
	}

	public static final class ActiveEvent {
		private final AntaresVfxEventMessage message;
		private final long sequence;

		private ActiveEvent(AntaresVfxEventMessage message, long sequence) {
			this.message = message;
			this.sequence = sequence;
		}

		public AntaresVfxEventMessage message() {
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
			return message.hasFlag(AntaresVfxEventMessage.FLAG_ESSENTIAL);
		}
	}

	public record HudState(int charges, int maximum, boolean manifested,
			float pulse, float extinctionChargeProgress) {
		private static final HudState EMPTY = new HudState(0, 3, false,
				0.0F, -1.0F);
	}
}
