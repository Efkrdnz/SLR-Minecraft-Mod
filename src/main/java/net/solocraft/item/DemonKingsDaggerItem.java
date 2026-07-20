
package net.solocraft.item;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import java.util.List;

public class DemonKingsDaggerItem extends SwordItem {
	public DemonKingsDaggerItem() {
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
		}, 3, -2.1f, new Item.Properties());
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A76ITEM CLASS: S"));
		list.add(Component.literal("\u00A76TYPE: DAGGER"));
		list.add(Component.literal("\u00A76ATTACK +220"));
		list.add(Component.literal("\u00A76DAGGER OBTAINED FROM THE DEMON KING BARAN"));
		list.add(Component.literal("\u00A76SET EFFECT WILL ACTIVATE IF BOTH \"DEMON KING'S DAGGERS\" ARE EQUIPPED AT THE SAME TIME."));
		list.add(Component.literal(".."));
		list.add(Component.literal("\u00A76SET EFFECT \"TWO AS ONE\":"));
		list.add(Component.literal("\u00A76ADDITIONAL ATTACK WIL APPLY TO EACH DAGGER BY THE AMOUNT OF STRENGTH STAT."));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get());
	}
}
