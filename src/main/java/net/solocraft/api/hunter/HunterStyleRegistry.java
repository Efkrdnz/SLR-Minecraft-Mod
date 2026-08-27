package net.solocraft.api.hunter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;

/**
 * Styles contributed by addons, held where the mod already keeps a hunter's.
 *
 * <p>The mod's own styles live in {@code ClassStyleRules}, numbered and keyed to
 * a numbered class. A contributed class has no number to key against, so its
 * styles are registered here and stored by their full id.
 *
 * <p>Storage is the mod's existing {@code classStyle} field, not a new one. That
 * matters: a hunter has exactly one style, and two mods keeping their own answer
 * to that question is how they end up disagreeing. It also means the checks the
 * mod already makes -- {@code "fire".equals(classStyle)} and its like -- see a
 * namespaced value, match nothing, and correctly conclude the hunter is not a
 * Fire Mage.
 *
 * <p>What this does not yet do is put a contributed style in the evaluation
 * screen. That screen chooses by numbered class and style, so offering one there
 * needs the evaluation session to carry class identity rather than a number.
 * Until then, assign a style from your own flow.
 */
public final class HunterStyleRegistry {
	private static final Map<ResourceLocation, HunterStyle> BY_ID = new LinkedHashMap<>();
	private static final Map<String, HunterStyle> BY_STORED_KEY = new LinkedHashMap<>();

	private HunterStyleRegistry() {
	}

	/**
	 * Adds a style. Call during mod construction.
	 *
	 * @throws IllegalArgumentException if the id is already taken
	 */
	public static synchronized HunterStyle register(HunterStyle style) {
		if (style == null)
			throw new IllegalArgumentException("Style must not be null");
		HunterStyle existing = BY_ID.putIfAbsent(style.id(), style);
		if (existing != null)
			throw new IllegalArgumentException("Duplicate style id: " + style.id());
		BY_STORED_KEY.put(HunterStyle.normalise(style.storedKey()), style);
		return style;
	}

	public static synchronized Optional<HunterStyle> byId(ResourceLocation id) {
		return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
	}

	public static synchronized Optional<HunterStyle> byStoredKey(String key) {
		return Optional.ofNullable(BY_STORED_KEY.get(HunterStyle.normalise(key)));
	}

	/** Every contributed style belonging to one class, in registration order. */
	public static synchronized List<HunterStyle> forClass(ResourceLocation classId) {
		List<HunterStyle> styles = new ArrayList<>();
		for (HunterStyle style : BY_ID.values())
			if (style.owningClass().equals(classId))
				styles.add(style);
		return List.copyOf(styles);
	}

	public static synchronized List<HunterStyle> all() {
		return List.copyOf(new ArrayList<>(BY_ID.values()));
	}

	/**
	 * The contributed style a hunter currently holds, if it is one of ours.
	 *
	 * <p>Empty for a hunter on a built-in style, which is the honest answer
	 * rather than a guess: ask {@code ClassStyleRules} for those.
	 */
	public static Optional<HunterStyle> of(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		if (variables == null || variables.classStyle == null || variables.classStyle.isBlank())
			return Optional.empty();
		return byStoredKey(variables.classStyle);
	}

	public static boolean hasStyle(Entity entity, HunterStyle style) {
		if (style == null)
			return false;
		return of(entity).map(current -> current.id().equals(style.id())).orElse(false);
	}

	/**
	 * Assigns a contributed style. Server-side.
	 *
	 * <p>Refuses when the hunter does not hold the style's class, because a style
	 * without its class is a state nothing else in the mod knows how to read.
	 *
	 * @return false when unregistered, mismatched, or the player has no variables
	 */
	public static boolean assign(Entity entity, HunterStyle style) {
		if (entity == null || style == null)
			return false;
		if (byId(style.id()).orElse(null) != style)
			return false;
		if (!HunterClassRegistry.of(entity).id().equals(style.owningClass()))
			return false;
		if (variablesOf(entity) == null)
			return false;

		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.classStyle = style.storedKey();
					capability.syncPlayerVariables(entity);
				});
		return true;
	}

	/** Clears the stored style, whether it was contributed or built in. */
	public static boolean clear(Entity entity) {
		if (entity == null || variablesOf(entity) == null)
			return false;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.classStyle = "";
					capability.syncPlayerVariables(entity);
				});
		return true;
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
