package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class SelectedConProcedure {
	public static boolean execute(Entity entity, int slot) {
		if (entity == null)
			return false;
		return (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Skillcycle == slot;
	}
}
