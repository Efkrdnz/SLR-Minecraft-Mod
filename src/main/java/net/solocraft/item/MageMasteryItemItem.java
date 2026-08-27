
package net.solocraft.item;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.MasterylvlupMageProcedure;

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

public class MageMasteryItemItem extends Item {
	public MageMasteryItemItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		list.add(Component.literal("+1 Mage mastery level"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		double playerClass = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).Classes;
		if (playerClass != 2.0D) {
			if (!world.isClientSide())
				entity.displayClientMessage(Component.literal("Only a Mage can use Mage mastery."), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!world.isClientSide())
			MasterylvlupMageProcedure.execute(entity);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}
}
