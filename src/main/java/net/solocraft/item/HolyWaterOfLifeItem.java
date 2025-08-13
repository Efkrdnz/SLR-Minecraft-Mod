
package net.solocraft.item;

import net.solocraft.procedures.PotionPlayerFinishesUsingItemProcedure;

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

public class HolyWaterOfLifeItem extends Item {
	public HolyWaterOfLifeItem() {
		super(new Item.Properties().stacksTo(6).rarity(Rarity.EPIC));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A76ITEM CLASS: S"));
		list.add(Component.literal("\u00A76TYPE: CONSUMABLE"));
		list.add(Component.literal("\u00A76A MYSTERIOUS POTION THAT CAN CURE ANY DISEASE WITH POWERFUL MAGIC. THE EFFECT WILL ONLY TAKE PLACE WHEN THE ENTIRE BOTTLE IS BEEN CONSUMED"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		PotionPlayerFinishesUsingItemProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity, ar.getObject());
		return ar;
	}
}
