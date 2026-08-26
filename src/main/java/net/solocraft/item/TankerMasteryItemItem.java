
package net.solocraft.item;

import net.solocraft.procedures.TankerProgressionHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TankerMasteryItemItem extends Item {
	public TankerMasteryItemItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.translatable("tooltip.sololeveling.tanker.mastery.grant")
				.withStyle(ChatFormatting.GOLD));
		list.add(Component.translatable("tooltip.sololeveling.tanker.mastery.order")
				.withStyle(ChatFormatting.GRAY));
		list.add(Component.translatable("tooltip.sololeveling.tanker.mastery.preserve")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		if (!world.isClientSide()) {
			if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)
					|| !TankerProgressionHelper.isTanker(player)) {
				entity.displayClientMessage(Component.translatable(
						"message.sololeveling.tanker.mastery.wrong_class"), true);
				return InteractionResultHolder.fail(stack);
			}
			String granted = TankerProgressionHelper.grantNextMasterySkill(player);
			if (granted.isEmpty()) {
				player.displayClientMessage(Component.translatable(
						"message.sololeveling.tanker.mastery.complete"), false);
			} else {
				if (!player.isCreative())
					stack.shrink(1);
				player.displayClientMessage(Component.translatable(
						"message.sololeveling.tanker.skill_gained", granted), false);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}
}
