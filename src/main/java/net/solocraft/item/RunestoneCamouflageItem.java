package net.solocraft.item;

import net.solocraft.procedures.RunestoneCamouflageRCProcedure;

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

public class RunestoneCamouflageItem extends Item {
	public RunestoneCamouflageItem() {
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
		list.add(Component.literal("Right click this rune to obtain \"Camouflage\""));
		list.add(Component.literal("Drop threat and turn translucent long enough to reposition."));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		RunestoneCamouflageRCProcedure.execute(entity, ar.getObject());
		return ar;
	}
}
