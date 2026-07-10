package net.solocraft.client.gui.system;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side queue of active System notifications (level up, quests, etc.).
 *
 * <p>Each notification carries an optional big <b>title</b> and an optional
 * small <b>undertext</b> (either may be null) which selects the layout:
 * title-only, title+undertext, or undertext-only. Timing is wall-clock based
 * (like {@code MageQTEState}) with an APPEARING → HOLD → DISAPPEARING phase.
 * {@link SystemNotificationOverlay} reads {@link #active()} each frame.
 */
public final class SystemNotificationManager {
	public static final SystemNotificationManager INSTANCE = new SystemNotificationManager();

	public static final long APPEAR_MS = 220L;
	public static final long DISAPPEAR_MS = 200L;
	private static final int MAX = 4;

	private final List<Notification> notifications = new ArrayList<>();

	private SystemNotificationManager() {
	}

	/** Push a notification. {@code title}/{@code undertext} may be null (selects the layout type). */
	public void push(int accentColor, int durationTicks, Component title, Component undertext) {
		push(accentColor, durationTicks, title, undertext, false);
	}

	/** Push a notification with the negative System sound. */
	public void push(int accentColor, int durationTicks, Component title, Component undertext, boolean negativeSound) {
		if (title == null && undertext == null)
			return;
		long holdMs = Math.max(0L, durationTicks) * 50L; // 20 tps
		notifications.add(new Notification(accentColor, title, undertext, System.currentTimeMillis(), holdMs));
		if (negativeSound)
			SystemGuiSounds.negativeNotification();
		else
			SystemGuiSounds.notification();
		while (notifications.size() > MAX)
			notifications.remove(0);
	}

	public void title(int accentColor, int durationTicks, Component title) {
		push(accentColor, durationTicks, title, null);
	}

	public void titleUnder(int accentColor, int durationTicks, Component title, Component undertext) {
		push(accentColor, durationTicks, title, undertext);
	}

	public void under(int accentColor, int durationTicks, Component undertext) {
		push(accentColor, durationTicks, null, undertext);
	}

	/** Prunes finished notifications and returns the current list (oldest first, newest last). */
	public List<Notification> active() {
		long now = System.currentTimeMillis();
		notifications.removeIf(n -> n.expired(now));
		return notifications;
	}

	/** One on-screen notification with its own timing. */
	public static final class Notification {
		public final int accent;
		public final Component title; // nullable
		public final Component under; // nullable
		public final long start;
		public final long holdMs;

		Notification(int accent, Component title, Component under, long start, long holdMs) {
			this.accent = accent;
			this.title = title;
			this.under = under;
			this.start = start;
			this.holdMs = holdMs;
		}

		public long lifeMs() {
			return APPEAR_MS + holdMs + DISAPPEAR_MS;
		}

		public boolean expired(long now) {
			return now - start >= lifeMs();
		}

		/** 0..1 horizontal-reveal factor: eases in during APPEARING, 1 during HOLD, eases out during DISAPPEARING. */
		public float reveal(long now) {
			long e = now - start;
			if (e < 0)
				return 0f;
			if (e < APPEAR_MS)
				return ease((float) e / APPEAR_MS);
			if (e < APPEAR_MS + holdMs)
				return 1f;
			if (e < lifeMs())
				return ease(1f - (float) (e - APPEAR_MS - holdMs) / DISAPPEAR_MS);
			return 0f;
		}

		/** True while appearing or disappearing (used to intensify the glitch). */
		public boolean transitioning(long now) {
			long e = now - start;
			return e < APPEAR_MS || e >= APPEAR_MS + holdMs;
		}

		private static float ease(float t) {
			t = t < 0f ? 0f : (t > 1f ? 1f : t);
			return t * t * (3f - 2f * t);
		}
	}
}
