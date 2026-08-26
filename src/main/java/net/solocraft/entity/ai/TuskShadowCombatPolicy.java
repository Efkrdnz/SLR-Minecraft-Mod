package net.solocraft.entity.ai;

/**
 * Pure tuning rules for Shadow Tusk's grounded artillery behaviour.
 *
 * <p>Keeping these decisions free of Minecraft state makes the important range,
 * scaling, and recovery thresholds directly regression-testable.</p>
 */
public final class TuskShadowCombatPolicy {
	public static final double RETREAT_RANGE = 7.0D;
	public static final double PREFERRED_MIN_RANGE = 10.0D;
	public static final double PREFERRED_MAX_RANGE = 20.0D;
	public static final double MAX_CAST_RANGE = 24.0D;
	public static final double OWNER_LEASH_RANGE = 32.0D;

	public static final int REPATH_STALLED_TICKS = 20;
	public static final int ESCAPE_STALLED_TICKS = 40;
	public static final int RECALL_STALLED_TICKS = 70;

	private TuskShadowCombatPolicy() {
	}

	public static boolean shouldRetreat(double surfaceDistance) {
		return surfaceDistance < RETREAT_RANGE;
	}

	public static boolean shouldApproach(double surfaceDistance,
			boolean hasLineOfSight) {
		return !hasLineOfSight || surfaceDistance > PREFERRED_MAX_RANGE;
	}

	public static boolean isUsefulCastingPosition(double surfaceDistance,
			boolean hasLineOfSight) {
		return hasLineOfSight && surfaceDistance >= RETREAT_RANGE
				&& surfaceDistance <= MAX_CAST_RANGE;
	}

	public static float soulFlameDamage(double attackDamage) {
		return scaledDamage(attackDamage, 0.70D, 7.0D);
	}

	public static float curseFieldDamage(double attackDamage) {
		return scaledDamage(attackDamage, 0.40D, 5.0D);
	}

	public static float groundSmashDamage(double attackDamage) {
		return scaledDamage(attackDamage, 1.0D, 12.0D);
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

	private static float scaledDamage(double attackDamage, double multiplier,
			double minimum) {
		double safeAttack = Double.isFinite(attackDamage)
				? Math.max(0.0D, attackDamage) : 0.0D;
		return (float) Math.max(minimum, safeAttack * multiplier);
	}

	public enum RecoveryStage {
		NONE,
		REPATH,
		ESCAPE,
		RECALL
	}
}
