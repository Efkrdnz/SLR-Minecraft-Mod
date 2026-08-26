package net.solocraft.api.hunter;

import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * One awakened hunter class.
 *
 * <p>Class identity has always been the raw number in {@code PlayerVariables.Classes},
 * compared against literals in dozens of places. That works only while every class
 * is known at compile time: a datapack or an addon has no number it could safely
 * claim, and no way to describe itself.
 *
 * <p>A class is therefore identified by {@link #id()}. The {@link #legacyId()} is
 * kept alongside it purely so existing saves and the code that still reads
 * {@code Classes} keep working while the two representations coexist.
 *
 * @param id             stable identity. Persisted; renaming one breaks saves.
 * @param legacyId       matching {@code Classes} value, or {@link #NO_LEGACY_ID}
 *                       for a class added after the numbering scheme.
 * @param translationKey lang key for the display name.
 * @param fallbackName   shown when the lang key is missing, so a datapack class
 *                       is readable before anyone writes translations for it.
 */
public record HunterClass(ResourceLocation id, int legacyId, String translationKey,
		String fallbackName) {

	/** Used by classes that never had a number in the original scheme. */
	public static final int NO_LEGACY_ID = -1;

	/** Reserved for {@link HunterClassRegistry#none()}; no other class may claim it. */
	public static final int UNAWAKENED_LEGACY_ID = 0;

	public HunterClass {
		Objects.requireNonNull(id, "Hunter class id must not be null");
		Objects.requireNonNull(translationKey, "Hunter class translation key must not be null");
		Objects.requireNonNull(fallbackName, "Hunter class fallback name must not be null");
		if (legacyId < NO_LEGACY_ID)
			throw new IllegalArgumentException("Legacy id must be " + NO_LEGACY_ID + " or higher, got " + legacyId);
		if (translationKey.isBlank())
			throw new IllegalArgumentException("Hunter class translation key must not be blank");
		if (fallbackName.isBlank())
			throw new IllegalArgumentException("Hunter class fallback name must not be blank");
	}

	/** Convenience for the built-ins, which all follow the same key shape. */
	public static HunterClass of(ResourceLocation id, int legacyId, String fallbackName) {
		return new HunterClass(id, legacyId,
				"hunterclass." + id.getNamespace() + "." + id.getPath(), fallbackName);
	}

	/** False for classes introduced after the numeric scheme; they are id-only. */
	public boolean hasLegacyId() {
		return legacyId >= UNAWAKENED_LEGACY_ID;
	}

	/** True for the placeholder held by a player who has not awakened yet. */
	public boolean isUnawakened() {
		return legacyId == UNAWAKENED_LEGACY_ID;
	}

	/**
	 * Falls back rather than showing a raw key, so a class from a datapack that
	 * ships no lang file still reads as its name instead of gibberish.
	 */
	public Component displayName() {
		return Component.translatableWithFallback(translationKey, fallbackName);
	}
}
