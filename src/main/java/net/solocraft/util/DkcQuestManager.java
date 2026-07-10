package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public final class DkcQuestManager {
	private DkcQuestManager() {
	}

	public static boolean isUnlocked(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return vars.dkc_unlocked > 0 || vars.dkc_started || vars.dkc_cleared > 0;
	}

	public static boolean isFinished(Entity entity) {
		return entity != null && vars(entity).dkc_cleared >= 20;
	}

	public static boolean isVisible(Entity entity) {
		return isUnlocked(entity) && !isFinished(entity);
	}

	public static void unlock(Entity entity) {
		if (entity == null || isFinished(entity))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.dkc_unlocked <= 0) {
				capability.dkc_unlocked = 1;
				capability.syncPlayerVariables(entity);
			}
		});
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}
}
