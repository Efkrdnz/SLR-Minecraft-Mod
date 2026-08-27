package net.solocraft.client.gui;

/**
 * Marks the window in which this mod is opening or closing a radial screen, so
 * the key router can tell the game's own bookkeeping from a real key press.
 *
 * <p>{@code Minecraft.setScreen} synthesises key state changes as a side effect:
 * it calls {@code KeyMapping.releaseAll()} when a screen opens, and
 * {@code grabMouse()} → {@code KeyMapping.setAll()} when one closes. Those are
 * housekeeping, not input, but a mixin on {@code KeyMapping} cannot tell the
 * difference -- and routing them re-entered {@code setScreen}:
 *
 * <pre>
 * begin() -> setScreen(wheel) -> releaseAll() -> releaseHotbarSkill()
 *   -> releaseAndSend() -> clear() -> setScreen(null) -> grabMouse()
 *     -> setAll() -> pressHotbarSkill() -> begin()   ... and round again
 * </pre>
 *
 * <p>That is a StackOverflowError, and it took the client down the moment a
 * player used the Curse Weave wheel.
 *
 * <p>Client-side and single-threaded: every one of these calls happens on the
 * render thread inside {@code setScreen}, so a plain flag is the whole
 * mechanism it needs.
 */
public final class RadialScreenTransition {
	private static boolean inTransition;

	private RadialScreenTransition() {
	}

	/** True while a radial is opening or closing its screen. */
	public static boolean isTransitioning() {
		return inTransition;
	}

	/**
	 * Runs a screen change, ignoring any attempt to start another while it is in
	 * progress.
	 *
	 * <p>The re-entrancy check is the point: a nested transition is always the
	 * game's own synthetic key events arriving back through the router, never a
	 * player pressing something in the microseconds inside a screen swap.
	 */
	public static void run(Runnable transition) {
		if (inTransition)
			return;
		inTransition = true;
		try {
			transition.run();
		} finally {
			inTransition = false;
		}
	}
}
