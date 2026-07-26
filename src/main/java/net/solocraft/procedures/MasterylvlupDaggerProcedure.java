package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

public class MasterylvlupDaggerProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		String list = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).Plist;
		String unlocked = !list.contains("Dagger Throw") ? "Dagger Throw"
				: !list.contains("Dagger Rush") ? "Dagger Rush"
				: !list.contains("Critical Attack") ? "Critical Attack" : "";
		if (unlocked.isEmpty())
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Plist = capability.Plist + unlocked + ",";
			capability.syncPlayerVariables(entity);
		});
		if (entity instanceof Player player && !player.level().isClientSide())
			player.displayClientMessage(Component.literal("Gained skill: " + unlocked), false);
	}
}
