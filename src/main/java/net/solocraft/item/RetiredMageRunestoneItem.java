package net.solocraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Save-compatible shell for runestone IDs whose unbound Mage skills were
 * retired. Keeping the registry entries prevents existing inventory stacks
 * from becoming missing mappings, while the item has no skill-grant action.
 */
public final class RetiredMageRunestoneItem extends Item {
	private final String formerSkill;

	public RetiredMageRunestoneItem(String formerSkill) {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		this.formerSkill = formerSkill;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
			TooltipFlag flag) {
		tooltip.add(Component.literal("Retired Mage runestone")
				.withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.literal(formerSkill + " was not tied to a Mage specialization.")
				.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("This item no longer grants or casts a skill.")
				.withStyle(ChatFormatting.RED));
	}
}
