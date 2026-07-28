package net.solocraft.procedures;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Grants exactly the first missing Tanker skill in the canonical order. */
public final class MasterylvlupTankerProcedure {
	private MasterylvlupTankerProcedure() {
	}

	public static void execute(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		String granted = TankerProgressionHelper.grantNextMasterySkill(player);
		if (granted.isEmpty()) {
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.tanker.mastery.complete"), false);
		} else {
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.tanker.skill_gained", granted), false);
		}
	}
}
