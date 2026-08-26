
package net.solocraft.item;

import net.solocraft.procedures.RunestoneTankLeapRCProcedure;
import net.solocraft.procedures.TankerProgressionHelper;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

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

public class RunestoneTankLeapItem extends Item {
	public RunestoneTankLeapItem() {
		super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		TankerTooltipHelper.addRunestone(list, TankerProgressionHelper.TANK_LEAP,
				"tooltip.sololeveling.tanker.runestone.tank_leap.description",
				"C", 260, "4", 14);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		if (!world.isClientSide())
			RunestoneTankLeapRCProcedure.execute(entity, stack);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}
}
