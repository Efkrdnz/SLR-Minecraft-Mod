package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

public class RewardNameReturn3Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		double rand = 0;
		String reward = "";
		String reward_list = "";
		reward = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_3;
		if (reward.startsWith("SP")) {
			try {
				int amount = Integer.parseInt(reward.substring(2));
				return "\u00A7l" + amount + " Skill Points";
			} catch (NumberFormatException e) {
				return "\u00A7lInvalid Reward";
			}
		}
		if (reward.startsWith("GOLD")) {
			try {
				int amount = Integer.parseInt(reward.substring(4));
				return "\u00A7l" + amount + " System Golds";
			} catch (NumberFormatException e) {
				return "\u00A7lInvalid Reward";
			}
		}
		if (reward.startsWith("ITEM:")) {
			String itemResourceLocation = reward.substring(5);
			try {
				ResourceLocation itemLocation = new ResourceLocation(itemResourceLocation);
				Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
				if (item != null && item != Items.AIR) {
					ItemStack itemStack = new ItemStack(item);
					return "\u00A7lItem: " + itemStack.getDisplayName().getString();
				} else {
					return "\u00A7lUnknown Item";
				}
			} catch (Exception e) {
				System.err.println("[SoloLeveling] Failed to display item reward: " + reward);
				return "\u00A7lInvalid Item";
			}
		}
		if ((reward).equals("FR")) {
			return "\u00A7lFull Recovery";
		}
		if ((reward).equals("ITEMBOX")) {
			return "\u00A7lRandom Item";
		}
		return "\u00A7lCollected!";
	}
}
