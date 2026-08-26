package net.solocraft.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards mass Arise.
 *
 * <p>Arise batched correctly the whole time; what capped it at one shadow was the
 * price. A flat 500 mana per soul was charged against {@code 1000 + 100 x INT},
 * and the Shadow Monarch has no reason to buy Intelligence, so a monarch near the
 * floor could pay for two souls at completely full mana and one otherwise. These
 * checks are about the affordability of a batch, not about the loop.
 */
public final class AriseMassExtractionRegression {
	private static final Path ARISE_SKILL = Path.of("src", "main", "java", "net",
			"solocraft", "procedures", "AriseSkillProcedure.java");

	private AriseMassExtractionRegression() {
	}

	public static void main(String[] args) throws Exception {
		lowIntelligenceMonarchCanRaiseASquad();
		extraSoulsAreCheaperThanTheFirst();
		affordableNeverExceedsWhatIsPaidFor();
		affordableIsMonotonicInMana();
		castChargesOnlyForShadowsActuallyRaised();
		partialBatchesReportTheLimit();
		System.out.println("AriseMassExtractionRegression passed");
	}

	/**
	 * The reported bug, as a number. A monarch who never invested Intelligence
	 * sits at the 1000 mana floor; at 80% of that bar a raise over a pile of
	 * corpses must take a squad, not a single shadow.
	 */
	private static void lowIntelligenceMonarchCanRaiseASquad() {
		double pool = ManaRules.maximumManaFor(0.0D);
		int raised = AriseExtractionRules.affordableSouls(pool * 0.8D, pool, 8);
		if (raised < 5)
			throw new AssertionError("a full-bar monarch at the Intelligence floor "
					+ "could only afford " + raised + " shadows; the old flat cost "
					+ "allowed 1");
		// The old rule, restated, so a regression back to a flat price is caught
		// rather than merely making this file look untrue.
		if ((int) Math.floor(pool * 0.8D / 500.0D) >= 5)
			throw new AssertionError("the flat 500 cost no longer reproduces the "
					+ "bug; this guard has stopped measuring anything");
	}

	private static void extraSoulsAreCheaperThanTheFirst() {
		double pool = ManaRules.maximumManaFor(20.0D);
		double first = AriseExtractionRules.manaCostForSouls(pool, 1);
		double second = AriseExtractionRules.manaCostForSouls(pool, 2) - first;
		if (!(second > 0.0D))
			throw new AssertionError("additional shadows must still cost mana");
		if (!(second < first))
			throw new AssertionError("a mass raise must be more efficient per "
					+ "shadow than repeating single raises, or players are pushed "
					+ "back to casting one at a time");
		if (AriseExtractionRules.manaCostForSouls(pool, 0) != 0.0D)
			throw new AssertionError("raising nothing must cost nothing");
	}

	private static void affordableNeverExceedsWhatIsPaidFor() {
		double pool = ManaRules.maximumManaFor(35.0D);
		for (int desired = 0; desired <= 20; desired++) {
			for (double mana = 0.0D; mana <= pool; mana += pool / 16.0D) {
				int granted = AriseExtractionRules.affordableSouls(mana, pool, desired);
				if (granted < 0 || granted > desired)
					throw new AssertionError("affordableSouls invented or lost "
							+ "shadows: " + granted + " of " + desired);
				if (AriseExtractionRules.manaCostForSouls(pool, granted) > mana)
					throw new AssertionError("granted a batch the caster cannot "
							+ "pay for at " + mana + " mana");
			}
		}
	}

	private static void affordableIsMonotonicInMana() {
		double pool = ManaRules.maximumManaFor(10.0D);
		int previous = 0;
		for (double mana = 0.0D; mana <= pool; mana += pool / 64.0D) {
			int granted = AriseExtractionRules.affordableSouls(mana, pool, 12);
			if (granted < previous)
				throw new AssertionError("more mana afforded fewer shadows");
			previous = granted;
		}
		if (previous <= 1)
			throw new AssertionError("a full bar still affords only " + previous
					+ " shadow");
	}

	/** Resisted and overwhelming targets must not be billed. */
	private static void castChargesOnlyForShadowsActuallyRaised() throws Exception {
		String source = Files.readString(ARISE_SKILL);
		// Matched on the argument rather than on the whole call, so renaming the
		// mana accessor cannot make this silently stop checking anything. Which
		// pool the price derives from is TrueMonarchRegression's job.
		// Anchored on the deduction assignment specifically: the first
		// manaCostForSouls call in this file is the shift-scan affordability
		// check, which correctly asks about a single soul.
		int deduction = source.indexOf("double cost = AriseExtractionRules.manaCostForSouls(");
		if (deduction < 0)
			throw new AssertionError("Arise no longer prices its deduction with "
					+ "manaCostForSouls");
		String call = source.substring(deduction,
				Math.min(source.length(), deduction + 160));
		if (!call.contains("revived"))
			throw new AssertionError("the mana deduction must be sized from the "
					+ "revived count, not the attempted count");
		if (call.contains("attempted"))
			throw new AssertionError("the mana deduction bills attempts, so "
					+ "resisted and overwhelming targets are charged for");
		if (source.contains("MANA_PER_SOUL"))
			throw new AssertionError("the flat per-soul cost is back in "
					+ "AriseSkillProcedure");
	}

	/**
	 * A cast that leaves corpses behind has to say so. Silently raising a subset
	 * is what turned a mana shortfall into a report that Arise raises one shadow.
	 */
	private static void partialBatchesReportTheLimit() throws Exception {
		String source = Files.readString(ARISE_SKILL);
		if (!source.contains("leftBehind"))
			throw new AssertionError("no partial-batch accounting in the cast");
		if (!source.contains("shadow storage full") || !source.contains("not enough mana"))
			throw new AssertionError("a capped cast must name which limit stopped "
					+ "it; both storage and mana have to be distinguishable");
		int reported = source.indexOf("leftBehind > 0");
		if (reported < 0)
			throw new AssertionError("leftBehind is computed but never surfaced "
					+ "to the player");
	}
}
