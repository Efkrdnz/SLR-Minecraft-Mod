package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ColdBloodSkillManager;
import net.solocraft.util.SkillListHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class RunestoneColdBloodRCProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		if (!SkillListHelper.skills(entity).contains(ColdBloodSkillManager.SKILL)) {
			String updated = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.orElse(new SololevelingModVariables.PlayerVariables()).Plist + ColdBloodSkillManager.SKILL + ",";
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.Plist = updated;
				capability.syncPlayerVariables(entity);
			});
			if (entity instanceof Player player)
				player.getInventory().clearOrCountMatchingItems(p -> itemstack.getItem() == p.getItem(), 1,
						player.inventoryMenu.getCraftSlots());
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Learned \"Cold Blood\""), true);
		} else if (entity instanceof Player player && !player.level().isClientSide()) {
			player.displayClientMessage(Component.literal("You already have this skill!"), false);
		}
	}
}
