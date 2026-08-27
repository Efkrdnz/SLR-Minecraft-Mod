package net.solocraft.api.vessel;

import java.util.Locale;
import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.solocraft.util.VesselManager;

/**
 * One Ruler or Monarch vessel a hunter can hold.
 *
 * <p>Vessels have always been identified by the number in {@code JOB}, compared
 * against literals throughout the mod. A vessel contributed by an addon has no
 * number it could safely claim, so identity moves to {@link #id()} and the number
 * stays behind as a mirror. Built-in vessels keep the numbers they have always
 * had; the mirror for the rest is deliberately shared, because a per-vessel
 * number could not stay stable across a changing mod list.
 *
 * @param id             stable identity. Persisted; renaming one breaks saves.
 * @param legacyJobId    matching {@code JOB} value, or {@link #NO_LEGACY_JOB_ID}.
 * @param kind           Ruler or Monarch. Drives selection-screen presentation.
 * @param identity       the short legacy identity string, e.g. {@code ashborn}.
 * @param fallbackName   the wielder, e.g. {@code Ashborn}, when untranslated.
 * @param fallbackPower  the power, e.g. {@code Shadow Monarch}, when untranslated.
 * @param description    the line the selection screen shows beneath the power.
 */
public record Vessel(ResourceLocation id, int legacyJobId, Kind kind, String identity,
		String fallbackName, String fallbackPower, String description) {

	/** Used by vessels added after the numbering scheme. */
	public static final int NO_LEGACY_JOB_ID = -1;

	/** Rulers all present identically; Monarchs each carry their own theming. */
	public enum Kind {
		RULER(VesselManager.RULER),
		MONARCH(VesselManager.MONARCH);

		private final String legacyType;

		Kind(String legacyType) {
			this.legacyType = legacyType;
		}

		/** The string the existing vessel code stores and compares. */
		public String legacyType() {
			return legacyType;
		}

		public static Kind fromLegacyType(String type) {
			if (type == null)
				return MONARCH;
			return VesselManager.RULER.equals(type.toLowerCase(Locale.ROOT)) ? RULER : MONARCH;
		}
	}

	public Vessel {
		Objects.requireNonNull(id, "Vessel id must not be null");
		Objects.requireNonNull(kind, "Vessel kind must not be null");
		Objects.requireNonNull(identity, "Vessel identity must not be null");
		Objects.requireNonNull(fallbackName, "Vessel name must not be null");
		Objects.requireNonNull(fallbackPower, "Vessel power name must not be null");
		description = description == null ? "" : description.trim();
		if (legacyJobId < NO_LEGACY_JOB_ID)
			throw new IllegalArgumentException(
					"Legacy job id must be " + NO_LEGACY_JOB_ID + " or higher, got " + legacyJobId);
		if (identity.isBlank() || fallbackName.isBlank() || fallbackPower.isBlank())
			throw new IllegalArgumentException("Vessel identity, name, and power must not be blank");
	}

	/** Convenience for a vessel whose identity string matches its id path. */
	/**
	 * A contributed vessel.
	 *
	 * <p>The description is the line the selection screen shows beneath the power
	 * name. Every shipped vessel has one, and a contributed vessel without one
	 * reads as unfinished sitting next to them.
	 */
	public static Vessel of(ResourceLocation id, Kind kind, String fallbackName,
			String fallbackPower, String description) {
		return new Vessel(id, NO_LEGACY_JOB_ID, kind, id.getPath(), fallbackName,
			fallbackPower, description);
	}

	/** False for vessels introduced after the numeric scheme; they are id-only. */
	public boolean hasLegacyJobId() {
		return legacyJobId > NO_LEGACY_JOB_ID;
	}

	public String translationKey() {
		return "vessel." + id.getNamespace() + "." + id.getPath();
	}

	public String powerTranslationKey() {
		return translationKey() + ".power";
	}

	/** Falls back rather than showing a raw key, so an untranslated vessel still reads. */
	public Component displayName() {
		return Component.translatableWithFallback(translationKey(), fallbackName);
	}

	public Component powerName() {
		return Component.translatableWithFallback(powerTranslationKey(), fallbackPower);
	}
}
