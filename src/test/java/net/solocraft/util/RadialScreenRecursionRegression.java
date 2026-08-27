package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps a radial screen from re-opening itself until the stack runs out.
 *
 * <p>{@code Minecraft.setScreen} synthesises key state changes as bookkeeping:
 * {@code KeyMapping.releaseAll()} when a screen opens, and {@code grabMouse()}
 * → {@code KeyMapping.setAll()} when one closes. A mixin on {@code KeyMapping}
 * cannot tell those from a player pressing the key, so routing them re-entered
 * {@code setScreen}:
 *
 * <pre>
 * begin() -> setScreen(wheel) -> releaseAll() -> releaseHotbarSkill()
 *   -> releaseAndSend() -> clear() -> setScreen(null) -> grabMouse()
 *     -> setAll() -> pressHotbarSkill() -> begin() -> ...
 * </pre>
 *
 * <p>That is a StackOverflowError that took the client down the first time a
 * player used the Curse Weave wheel. The Frost Architecture radial is built the
 * same way and would have done the same thing.
 *
 * <p>Checked as source text because reproducing it needs a running client, and
 * the failure is a shape -- an unguarded call -- rather than a value.
 */
public final class RadialScreenRecursionRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");

	/** Every client state that opens a radial from a key handler. */
	private static final String[] RADIAL_STATES = {
			"CurseWheelClientState", "FrostArchitectureClientState",
	};

	private RadialScreenRecursionRegression() {
	}

	public static void main(String[] args) throws IOException {
		for (String state : RADIAL_STATES) {
			String source = read("client", "gui", state + ".java");
			expect(source.contains("RadialScreenTransition.run("),
					state + " must change screens through RadialScreenTransition.run, "
							+ "or the key events setScreen synthesises come straight "
							+ "back in and re-open it");
			expect(!containsBareSetScreen(source),
					state + " still calls minecraft.setScreen directly; every radial "
							+ "screen change has to go through the guard");
		}

		String mixin = read("mixins", "DisableHotbarKeymappingMixin.java");
		expect(mixin.contains("RadialScreenTransition.isTransitioning()"),
				"The hotbar key router must ignore key events while a radial is "
						+ "opening or closing; those are Minecraft's bookkeeping, not "
						+ "the player");

		int guard = mixin.indexOf("RadialScreenTransition.isTransitioning()");
		int route = mixin.indexOf("solocraft$slotDown = isDown");
		expect(guard >= 0 && route >= 0 && guard < route,
				"The transition check must come before the router records the key "
						+ "state, or a synthetic event still counts as a real one");

		String guardSource = read("client", "gui", "RadialScreenTransition.java");
		expect(guardSource.contains("if (inTransition)")
						&& guardSource.contains("finally"),
				"The guard must refuse to nest and must clear itself even when the "
						+ "screen change throws");

		System.out.println("Radial screen recursion regression passed ("
				+ RADIAL_STATES.length + " radials checked).");
	}

	/** A setScreen call that is not wrapped by the guard on the same line. */
	private static boolean containsBareSetScreen(String source) {
		for (String line : source.split("\n")) {
			if (line.contains("minecraft.setScreen(")
					&& !line.contains("RadialScreenTransition.run("))
				return true;
		}
		return false;
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
