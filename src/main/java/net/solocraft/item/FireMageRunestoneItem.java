package net.solocraft.item;

import net.solocraft.util.FireMageSpellManager;
import net.solocraft.util.MageSpellProgression;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** One implementation shared by the staged Fire Mage runestones. */
public class FireMageRunestoneItem extends Item {
	private final String skill;
	private final String description;

	public FireMageRunestoneItem(String skill, String description) {
		super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
		this.skill = skill;
		this.description = description;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Unlocks " + skill).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		tooltip.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Evolves through five output stages with Rank and Intelligence.")
				.withStyle(ChatFormatting.DARK_RED));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			if (MageSpellProgression.unlockSkill(player, skill, false)) {
				if (!player.isCreative())
					stack.shrink(1);
				player.displayClientMessage(Component.literal("Skill acquired: " + skill)
						.withStyle(ChatFormatting.GOLD), false);
			} else {
				player.displayClientMessage(Component.literal("You already know " + skill + ".")
						.withStyle(ChatFormatting.RED), true);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	public String getSkill() {
		return skill;
	}
}
