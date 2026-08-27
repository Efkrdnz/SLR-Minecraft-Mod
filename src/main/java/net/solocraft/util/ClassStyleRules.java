package net.solocraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class-style registry used by Hunter Evaluation, progression and UI.
 *
 * <p>Every style a class will ever own is declared here, but a style is only
 * drawable once {@link Style#enabled()} is true. That gate is what keeps an
 * unfinished ability tree out of the evaluator while still letting the rest of
 * the game resolve its name, colour and saved identifier.</p>
 *
 * <p>Numeric style ids are persisted in the evaluation session and in the
 * remaining-style bitmask, so the order within a class must never change. The
 * string key is the value written to {@code classStyle} and is likewise
 * permanent; display names are free to change.</p>
 */
public final class ClassStyleRules {
	public static final int ASSASSIN_CLASS_ID = 1;
	public static final int MAGE_CLASS_ID = 2;
	public static final int FIGHTER_CLASS_ID = 3;
	public static final int TANKER_CLASS_ID = 4;
	public static final int HEALER_CLASS_ID = 5;
	public static final int RANGER_CLASS_ID = 6;

	/** Mage style ids, persisted since the first class-style release. */
	public static final int FIRE_MAGE = 1;
	public static final int BARRIER_MAGE = 2;
	public static final int ARCANE_MAGE = 3;
	public static final int STORM_MAGE = 4;
	public static final int CURSE_MAGE = 5;
	public static final int MAGE_STYLE_COUNT = 5;
	public static final int ALL_MAGE_STYLES_MASK = (1 << MAGE_STYLE_COUNT) - 1;

	/** Assassin style ids. Disabled until their ability trees are playable. */
	public static final int INFILTRATOR = 1;
	public static final int CUTTHROAT = 2;

	private static final String UNRESOLVED_NAME = "Unresolved";
	private static final String UNRESOLVED_DESCRIPTION =
			"Awaiting mana-aspect analysis";
	private static final int UNRESOLVED_COLOR = 0xFFBF5FFF;

	/**
	 * One class-scoped style definition.
	 *
	 * @param enabled whether the evaluator may draw and commit this style
	 */
	public record Style(int classId, int styleId, String key, String displayName,
			String description, int accentColor, boolean enabled) {
	}

	private static final List<Style> STYLES = List.of(
			// Assassin — Tempo spent on concealment versus dagger pressure.
			new Style(ASSASSIN_CLASS_ID, INFILTRATOR, "infiltration", "Infiltrator",
					"Veil, isolation and strikes from unseen angles",
					0xFF9B8CFF, true),
			new Style(ASSASSIN_CLASS_ID, CUTTHROAT, "assault", "Cutthroat",
					"Dagger tempo, critical windows and rapid finishes",
					0xFFE0243C, true),

			// Mage — the completed reference implementation.
			new Style(MAGE_CLASS_ID, FIRE_MAGE, "fire", "Fire Mage",
					"Scorch, explosive damage and area denial",
					0xFFFF5A2A, true),
			new Style(MAGE_CLASS_ID, BARRIER_MAGE, "barrier", "Barrier Mage",
					"Constructs, protection and battlefield control",
					0xFF5CE8FF, true),
			new Style(MAGE_CLASS_ID, ARCANE_MAGE, "arcane", "Arcane Mage",
					"Spatial formulas, mobility and precision",
					0xFF8A5CFF, true),
			new Style(MAGE_CLASS_ID, STORM_MAGE, "storm", "Storm Mage",
					"Voltage, movement and controlled chain damage",
					0xFFFFD45A, true),
			new Style(MAGE_CLASS_ID, CURSE_MAGE, "curse", "Curse Mage",
					"Afflictions, decay and misfortune worked through others",
					0xFFA05CFF, true),

			// Fighter
			new Style(FIGHTER_CLASS_ID, 1, "impact", "Striker",
					"Guard breaks, punch strings and close-range impact",
					0xFFFF8A3D, true),
			new Style(FIGHTER_CLASS_ID, 2, "blade", "Swordsman",
					"Movement cuts, sword waves and precise finishers",
					0xFFF2E08A, true),
			// Renamed from "Ravager"; the stored key stays "feral" so every
			// hunter who already took this style keeps it.
			new Style(FIGHTER_CLASS_ID, 3, "feral", "Mutant",
					"Beast senses, claws and partial transformation",
					0xFFFFA726, true),

			// Tanker
			new Style(TANKER_CLASS_ID, 1, "guard", "Sentinel",
					"Shield control, threat and party protection",
					0xFF6FA8DC, true),
			new Style(TANKER_CLASS_ID, 2, "mass", "Juggernaut",
					"Body mass, grapples and space denial",
					0xFFC97B3A, true),

			// Healer
			new Style(HEALER_CLASS_ID, 1, "focused", "Saint",
					"Targeted rescue, cleansing and large single-target recovery",
					0xFF9BF0C0, true),
			new Style(HEALER_CLASS_ID, 2, "area", "Shepherd",
					"Proximity healing, sanctuaries and group rescue",
					0xFF6FE0B0, true),

			// Ranger
			new Style(RANGER_CLASS_ID, 1, "bow", "Bowmaster",
					"Mana arrows, marks and long-range precision",
					0xFF4FE3A1, false),
			new Style(RANGER_CLASS_ID, 2, "thrown", "Saboteur",
					"Thrown blades, ricochets, traps and explosives",
					0xFFBFC9D4, false),
			new Style(RANGER_CLASS_ID, 3, "firearm", "Gunslinger",
					"Firearms, ammunition control and precision bursts",
					0xFFFFC24D, false));

	private ClassStyleRules() {
	}

	/**
	 * Where contributed styles come from, installed at mod construction.
	 *
	 * <p>An indirection rather than a direct call because this class is
	 * dependency-free on purpose -- the regressions exercise the style economy
	 * without Minecraft on the classpath, and reaching into the contributed
	 * registry from here would drag {@code ResourceLocation} in with it. Nothing
	 * installs it in a test, so the built-in behaviour is exactly what it was.
	 */
	private static volatile java.util.function.IntFunction<List<Style>> contributedSource =
			classId -> List.of();

	/** Called once during mod construction. */
	public static void installContributedSource(
			java.util.function.IntFunction<List<Style>> source) {
		contributedSource = source == null ? classId -> List.of() : source;
	}

	/**
	 * Every style for a class: the shipped ones, or a contributed class's own.
	 *
	 * <p>Built-ins are consulted first and a class that has any keeps only those,
	 * so an addon can never add a style to a shipped class through this path.
	 */
	private static List<Style> stylesFor(int classId) {
		List<Style> builtIn = new ArrayList<>();
		for (Style style : STYLES) {
			if (style.classId() == classId)
				builtIn.add(style);
		}
		if (!builtIn.isEmpty())
			return builtIn;
		try {
			List<Style> contributed = contributedSource.apply(classId);
			return contributed == null ? List.of() : contributed;
		} catch (RuntimeException exception) {
			// A broken addon must not stop the evaluator from resolving a class.
			return List.of();
		}
	}

	/** True when a class has at least one style the evaluator may draw. */
	public static boolean supportsStyles(int classId) {
		return allStylesMask(classId) != 0;
	}

	/** Bitmask of every enabled style in a class. Disabled styles are excluded. */
	public static int allStylesMask(int classId) {
		int mask = 0;
		for (Style style : stylesFor(classId)) {
			if (style.enabled())
				mask |= bit(style.styleId());
		}
		return mask;
	}

	/** A style the evaluator may draw and commit: registered and enabled. */
	public static boolean isValidStyle(int classId, int styleId) {
		Style style = find(classId, styleId);
		return style != null && style.enabled();
	}

	/** Every declared style for a class, including gated ones. */
	public static List<Style> registeredStyles(int classId) {
		return List.copyOf(stylesFor(classId));
	}

	/** Resolves a saved key to its id, or 0 when the style is not enabled. */
	public static int styleId(int classId, String key) {
		if (key == null)
			return 0;
		String normalized = key.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty())
			return 0;
		for (Style style : stylesFor(classId)) {
			if (style.enabled() && style.key().equals(normalized))
				return style.styleId();
		}
		return 0;
	}

	/** The persisted key, or "" when the style is not enabled. */
	public static String styleKey(int classId, int styleId) {
		return isValidStyle(classId, styleId)
				? find(classId, styleId).key() : "";
	}

	// Presentation resolves any declared style so a gated or admin-granted
	// value still renders with its real name rather than "Unresolved".

	public static String styleName(int classId, int styleId) {
		Style style = find(classId, styleId);
		return style == null ? UNRESOLVED_NAME : style.displayName();
	}

	/**
	 * Whether a class's style is shown to other people.
	 *
	 * <p>A Healer is a Healer in public. Saint and Shepherd are how the player
	 * heals, not a separate profession, and advertising which one they took on
	 * an association ID card tells everyone whether to expect single-target
	 * rescue or group coverage before the fight starts.
	 *
	 * <p>The style itself is unaffected -- it still governs the skill tree and
	 * everything internal. This only controls whether it is <em>named</em> on
	 * player-facing identity surfaces.
	 */
	public static boolean isStylePublic(int classId) {
		return classId != HEALER_CLASS_ID;
	}

	/** Style name for identity surfaces, or empty when the class hides it. */
	public static String publicStyleName(int classId, int styleId) {
		return isStylePublic(classId) ? styleName(classId, styleId) : "";
	}

	public static String styleDescription(int classId, int styleId) {
		Style style = find(classId, styleId);
		return style == null ? UNRESOLVED_DESCRIPTION : style.description();
	}

	public static int styleColor(int classId, int styleId) {
		Style style = find(classId, styleId);
		return style == null ? UNRESOLVED_COLOR : style.accentColor();
	}

	/**
	 * Draws from a non-repeating per-class style bag. An exhausted bag refills,
	 * while the current style remains excluded from the first draw so a reroll
	 * never immediately repeats its visible result.
	 */
	public static StyleDraw drawStyle(int classId, int remainingMask,
			int currentStyle, int randomIndex) {
		int all = allStylesMask(classId);
		if (all == 0)
			return new StyleDraw(0, 0);

		int mask = remainingMask & all;
		int currentBit = isValidStyle(classId, currentStyle)
				? bit(currentStyle) : 0;
		int candidates = mask & ~currentBit;
		if (candidates == 0) {
			mask = all;
			candidates = mask & ~currentBit;
		}
		// A class with exactly one enabled style has nothing else to offer, so
		// the current result is the only legal draw rather than an error.
		if (candidates == 0) {
			candidates = all;
			mask = all;
		}

		int wanted = Math.floorMod(randomIndex, Integer.bitCount(candidates));
		int chosen = 0;
		for (Style style : stylesFor(classId)) {
			if ((candidates & bit(style.styleId())) == 0)
				continue;
			if (wanted-- == 0) {
				chosen = style.styleId();
				break;
			}
		}
		return new StyleDraw(chosen, mask & ~bit(chosen));
	}

	private static Style find(int classId, int styleId) {
		for (Style style : stylesFor(classId)) {
			if (style.styleId() == styleId)
				return style;
		}
		return null;
	}

	private static int bit(int styleId) {
		return styleId >= 1 && styleId <= 32 ? 1 << (styleId - 1) : 0;
	}

	public record StyleDraw(int styleId, int remainingMask) {
	}
}
