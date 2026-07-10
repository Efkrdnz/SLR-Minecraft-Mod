package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;
import java.util.Map;

/**
 * Fills the potion shop's 9 display slots each tick while it is open.
 * Layout: rows HP / MP / FTG, columns Small / Medium / Large.
 * Slot index i = row*3 + col — matches StorePotionNewButtonMessage id (i + 1).
 */
public class StorePotionNewFillProcedure {
	public static void execute(Entity entity) {
		if (entity == null || !(entity instanceof Player player))
			return;
		if (!(player.containerMenu instanceof Supplier<?> current) || !(current.get() instanceof Map<?, ?> slots))
			return;
		Item[] potions = {
				SololevelingModItems.SMALL_HEALTH_POTION.get(), SololevelingModItems.MEDIUM_HEALTH_POTION.get(), SololevelingModItems.LARGE_HEALTH_POTION.get(),
				SololevelingModItems.SMALL_MANA_POTION.get(), SololevelingModItems.MEDIUM_MANA_POTION.get(), SololevelingModItems.LARGE_MANA_POTION.get(),
				SololevelingModItems.SMALL_FATIGUE_POTION.get(), SololevelingModItems.MEDIUM_FATIGUE_POTION.get(), SololevelingModItems.LARGE_FATIGUE_POTION.get()
		};
		for (int i = 0; i < potions.length; i++) {
			Object slot = slots.get(i);
			if (slot instanceof Slot s) {
				ItemStack stack = new ItemStack(potions[i]);
				stack.setCount(1);
				s.set(stack);
			}
		}
		player.containerMenu.broadcastChanges();
	}
}
