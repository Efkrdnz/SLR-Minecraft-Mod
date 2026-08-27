package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Deterministic regressions for Shadow Igris's rank-aware authored attacks and
 * the intended Grand Marshal matchup against the Ant King.
 */
public final class ShadowIgrisBalanceRegression {
	private static final Path PROCEDURES = Path.of("src", "main", "java",
			"net", "solocraft", "procedures");
	private static final double ANT_KING_HEALTH = 200.0D;
	private static final double ANT_KING_ARMOR = 90.0D;
	private static final double IGRIS_BASE_HEALTH = 90.0D;
	private static final double IGRIS_HEALTH_PER_LEVEL = 8.0D;
	private static final double IGRIS_ATTACK_PER_LEVEL = 1.25D;

	private ShadowIgrisBalanceRegression() {
	}

	public static void main(String[] args) throws IOException {
		rankScalingIsStrictAndBounded();
		shadowLevelsFeedEveryAuthoredImpact();
		grandMarshalIgrisDecisivelyBeatsTheAntKing();
		lowerRanksDoNotSkipTheProgressionCurve();
	}

	private static void rankScalingIsStrictAndBounded() {
		double previous = 0.0D;
		for (int rank = ShadowMonarchManager.RANK_NORMAL;
				rank <= ShadowMonarchManager.RANK_GRAND_MARSHAL; rank++) {
			double current = ShadowIgrisCombatBalance.rankMultiplier(rank);
			expectTrue(current > previous,
					"Every Igris rank must grant a strictly stronger sword multiplier");
			previous = current;
		}
		expectNear(2.15D, previous, 0.0001D,
				"Grand Marshal needs the curated cap, not unbounded rank math");
		expectNear(previous,
				ShadowIgrisCombatBalance.rankMultiplier(Integer.MAX_VALUE),
				0.0001D, "Malformed high ranks must clamp to Grand Marshal");
	}

	private static void shadowLevelsFeedEveryAuthoredImpact()
			throws IOException {
		double levelOne = ShadowIgrisCombatBalance.abilityDamage(11.0D,
				0.0D, ShadowMonarchManager.RANK_KNIGHT);
		double levelTwenty = ShadowIgrisCombatBalance.abilityDamage(11.0D,
				attackAtLevel(20), ShadowMonarchManager.RANK_GENERAL);
		double levelForty = ShadowIgrisCombatBalance.abilityDamage(11.0D,
				attackAtLevel(40), ShadowMonarchManager.RANK_GRAND_MARSHAL);
		expectTrue(levelTwenty > levelOne * 2.0D
						&& levelForty > levelTwenty * 2.0D,
				"Igris's authored attacks must consume both level attack and rank");

		int routedImpacts = 0;
		for (String file : new String[] {"ShadowIgrisSpinProcedure.java",
				"ShadowIgrisStabProcedure.java",
				"ShadowIgrisSlamProcedure.java"}) {
			String source = Files.readString(PROCEDURES.resolve(file));
			int damageSites = count(source, ".hurt(");
			int balancedSites = count(source,
					"ShadowIgrisCombatBalance.abilityDamage(entity,");
			expectTrue(damageSites == balancedSites,
					file + " must route every authored damage site through the balance helper");
			routedImpacts += balancedSites;
		}
		expectEquals(5, routedImpacts,
				"Spin, stab and slam must expose all five balanced impacts");
	}

	private static void grandMarshalIgrisDecisivelyBeatsTheAntKing() {
		int level = 40;
		// Marshal+ shadows have permanent Strength II and Resistance II from
		// Monarch's Domain. Strength II contributes six attack attribute points.
		double attack = attackAtLevel(level) + 6.0D;
		double igrisDps = expectedIgrisDps(attack,
				ShadowMonarchManager.RANK_GRAND_MARSHAL);
		double antKingTimeToFall = ANT_KING_HEALTH / igrisDps;

		double health = healthAtLevel(level);
		double antKingDps = expectedAntKingDps(true);
		double igrisTimeToFall = health / antKingDps;

		expectTrue(antKingTimeToFall < 30.0D,
				"Grand Marshal Igris must cut through Ant King's 90 armor in a practical fight");
		expectTrue(antKingTimeToFall < igrisTimeToFall * 0.50D,
				"Grand Marshal Igris needs a decisive safety margin against Ant King");
	}

	private static void lowerRanksDoNotSkipTheProgressionCurve() {
		double knightOpeningHit = ShadowIgrisCombatBalance.abilityDamage(
				13.5D, attackAtLevel(1), ShadowMonarchManager.RANK_KNIGHT);
		expectTrue(knightOpeningHit < 16.0D,
				"Level-one Knight Igris must remain near the authored early-game damage");

		int level = 20;
		double generalTimeToKill = ANT_KING_HEALTH / expectedIgrisDps(
				attackAtLevel(level), ShadowMonarchManager.RANK_GENERAL);
		double generalTimeToFall = healthAtLevel(level)
				/ expectedAntKingDps(false);
		expectTrue(generalTimeToKill > generalTimeToFall,
				"General-rank Igris must not inherit the Grand Marshal Ant King benchmark");
	}

	private static double expectedIgrisDps(double attack, int rank) {
		double spin = mitigatedIgrisHit(10.0D, attack, rank)
				+ mitigatedIgrisHit(13.5D, attack, rank);
		double stab = mitigatedIgrisHit(11.0D, attack, rank)
				+ mitigatedIgrisHit(9.0D, attack, rank);
		double slam = mitigatedIgrisHit(9.0D, attack, rank);
		double meanDamage = (spin + stab + slam) / 3.0D;
		double meanCycleTicks = (62.0D + 62.0D + 44.0D) / 3.0D;
		return meanDamage * 20.0D / meanCycleTicks;
	}

	private static double mitigatedIgrisHit(double baseDamage, double attack,
			int rank) {
		return armorReduced(ShadowIgrisCombatBalance.abilityDamage(baseDamage,
				attack, rank), ANT_KING_ARMOR, 0.0D);
	}

	private static double expectedAntKingDps(boolean domainResistance) {
		double resistance = domainResistance ? 0.60D : 1.0D;
		double melee = armorReduced(18.0D, 24.0D, 0.0D) * resistance;
		double upslam = 8.0D * resistance / 6.0D;
		double downslam = armorReduced(12.0D, 24.0D, 0.0D)
				* resistance / 17.5D;
		return melee + upslam + downslam;
	}

	private static double armorReduced(double damage, double armor,
			double toughness) {
		double divisor = 2.0D + toughness / 4.0D;
		double protection = Math.min(20.0D,
				Math.max(armor / 5.0D, armor - damage / divisor));
		return damage * (1.0D - protection / 25.0D);
	}

	private static double attackAtLevel(int level) {
		return Math.max(0, level - 1) * IGRIS_ATTACK_PER_LEVEL;
	}

	private static double healthAtLevel(int level) {
		return IGRIS_BASE_HEALTH
				+ Math.max(0, level - 1) * IGRIS_HEALTH_PER_LEVEL;
	}

	private static int count(String source, String needle) {
		int matches = 0;
		for (int index = source.indexOf(needle); index >= 0;
				index = source.indexOf(needle, index + needle.length()))
			matches++;
		return matches;
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + " (expected " + expected
					+ ", got " + actual + ")");
	}

	private static void expectNear(double expected, double actual,
			double tolerance, String message) {
		if (Math.abs(expected - actual) > tolerance)
			throw new AssertionError(message + " (expected " + expected
					+ ", got " + actual + ")");
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
