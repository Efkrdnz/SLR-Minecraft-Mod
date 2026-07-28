
package net.solocraft.item;

import net.solocraft.procedures.RunestoneShieldBashRCProcedure;
import net.solocraft.procedures.TankerProgressionHelper;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

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

public class RunestoneShieldBashItem extends Item {
	public RunestoneShieldBashItem() {
		super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		TankerTooltipHelper.addRunestone(list, TankerProgressionHelper.SHIELD_BASH,
				"tooltip.sololeveling.tanker.runestone.shield_bash.description",
				"E", 180, "2.5", 8);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		if (!world.isClientSide())
			RunestoneShieldBashRCProcedure.execute(entity, stack);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}
}
