package net.solocraft.util;

/**
 * Pure model of the 1.21 tiered-item durability pipeline.
 *
 * <p>TieredItem's constructor calls {@code properties.durability(tier.getUses())}
 * on the properties it is handed, so it always has the last word on the
 * max-damage and damage components. Anything a subclass writes beforehand is
 * overwritten. Only flags TieredItem does not touch survive.
 *
 * <p>Modelling that ordering here lets the regression exercise the real break
 * condition without bootstrapping Minecraft.
 */
public final class LegacyWeaponDurabilityRules {
	private LegacyWeaponDurabilityRules() {
	}

	/**
	 * A zero-use legacy tier meant "indestructible" in 1.20, where max damage of
	 * zero made the stack non-damageable. In 1.21 that same stack is damageable
	 * with a max damage of zero, so it must be stated unbreakable instead.
	 */
	public static boolean requiresUnbreakable(int tierUses) {
		return tierUses <= 0;
	}

	/** What TieredItem ends up writing, whatever a subclass wrote first. */
	public static int effectiveMaxDamage(int tierUses) {
		return Math.max(tierUses, 0);
	}

	/**
	 * ItemStack#isDamageableItem. The max-damage and damage components are always
	 * present after TieredItem's write, so only the unbreakable flag decides it.
	 */
	public static boolean isDamageable(boolean unbreakable) {
		return !unbreakable;
	}

	/**
	 * The hit number on which ItemStack#hurtAndBreak destroys the weapon, or
	 * {@link Integer#MAX_VALUE} when it never breaks. Each entity hit costs one
	 * durability and the stack breaks once accumulated damage reaches max damage.
	 */
	public static int breaksOnHit(int tierUses, boolean unbreakable) {
		if (!isDamageable(unbreakable))
			return Integer.MAX_VALUE;
		return Math.max(effectiveMaxDamage(tierUses), 1);
	}
}
