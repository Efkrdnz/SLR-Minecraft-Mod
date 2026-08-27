package net.solocraft.util;

import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Tiny client-side, global (per-installation) preferences persisted to
 * {@code config/sololeveling-client.properties}. Purely visual settings that
 * remain local to one installation, including notification sizing, damage
 * numbers, HUD presentation, and private entity-outline preferences.
 */
public final class SystemClientConfig {
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 2.0f;
	public static final float DEFAULT_SCALE = 1.0f;
	public static final float MIN_NOTIFICATION_POSITION = -1.4f;
	public static final float MAX_NOTIFICATION_POSITION = 1.4f;
	public static final float DEFAULT_NOTIFICATION_POSITION = 0.0f;
	public static final float MIN_NOTIFICATION_LIFETIME = 1.0f;
	public static final float MAX_NOTIFICATION_LIFETIME = 10.0f;
	public static final float DEFAULT_NOTIFICATION_LIFETIME = 5.0f;
	public static final int OUTLINE_DENSITY_MINIMAL = 0;
	public static final int OUTLINE_DENSITY_BALANCED = 1;
	public static final int OUTLINE_DENSITY_HIGH = 2;

	private static final String KEY_NOTIF_SCALE = "notificationScale";
	private static final String KEY_NOTIF_POSITION = "notificationHorizontalOffset";
	private static final String KEY_NOTIF_LIFETIME = "notificationLifetimeSeconds";
	private static final String KEY_NOTIF_DYNAMIC = "dynamicNotificationsEnabled";
	private static final String KEY_DAMAGE_NUMBERS = "damageNumbersEnabled";
	private static final String KEY_LEGACY_OVERLAY = "legacyOverlayEnabled";
	private static final String KEY_ENTITY_OUTLINES = "entityOutlinesEnabled";
	private static final String KEY_PERCEPTION_OUTLINES = "perceptionOutlinesEnabled";
	private static final String KEY_ENCOUNTER_OUTLINES = "encounterOutlinesEnabled";
	private static final String KEY_OUTLINE_DENSITY = "outlineDensity";

	private static float notificationScale = DEFAULT_SCALE;
	private static float notificationHorizontalOffset = DEFAULT_NOTIFICATION_POSITION;
	private static float notificationLifetimeSeconds = DEFAULT_NOTIFICATION_LIFETIME;
	private static boolean dynamicNotificationsEnabled = false;
	private static boolean damageNumbersEnabled = true;
	private static boolean legacyOverlayEnabled = false;
	private static boolean entityOutlinesEnabled = true;
	private static boolean perceptionOutlinesEnabled = true;
	private static boolean encounterOutlinesEnabled = true;
	private static int outlineDensity = OUTLINE_DENSITY_BALANCED;
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

	public static synchronized float getNotificationHorizontalOffset() {
		ensureLoaded();
		return notificationHorizontalOffset;
	}

	public static synchronized void setNotificationHorizontalOffset(float value) {
		ensureLoaded();
		notificationHorizontalOffset = clamp(value,
				MIN_NOTIFICATION_POSITION, MAX_NOTIFICATION_POSITION);
		save();
	}

	public static synchronized float getNotificationLifetimeSeconds() {
		ensureLoaded();
		return notificationLifetimeSeconds;
	}

	public static synchronized void setNotificationLifetimeSeconds(float value) {
		ensureLoaded();
		notificationLifetimeSeconds = clamp(value,
				MIN_NOTIFICATION_LIFETIME, MAX_NOTIFICATION_LIFETIME);
		save();
	}

	public static synchronized boolean isDynamicNotificationsEnabled() {
		ensureLoaded();
		return dynamicNotificationsEnabled;
	}

	public static synchronized void toggleDynamicNotifications() {
		ensureLoaded();
		dynamicNotificationsEnabled = !dynamicNotificationsEnabled;
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

	public static synchronized boolean isEntityOutlinesEnabled() {
		ensureLoaded();
		return entityOutlinesEnabled;
	}

	public static synchronized void toggleEntityOutlines() {
		ensureLoaded();
		entityOutlinesEnabled = !entityOutlinesEnabled;
		save();
	}

	public static synchronized boolean isPerceptionOutlinesEnabled() {
		ensureLoaded();
		return perceptionOutlinesEnabled;
	}

	public static synchronized void togglePerceptionOutlines() {
		ensureLoaded();
		perceptionOutlinesEnabled = !perceptionOutlinesEnabled;
		save();
	}

	public static synchronized boolean isEncounterOutlinesEnabled() {
		ensureLoaded();
		return encounterOutlinesEnabled;
	}

	public static synchronized void toggleEncounterOutlines() {
		ensureLoaded();
		encounterOutlinesEnabled = !encounterOutlinesEnabled;
		save();
	}

	public static synchronized int getOutlineDensity() {
		ensureLoaded();
		return outlineDensity;
	}

	public static synchronized void cycleOutlineDensity() {
		ensureLoaded();
		outlineDensity = outlineDensity >= OUTLINE_DENSITY_HIGH
				? OUTLINE_DENSITY_MINIMAL : outlineDensity + 1;
		save();
	}

	public static synchronized String getOutlineDensityLabel() {
		return switch (getOutlineDensity()) {
			case OUTLINE_DENSITY_MINIMAL -> "Minimal";
			case OUTLINE_DENSITY_HIGH -> "High";
			default -> "Balanced";
		};
	}

	private static float clamp(float v) {
		return v < MIN_SCALE ? MIN_SCALE : (v > MAX_SCALE ? MAX_SCALE : v);
	}

	private static float clamp(float value, float minimum, float maximum) {
		return value < minimum ? minimum : Math.min(value, maximum);
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
				String position = p.getProperty(KEY_NOTIF_POSITION);
				if (position != null)
					notificationHorizontalOffset = clamp(Float.parseFloat(position),
							MIN_NOTIFICATION_POSITION, MAX_NOTIFICATION_POSITION);
				String lifetime = p.getProperty(KEY_NOTIF_LIFETIME);
				if (lifetime != null)
					notificationLifetimeSeconds = clamp(Float.parseFloat(lifetime),
							MIN_NOTIFICATION_LIFETIME, MAX_NOTIFICATION_LIFETIME);
				String dynamic = p.getProperty(KEY_NOTIF_DYNAMIC);
				if (dynamic != null)
					dynamicNotificationsEnabled = Boolean.parseBoolean(dynamic);
				String damageNumbers = p.getProperty(KEY_DAMAGE_NUMBERS);
				if (damageNumbers != null)
					damageNumbersEnabled = Boolean.parseBoolean(damageNumbers);
				String legacyOverlay = p.getProperty(KEY_LEGACY_OVERLAY);
				if (legacyOverlay != null)
					legacyOverlayEnabled = Boolean.parseBoolean(legacyOverlay);
				String entityOutlines = p.getProperty(KEY_ENTITY_OUTLINES);
				if (entityOutlines != null)
					entityOutlinesEnabled = Boolean.parseBoolean(entityOutlines);
				String perceptionOutlines = p.getProperty(KEY_PERCEPTION_OUTLINES);
				if (perceptionOutlines != null)
					perceptionOutlinesEnabled = Boolean.parseBoolean(perceptionOutlines);
				String encounterOutlines = p.getProperty(KEY_ENCOUNTER_OUTLINES);
				if (encounterOutlines != null)
					encounterOutlinesEnabled = Boolean.parseBoolean(encounterOutlines);
				String density = p.getProperty(KEY_OUTLINE_DENSITY);
				if (density != null)
					outlineDensity = Math.max(OUTLINE_DENSITY_MINIMAL,
							Math.min(OUTLINE_DENSITY_HIGH, Integer.parseInt(density)));
			}
		} catch (Throwable ignored) {
			// keep defaults on any read error
		}
	}

	private static void save() {
		try {
			Properties p = new Properties();
			p.setProperty(KEY_NOTIF_SCALE, Float.toString(notificationScale));
			p.setProperty(KEY_NOTIF_POSITION, Float.toString(notificationHorizontalOffset));
			p.setProperty(KEY_NOTIF_LIFETIME, Float.toString(notificationLifetimeSeconds));
			p.setProperty(KEY_NOTIF_DYNAMIC, Boolean.toString(dynamicNotificationsEnabled));
			p.setProperty(KEY_DAMAGE_NUMBERS, Boolean.toString(damageNumbersEnabled));
			p.setProperty(KEY_LEGACY_OVERLAY, Boolean.toString(legacyOverlayEnabled));
			p.setProperty(KEY_ENTITY_OUTLINES, Boolean.toString(entityOutlinesEnabled));
			p.setProperty(KEY_PERCEPTION_OUTLINES, Boolean.toString(perceptionOutlinesEnabled));
			p.setProperty(KEY_ENCOUNTER_OUTLINES, Boolean.toString(encounterOutlinesEnabled));
			p.setProperty(KEY_OUTLINE_DENSITY, Integer.toString(outlineDensity));
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
