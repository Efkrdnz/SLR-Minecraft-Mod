package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;

import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class RandomSpecialBoxRightclickedProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		double rand = 0;
		double rand2 = 0;
		if (entity instanceof Player _player) {
			ItemStack _stktoremove = itemstack;
			_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
		}
		rand = Math.random();
		if (rand <= 0.2) {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(SololevelingModItems.KAMISH_WRATH.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(SololevelingModItems.KAMISH_WRATH_2.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
		} else if (rand <= 0.4) {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(SololevelingModItems.DEMON_KINGS_DAGGER.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(SololevelingModItems.DEMON_KINGS_DAGGER.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
		} else if (rand <= 0.6) {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(SololevelingModItems.DEMON_KINGS_LONG_SWORD.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
		} else if (rand <= 1) {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(SololevelingModItems.MANA_CRYSTAL_S.get());
				_setstack.setCount(Mth.nextInt(RandomSource.create(), 1, 4));
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
		}
	}
}
