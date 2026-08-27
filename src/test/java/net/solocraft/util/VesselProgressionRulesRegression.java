package net.solocraft.util;

/**
 * Dependency-free regression checks for progression rules. This remains a
	 * small Java entry point so the NeoForge project does not need a second test
 * framework solely for pure entitlement predicates.
 */
public final class VesselProgressionRulesRegression {
	private VesselProgressionRulesRegression() {
	}

	public static void main(String[] args) {
		monarchsDomainIsIndependentOfCompletionOrder();
		shadowPrerequisitesAreReconciledFromCurrentState();
	}

	private static void monarchsDomainIsIndependentOfCompletionOrder() {
		expectFalse(VesselProgressionRules.hasMonarchsDomain(0, 10),
				"Clearing floor 10 before choosing a vessel must only record the milestone");
		expectTrue(VesselProgressionRules.hasMonarchsDomain(1, 10),
				"Choosing Shadow Monarch after floor 10 must grant Monarch's Domain");

		expectFalse(VesselProgressionRules.hasMonarchsDomain(1, 9),
				"Choosing Shadow Monarch first must not grant the floor-10 reward early");
		expectTrue(VesselProgressionRules.hasMonarchsDomain(1, 10),
				"Clearing floor 10 after choosing Shadow Monarch must grant the same reward");
		expectFalse(VesselProgressionRules.hasMonarchsDomain(3, 20),
				"DKC completion must not grant a Shadow-only skill to another job");
	}

	private static void shadowPrerequisitesAreReconciledFromCurrentState() {
		expectTrue(VesselProgressionRules.canUseShadowExchangeRunestone(1, false),
				"The Shadow Monarch must be able to consume Baran's runestone");
		expectFalse(VesselProgressionRules.canUseShadowExchangeRunestone(0, false),
				"Another job must not be able to consume Baran's Shadow Exchange reward");
		expectFalse(VesselProgressionRules.canUseShadowExchangeRunestone(1, true),
				"An already-unlocked Shadow Exchange must not consume another runestone");

		expectFalse(VesselProgressionRules.canUnlockShadowManifestation(
				0, false, 120, 60, true, 10),
				"Completing every prerequisite before the job must not grant another job's skill");
		expectTrue(VesselProgressionRules.canUnlockShadowManifestation(
				1, false, 120, 60, true, 10),
				"Choosing Shadow Monarch last must reconcile Spiritual Body Manifestation");
		expectFalse(VesselProgressionRules.canUnlockShadowManifestation(
				1, false, 120, 60, true, 9),
				"Manifestation must retain its floor-10 boundary");
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
