package net.solocraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.Unbreakable;

import net.solocraft.util.LegacyWeaponDurabilityRules;

/** Preserves the pre-1.21 SwordItem constructor while using data components. */
public abstract class LegacySwordItem extends SwordItem {
	protected LegacySwordItem(Tier tier, int attackDamage, float attackSpeed,
			Item.Properties properties) {
		super(tier, withLegacyDurability(tier, properties).attributes(
				SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
	}

	/**
	 * A zero-use legacy tier meant "indestructible" in 1.20, where a max damage of
	 * zero made {@code isDamageableItem()} false. 1.21 still copies
	 * {@link Tier#getUses()} into the max-damage component, but TieredItem does it
	 * *after* the properties handed to this constructor, so writing a durability
	 * here is simply overwritten. The resulting stack has max damage 0 plus a
	 * damage component, which counts as damageable: the first hit raises damage to
	 * 1, and 1 >= 0 breaks the weapon. Marking it unbreakable is the one flag that
	 * survives TieredItem's write, and it restores the 1.20 behaviour of no
	 * durability bar and no breaking. Positive-use tiers need nothing; TieredItem
	 * already handles those correctly.
	 */
	private static Item.Properties withLegacyDurability(Tier tier,
			Item.Properties properties) {
		if (LegacyWeaponDurabilityRules.requiresUnbreakable(tier.getUses()))
			properties.component(DataComponents.UNBREAKABLE, new Unbreakable(false));
		return properties;
	}
}
