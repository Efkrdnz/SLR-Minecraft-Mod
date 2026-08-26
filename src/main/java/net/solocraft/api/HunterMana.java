package net.solocraft.api;

import net.minecraft.world.entity.Entity;

import net.solocraft.util.ManaRules;

/**
 * Reads and spends a hunter's mana.
 *
 * <p>Spending goes through here rather than through the player attachment so an
 * ability inherits the rules the rest of the mod already follows: Creative costs
 * nothing, mana never goes negative, and the client is synchronised. An addon
 * that subtracted the field itself would desynchronise the HUD and let a
 * Creative player be charged.
 *
 * <p>Server-side only for anything that spends. The client may read to draw a
 * cost, but it must never decide that a cast was paid for.
 */
public final class HunterMana {
	private HunterMana() {
	}

	public static double current(Entity entity) {
		return ManaRules.currentMana(entity);
	}

	public static double maximum(Entity entity) {
		return ManaRules.maximumMana(entity);
	}

	/** True in Creative, where abilities are free and spending is a no-op. */
	public static boolean isFree(Entity entity) {
		return ManaRules.isFree(entity);
	}

	/** What an ability of this weight costs this hunter right now. */
	public static int cost(Entity entity, AbilityCost weight) {
		if (weight == null)
			return 0;
		return ManaRules.cost(entity, weight.band());
	}

	/**
	 * Cost for a staged or multi-target ability.
	 *
	 * @param stage            1-5; later stages of the same ability cost more
	 * @param acceptedTargets  how many targets it actually affected
	 * @param executionModifier ability-specific multiplier; 1.0 for none
	 */
	public static int cost(Entity entity, AbilityCost weight, int stage,
			int acceptedTargets, double executionModifier) {
		if (weight == null)
			return 0;
		return ManaRules.cost(entity, weight.band(), stage, acceptedTargets, executionModifier);
	}

	public static boolean canAfford(Entity entity, int cost) {
		return ManaRules.canAfford(entity, cost);
	}

	/**
	 * Charges the hunter.
	 *
	 * @return false when they could not pay, in which case nothing was deducted
	 *         and the ability must not fire
	 */
	public static boolean spend(Entity entity, int cost) {
		return ManaRules.spend(entity, cost);
	}

	/** Convenience: charge for an ability of this weight, or report failure. */
	public static boolean spend(Entity entity, AbilityCost weight) {
		return spend(entity, cost(entity, weight));
	}
}
