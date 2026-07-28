package net.solocraft.procedures;

import java.util.List;

/** Dependency-free regression checks for Tanker progression and aliases. */
public final class TankerProgressionRulesRegression {
	private TankerProgressionRulesRegression() {
	}

	public static void main(String[] args) {
		canonicalAliasesAreExactAndTokenSafe();
		rankEntitlementsAreDeterministic();
		masteryAlwaysFindsTheFirstMissingSkill();
	}

	private static void canonicalAliasesAreExactAndTokenSafe() {
		expectEquals("Willpower", TankerProgressionRules.canonicalName("will_power"),
				"Snake-case Willpower alias must canonicalize");
		expectEquals("Tank Leap", TankerProgressionRules.canonicalName("Leap Strike"),
				"Legacy Leap Strike alias must canonicalize");
		expectEquals("Shield Bash", TankerProgressionRules.canonicalName("ShieldBash"),
				"Unspaced Shield Bash alias must canonicalize");
		expectEquals("Not Taunt Aura", TankerProgressionRules.canonicalName("Not Taunt Aura"),
				"Substring matches must never become Tanker skills");

		String migrated = TankerProgressionRules.canonicalizeSkillList(
				".Will Power,Other Skill,will_power,ShieldBash,Unknown Skill,");
		expectEquals(".Willpower,Other Skill,Shield Bash,Unknown Skill,", migrated,
				"Migration must de-duplicate only canonical Tanker tokens and preserve unknown order");
	}

	private static void rankEntitlementsAreDeterministic() {
		expectEquals(List.of("Shield Bash"),
				TankerProgressionRules.entitlementsForRank(1),
				"E rank must receive the shield baseline");
		expectEquals(List.of("Shield Bash", "Taunt", "Tank Leap", "Reinforcement"),
				TankerProgressionRules.entitlementsForRank(4),
				"B rank must receive every preceding deterministic entitlement");
		expectEquals(TankerProgressionRules.MASTERY_ORDER,
				TankerProgressionRules.entitlementsForRank(6),
				"S rank must receive all six skills");
	}

	private static void masteryAlwaysFindsTheFirstMissingSkill() {
		expectEquals("Shield Bash",
				TankerProgressionRules.firstMissingSkill(".Tank Leap,WillPower,"),
				"Old-order learned skills must remain while mastery fills the baseline first");
		expectEquals("Tank Leap",
				TankerProgressionRules.firstMissingSkill(
						".Shield Bash,Taunt,Willpower,Protection Mark,"),
				"Mastery must select the first missing canonical entry");
		expectEquals("",
				TankerProgressionRules.firstMissingSkill(
						".Shield Bash,Taunt,Tank Leap,Reinforcement,Willpower,Protection Mark,"),
				"A complete kit must terminate without random retry loops");
	}

	private static void expectEquals(Object expected, Object actual, String message) {
		if (!expected.equals(actual))
			throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
	}
}
