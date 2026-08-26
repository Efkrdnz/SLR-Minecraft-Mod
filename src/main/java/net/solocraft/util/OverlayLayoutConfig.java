package net.solocraft.util;

import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Client-side, per-installation placement for the movable System overlays.
 *
 * <p>Each element stores an offset from its original anchor plus a scale, so a
 * default install renders byte-for-byte where it always did and an untouched
 * element keeps following any future change to its base position.</p>
 */
public final class OverlayLayoutConfig {
	/** Class passives: Tempo, Veil, Combat Drive, Feral, Poise and so on. */
	public static final int PASSIVES = 0;
	/** Health, mana and the hunter identity plate. */
	public static final int VITALS = 1;
	/** Daily, urgent and story quest tracking. */
	public static final int QUESTS = 2;
	public static final int ELEMENT_COUNT = 3;

	public static final float MIN_SCALE = 0.5F;
	public static final float MAX_SCALE = 2.0F;
	public static final float DEFAULT_SCALE = 1.0F;
	/** Offsets are clamped so an element can never be dragged off-screen. */
	public static final int MAX_OFFSET = 4000;

	private static final String[] KEYS = { "passives", "vitals", "quests" };
	private static final String[] LABELS = {
			"Class Passives", "Health & Mana", "Quest Tracker" };
	/**
	 * Shipped top-left anchor of each element. Scaling pivots here so an
	 * element grows away from its corner instead of drifting, and the layout
	 * editor draws its grab boxes from the same numbers.
	 */
	private static final int[] ANCHOR_X = { 8, 8, 8 };
	private static final int[] ANCHOR_Y = { 76, 8, 134 };

	public static int anchorX(int element) {
		return ANCHOR_X[bounded(element)];
	}

	public static int anchorY(int element) {
		return ANCHOR_Y[bounded(element)];
	}

	private static final int[] OFFSET_X = new int[ELEMENT_COUNT];
	private static final int[] OFFSET_Y = new int[ELEMENT_COUNT];
	private static final float[] SCALE = new float[ELEMENT_COUNT];

	private static boolean loaded;

	static {
		resetAllInternal();
	}

	private OverlayLayoutConfig() {
	}

	public static String label(int element) {
		return LABELS[bounded(element)];
	}

	public static synchronized int getOffsetX(int element) {
		ensureLoaded();
		return OFFSET_X[bounded(element)];
	}

	public static synchronized int getOffsetY(int element) {
		ensureLoaded();
		return OFFSET_Y[bounded(element)];
	}

	public static synchronized float getScale(int element) {
		ensureLoaded();
		return SCALE[bounded(element)];
	}

	public static synchronized void setOffset(int element, int x, int y) {
		ensureLoaded();
		int index = bounded(element);
		OFFSET_X[index] = clampOffset(x);
		OFFSET_Y[index] = clampOffset(y);
		save();
	}

	public static synchronized void setScale(int element, float scale) {
		ensureLoaded();
		SCALE[bounded(element)] = clampScale(scale);
		save();
	}

	/** True when this element still sits exactly where the mod shipped it. */
	public static synchronized boolean isDefault(int element) {
		ensureLoaded();
		int index = bounded(element);
		return OFFSET_X[index] == 0 && OFFSET_Y[index] == 0
				&& Math.abs(SCALE[index] - DEFAULT_SCALE) < 0.001F;
	}

	public static synchronized void reset(int element) {
		ensureLoaded();
		int index = bounded(element);
		OFFSET_X[index] = 0;
		OFFSET_Y[index] = 0;
		SCALE[index] = DEFAULT_SCALE;
		save();
	}

	public static synchronized void resetAll() {
		ensureLoaded();
		resetAllInternal();
		save();
	}

	private static void resetAllInternal() {
		for (int index = 0; index < ELEMENT_COUNT; index++) {
			OFFSET_X[index] = 0;
			OFFSET_Y[index] = 0;
			SCALE[index] = DEFAULT_SCALE;
		}
	}

	private static int bounded(int element) {
		return Math.max(0, Math.min(ELEMENT_COUNT - 1, element));
	}

	private static int clampOffset(int value) {
		return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
	}

	private static float clampScale(float value) {
		if (!Float.isFinite(value))
			return DEFAULT_SCALE;
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
	}

	private static Path configPath() {
		return FMLPaths.CONFIGDIR.get().resolve("sololeveling-overlay-layout.properties");
	}

	private static void ensureLoaded() {
		if (loaded)
			return;
		loaded = true;
		Path path = configPath();
		if (!Files.isRegularFile(path))
			return;
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(path)) {
			properties.load(input);
		} catch (Exception ignored) {
			// A damaged layout file must never stop the HUD from rendering.
			return;
		}
		for (int index = 0; index < ELEMENT_COUNT; index++) {
			OFFSET_X[index] = clampOffset(readInt(properties, KEYS[index] + "X", 0));
			OFFSET_Y[index] = clampOffset(readInt(properties, KEYS[index] + "Y", 0));
			SCALE[index] = clampScale(readFloat(properties, KEYS[index] + "Scale",
					DEFAULT_SCALE));
		}
	}

	private static void save() {
		Properties properties = new Properties();
		for (int index = 0; index < ELEMENT_COUNT; index++) {
			properties.setProperty(KEYS[index] + "X", Integer.toString(OFFSET_X[index]));
			properties.setProperty(KEYS[index] + "Y", Integer.toString(OFFSET_Y[index]));
			properties.setProperty(KEYS[index] + "Scale", Float.toString(SCALE[index]));
		}
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (OutputStream output = Files.newOutputStream(path)) {
				properties.store(output, "SoloCraft Reawakening overlay layout");
			}
		} catch (Exception ignored) {
			// Persisting is best effort; the session still honours the change.
		}
	}

	private static int readInt(Properties properties, String key, int fallback) {
		try {
			return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static float readFloat(Properties properties, String key, float fallback) {
		try {
			return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}
}
