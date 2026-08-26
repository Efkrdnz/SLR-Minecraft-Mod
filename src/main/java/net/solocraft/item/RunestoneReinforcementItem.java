
package net.solocraft.item;

import net.solocraft.procedures.RunestoneReinforcementRCProcedure;
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

public class RunestoneReinforcementItem extends Item {
	public RunestoneReinforcementItem() {
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
		TankerTooltipHelper.addRunestone(list, TankerProgressionHelper.REINFORCEMENT,
				"tooltip.sololeveling.tanker.runestone.reinforcement.description",
				"B", 400, "6", 22);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		if (!world.isClientSide())
			RunestoneReinforcementRCProcedure.execute(entity, stack);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}
}
