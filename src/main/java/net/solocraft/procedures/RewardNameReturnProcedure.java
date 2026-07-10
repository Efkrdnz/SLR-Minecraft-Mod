package net.solocraft.procedures;

import net.solocraft.util.RewardManager;

import net.minecraft.world.entity.Entity;

public class RewardNameReturnProcedure {
	public static String execute(Entity entity) {
		return execute(entity, 1);
	}

	public static String execute(Entity entity, int slot) {
		if (entity == null)
			return "";
		return RewardManager.displayName(entity, slot);
	}
}
