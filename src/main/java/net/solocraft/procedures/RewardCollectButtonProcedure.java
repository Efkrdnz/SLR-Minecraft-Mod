package net.solocraft.procedures;

import net.solocraft.util.RewardManager;

import net.minecraft.world.entity.Entity;

public class RewardCollectButtonProcedure {
	public static void execute(Entity entity, int slot) {
		if (entity == null)
			return;
		RewardManager.claimReward(entity, slot);
	}
}
