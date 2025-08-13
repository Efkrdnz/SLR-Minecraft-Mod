
package net.solocraft.item;

import net.solocraft.procedures.KasakasVenomFangsLivingEntityIsHitWithToolProcedure;
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

public class KasakasAwakenedVenomFangItem extends SwordItem {
	public KasakasAwakenedVenomFangItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 9f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 0;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -1.8f, new Item.Properties());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		KasakasVenomFangsLivingEntityIsHitWithToolProcedure.execute(entity.level(), entity, sourceentity);
		return retval;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A79Level Of Difficulty: \u00A76S"));
		list.add(Component.literal("\u00A79Type: \u00A76Dagger"));
		list.add(Component.literal("\u00A79Attack: \u00A76+200"));
		list.add(Component.literal("\u00A79A DAGGER MADE FROM KASAKAS VENOM FANG. KASAKAS VENOM STILL REMAINS WITHIN IT, THUS CAUSING PARALYZATION AND BLEEDING WHEN USED UPON AN OPONENT."));
		list.add(Component.literal("\u00A79-Effect: \"\u00A76PARALYZE\": \u00A79The oponent will be paralyzed for certain rate."));
		list.add(Component.literal("\u00A79-Effect: \"\u00A76BLEED\": \u00A79The oponent will be bleeding for certain rate."));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		Entity entity = Minecraft.getInstance().player;
		return DemonKingsLongSwordHasItemGlowingEffectProcedure.execute(entity);
	}
}
