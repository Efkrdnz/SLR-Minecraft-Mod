package net.solocraft.util;

import java.util.List;

/**
 * Dependency-free characterization checks for Tanker skill naming, migration,
 * rank entitlement, and deterministic mastery order.
 */
public final class TankerSkillManagerProgressionRegression {
	private TankerSkillManagerProgressionRegression() {
	}

	public static void main(String[] args) {
		canonicalNamesAndAliasesAreStable();
		encodedSkillListsMigrateTokenSafely();
		rankEntitlementsAreCumulative();
		masteryAlwaysChoosesTheFirstMissingSkill();
		unlockOrderIsStableAndImmutable();
	}

	private static void canonicalNamesAndAliasesAreStable() {
		expectEquals("Taunt", TankerSkillManager.TAUNT, "Taunt constant");
		expectEquals("Reinforcement", TankerSkillManager.REINFORCEMENT, "Reinforcement constant");
		expectEquals("Tank Leap", TankerSkillManager.TANK_LEAP, "Tank Leap constant");
		expectEquals("Shield Bash", TankerSkillManager.SHIELD_BASH, "Shield Bash constant");
		expectEquals("Willpower", TankerSkillManager.WILLPOWER, "Willpower constant");
		expectEquals("Protection Mark", TankerSkillManager.PROTECTION_MARK, "Protection Mark constant");

		expectEquals(TankerSkillManager.WILLPOWER,
				TankerSkillManager.canonicalName(" Will Power "), "spaced Willpower alias");
		expectEquals(TankerSkillManager.WILLPOWER,
				TankerSkillManager.canonicalName("WillPower"), "joined Willpower alias");
		expectEquals(TankerSkillManager.WILLPOWER,
				TankerSkillManager.canonicalName("will_power"), "underscored Willpower alias");
		expectEquals(TankerSkillManager.TANK_LEAP,
				TankerSkillManager.canonicalName("Leap Strike"), "legacy Leap Strike alias");
		expectEquals(TankerSkillManager.TANK_LEAP,
				TankerSkillManager.canonicalName("tankleap"), "joined Tank Leap alias");
		expectEquals(TankerSkillManager.SHIELD_BASH,
				TankerSkillManager.canonicalName("shieldbash"), "joined Shield Bash alias");
		expectEquals(TankerSkillManager.PROTECTION_MARK,
				TankerSkillManager.canonicalName("ProtectionMark"), "joined Protection Mark alias");

		expectEquals(TankerSkillManager.REINFORCEMENT,
				TankerSkillManager.canonicalName(" rEiNfOrCeMeNt "), "canonical case normalization");
		expectEquals("Taunting",
				TankerSkillManager.canonicalName(" Taunting "), "unknown tokens must not match substrings");
		expectEquals("", TankerSkillManager.canonicalName(null), "null canonicalizes to an empty token");
	}

	private static void encodedSkillListsMigrateTokenSafely() {
		String encoded = ".Will Power,Unknown Skill,willpower,TankLeap,Unknown Skill,"
				+ "ShieldBash,ProtectionMark,";
		String expected = ".Willpower,Unknown Skill,Tank Leap,Unknown Skill,"
				+ "Shield Bash,Protection Mark,";

		String migrated = TankerSkillManager.canonicalizeSkillList(encoded);
		expectEquals(expected, migrated,
				"Tanker aliases must de-duplicate while unknown token order and duplicates are preserved");
		expectEquals(expected, TankerSkillManager.canonicalizeSkillList(migrated),
				"skill-list migration must be idempotent");
	}

	private static void rankEntitlementsAreCumulative() {
		expectList(List.of(TankerSkillManager.SHIELD_BASH),
				TankerSkillManager.entitlementsForRank(0),
				"a class-assigned rank below E still receives the baseline");
		expectList(List.of(TankerSkillManager.SHIELD_BASH),
				TankerSkillManager.entitlementsForRank(1), "E-rank entitlement");
		expectList(List.of(TankerSkillManager.SHIELD_BASH, TankerSkillManager.TAUNT),
				TankerSkillManager.entitlementsForRank(2), "D-rank entitlement");
		expectList(List.of(TankerSkillManager.SHIELD_BASH, TankerSkillManager.TAUNT,
						TankerSkillManager.TANK_LEAP),
				TankerSkillManager.entitlementsForRank(3), "C-rank entitlement");
		expectList(List.of(TankerSkillManager.SHIELD_BASH, TankerSkillManager.TAUNT,
						TankerSkillManager.TANK_LEAP, TankerSkillManager.REINFORCEMENT),
				TankerSkillManager.entitlementsForRank(4), "B-rank entitlement");
		expectList(List.of(TankerSkillManager.SHIELD_BASH, TankerSkillManager.TAUNT,
						TankerSkillManager.TANK_LEAP, TankerSkillManager.REINFORCEMENT,
						TankerSkillManager.WILLPOWER),
				TankerSkillManager.entitlementsForRank(5), "A-rank entitlement");
		expectList(TankerSkillManager.UNLOCK_ORDER,
				TankerSkillManager.entitlementsForRank(6), "S-rank entitlement");
		expectList(TankerSkillManager.UNLOCK_ORDER,
				TankerSkillManager.entitlementsForRank(99), "rank values above S remain capped");
	}

	private static void masteryAlwaysChoosesTheFirstMissingSkill() {
		expectEquals(TankerSkillManager.SHIELD_BASH,
				TankerSkillManager.firstMissingSkill("."), "empty learned set");
		expectEquals(TankerSkillManager.SHIELD_BASH,
				TankerSkillManager.firstMissingSkill(".Taunt,"),
				"old-order Taunt does not skip the baseline skill");
		expectEquals(TankerSkillManager.TANK_LEAP,
				TankerSkillManager.firstMissingSkill(".ShieldBash,taunt,"),
				"aliases count as learned without changing deterministic order");
		expectEquals(TankerSkillManager.WILLPOWER,
				TankerSkillManager.firstMissingSkill(
						".Protection Mark,Reinforcement,Tank Leap,Shield Bash,Taunt,"),
				"acquisition order does not affect the first missing result");
		expectEquals("",
				TankerSkillManager.firstMissingSkill(
						".Shield Bash,Taunt,Tank Leap,Reinforcement,Willpower,Protection Mark,"),
				"a complete kit has no mastery grant candidate");
	}

	private static void unlockOrderIsStableAndImmutable() {
		List<String> expected = List.of(
				TankerSkillManager.SHIELD_BASH,
				TankerSkillManager.TAUNT,
				TankerSkillManager.TANK_LEAP,
				TankerSkillManager.REINFORCEMENT,
				TankerSkillManager.WILLPOWER,
				TankerSkillManager.PROTECTION_MARK);
		List<String> actual = TankerSkillManager.UNLOCK_ORDER;
		expectList(expected, actual, "canonical unlock order");
		try {
			actual.add("Unexpected Skill");
			throw new AssertionError("unlockOrder must return an immutable list");
		} catch (UnsupportedOperationException expectedException) {
			// Expected: callers cannot mutate the manager's canonical order.
		}
	}

	private static void expectList(List<String> expected, List<String> actual, String context) {
		if (!expected.equals(actual))
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}

	private static void expectEquals(String expected, String actual, String context) {
		if (expected == null ? actual != null : !expected.equals(actual))
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}
}
