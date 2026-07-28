
package net.solocraft.item;

import net.solocraft.procedures.TankerStarterpackRightclickedProcedure;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
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

public class TankerStarterpackItem extends Item {
	public TankerStarterpackItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.translatable("tooltip.sololeveling.tanker.starter.baseline")
				.withStyle(ChatFormatting.GOLD));
		list.add(Component.translatable("tooltip.sololeveling.tanker.starter.contents")
				.withStyle(ChatFormatting.GRAY));
		list.add(Component.translatable("tooltip.sololeveling.tanker.starter.shield")
				.withStyle(ChatFormatting.BLUE));
		list.add(Component.translatable("tooltip.sololeveling.tanker.starter.redemption")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		if (!world.isClientSide() && entity instanceof ServerPlayer player)
			TankerStarterpackRightclickedProcedure.execute(player, stack);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}
}
