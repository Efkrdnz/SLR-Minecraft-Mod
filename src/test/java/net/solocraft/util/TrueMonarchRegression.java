package net.solocraft.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards the Black Heart against the cost system.
 *
 * <p>The Black Heart grants a hundred thousand maximum mana. Mana costs in this
 * mod are fractions of a pool, so any cost that reads the <em>reservoir</em>
 * instead of the Intelligence-derived basis gets multiplied by roughly
 * twenty-five the moment the reward is granted -- and the ability breaks for
 * precisely the player who just earned the capstone. Arise is the live example:
 * it prices a raise at a percentage of the pool.
 *
 * <p>These checks exist so that trap cannot be reintroduced silently.
 */
public final class TrueMonarchRegression {
	private static final Path ARISE = Path.of("src", "main", "java", "net",
			"solocraft", "procedures", "AriseSkillProcedure.java");
	private static final Path STORAGE_TIERS = Path.of("src", "main", "java", "net",
			"solocraft", "procedures", "ShadowStorageTiersProcedure.java");

	private TrueMonarchRegression() {
	}

	public static void main(String[] args) throws Exception {
		rewardsAreExactlyThreeStates();
		shadowMonarchAloneGetsTheBlackHeart();
		theGrantDoesNotChangeWhatAnythingCosts();
		ariseIsPricedOffTheBasisNotTheReservoir();
		unlimitedStorageIsASentinelNotABigNumber();
		theStorageTierSweepRespectsTheHeart();
		System.out.println("TrueMonarchRegression passed");
	}

	private static void rewardsAreExactlyThreeStates() {
		if (TrueMonarchRules.isAwakened(TrueMonarchRules.NONE))
			throw new AssertionError("the empty state must not read as awakened");
		for (String garbage : new String[] {null, "", "  ", "BLACK HEART", "gold", "true"}) {
			String normalized = TrueMonarchRules.normalize(garbage);
			if (!normalized.equals(TrueMonarchRules.NONE)
					&& !normalized.equals(TrueMonarchRules.BLACK_HEART)
					&& !normalized.equals(TrueMonarchRules.TRUE_VESSEL))
				throw new AssertionError("normalize let through " + normalized);
		}
		// Case and padding are how a hand-edited save or a command reaches this.
		if (!TrueMonarchRules.hasBlackHeart("  BLACK  ".trim().toLowerCase(java.util.Locale.ROOT)))
			throw new AssertionError("a trimmed, lowercased black heart was rejected");
	}

	private static void shadowMonarchAloneGetsTheBlackHeart() {
		if (!TrueMonarchRules.BLACK_HEART.equals(TrueMonarchRules.rewardForJob(1)))
			throw new AssertionError("the Shadow Monarch did not receive the Black Heart");
		for (int job : new int[] {0, 2, 3, 4, 5, 10, 11}) {
			if (!TrueMonarchRules.TRUE_VESSEL.equals(TrueMonarchRules.rewardForJob(job)))
				throw new AssertionError("job " + job + " received the Shadow "
						+ "Monarch's reward");
		}
		if (TrueMonarchRules.flatManaBonus(TrueMonarchRules.TRUE_VESSEL) != 0.0D)
			throw new AssertionError("True Vessel must not carry the mana grant");
		if (TrueMonarchRules.hasUnlimitedShadowStorage(TrueMonarchRules.TRUE_VESSEL))
			throw new AssertionError("True Vessel must not lift the storage ceiling");
	}

	/**
	 * The central invariant. A cost computed from the Intelligence basis has to
	 * be identical with and without the Black Heart.
	 */
	private static void theGrantDoesNotChangeWhatAnythingCosts() {
		for (double intelligence : new double[] {0.0D, 10.0D, 45.0D, 110.0D}) {
			double basis = ManaRules.maximumManaFor(intelligence);
			double reservoir = basis
					+ TrueMonarchRules.flatManaBonus(TrueMonarchRules.BLACK_HEART);
			if (reservoir <= basis)
				throw new AssertionError("the Black Heart did not grow the reservoir");
			int plain = ManaRules.costFor(intelligence, ManaRules.Band.MEDIUM, 1, 1, 1.0D);
			int withHeart = ManaRules.costFor(intelligence, ManaRules.Band.MEDIUM, 1, 1, 1.0D);
			if (plain != withHeart)
				throw new AssertionError("spell cost is not stable across the grant");
			// The reservoir must dwarf the basis, which is exactly why nothing may
			// price against it.
			if (reservoir < basis * 8.0D)
				throw new AssertionError("the grant is no longer large enough for "
						+ "this guard to be meaningful");
		}
	}

	private static void ariseIsPricedOffTheBasisNotTheReservoir() throws Exception {
		double basis = ManaRules.maximumManaFor(20.0D);
		double reservoir = basis
				+ TrueMonarchRules.flatManaBonus(TrueMonarchRules.BLACK_HEART);
		double pricedCorrectly = AriseExtractionRules.manaCostForSouls(basis, 5);
		double pricedWrongly = AriseExtractionRules.manaCostForSouls(reservoir, 5);
		if (pricedWrongly <= pricedCorrectly * 5.0D)
			throw new AssertionError("pricing Arise off the reservoir is no longer "
					+ "catastrophic, so this guard has stopped measuring anything");

		String source = Files.readString(ARISE);
		if (source.contains("ManaRules.maximumMana("))
			throw new AssertionError("AriseSkillProcedure prices a raise off the "
					+ "reservoir; with the Black Heart that costs ~"
					+ Math.round(pricedWrongly) + " instead of "
					+ Math.round(pricedCorrectly) + " mana for five shadows");
		if (!source.contains("ManaRules.costBasis("))
			throw new AssertionError("Arise no longer reads the Intelligence basis");
	}

	private static void unlimitedStorageIsASentinelNotABigNumber() {
		int free = TrueMonarchRules.freeShadowStorage(TrueMonarchRules.BLACK_HEART,
				20.0D, 19.0D);
		if (free != TrueMonarchRules.UNLIMITED_STORAGE)
			throw new AssertionError("the Black Heart did not lift the ceiling");
		// A full roster must still read as unlimited, not as the leftover capacity.
		if (TrueMonarchRules.freeShadowStorage(TrueMonarchRules.BLACK_HEART,
				20.0D, 500.0D) != TrueMonarchRules.UNLIMITED_STORAGE)
			throw new AssertionError("an over-full roster defeated unlimited storage");
		if (TrueMonarchRules.freeShadowStorage(TrueMonarchRules.NONE, 20.0D, 19.0D) != 1)
			throw new AssertionError("ordinary storage arithmetic changed");
		if (TrueMonarchRules.freeShadowStorage(TrueMonarchRules.NONE, 20.0D, 44.0D) != 0)
			throw new AssertionError("an over-full roster must report zero, not negative");
	}

	/** The tier sweep runs every 40 ticks and must not stamp over the grant. */
	private static void theStorageTierSweepRespectsTheHeart() throws Exception {
		String source = Files.readString(STORAGE_TIERS);
		if (!source.contains("hasUnlimitedShadowStorage"))
			throw new AssertionError("ShadowStorageTiersProcedure will overwrite "
					+ "unlimited storage with the level tier two seconds after the "
					+ "Black Heart is granted");
	}
}
