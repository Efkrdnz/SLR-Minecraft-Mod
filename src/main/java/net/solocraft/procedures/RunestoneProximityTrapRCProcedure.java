package net.solocraft.procedures;

import net.solocraft.util.RangerCombatManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy registry hook: the old Proximity Trap stone now unlocks Arrow Shower
 * so existing worlds and item IDs remain valid.
 */
public final class RunestoneProximityTrapRCProcedure {
	private RunestoneProximityTrapRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemStack) {
		if (!(entity instanceof ServerPlayer player))
			return;
		if (!RangerCombatManager.grantSkill(player, RangerCombatManager.ARROW_SHOWER)) {
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.ranger.skill_known"), false);
			return;
		}
		if (!player.isCreative())
			itemStack.shrink(1);
		player.displayClientMessage(Component.translatable(
				"message.sololeveling.ranger.skill_gained",
				RangerCombatManager.ARROW_SHOWER), false);
	}
}
