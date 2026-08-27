package net.solocraft.procedures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure, deterministic entitlement and token rules for the Hunter classes whose
 * progression used to be written as MCreator {@code Math.random()} chains.
 *
 * <p>This class has no Minecraft dependencies so rank ordering, alias handling
 * and list migration can be regression-tested without starting NeoForge. It is
 * the same contract {@link TankerProgressionRules} already provides for Tanker,
 * generalised so Assassin, Fighter and Healer share one implementation instead
 * of three near-copies.</p>
 *
 * <p>Each class owns one <em>vocabulary</em> — every canonical name and alias
 * it can ever hold — and one or more <em>trees</em>. The default tree applies
 * to a hunter with no class style; a style key selects its own tree. Keeping
 * the vocabulary class-wide means a list containing another style's ability
 * still canonicalizes and de-duplicates correctly.</p>
 *
 * <p>Two behaviours are deliberately different from the procedures this
 * replaces. Grants are no longer probabilistic, so two hunters of the same
 * class, style and rank always receive the same kit; and the E rank now
 * receives its first ability, matching Tanker, Ranger and Mage.</p>
 */
public final class ClassProgressionRules {
	public static final int ASSASSIN_CLASS_ID = 1;
	public static final int FIGHTER_CLASS_ID = 3;
	public static final int HEALER_CLASS_ID = 5;

	/** Style keys, mirroring {@code net.solocraft.util.ClassStyleRules}. */
	public static final String INFILTRATION = "infiltration";
	public static final String ASSAULT = "assault";

	private static final ClassProgressionRules ASSASSIN_RULES = new ClassProgressionRules(
			ASSASSIN_CLASS_ID,
			// Vocabulary: everything an Assassin can hold, including abilities
			// that belong to no rank tree.
			List.of("Ghost Step", "Night Rend", "Stealth", "Flash Cut",
					"Critical Attack", "Dualwield", "Dagger Throw", "Mutilation",
					"Shadow Feint", "Silent Domain", "Zero Presence",
					"Dagger Rush", "Murderious Intent", "Cold Blood"),
			Map.ofEntries(
					Map.entry("shadowstep", "Ghost Step"),
					Map.entry("ghoststep", "Ghost Step"),
					Map.entry("backstab", "Night Rend"),
					Map.entry("nightrend", "Night Rend"),
					Map.entry("quickslashes", "Flash Cut"),
					Map.entry("flashcut", "Flash Cut"),
					Map.entry("dual wield", "Dualwield"),
					Map.entry("dual-wield", "Dualwield"),
					Map.entry("criticalattack", "Critical Attack"),
					Map.entry("daggerthrow", "Dagger Throw"),
					Map.entry("daggerrush", "Dagger Rush"),
					Map.entry("murderous intent", "Murderious Intent"),
					Map.entry("coldblood", "Cold Blood")),
			// Default tree for an Assassin with no style, preserving the kit
			// the class grants today.
			List.of("Ghost Step", "Night Rend", "Stealth", "Flash Cut",
					"Critical Attack", "Dualwield"),
			// Dagger Throw and Dagger Rush are founded on Ruler's Authority, not
			// on ordinary Assassin progression, so neither is a native rank
			// entitlement. They stay in the vocabulary above so migration and
			// de-duplication still recognise them.
			Map.of(
					INFILTRATION, List.of("Ghost Step", "Stealth", "Night Rend",
							"Shadow Feint", "Silent Domain", "Zero Presence"),
					ASSAULT, List.of("Night Rend", "Ghost Step",
							"Critical Attack", "Flash Cut", "Mutilation",
							"Dualwield")));

	/** Fighter style keys, mirroring {@code net.solocraft.util.ClassStyleRules}. */
	public static final String IMPACT = "impact";
	public static final String BLADE = "blade";
	public static final String FERAL = "feral";

	private static final ClassProgressionRules FIGHTER_RULES = new ClassProgressionRules(
			FIGHTER_CLASS_ID,
			// Vocabulary: every Fighter ability, tree member or not. Critical
			// Strike and Sword Beam stay recognisable so migration keeps them.
			List.of("Ground Slam", "Slash Dash", "Cross Strike", "Slash Fury",
					"Sword Dance", "Sword of Light", "Sword Beam", "Critical Strike",
					"Iron Knuckle", "Breaker Combo", "Meteor Fist", "Titan's Barrage",
					"Radiant Execution", "Magical Eye", "Claw Strikes", "Beast Sense",
					"Partial Transformation", "Predator Rush",
					"Full Beast Transformation"),
			Map.ofEntries(
					Map.entry("groundslam", "Ground Slam"),
					Map.entry("slashdash", "Slash Dash"),
					Map.entry("crossstrike", "Cross Strike"),
					Map.entry("slashfury", "Slash Fury"),
					Map.entry("slash furry", "Slash Fury"),
					Map.entry("sworddance", "Sword Dance"),
					Map.entry("swordoflight", "Sword of Light"),
					Map.entry("swordbeam", "Sword Beam"),
					Map.entry("ironknuckle", "Iron Knuckle"),
					Map.entry("breakercombo", "Breaker Combo"),
					Map.entry("meteorfist", "Meteor Fist"),
					Map.entry("titans barrage", "Titan's Barrage"),
					Map.entry("radiantexecution", "Radiant Execution"),
					Map.entry("magicaleye", "Magical Eye"),
					Map.entry("clawstrikes", "Claw Strikes"),
					Map.entry("beastsense", "Beast Sense")),
			// Default tree for a Fighter with no style, unchanged from ship.
			List.of("Ground Slam", "Slash Dash", "Cross Strike", "Slash Fury",
					"Sword Dance", "Sword of Light"),
			Map.of(
					IMPACT, List.of("Ground Slam", "Cross Strike", "Iron Knuckle",
							"Breaker Combo", "Meteor Fist", "Titan's Barrage"),
					BLADE, List.of("Slash Dash", "Sword Beam", "Slash Fury",
							"Radiant Execution", "Sword Dance", "Sword of Light"),
					FERAL, List.of("Magical Eye", "Claw Strikes", "Beast Sense",
							"Partial Transformation", "Predator Rush",
							"Full Beast Transformation")));

	/**
	 * Healer E through S. The S slot is Blessing Mark; the procedure this
	 * replaces checked for Blessing Mark and then appended Sword of Light, so an
	 * S-rank Healer received a Fighter sword skill and never got its capstone.
	 */
	/** Healer style keys, mirroring {@code net.solocraft.util.ClassStyleRules}. */
	public static final String FOCUSED = "focused";
	public static final String AREA = "area";

	private static final ClassProgressionRules HEALER_RULES = new ClassProgressionRules(
			HEALER_CLASS_ID,
			List.of("Heal Beam", "Haste Buff", "Purification", "Physical Buff",
					"Overheal", "Blessing Mark", "Healing Pulse", "Camouflage",
					"Purifying Wave", "Guardian Step", "Sanctuary", "Second Wind",
					"Guardian Ward", "Mana Font", "Vitality Surge", "Divine Favor"),
			Map.ofEntries(
					Map.entry("healbeam", "Heal Beam"),
					Map.entry("hastebuff", "Haste Buff"),
					Map.entry("haste", "Haste Buff"),
					Map.entry("physicalbuff", "Physical Buff"),
					Map.entry("blessingmark", "Blessing Mark"),
					Map.entry("purificaton", "Purification"),
					Map.entry("over heal", "Overheal"),
					Map.entry("healingpulse", "Healing Pulse"),
					Map.entry("purifyingwave", "Purifying Wave"),
					Map.entry("guardianstep", "Guardian Step"),
					Map.entry("secondwind", "Second Wind"),
					Map.entry("guardianward", "Guardian Ward"),
					Map.entry("manafont", "Mana Font"),
					Map.entry("vitalitysurge", "Vitality Surge"),
					Map.entry("divinefavor", "Divine Favor"),
					Map.entry("divine favour", "Divine Favor")),
			// Default tree for a Healer with no style, unchanged from ship.
			List.of("Heal Beam", "Haste Buff", "Purification", "Physical Buff",
					"Overheal", "Blessing Mark"),
			Map.of(
					FOCUSED, List.of("Heal Beam", "Haste Buff", "Purification",
							"Physical Buff", "Overheal", "Blessing Mark"),
					AREA, List.of("Healing Pulse", "Camouflage", "Purifying Wave",
							"Guardian Step", "Sanctuary", "Second Wind")));

	public static final ClassProgressionRules ASSASSIN = ASSASSIN_RULES;
	public static final ClassProgressionRules FIGHTER = FIGHTER_RULES;
	public static final ClassProgressionRules HEALER = HEALER_RULES;

	private static final List<ClassProgressionRules> ALL =
			List.of(ASSASSIN_RULES, FIGHTER_RULES, HEALER_RULES);

	private final int classId;
	private final List<String> vocabulary;
	private final Map<String, String> canonicalByLowercase;
	private final List<String> defaultOrder;
	private final Map<String, List<String>> styleOrders;

	private ClassProgressionRules(int classId, List<String> vocabulary,
			Map<String, String> aliases, List<String> defaultOrder,
			Map<String, List<String>> styleOrders) {
		this.classId = classId;
		this.vocabulary = List.copyOf(vocabulary);
		this.defaultOrder = List.copyOf(defaultOrder);
		Map<String, List<String>> orders = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : styleOrders.entrySet())
			orders.put(entry.getKey().toLowerCase(Locale.ROOT),
					List.copyOf(entry.getValue()));
		this.styleOrders = Map.copyOf(orders);

		Map<String, String> lookup = new LinkedHashMap<>();
		for (String skill : this.vocabulary)
			lookup.put(skill.toLowerCase(Locale.ROOT), skill);
		for (Map.Entry<String, String> alias : aliases.entrySet())
			lookup.put(alias.getKey().toLowerCase(Locale.ROOT), alias.getValue());
		this.canonicalByLowercase = Map.copyOf(lookup);
	}

	/** Returns the rules for a class id, or null when that class is elsewhere. */
	public static ClassProgressionRules forClassId(int classId) {
		for (ClassProgressionRules rules : ALL) {
			if (rules.classId == classId)
				return rules;
		}
		return null;
	}

	public int classId() {
		return classId;
	}

	/** Every canonical name the class can hold, tree member or not. */
	public List<String> vocabulary() {
		return vocabulary;
	}

	/** The tree for a style key, falling back to the default tree. */
	public List<String> masteryOrder(String styleKey) {
		if (styleKey == null)
			return defaultOrder;
		List<String> order =
				styleOrders.get(styleKey.trim().toLowerCase(Locale.ROOT));
		return order == null ? defaultOrder : order;
	}

	/** The tree used by a hunter with no class style. */
	public List<String> masteryOrder() {
		return defaultOrder;
	}

	public Set<String> styleKeys() {
		return styleOrders.keySet();
	}

	/**
	 * HunterRank uses 1=E through 6=S. Rank 1 receives the first ability, so a
	 * freshly evaluated hunter is never left with an empty kit.
	 */
	public List<String> entitlementsForRank(String styleKey, int rawRank) {
		List<String> order = masteryOrder(styleKey);
		int rank = Math.max(1, Math.min(order.size(), rawRank));
		return order.subList(0, rank);
	}

	public List<String> entitlementsForRank(int rawRank) {
		return entitlementsForRank(null, rawRank);
	}

	/**
	 * Returns the canonical name for an exact skill or alias, otherwise the
	 * trimmed input. This never performs substring matching, so "Critical
	 * Strike" can no longer be mistaken for "Critical Attack".
	 */
	public String canonicalName(String rawName) {
		String canonical = canonicalOrNull(rawName);
		return canonical == null ? cleanToken(rawName) : canonical;
	}

	public boolean isClassSkill(String rawName) {
		return canonicalOrNull(rawName) != null;
	}

	public boolean hasSkill(String rawList, String requestedSkill) {
		String requested = canonicalOrNull(requestedSkill);
		if (requested == null || rawList == null || rawList.isBlank())
			return false;
		for (String rawToken : rawList.split(",", -1)) {
			if (requested.equals(canonicalOrNull(cleanToken(rawToken))))
				return true;
		}
		return false;
	}

	/**
	 * Canonicalizes only this class's tokens, de-duplicates only this class's
	 * skills, preserves the order of unrelated tokens, and emits the
	 * repository's dot-prefixed list shape.
	 */
	public String canonicalizeSkillList(String rawList) {
		List<String> output = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		if (rawList != null) {
			for (String rawToken : rawList.split(",", -1)) {
				String token = cleanToken(rawToken);
				if (token.isEmpty())
					continue;
				String canonical = canonicalOrNull(token);
				if (canonical == null)
					output.add(token);
				else if (seen.add(canonical))
					output.add(canonical);
			}
		}
		return output.isEmpty() ? "." : "." + String.join(",", output) + ",";
	}

	public String ensureSkill(String rawList, String requestedSkill) {
		String canonical = canonicalOrNull(requestedSkill);
		String migrated = canonicalizeSkillList(rawList);
		if (canonical == null || hasSkill(migrated, canonical))
			return migrated;
		return ".".equals(migrated)
				? "." + canonical + ","
				: migrated + canonical + ",";
	}

	/** First unowned ability in tree order, or "" when the tree is complete. */
	public String firstMissingSkill(String styleKey, String rawList) {
		String migrated = canonicalizeSkillList(rawList);
		for (String skill : masteryOrder(styleKey)) {
			if (!hasSkill(migrated, skill))
				return skill;
		}
		return "";
	}

	public String firstMissingSkill(String rawList) {
		return firstMissingSkill(null, rawList);
	}

	/**
	 * Canonicalizes a selected-skill or slot value only when it is an exact
	 * name or alias. Unrelated values are returned unchanged.
	 */
	public String canonicalizeReference(String rawValue) {
		if (rawValue == null)
			return "";
		String canonical = canonicalOrNull(rawValue);
		return canonical == null ? rawValue : canonical;
	}

	private String canonicalOrNull(String rawName) {
		String clean = cleanToken(rawName);
		return clean.isEmpty()
				? null : canonicalByLowercase.get(clean.toLowerCase(Locale.ROOT));
	}

	private static String cleanToken(String rawToken) {
		if (rawToken == null)
			return "";
		String clean = rawToken.trim();
		while (clean.startsWith("."))
			clean = clean.substring(1).trim();
		return clean;
	}
}
