package net.solocraft.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Rank-aware damage rules for Shadow Igris's authored sword attacks.
 *
 * <p>Igris's animations apply damage directly instead of using
 * {@link net.minecraft.world.entity.Mob#doHurtTarget(Entity)}. That means the
 * attack attribute granted by shadow levels, Strength and other modifiers must
 * be folded into each authored impact explicitly.</p>
 */
public final class ShadowIgrisCombatBalance {
	public static final String APPLIED_RANK_TAG = "sl_shadow_applied_rank";
	public static final double ATTACK_ATTRIBUTE_WEIGHT = 0.40D;
	private static final double MAX_ABILITY_DAMAGE = 1_000_000.0D;

	private ShadowIgrisCombatBalance() {
	}

	public static float abilityDamage(Entity source, double baseDamage) {
		double attackDamage = source instanceof LivingEntity living
				? living.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0D;
		int rank = source == null ? ShadowMonarchManager.RANK_NORMAL
				: source.getPersistentData().getInt(APPLIED_RANK_TAG);
		return (float) abilityDamage(baseDamage, attackDamage, rank);
	}

	/**
	 * Pure form used by deterministic balance checks.
	 */
	public static double abilityDamage(double baseDamage, double attackDamage,
			int rank) {
		double authoredDamage = finiteNonNegative(baseDamage);
		double progressedAttack = finiteNonNegative(attackDamage);
		double result = (authoredDamage
				+ progressedAttack * ATTACK_ATTRIBUTE_WEIGHT)
				* rankMultiplier(rank);
		if (!Double.isFinite(result))
			return MAX_ABILITY_DAMAGE;
		return Math.min(MAX_ABILITY_DAMAGE, Math.max(0.0D, result));
	}

	/**
	 * Rank remains a meaningful combat promotion without making early Igris
	 * ranks skip the level curve. Igris naturally begins at Knight.
	 */
	public static double rankMultiplier(int rank) {
		return switch (Math.max(ShadowMonarchManager.RANK_NORMAL,
				Math.min(ShadowMonarchManager.RANK_GRAND_MARSHAL, rank))) {
			case ShadowMonarchManager.RANK_ELITE -> 1.08D;
			case ShadowMonarchManager.RANK_KNIGHT -> 1.18D;
			case ShadowMonarchManager.RANK_ELITE_KNIGHT -> 1.32D;
			case ShadowMonarchManager.RANK_GENERAL -> 1.50D;
			case ShadowMonarchManager.RANK_MARSHAL -> 1.75D;
			case ShadowMonarchManager.RANK_GRAND_MARSHAL -> 2.15D;
			default -> 1.0D;
		};
	}

	private static double finiteNonNegative(double value) {
		return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
	}
}
