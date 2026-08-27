package net.solocraft.entity.ai;

/**
 * Pure tuning rules for Shadow Iron's guardian-tank combat behaviour.
 *
 * <p>Keeping the important numbers independent of Minecraft state makes the
 * damage, mitigation, Taunt, rescue, and stuck-recovery contracts directly
 * regression-testable.</p>
 */
public final class ShadowIronCombatPolicy {
	public static final double MELEE_REACH = 3.4D;
	public static final double OWNER_LEASH_RANGE = 28.0D;
	public static final double GUARDIAN_SCAN_RANGE = 16.0D;
	public static final double TAUNT_RANGE = 12.0D;
	public static final int TAUNT_TARGET_CAP = 8;

	public static final int ATTACK_IMPACT_TICK = 10;
	public static final int ATTACK_END_TICK = 20;
	public static final int BLOCK_ACTIVE_START_TICK = 4;
	public static final int BLOCK_ACTIVE_END_TICK = 12;
	public static final int BLOCK_END_TICK = 25;
	public static final int ROAR_PULSE_TICK = 12;
	public static final int ROAR_END_TICK = 35;

	public static final int ATTACK_COOLDOWN_TICKS = 26;
	public static final int BLOCK_COOLDOWN_TICKS = 80;
	public static final int ROAR_COOLDOWN_TICKS = 360;
	public static final int DEFAULT_INTERCEPT_COOLDOWN_TICKS = 200;
	public static final int PROTECT_INTERCEPT_COOLDOWN_TICKS = 160;

	public static final int NORMAL_TAUNT_TICKS = 100;
	public static final int ELITE_TAUNT_TICKS = 60;
	public static final int BOSS_TAUNT_TICKS = 30;
	public static final int PASSIVE_CHALLENGE_TICKS = 60;
	public static final int PASSIVE_BOSS_CHALLENGE_TICKS = 20;

	public static final int REPATH_STALLED_TICKS = 20;
	public static final int ESCAPE_STALLED_TICKS = 40;
	public static final int RECALL_STALLED_TICKS = 70;

	private ShadowIronCombatPolicy() {
	}

	public static float primaryDamage(double attackDamage, boolean counter) {
		double safeAttack = Double.isFinite(attackDamage)
				? Math.max(0.0D, attackDamage) : 0.0D;
		return (float) Math.max(4.0D, safeAttack * (counter ? 1.15D : 1.0D));
	}

	public static float secondaryDamage(double attackDamage) {
		double safeAttack = Double.isFinite(attackDamage)
				? Math.max(0.0D, attackDamage) : 0.0D;
		return (float) Math.max(1.5D, safeAttack * 0.35D);
	}

	/** Fraction of incoming damage removed by a correctly faced shield. */
	public static float blockReduction(boolean projectile, boolean boss) {
		if (boss)
			return projectile ? 0.45F : 0.35F;
		return projectile ? 0.70F : 0.60F;
	}

	/** Fraction removed while Iron is fortified after a successful roar. */
	public static float fortificationReduction(int tauntedTargets) {
		if (tauntedTargets <= 0)
			return 0.0F;
		return Math.min(0.24F, 0.12F + Math.min(6, tauntedTargets) * 0.02F);
	}

	public static int tauntDuration(boolean boss, boolean elite) {
		return boss ? BOSS_TAUNT_TICKS
				: elite ? ELITE_TAUNT_TICKS : NORMAL_TAUNT_TICKS;
	}

	public static int passiveChallengeDuration(boolean boss) {
		return boss ? PASSIVE_BOSS_CHALLENGE_TICKS
				: PASSIVE_CHALLENGE_TICKS;
	}

	public static boolean shouldEmergencyIntercept(boolean protectCommand,
			double ownerHealthRatio, double threatDistance, boolean projectile) {
		if (projectile)
			return true;
		return protectCommand || ownerHealthRatio <= 0.60D
				|| threatDistance <= 3.5D;
	}

	/** Damage left on the owner after a successful rescue. */
	public static float ownerDamageFraction(boolean boss) {
		return boss ? 0.35F : 0.15F;
	}

	/** Raw damage handed to Iron before his directional block mitigation. */
	public static float redirectedDamageFraction(boolean boss) {
		return boss ? 0.75F : 0.65F;
	}

	public static RecoveryStage recoveryStage(int stalledTicks) {
		if (stalledTicks >= RECALL_STALLED_TICKS)
			return RecoveryStage.RECALL;
		if (stalledTicks >= ESCAPE_STALLED_TICKS)
			return RecoveryStage.ESCAPE;
		if (stalledTicks >= REPATH_STALLED_TICKS)
			return RecoveryStage.REPATH;
		return RecoveryStage.NONE;
	}

	public enum RecoveryStage {
		NONE,
		REPATH,
		ESCAPE,
		RECALL
	}
}
