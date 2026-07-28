package net.solocraft.util;

/**
 * Pure, order-independent entitlement predicates shared by live progression
 * reconciliation and regression tests.
 */
final class VesselProgressionRules {
	static final int SHADOW_MONARCH_JOB = 1;
	static final double MONARCHS_DOMAIN_FLOOR = 10.0D;
	static final double SHADOW_EXCHANGE_STORAGE = 30.0D;
	static final double SHADOW_MANIFESTATION_LEVEL = 120.0D;
	static final double SHADOW_MANIFESTATION_STORAGE = 60.0D;

	private VesselProgressionRules() {
	}

	static boolean hasMonarchsDomain(int resolvedJob, double dkcCleared) {
		return resolvedJob == SHADOW_MONARCH_JOB && dkcCleared >= MONARCHS_DOMAIN_FLOOR;
	}

	static boolean canUnlockShadowExchange(int resolvedJob, boolean alreadyUnlocked,
			double shadowStorageUsage) {
		return resolvedJob == SHADOW_MONARCH_JOB
				&& (alreadyUnlocked || shadowStorageUsage >= SHADOW_EXCHANGE_STORAGE);
	}

	static boolean canUnlockShadowManifestation(int resolvedJob, boolean alreadyUnlocked,
			double level, double shadowStorageUsage, boolean shadowExchange, double dkcCleared) {
		if (resolvedJob != SHADOW_MONARCH_JOB)
			return false;
		if (alreadyUnlocked)
			return true;
		return level >= SHADOW_MANIFESTATION_LEVEL
				&& shadowStorageUsage >= SHADOW_MANIFESTATION_STORAGE
				&& shadowExchange
				&& dkcCleared >= MONARCHS_DOMAIN_FLOOR;
	}
}
