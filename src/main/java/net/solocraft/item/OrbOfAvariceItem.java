package net.solocraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A-rank magic amplifier formed from Vulcan's petrified blood. */
public class OrbOfAvariceItem extends Item {
	public OrbOfAvariceItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("A-RANK MAGIC ITEM")
				.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		tooltip.add(Component.literal("Desire for Destruction")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		tooltip.add(Component.literal("Doubles magic damage while held.")
				.withStyle(ChatFormatting.RED));
		tooltip.add(Component.literal("Mana costs are increased by 50% while held.")
				.withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.literal("Fire magic burns blue under its influence.")
				.withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.empty());
		tooltip.add(Component.literal("A sphere of petrified blood taken from Vulcan.")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
