package net.solocraft.entity.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Preserves the 1.20 melee-reach extension points used by the generated
 * entities while running on the 1.21 melee goal implementation.
 */
public class LegacyMeleeAttackGoal extends MeleeAttackGoal {
	public LegacyMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
		super(mob, speedModifier, followingTargetEvenIfNotSeen);
	}

	/**
	 * The pre-1.21 hook retained for the many entities with deliberately
	 * different attack ranges.
	 */
	protected double getAttackReachSqr(LivingEntity target) {
		float reach = this.mob.getBbWidth() * 2.0F;
		return (double) (reach * reach + target.getBbWidth());
	}

	/**
	 * The pre-1.21 two-argument hook retained for goals that trigger an attack
	 * animation before delegating to vanilla damage handling.
	 */
	protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
		if (distanceSqr <= this.getAttackReachSqr(target) && this.isTimeToAttack()) {
			this.resetAttackCooldown();
			this.mob.swing(InteractionHand.MAIN_HAND);
			this.mob.doHurtTarget(target);
		}
	}

	@Override
	protected void checkAndPerformAttack(LivingEntity target) {
		this.checkAndPerformAttack(target, this.mob.distanceToSqr(target));
	}

	@Override
	protected boolean canPerformAttack(LivingEntity target) {
		return this.isTimeToAttack() && this.mob.distanceToSqr(target) <= this.getAttackReachSqr(target);
	}
}
