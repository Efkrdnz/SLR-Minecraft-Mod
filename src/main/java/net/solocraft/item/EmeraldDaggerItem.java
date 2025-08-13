
package net.solocraft.item;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;

import java.util.List;

public class EmeraldDaggerItem extends SwordItem {
	public EmeraldDaggerItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 8f;
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
		}, 3, -2.3f, new Item.Properties().fireResistant());
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A79Level Of Difficulty: \u00A7fA"));
		list.add(Component.literal("\u00A79Type: \u00A7fDAGGER"));
		list.add(Component.literal("\u00A79Attack: \u00A7f+125"));
		list.add(Component
				.literal("\u00A79\"A BLADE FORGED FROM CRYSTALLIZED MOONLIGHT, SHIMMERING WITH ETHEREAL BRILLIANCE. IN THE HANDS OF A SKILLED HUNTER, IT BENDS LIGHT ITSELF, STRIKING FROM UNSEEN ANGLES AND LEAVING ONLY THE GLOW OF ITS AFTERIMAGE"));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}
}
