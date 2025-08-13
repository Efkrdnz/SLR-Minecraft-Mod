package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.entity.HunterEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;

public class HunterRightClickedOnEntityProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!(sourceentity instanceof Player player))
			return;
		String hunterRank = entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "";
		boolean canRecruit = false;
		switch (hunterRank) {
			case "S" :
				canRecruit = hasAndConsumeItems(player, SololevelingModItems.SHADOW_ARMOR_BOOTS.get(), 2);
				break;
			case "A" :
				canRecruit = hasAndConsumeItems(player, SololevelingModItems.SHADOW_ARMOR_BOOTS.get(), 1);
				break;
			case "B" :
				canRecruit = hasAndConsumeItems(player, SololevelingModItems.MANA_CRYSTAL_A.get(), 5);
				break;
			case "C" :
				canRecruit = hasAndConsumeItems(player, SololevelingModItems.MANA_CRYSTAL_A.get(), 3);
				break;
			case "D" :
				canRecruit = hasAndConsumeItems(player, SololevelingModItems.MANA_CRYSTAL_B.get(), 10);
				break;
		}
		if (canRecruit && entity instanceof TamableAnimal _toTame) {
			_toTame.tame(player);
			// Optional: Add success message or sound effect here
		} else if (!canRecruit) {
			// Optional: Add failure message here (not enough items)
			// player.displayClientMessage(Component.literal("Not enough mana stones!"), true);
		}
	}

	private static boolean hasAndConsumeItems(Player player, net.minecraft.world.item.Item item, int count) {
		Inventory inventory = player.getInventory();
		// First, check if player has enough items
		int foundCount = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() == item) {
				foundCount += stack.getCount();
				if (foundCount >= count) {
					break;
				}
			}
		}
		// If not enough items, return false
		if (foundCount < count) {
			return false;
		}
		// If we have enough, consume the items
		int remaining = count;
		for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() == item) {
				int toRemove = Math.min(stack.getCount(), remaining);
				stack.shrink(toRemove);
				remaining -= toRemove;
			}
		}
		return true;
	}
}
