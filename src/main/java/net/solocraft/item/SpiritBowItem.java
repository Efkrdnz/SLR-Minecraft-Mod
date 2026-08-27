
package net.solocraft.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BowItem;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Ranger bow that now follows the same draw/release path as every other bow.
 * Mana Quiver interception is handled centrally by RangerCombatManager.
 */
public class SpiritBowItem extends BowItem {
	public SpiritBowItem() {
		super(new Properties().stacksTo(1).durability(1024).rarity(Rarity.RARE));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("tooltip.sololeveling.spirit_bow.mana"));
		list.add(Component.translatable("tooltip.sololeveling.spirit_bow.lore"));
	}
}
