
package net.solocraft.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BarukasDaggerItem extends SwordItem {
	public BarukasDaggerItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 7f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2f, new Item.Properties());
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A76LEVEL OF DIFFICULTY: A"));
		list.add(Component.literal("\u00A76TYPE: DAGGER"));
		list.add(Component.literal("\u00A76ATTACK: +110"));
		list.add(Component.literal("\u00A76AGILITY: +10"));
		list.add(Component.literal("\u00A76THIS DAGGER WAS ONCE USED BY WARLORD \"BARUKA\" "));
		list.add(Component.literal("\u00A76THIS WEAPON IS INFUSED WITH MAGIC THAT REDUCES THE WEIGHT OF ITS WIELDER"));
		list.add(Component.literal("\u00A76ALLOWING FOR GREATER AGILITY"));
	}
}
