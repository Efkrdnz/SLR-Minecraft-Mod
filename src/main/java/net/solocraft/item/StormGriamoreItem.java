
package net.solocraft.item;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import java.util.List;

public class StormGriamoreItem extends Item {
	public StormGriamoreItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get());
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A76LEVEL OF DIFFICULTY: S"));
		list.add(Component.literal("\u00A76TYPE: MAGIC BOOK"));
		list.add(Component.literal("\u00A76ATTACK: +225"));
		list.add(Component.literal("\u00A76A MYSTICAL TOME THAT COMMANDS STORMS AND LIGHTNING, UNLEASHING THE FURY OF THE SKIES WITH EVERY PAGE TURNED. WIELD ITS POWER, BUT BEWARE\u2014IT MAY DRAW YOU INTO THE STORM'S CHAOTIC EMBRACE"));
		list.add(Component.literal(""));
	}
}
