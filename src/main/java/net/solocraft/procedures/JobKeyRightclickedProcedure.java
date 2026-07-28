package net.solocraft.procedures;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class JobKeyRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		if (!JobChangeQuestManager.isOverworld(player)) {
			player.displayClientMessage(Component.literal(
					"\u00A7cThe Job Change Quest can only be unlocked in the Minecraft Overworld."), true);
			return;
		}
		if (JobChangeQuestManager.isFinished(player)) {
			player.displayClientMessage(Component.literal("\u00A75Job Change Quest is already complete."), true);
			return;
		}
		if (!JobChangeQuestManager.unlock(player))
			return;
		ItemStack key = new ItemStack(SololevelingModItems.JOB_KEY.get());
		player.getInventory().clearOrCountMatchingItems(stack -> key.getItem() == stack.getItem(), 1, player.inventoryMenu.getCraftSlots());
		player.displayClientMessage(Component.literal("\u00A75Job Change Quest unlocked in System > Quests."), true);
	}
}
