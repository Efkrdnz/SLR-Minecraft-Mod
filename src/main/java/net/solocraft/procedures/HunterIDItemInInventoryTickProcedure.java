package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public class HunterIDItemInInventoryTickProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if ((itemstack.getOrCreateTag().getString("Rank")).equals("")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank == 1) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A7aE");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank == 2) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A70D");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank == 3) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A7aC");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank == 4) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A79B");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank == 5) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A79A");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank == 6) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A76S");
			}
		}
		if ((itemstack.getOrCreateTag().getString("Class")).equals("")) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 1) {
				itemstack.getOrCreateTag().putString("Class", "\u00A79Assassin");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 2) {
				itemstack.getOrCreateTag().putString("Class", "\u00A76Mage");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 3) {
				itemstack.getOrCreateTag().putString("Class", "\u00A7cFighter");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 4) {
				itemstack.getOrCreateTag().putString("Class", "\u00A78Tanker");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 5) {
				itemstack.getOrCreateTag().putString("Class", "\u00A7aHealer");
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 6) {
				itemstack.getOrCreateTag().putString("Class", "\u00A73Ranger");
			}
		}
		if ((itemstack.getOrCreateTag().getString("Person")).equals("")) {
			itemstack.getOrCreateTag().putString("Person", ("\u00A7c" + entity.getDisplayName().getString()));
		}
	}
}
