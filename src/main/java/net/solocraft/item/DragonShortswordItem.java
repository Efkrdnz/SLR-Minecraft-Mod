package net.solocraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.List;

/** A fast National Rank weapon forged from a sovereign dragon's fang. */
public class DragonShortswordItem extends SwordItem {
	private static final Tier NATIONAL_RANK_TIER = new Tier() {
		@Override
		public int getUses() {
			return 2800;
		}

		@Override
		public float getSpeed() {
			return 5.0F;
		}

		@Override
		public float getAttackDamageBonus() {
			return 16.0F;
		}

		@Override
		public int getLevel() {
			return 4;
		}

		@Override
		public int getEnchantmentValue() {
			return 20;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.of();
		}
	};

	public DragonShortswordItem() {
		super(NATIONAL_RANK_TIER, 4, -1.9F,
				new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		super.appendHoverText(stack, level, lines, flag);
		lines.add(Component.literal("NATIONAL RANK WEAPON")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		lines.add(Component.literal("DRAGON SHORTSWORD")
				.withStyle(ChatFormatting.YELLOW));
		lines.add(Component.literal("POWER OUTPUT  ")
				.withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal("[MAXIMUM]")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
		lines.add(Component.empty());
		lines.add(Component.literal("Forged from the fang of a sovereign dragon.")
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		lines.add(Component.literal("Its edge yields only to overwhelming power.")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
