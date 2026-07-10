package net.solocraft.util;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Tiny client-side, global (per-installation) preferences persisted to
 * {@code config/sololeveling-client.properties}. Purely visual settings that
 * don't belong in the per-player capability — currently the System notification
 * popup size multiplier.
 */
public final class SystemClientConfig {
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 2.0f;
	public static final float DEFAULT_SCALE = 1.0f;

	private static final String KEY_NOTIF_SCALE = "notificationScale";
	private static final String KEY_DAMAGE_NUMBERS = "damageNumbersEnabled";
	private static final String KEY_LEGACY_OVERLAY = "legacyOverlayEnabled";

	private static float notificationScale = DEFAULT_SCALE;
	private static boolean damageNumbersEnabled = true;
	private static boolean legacyOverlayEnabled = false;
	private static boolean loaded = false;

	private SystemClientConfig() {
	}

	public static synchronized float getNotificationScale() {
		ensureLoaded();
		return notificationScale;
	}

	public static synchronized void setNotificationScale(float value) {
		ensureLoaded();
		notificationScale = clamp(value);
		save();
	}

	public static synchronized boolean isDamageNumbersEnabled() {
		ensureLoaded();
		return damageNumbersEnabled;
	}

	public static synchronized void setDamageNumbersEnabled(boolean enabled) {
		ensureLoaded();
		damageNumbersEnabled = enabled;
		save();
	}

	public static synchronized void toggleDamageNumbers() {
		setDamageNumbersEnabled(!isDamageNumbersEnabled());
	}

	public static synchronized boolean isLegacyOverlayEnabled() {
		ensureLoaded();
		return legacyOverlayEnabled;
	}

	public static synchronized void setLegacyOverlayEnabled(boolean enabled) {
		ensureLoaded();
		legacyOverlayEnabled = enabled;
		save();
	}

	public static synchronized void toggleLegacyOverlay() {
		setLegacyOverlayEnabled(!isLegacyOverlayEnabled());
	}

	private static float clamp(float v) {
		return v < MIN_SCALE ? MIN_SCALE : (v > MAX_SCALE ? MAX_SCALE : v);
	}

	private static Path file() {
		return FMLPaths.CONFIGDIR.get().resolve("sololeveling-client.properties");
	}

	private static void ensureLoaded() {
		if (loaded)
			return;
		loaded = true;
		try {
			Path f = file();
			if (Files.exists(f)) {
				Properties p = new Properties();
				try (InputStream in = Files.newInputStream(f)) {
					p.load(in);
				}
				String s = p.getProperty(KEY_NOTIF_SCALE);
				if (s != null)
					notificationScale = clamp(Float.parseFloat(s));
				String damageNumbers = p.getProperty(KEY_DAMAGE_NUMBERS);
				if (damageNumbers != null)
					damageNumbersEnabled = Boolean.parseBoolean(damageNumbers);
				String legacyOverlay = p.getProperty(KEY_LEGACY_OVERLAY);
				if (legacyOverlay != null)
					legacyOverlayEnabled = Boolean.parseBoolean(legacyOverlay);
			}
		} catch (Throwable ignored) {
			// keep defaults on any read error
		}
	}

	private static void save() {
		try {
			Properties p = new Properties();
			p.setProperty(KEY_NOTIF_SCALE, Float.toString(notificationScale));
			p.setProperty(KEY_DAMAGE_NUMBERS, Boolean.toString(damageNumbersEnabled));
			p.setProperty(KEY_LEGACY_OVERLAY, Boolean.toString(legacyOverlayEnabled));
			Path f = file();
			Files.createDirectories(f.getParent());
			try (OutputStream out = Files.newOutputStream(f)) {
				p.store(out, "Solo Leveling client settings");
			}
		} catch (Throwable ignored) {
			// non-fatal
		}
	}
}
