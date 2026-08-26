package net.solocraft.api.hunter;

import java.util.Locale;
import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A subclass within a hunter class -- what the mod calls a style.
 *
 * <p>Mirrors the shape the mod already uses for its own: Fire Mage and Barrier
 * Mage inside Mage, Infiltrator and Cutthroat inside Assassin. Those are held as
 * numbered entries keyed to a numbered class, which is a scheme a contributed
 * class cannot join. Styles here are identified the same way classes are.
 *
 * @param id           stable identity, namespaced to your mod. Persisted.
 * @param owningClass  the class this style belongs to. May be a built-in class
 *                     id, which is how you add a style to a shipped class.
 * @param displayName  shown wherever the style is named.
 * @param description  one line saying what the style is about, as the mod's own
 *                     styles carry.
 * @param accentColor  packed RGB used when the style is drawn.
 */
public record HunterStyle(ResourceLocation id, ResourceLocation owningClass, String displayName,
		String description, int accentColor) {

	public HunterStyle {
		Objects.requireNonNull(id, "Style id must not be null");
		Objects.requireNonNull(owningClass, "A style must belong to a class");

		displayName = clean(displayName);
		description = description == null ? "" : clean(description);

		if (displayName.isEmpty())
			throw new IllegalArgumentException("Style " + id + " must have a display name");
	}

	/**
	 * What gets written to the hunter's stored style.
	 *
	 * <p>The full id, so it cannot collide with a built-in key like {@code fire}
	 * and cannot be matched by a check written before this style existed. Those
	 * checks correctly find nothing.
	 */
	public String storedKey() {
		return id.toString();
	}

	public String translationKey() {
		return "hunterstyle." + id.getNamespace() + "." + id.getPath();
	}

	public Component displayComponent() {
		return Component.translatableWithFallback(translationKey(), displayName);
	}

	private static String clean(String value) {
		return value == null ? "" : value.trim();
	}

	/** Convenience for a style whose display name is derived from its path. */
	public static HunterStyle of(ResourceLocation id, ResourceLocation owningClass,
			String displayName, String description, int accentColor) {
		return new HunterStyle(id, owningClass, displayName, description, accentColor);
	}

	/** Normalises a stored key for comparison. */
	public static String normalise(String key) {
		return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
	}
}
