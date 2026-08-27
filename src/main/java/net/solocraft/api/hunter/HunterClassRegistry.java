package net.solocraft.api.hunter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;

/**
 * Every hunter class the game knows about, built-in or added.
 *
 * <p>The six original classes are registered here with the {@code Classes} values
 * they have always had, so this is a description of the existing scheme rather
 * than a replacement for it. Nothing about saves or gameplay changes by adding a
 * class here; it becomes addressable, not active.
 *
 * <p>Registration is open so an addon can contribute a class during mod
 * construction. Awakening into one still needs the persistence work that follows
 * this: {@code Classes} is a number, and a registered class without a legacy id
 * has no number to store yet.
 */
public final class HunterClassRegistry {
	public static final String NAMESPACE = "sololeveling";

	private static final Map<ResourceLocation, HunterClass> BY_ID = new ConcurrentHashMap<>();
	private static final Map<Integer, HunterClass> BY_LEGACY_ID = new ConcurrentHashMap<>();

	/** Held by a player who has not awakened. Never absent, so lookups need no null branch. */
	public static final HunterClass NONE = register(
			HunterClass.of(id("none"), HunterClass.UNAWAKENED_LEGACY_ID, "No Class"));

	// Numbers below are the existing Classes values and are not free to change.
	public static final HunterClass ASSASSIN = register(HunterClass.of(id("assassin"), 1, "Assassin"));
	public static final HunterClass COMBAT_MAGE = register(HunterClass.of(id("combat_mage"), 2, "Combat Mage"));
	public static final HunterClass FIGHTER = register(HunterClass.of(id("fighter"), 3, "Fighter"));
	public static final HunterClass TANKER = register(HunterClass.of(id("tanker"), 4, "Tanker"));
	public static final HunterClass SUPPORT_MAGE = register(HunterClass.of(id("support_mage"), 5, "Support Mage"));
	public static final HunterClass RANGER = register(HunterClass.of(id("ranger"), 6, "Ranger"));

	/**
	 * Evaluator presentation per class, from data packs.
	 *
	 * <p>Replaced wholesale on reload, and mirrored onto clients so the
	 * Evaluator can draw a contributed class there too.
	 */
	private static final Map<ResourceLocation, HunterClassPresentation> PRESENTATION =
			new ConcurrentHashMap<>();

	private HunterClassRegistry() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}

	/**
	 * Adds a class. Call during mod construction, before anything reads the registry.
	 *
	 * @throws IllegalArgumentException if the id or legacy id is already taken
	 */
	public static HunterClass register(HunterClass hunterClass) {
		if (hunterClass == null)
			throw new IllegalArgumentException("Hunter class must not be null");

		HunterClass existingId = BY_ID.putIfAbsent(hunterClass.id(), hunterClass);
		if (existingId != null)
			throw new IllegalArgumentException("Duplicate hunter class id: " + hunterClass.id());

		if (hunterClass.hasLegacyId()) {
			HunterClass existingLegacy = BY_LEGACY_ID.putIfAbsent(hunterClass.legacyId(), hunterClass);
			if (existingLegacy != null) {
				// Roll the id back so a rejected registration leaves nothing behind.
				BY_ID.remove(hunterClass.id(), hunterClass);
				throw new IllegalArgumentException("Legacy class id " + hunterClass.legacyId()
						+ " is already held by " + existingLegacy.id());
			}
		}
		return hunterClass;
	}

	/** Replaces every presentation. Called by the loader and by the client sync. */
	public static synchronized void replacePresentations(
			Map<ResourceLocation, HunterClassPresentation> presentations) {
		PRESENTATION.clear();
		if (presentations != null)
			PRESENTATION.putAll(presentations);
	}

	public static Optional<HunterClassPresentation> presentation(ResourceLocation classId) {
		return classId == null ? Optional.empty() : Optional.ofNullable(PRESENTATION.get(classId));
	}

	public static synchronized List<HunterClassPresentation> presentations() {
		return List.copyOf(new ArrayList<>(PRESENTATION.values()));
	}

	/**
	 * Contributed classes the Evaluator may draw, in a fixed order.
	 *
	 * <p>Only classes with a presentation, and sorted by id so a client and a
	 * server walking this list agree on which one a drawn number refers to.
	 * Registration order would not survive two mods loading in a different
	 * sequence on either side.
	 */
	public static synchronized List<HunterClass> evaluatorClasses() {
		List<HunterClass> classes = new ArrayList<>();
		for (HunterClass hunterClass : BY_ID.values())
			if (!hunterClass.hasLegacyId() && PRESENTATION.containsKey(hunterClass.id()))
				classes.add(hunterClass);
		classes.sort(Comparator.comparing(value -> value.id().toString()));
		return List.copyOf(classes);
	}

	public static HunterClass none() {
		return NONE;
	}

	public static Optional<HunterClass> byId(ResourceLocation id) {
		return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
	}

	public static Optional<HunterClass> byLegacyId(int legacyId) {
		return Optional.ofNullable(BY_LEGACY_ID.get(legacyId));
	}

	/**
	 * Ordered by legacy id first so the built-ins keep the order players know,
	 * then by id so classes without a number are still deterministic regardless
	 * of mod load order.
	 */
	public static List<HunterClass> all() {
		List<HunterClass> classes = new ArrayList<>(BY_ID.values());
		classes.sort(Comparator.comparingInt((HunterClass value) ->
						value.hasLegacyId() ? value.legacyId() : Integer.MAX_VALUE)
				.thenComparing(value -> value.id().toString()));
		return List.copyOf(classes);
	}

	/**
	 * Value written to the legacy Classes mirror for a class that has no number
	 * of its own.
	 *
	 * <p>Zero would read as "never awakened" to the comparisons still scattered
	 * through the codebase, and any real class number would impersonate a
	 * built-in. A single reserved value outside the original range satisfies
	 * both: "awakened, but not one you know". It is deliberately shared by every
	 * unnumbered class, because the identity lives in hunterClassId and a
	 * per-class number could not stay stable across a changing mod list.
	 */
	public static final int CUSTOM_CLASS_LEGACY_MIRROR = 1000;

	/**
	 * Resolves the class a player currently holds.
	 *
	 * <p>Prefers the stored identifier and falls back to the legacy number, so
	 * saves written before this field existed still resolve. An identifier that
	 * no longer resolves -- the mod that registered it was removed -- falls back
	 * too, and ultimately to {@link #none()} rather than throwing: a save from a
	 * newer or differently-modded install must not crash this one.
	 */
	public static HunterClass of(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		if (variables == null)
			return NONE;

		String storedId = variables.hunterClassId;
		if (storedId != null && !storedId.isBlank()) {
			ResourceLocation parsed = ResourceLocation.tryParse(storedId.trim());
			if (parsed != null) {
				HunterClass resolved = BY_ID.get(parsed);
				if (resolved != null)
					return resolved;
			}
		}
		return byLegacyId((int) Math.floor(variables.Classes)).orElse(NONE);
	}

	/** True once the player has awakened into any class. */
	public static boolean isAwakened(Entity entity) {
		return !of(entity).isUnawakened();
	}

	/**
	 * Assigns a class, writing the identifier and the legacy mirror together.
	 *
	 * <p>The only supported way to change a player's class. Writing one field
	 * without the other leaves the two representations disagreeing, and which
	 * one wins then depends on which code path happens to read it.
	 *
	 * @return false if the class is unknown to the registry or the player has no
	 *         variables attached
	 */
	public static boolean assign(Entity entity, HunterClass hunterClass) {
		if (entity == null || hunterClass == null)
			return false;
		if (BY_ID.get(hunterClass.id()) != hunterClass)
			return false;
		if (variablesOf(entity) == null)
			return false;

		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.hunterClassId = hunterClass.isUnawakened()
							? ""
							: hunterClass.id().toString();
					capability.Classes = hunterClass.hasLegacyId()
							? hunterClass.legacyId()
							: CUSTOM_CLASS_LEGACY_MIRROR;
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
