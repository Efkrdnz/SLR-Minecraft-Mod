
package net.solocraft.item;

import net.solocraft.procedures.DemonKingsLongSwordLivingEntityIsHitWithToolProcedure;
import net.solocraft.procedures.DemonKingsLongSwordHasItemGlowingEffectProcedure;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import java.util.List;

public class DemonKingsLongSwordItem extends SwordItem {
	public DemonKingsLongSwordItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 10f;
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
		}, 3, -2.8f, new Item.Properties().fireResistant());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		DemonKingsLongSwordLivingEntityIsHitWithToolProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, sourceentity, itemstack);
		return retval;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A76LEVEL OF DIFFICULTY: S"));
		list.add(Component.literal("\u00A76ATTACK +350"));
		list.add(Component.literal("\u00A76TYPE: LONGSWORD"));
		list.add(Component.literal("\u00A76A LONGSWORD CONTANING THE POWERS OF BARAN, THE DEMON KING. THE EFFECT \"STORM OF THE FLAMES\" WILL ACTIVATE EVERY TIME THIS SWORD IS SWUNG."));
		list.add(Component.literal("\u00A76EFFECT \"STORM OF THE FLAMES\" : A VIOLENT THUNDERSTORM IS SUMMONED WITHIN A SPECIFIED AREA"));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		Entity entity = Minecraft.getInstance().player;
		return DemonKingsLongSwordHasItemGlowingEffectProcedure.execute(entity);
	}
}
