
package net.solocraft.item;

import net.solocraft.procedures.KasakasVenomFangsLivingEntityIsHitWithToolProcedure;
import net.solocraft.init.SololevelingModMobEffects;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

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

public class KasakasVenomFangsItem extends LegacySwordItem {
	public KasakasVenomFangsItem() {
		super(new Tier() {
			public int getUses() {
				return 6000;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 5f;
			}

			public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
				return net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL;
			}

			public int getEnchantmentValue() {
				return 0;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.1f, new Item.Properties());
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		boolean retval = super.hurtEnemy(itemstack, entity, sourceentity);
		KasakasVenomFangsLivingEntityIsHitWithToolProcedure.execute(entity.level(), entity, sourceentity, 3);
		return retval;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.literal("Level Of Difficulty: \u00A73C"));
		list.add(Component.literal("Type: Dagger"));
		list.add(Component.literal("Attack: \u00A76+25"));
		list.add(Component.literal("A DAGGER MADE FROM KASAKAS VENOM FANG. KASAKAS VENOM STILL REMAINS WITHIN IT, THUS CAUSING PARALYZATION AND BLEEDING WHEN USED UPON AN OPONENT."));
		list.add(Component.literal("-Effect: \"\u00A76PARALYZE\": Effective against C-rank targets and below."));
		list.add(Component.literal("-Effect: \"\u00A76BLEED\": The oponent will be bleeding for certain rate."));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE);
	}
}
