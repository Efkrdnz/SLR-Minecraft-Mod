package net.solocraft.util;

/**
 * Dependency-free balance and state rules for the Sillad boss encounter.
 *
 * <p>Keeping phase, multiplayer, and Frostbite bounds independent of Minecraft
 * state makes the encounter's most important safeguards directly testable.</p>
 */
public final class SilladBossRules {
	public static final int PHASE_ONE = 1;
	public static final int PHASE_TWO = 2;
	public static final int PHASE_THREE = 3;
	public static final float PHASE_TWO_HEALTH_FRACTION = 0.70F;
	public static final float PHASE_THREE_HEALTH_FRACTION = 0.40F;

	public static final int MAX_ENGAGED_PLAYERS = 4;
	public static final float ADDITIONAL_PLAYER_MITIGATION = 0.45F;
	public static final float ADDITIONAL_PLAYER_DAMAGE = 0.05F;

	public static final int MAX_FROSTBITE = 5;
	public static final int FROSTBITE_LIFETIME = 120;
	public static final int PLAYER_ROOT_TICKS = 12;
	public static final int NON_PLAYER_ROOT_TICKS = 50;
	public static final int FREEZE_IMMUNITY_TICKS = 120;
	public static final int BRITTLE_DELAY_TICKS = 20;
	public static final int BRITTLE_DURATION_TICKS = 40;
	public static final int EXECUTION_WINDOW_TICKS = 200;
	public static final int PRISON_DURATION_TICKS = 480;
	public static final float PRISON_HIT_CAP = 16.0F;
	public static final float PRISON_REGEN_FRACTION = 0.06F;
	public static final float PRISON_INTEGRITY_PER_RANK = 24.0F;
	public static final int FROZEN_DOMAIN_COLUMN_BUDGET = 48;

	/** No ordinary player-authored hit may skip a complete encounter section. */
	public static final float MAX_SINGLE_HIT = 125.0F;

	private SilladBossRules() {
	}

	/** Returns phase 1, 2, or 3 for the supplied health state. */
	public static int phaseForHealth(float health, float maximumHealth) {
		if (!Float.isFinite(maximumHealth) || maximumHealth <= 0.0F
				|| !Float.isFinite(health))
			return PHASE_ONE;
		float fraction = Math.max(0.0F, Math.min(maximumHealth, health))
				/ maximumHealth;
		if (fraction <= PHASE_THREE_HEALTH_FRACTION)
			return PHASE_THREE;
		if (fraction <= PHASE_TWO_HEALTH_FRACTION)
			return PHASE_TWO;
		return PHASE_ONE;
	}

	/**
	 * Effective-health scaling for a locked encounter roster. Physical maximum
	 * health stays below Minecraft's normal attribute ceiling.
	 */
	public static float incomingDamageMultiplier(int engagedPlayers) {
		int additional = clampEngagedPlayers(engagedPlayers) - 1;
		return 1.0F / (1.0F + ADDITIONAL_PLAYER_MITIGATION * additional);
	}

	/** Raw boss damage scales only slightly; multiplayer pressure comes from AI. */
	public static float outgoingDamageMultiplier(int engagedPlayers) {
		int additional = clampEngagedPlayers(engagedPlayers) - 1;
		return 1.0F + ADDITIONAL_PLAYER_DAMAGE * additional;
	}

	public static int clampEngagedPlayers(int engagedPlayers) {
		return Math.max(1, Math.min(MAX_ENGAGED_PLAYERS, engagedPlayers));
	}

	public static int clampFrostbite(int stacks) {
		return Math.max(0, Math.min(MAX_FROSTBITE, stacks));
	}

	/** Freeze immunity still permits setup, but never another immediate Freeze. */
	public static int frostbiteCap(boolean freezeImmune) {
		return freezeImmune ? MAX_FROSTBITE - 1 : MAX_FROSTBITE;
	}

	public static int addFrostbite(int current, int amount,
			boolean freezeImmune) {
		long safeGain = Math.max(0, amount);
		long total = (long) clampFrostbite(current) + safeGain;
		return (int) Math.min(frostbiteCap(freezeImmune), total);
	}

	public static float clampIncomingHit(float amount) {
		if (!Float.isFinite(amount) || amount <= 0.0F)
			return 0.0F;
		return Math.min(MAX_SINGLE_HIT, amount);
	}

	public static float prisonIntegrity(int engagedPlayers,
			float targetMaximumHealth) {
		return prisonIntegrity(engagedPlayers, targetMaximumHealth, 0);
	}

	public static float prisonIntegrity(int engagedPlayers,
			float targetMaximumHealth, int shadowRank) {
		float health = Float.isFinite(targetMaximumHealth)
				? Math.max(0.0F, targetMaximumHealth) : 0.0F;
		int rank = Math.max(0, Math.min(6, shadowRank));
		return 72.0F + 12.0F * (clampEngagedPlayers(engagedPlayers) - 1)
				+ Math.min(30.0F, health * 0.04F)
				+ PRISON_INTEGRITY_PER_RANK * rank;
	}

	public static float prisonRegeneration(float maximumIntegrity) {
		if (!Float.isFinite(maximumIntegrity) || maximumIntegrity <= 0.0F)
			return 0.0F;
		return maximumIntegrity * PRISON_REGEN_FRACTION;
	}

	public static float prisonDot(float targetMaximumHealth,
			int engagedPlayers) {
		float health = Float.isFinite(targetMaximumHealth)
				? Math.max(0.0F, targetMaximumHealth) : 0.0F;
		float base = Math.max(3.0F, Math.min(8.0F,
				2.0F + health * 0.015F));
		return base * outgoingDamageMultiplier(engagedPlayers);
	}

	public static double prisonManaDrain(double maximumMana,
			int ownedPrisons) {
		if (ownedPrisons <= 0)
			return 0.0D;
		double mana = Double.isFinite(maximumMana)
				? Math.max(0.0D, maximumMana) : 0.0D;
		double base = Math.max(12.0D,
				Math.min(30.0D, Math.ceil(mana * 0.0075D)));
		return Math.min(48.0D,
				base + 3.0D * Math.max(0, ownedPrisons - 1));
	}

	public static float prisonerAttackDamage(double attackDamage) {
		double safeAttack = Double.isFinite(attackDamage)
				? Math.max(0.0D, attackDamage) : 0.0D;
		return (float) Math.max(4.0D,
				Math.min(10.0D, 3.5D + safeAttack * 0.15D));
	}

	/** Permanent physical arena-control radius for each encounter phase. */
	public static int frozenDomainRadius(int phase) {
		return switch (Math.max(PHASE_ONE, Math.min(PHASE_THREE, phase))) {
			case PHASE_TWO -> 23;
			case PHASE_THREE -> 30;
			default -> 16;
		};
	}

	/** Passive Frostbite cadence inside Sillad's physically frozen domain. */
	public static int frozenDomainPulseInterval(int phase) {
		return switch (Math.max(PHASE_ONE, Math.min(PHASE_THREE, phase))) {
			case PHASE_TWO -> 30;
			case PHASE_THREE -> 22;
			default -> 40;
		};
	}

	/**
	 * Armor-bypassing payoff after a complete Freeze. Players receive a hard
	 * safety cap; non-player combatants take a Monarch-scale percentage hit.
	 */
	public static float trueFrostExecutionDamage(float targetMaximumHealth,
			boolean player, int phase, int engagedPlayers) {
		if (!Float.isFinite(targetMaximumHealth)
				|| targetMaximumHealth <= 0.0F)
			return 0.0F;
		float health = targetMaximumHealth;
		float amount;
		if (player) {
			amount = 18.0F + 2.0F * Math.max(0,
					Math.min(PHASE_THREE, phase) - PHASE_ONE);
			amount *= outgoingDamageMultiplier(engagedPlayers);
			return Math.min(26.0F, amount);
		}
		amount = 24.0F + Math.min(48.0F, health * 0.10F);
		amount *= 1.0F + 0.10F * Math.max(0,
				Math.min(PHASE_THREE, phase) - PHASE_ONE);
		return Math.min(86.0F,
				amount * outgoingDamageMultiplier(engagedPlayers));
	}

	/** Canonical semantic actions shared by AI, animation, and regressions. */
	public enum Action {
		IDLE,
		PHASE_TRANSITION,
		FROST_CLEAVE,
		ICE_SPEAR,
		FLASH_FREEZE,
		FROZEN_PATH,
		FROST_COUNTER,
		STILLNESS_DECREE,
		SPIRE_CAGE,
		WHITEOUT_PROCESSION,
		WINTER_REMEMBERS,
		CROWN_OF_WINTER,
		ABSOLUTE_ZERO,
		GLACIAL_EXECUTION,
		FROST_STEP
	}
}
