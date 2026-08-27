package net.solocraft.procedures;

import net.solocraft.util.ManaRules;

import java.util.List;

/**
 * Regression checks for the deterministic Assassin, Fighter and Healer
 * entitlements and for the shared mana cost contract.
 *
 * <p>These replace behaviour that used to be probabilistic, so the assertions
 * are mostly about outcomes being repeatable rather than about specific
 * balance numbers.</p>
 */
public final class ClassProgressionRulesRegression {
	private ClassProgressionRulesRegression() {
	}

	public static void main(String[] args) throws java.io.IOException {
		assassinStyleTreesAreDistinctAndComplete();
		fighterStyleTreesAreDistinctAndComplete();
		everyGrantableAbilityHasADescription();
		defaultTreeIsUnchangedForStylelessHunters();
		implementedTokensMatchTheRuntimeManager();
		rankEntitlementsAreDeterministic();
		healerCapstoneIsBlessingMark();
		tokenMatchingIsExactNeverSubstring();
		aliasesCanonicalizeWithoutLosingUnknownSkills();
		masteryWalksTheOrderWithoutRandomRetries();
		classesOwnedElsewhereAreNotClaimed();
		manaBandFloorsReproduceLegacyCosts();
		intelligenceIsNeverTaxedTwice();
		loadMultipliersAreBounded();
		System.out.println("Class progression and mana rule checks passed.");
	}

	private static void assassinStyleTreesAreDistinctAndComplete() {
		ClassProgressionRules assassin = ClassProgressionRules.ASSASSIN;
		List<String> infiltrator =
				assassin.masteryOrder(ClassProgressionRules.INFILTRATION);
		List<String> cutthroat =
				assassin.masteryOrder(ClassProgressionRules.ASSAULT);

		expectEquals(List.of("Ghost Step", "Stealth", "Night Rend",
						"Shadow Feint", "Silent Domain", "Zero Presence"),
				infiltrator, "Infiltrator tree");
		expectEquals(List.of("Night Rend", "Ghost Step", "Critical Attack",
						"Flash Cut", "Mutilation", "Dualwield"),
				cutthroat, "Cutthroat tree");

		expectTrue(!infiltrator.equals(cutthroat),
				"The two Assassin styles must not share a tree");
		// Ghost Step and Night Rend are the shared Assassin baseline; each style
		// places them at a different rank. Nothing else may overlap.
		List<String> shared = new java.util.ArrayList<>(infiltrator);
		shared.retainAll(cutthroat);
		expectEquals(List.of("Ghost Step", "Night Rend"), shared,
				"Only Ghost Step and Night Rend may be shared between styles");
		expectTrue(infiltrator.indexOf("Night Rend") != cutthroat.indexOf("Night Rend"),
				"A shared ability must sit at a different rank in each style");

		// Every tree entry must be part of the class vocabulary, or list
		// migration would treat a native ability as an unknown token.
		for (String skill : infiltrator)
			expectTrue(assassin.isClassSkill(skill),
					skill + " must be in the Assassin vocabulary");
		for (String skill : cutthroat)
			expectTrue(assassin.isClassSkill(skill),
					skill + " must be in the Assassin vocabulary");

		// Off-tree abilities stay recognisable so they survive migration.
		// Dagger Throw and Dagger Rush are founded on Ruler's Authority rather
		// than ordinary Assassin progression, so neither may ever be granted as
		// a native rank entitlement by either style.
		for (String offTree : List.of("Dagger Throw", "Dagger Rush",
				"Murderious Intent", "Cold Blood")) {
			expectTrue(assassin.isClassSkill(offTree),
					offTree + " must remain canonicalizable");
			expectTrue(!infiltrator.contains(offTree)
							&& !cutthroat.contains(offTree),
					offTree + " must never be a native rank entitlement");
		}

		// An unknown or blank style falls back rather than granting nothing.
		expectEquals(assassin.masteryOrder(), assassin.masteryOrder("nonsense"),
				"An unknown style key must fall back to the default tree");
		expectEquals(assassin.masteryOrder(), assassin.masteryOrder(""),
				"A blank style key must fall back to the default tree");
		expectEquals(assassin.masteryOrder(), assassin.masteryOrder(null),
				"A null style key must fall back to the default tree");
	}

	/**
	 * Fighter's three trees share nothing — fists, swords and claws are
	 * different weapon contracts — and every entry must be dispatchable.
	 */
	private static void fighterStyleTreesAreDistinctAndComplete() throws java.io.IOException {
		ClassProgressionRules fighter = ClassProgressionRules.FIGHTER;
		List<String> striker = fighter.masteryOrder(ClassProgressionRules.IMPACT);
		List<String> swordsman = fighter.masteryOrder(ClassProgressionRules.BLADE);
		List<String> ravager = fighter.masteryOrder(ClassProgressionRules.FERAL);

		expectEquals(List.of("Ground Slam", "Cross Strike", "Iron Knuckle",
						"Breaker Combo", "Meteor Fist", "Titan's Barrage"),
				striker, "Striker tree");
		expectEquals(List.of("Slash Dash", "Sword Beam", "Slash Fury",
						"Radiant Execution", "Sword Dance", "Sword of Light"),
				swordsman, "Swordsman tree");
		expectEquals(List.of("Magical Eye", "Claw Strikes", "Beast Sense",
						"Partial Transformation", "Predator Rush",
						"Full Beast Transformation"),
				ravager, "Ravager tree");

		// Unlike Assassin, no Fighter ability may appear in two trees.
		for (List<String> first : List.of(striker, swordsman, ravager)) {
			for (List<String> second : List.of(striker, swordsman, ravager)) {
				if (first == second)
					continue;
				List<String> shared = new java.util.ArrayList<>(first);
				shared.retainAll(second);
				expectTrue(shared.isEmpty(),
						"Fighter trees must not share abilities: " + shared);
			}
		}

		for (List<String> tree : List.of(striker, swordsman, ravager)) {
			for (String skill : tree)
				expectTrue(fighter.isClassSkill(skill),
						skill + " must be in the Fighter vocabulary");
		}

		// New abilities must be dispatchable by FighterSkillManager; the seven
		// legacy ones are still routed through their own procedures.
		java.util.Set<String> managed = declaredConstants("FighterSkillManager.java");
		for (String newSkill : List.of("Iron Knuckle", "Breaker Combo", "Meteor Fist",
				"Titan's Barrage", "Radiant Execution", "Magical Eye", "Claw Strikes",
				"Beast Sense", "Partial Transformation", "Predator Rush",
				"Full Beast Transformation")) {
			expectTrue(managed.contains(newSkill),
					newSkill + " must be declared by FighterSkillManager");
		}
	}

	private static java.util.Set<String> declaredConstants(String fileName)
			throws java.io.IOException {
		String source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src", "main", "java", "net", "solocraft", "util", fileName));
		java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("public static final String \\w+ = \"([^\"]+)\";")
				.matcher(source);
		java.util.Set<String> declared = new java.util.HashSet<>();
		while (matcher.find())
			declared.add(matcher.group(1));
		return declared;
	}

	/**
	 * Every ability a player can be granted must describe itself in the skill
	 * list. Anything missing falls through to a placeholder that prints the
	 * skill name twice, which is what the whole registry exists to prevent.
	 *
	 * <p>Checked against source text rather than by loading the registry, so
	 * this stays a pure regression with no Minecraft bootstrap.</p>
	 */
	private static void everyGrantableAbilityHasADescription() throws java.io.IOException {
		String source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src", "main", "java", "net", "solocraft", "util",
				"SkillDescriptionRegistry.java"));

		java.util.Set<String> described = new java.util.HashSet<>();
		java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("put\\(\"([^\"]+)\"")
				.matcher(source);
		while (matcher.find())
			described.add(matcher.group(1));

		java.util.Set<String> grantable = new java.util.LinkedHashSet<>();
		for (ClassProgressionRules rules : List.of(ClassProgressionRules.ASSASSIN,
				ClassProgressionRules.FIGHTER, ClassProgressionRules.HEALER)) {
			grantable.addAll(rules.masteryOrder());
			for (String styleKey : rules.styleKeys())
				grantable.addAll(rules.masteryOrder(styleKey));
		}
		grantable.addAll(TankerProgressionRules.MASTERY_ORDER);
		grantable.addAll(TankerProgressionRules.MASS_ORDER);

		for (String skill : grantable) {
			expectTrue(described.contains(skill),
					skill + " must have a skill-list description");
		}
	}

	private static void defaultTreeIsUnchangedForStylelessHunters() {
		// Assassin styles are gated, so every live Assassin uses this tree.
		// Changing it would silently rewrite existing characters' kits.
		expectEquals(List.of("Ghost Step", "Night Rend", "Stealth", "Flash Cut",
						"Critical Attack", "Dualwield"),
				ClassProgressionRules.ASSASSIN.masteryOrder(),
				"Styleless Assassin tree must stay as shipped");
		expectEquals(List.of("Ground Slam", "Slash Dash", "Cross Strike",
						"Slash Fury", "Sword Dance", "Sword of Light"),
				ClassProgressionRules.FIGHTER.masteryOrder(),
				"Styleless Fighter tree must stay as shipped");
		expectEquals(List.of("Heal Beam", "Haste Buff", "Purification",
						"Physical Buff", "Overheal", "Blessing Mark"),
				ClassProgressionRules.HEALER.masteryOrder(),
				"Styleless Healer tree must stay as shipped");
		expectEquals(List.of("Healing Pulse", "Camouflage", "Purifying Wave",
						"Guardian Step", "Sanctuary", "Second Wind"),
				ClassProgressionRules.HEALER.masteryOrder(ClassProgressionRules.AREA),
				"Shepherd tree");
		// The universal blessings are runestone-only and never rank grants.
		for (String blessing : List.of("Guardian Ward", "Mana Font",
				"Vitality Surge", "Divine Favor")) {
			expectTrue(ClassProgressionRules.HEALER.isClassSkill(blessing),
					blessing + " must be canonicalizable");
			for (String styleKey : List.of(ClassProgressionRules.FOCUSED,
					ClassProgressionRules.AREA)) {
				expectTrue(!ClassProgressionRules.HEALER.masteryOrder(styleKey)
								.contains(blessing),
						blessing + " must never be a native rank entitlement");
			}
		}
	}

	/**
	 * The pure rules cannot import {@code AssassinSkillManager}, so exact token
	 * agreement is checked against its source instead. A rename on either side
	 * that is not mirrored would silently stop granting an ability.
	 */
	private static void implementedTokensMatchTheRuntimeManager()
			throws java.io.IOException {
		String source = java.nio.file.Files.readString(java.nio.file.Path.of(
				"src", "main", "java", "net", "solocraft", "util",
				"AssassinSkillManager.java"));
		java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("public static final String \\w+ = \"([^\"]+)\";")
				.matcher(source);
		java.util.Set<String> declared = new java.util.HashSet<>();
		while (matcher.find())
			declared.add(matcher.group(1));

		// Every Assassin rank ability, both styles, now exists in the manager.
		for (String implemented : List.of("Ghost Step", "Night Rend", "Stealth",
				"Flash Cut", "Dualwield", "Critical Attack", "Mutilation",
				"Shadow Feint", "Silent Domain", "Zero Presence")) {
			expectTrue(declared.contains(implemented),
					implemented + " must match an AssassinSkillManager constant");
			expectTrue(ClassProgressionRules.ASSASSIN.isClassSkill(implemented),
					implemented + " must be in the Assassin vocabulary");
		}

		// Every native rank ability must be owned by AssassinSkillManager. A
		// granted ability its dispatch chain does not recognise would be dead
		// in a player's skill list.
		for (String styleKey : List.of(ClassProgressionRules.INFILTRATION,
				ClassProgressionRules.ASSAULT)) {
			for (String skill : ClassProgressionRules.ASSASSIN.masteryOrder(styleKey)) {
				expectTrue(declared.contains(skill),
						skill + " must be dispatchable by AssassinSkillManager");
			}
		}
	}

	private static void rankEntitlementsAreDeterministic() {
		// Rank 1 (E) must grant the first ability. The procedures these rules
		// replace started at "rank > 1", leaving an E-rank hunter with no kit
		// while Tanker, Ranger and Mage all granted one ability at rank 1.
		for (ClassProgressionRules rules : List.of(ClassProgressionRules.ASSASSIN,
				ClassProgressionRules.FIGHTER, ClassProgressionRules.HEALER)) {
			expectEquals(1, rules.entitlementsForRank(1).size(),
					"E rank must receive exactly one ability");
			expectEquals(6, rules.entitlementsForRank(6).size(),
					"S rank must receive the full six-ability tree");
			expectEquals(rules.masteryOrder(), rules.entitlementsForRank(6),
					"S rank must equal the declared mastery order");
			// Repeated calls must be identical: the old chains rolled dice.
			expectEquals(rules.entitlementsForRank(4), rules.entitlementsForRank(4),
					"Entitlements must be repeatable");
			// Ranks must be strictly cumulative.
			for (int rank = 2; rank <= 6; rank++)
				expectTrue(rules.entitlementsForRank(rank)
								.containsAll(rules.entitlementsForRank(rank - 1)),
						"Rank " + rank + " must contain every lower-rank ability");
		}

		expectEquals(List.of("Ground Slam", "Slash Dash", "Cross Strike"),
				ClassProgressionRules.FIGHTER.entitlementsForRank(3),
				"C-rank Fighter must receive both Slash Dash and Cross Strike");
		expectEquals(List.of("Heal Beam", "Haste Buff", "Purification"),
				ClassProgressionRules.HEALER.entitlementsForRank(3),
				"C-rank Healer must receive both Haste Buff and Purification");
	}

	private static void healerCapstoneIsBlessingMark() {
		List<String> healerKit = ClassProgressionRules.HEALER.entitlementsForRank(6);
		expectEquals("Blessing Mark", healerKit.get(healerKit.size() - 1),
				"S-rank Healer capstone must be Blessing Mark");
		expectTrue(!healerKit.contains("Sword of Light"),
				"Healer must never be awarded the Fighter Sword of Light");
	}

	private static void tokenMatchingIsExactNeverSubstring() {
		expectEquals("Critical Strike",
				ClassProgressionRules.ASSASSIN.canonicalName("Critical Strike"),
				"Critical Strike must not be absorbed into Critical Attack");
		expectTrue(!ClassProgressionRules.ASSASSIN.isClassSkill("Critical Strike"),
				"Critical Strike is a Fighter/runestone skill, not an Assassin one");
		expectTrue(!ClassProgressionRules.HEALER.hasSkill(".Overhealing Aura,", "Overheal"),
				"A longer token must not satisfy an Overheal ownership check");
		expectTrue(!ClassProgressionRules.FIGHTER.hasSkill(".Ground Slammer,", "Ground Slam"),
				"A longer token must not satisfy a Ground Slam ownership check");
		expectTrue(ClassProgressionRules.FIGHTER.hasSkill(".Ground Slam,Other,", "Ground Slam"),
				"An exact token must satisfy the ownership check");
	}

	private static void aliasesCanonicalizeWithoutLosingUnknownSkills() {
		expectEquals("Ghost Step",
				ClassProgressionRules.ASSASSIN.canonicalName("Shadowstep"),
				"Legacy Shadowstep alias must canonicalize");
		expectEquals("Night Rend",
				ClassProgressionRules.ASSASSIN.canonicalName("Backstab"),
				"Legacy Backstab alias must canonicalize");
		expectEquals("Flash Cut",
				ClassProgressionRules.ASSASSIN.canonicalName("Quickslashes"),
				"Legacy Quickslashes alias must canonicalize");

		String migrated = ClassProgressionRules.ASSASSIN.canonicalizeSkillList(
				".Shadowstep,Mana Quiver,Ghost Step,Backstab,Arise,");
		expectEquals(".Ghost Step,Mana Quiver,Night Rend,Arise,", migrated,
				"Migration must de-duplicate only class tokens and keep unknown order");

		// A runestone skill from another class must survive reconciliation.
		String preserved = ClassProgressionRules.HEALER.ensureSkill(
				".Sword Beam,Heal Beam,", "Purification");
		expectTrue(preserved.contains("Sword Beam"),
				"Reconciliation must never delete a foreign learned skill");
		expectTrue(preserved.contains("Purification"),
				"Reconciliation must add the requested entitlement");
	}

	private static void masteryWalksTheOrderWithoutRandomRetries() {
		expectEquals("Ground Slam",
				ClassProgressionRules.FIGHTER.firstMissingSkill(".Slash Fury,Sword Dance,"),
				"Mastery must fill the earliest gap first");
		expectEquals("",
				ClassProgressionRules.FIGHTER.firstMissingSkill(
						".Ground Slam,Slash Dash,Cross Strike,Slash Fury,Sword Dance,Sword of Light,"),
				"A complete kit must terminate instead of looping");
		expectEquals("Heal Beam",
				ClassProgressionRules.HEALER.firstMissingSkill("."),
				"An empty kit must start at the E-rank ability");
	}

	private static void classesOwnedElsewhereAreNotClaimed() {
		expectTrue(ClassProgressionRules.forClassId(1) != null, "Assassin is class 1");
		expectTrue(ClassProgressionRules.forClassId(3) != null, "Fighter is class 3");
		expectTrue(ClassProgressionRules.forClassId(5) != null, "Healer is class 5");
		// Mage, Tanker and Ranger keep their own progression owners.
		expectTrue(ClassProgressionRules.forClassId(2) == null, "Mage stays with MageSpellProgression");
		expectTrue(ClassProgressionRules.forClassId(4) == null, "Tanker stays with TankerProgressionRules");
		expectTrue(ClassProgressionRules.forClassId(6) == null, "Ranger stays with RangerCombatManager");
		expectTrue(ClassProgressionRules.forClassId(0) == null, "Classless hunters have no entitlements");
	}

	private static void manaBandFloorsReproduceLegacyCosts() {
		// At zero Intelligence every floor must reproduce a cost the game
		// already charged, so migrating an ability to a band is feel-neutral.
		expectEquals(20, ManaRules.costFor(0.0D, ManaRules.Band.NOMINAL), "Nominal floor");
		expectEquals(80, ManaRules.costFor(0.0D, ManaRules.Band.LOW),
				"Low floor must match Ghost Step's legacy flat cost");
		expectEquals(200, ManaRules.costFor(0.0D, ManaRules.Band.MEDIUM),
				"Medium floor must match Purification and Haste Buff");
		expectEquals(600, ManaRules.costFor(0.0D, ManaRules.Band.HIGH),
				"High floor must match Stealth, Heal Beam and Physical Buff");
		expectEquals(1500, ManaRules.costFor(0.0D, ManaRules.Band.APEX),
				"Apex floor must match Blessing Mark");
	}

	private static void intelligenceIsNeverTaxedTwice() {
		double lowIntelligence = 0.0D;
		double highIntelligence = 100.0D;

		int lowCost = ManaRules.costFor(lowIntelligence, ManaRules.Band.MEDIUM);
		int highCost = ManaRules.costFor(highIntelligence, ManaRules.Band.MEDIUM);
		expectTrue(highCost >= lowCost, "Cost may grow with maximum mana");

		// The contract that matters: cost must never outrun maximum mana, so a
		// hunter can always afford proportionally the same number of casts.
		// The old "200 + INT * 10" term broke exactly this.
		double lowShare = lowCost / ManaRules.maximumManaFor(lowIntelligence);
		double highShare = highCost / ManaRules.maximumManaFor(highIntelligence);
		expectTrue(highShare <= lowShare + 1.0e-9,
				"A band's share of the mana pool must never increase with Intelligence");

		int legacyDoubleTaxedCost = (int) (200 + highIntelligence * 10);
		expectTrue(highCost < legacyDoubleTaxedCost,
				"Banded cost must undercut the retired double-taxed formula");
	}

	private static void loadMultipliersAreBounded() {
		expectEquals(1.0D, ManaRules.effectLoad(1), "A single target adds no load");
		expectEquals(1.0D, ManaRules.effectLoad(0), "A zero-target cast cannot go below base");
		expectEquals(1.3D, round(ManaRules.effectLoad(3)), "Each extra target adds 15 percent");
		expectEquals(1.0D, ManaRules.stageLoad(1), "Stage one is the baseline");
		expectEquals(1.4D, round(ManaRules.stageLoad(5)), "Stage five is the documented ceiling");
		expectEquals(ManaRules.stageLoad(5), ManaRules.stageLoad(9),
				"Out-of-range stages must clamp rather than throw");
		expectEquals(ManaRules.stageLoad(1), ManaRules.stageLoad(-4),
				"Negative stages must clamp to the baseline");
	}

	private static double round(double value) {
		return Math.round(value * 1000.0D) / 1000.0D;
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectEquals(Object expected, Object actual, String message) {
		if (!expected.equals(actual))
			throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
	}
}
