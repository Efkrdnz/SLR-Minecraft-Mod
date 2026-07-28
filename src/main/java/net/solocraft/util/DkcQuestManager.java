package net.solocraft.util;

import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

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
		if (hasRadiruCastleAccess(entity))
			return true;
		// Keep the return controls visible until a victorious player has actually
		// left the final floor; outside the castle the completed quest stays hidden.
		return isUnlocked(entity) && (!isFinished(entity) || DkcFloorRegistry.isDkc(entity.level()));
	}

	public static boolean hasRadiruCastleAccess(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return vars.radiru_side_quest_unlocked
				// Backward compatibility for saves that conquered the pre-Radiru
				// Floor 15 and therefore never had an outcome flag to set.
				|| vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR && !vars.radiru_slaughtered;
	}

	public static Component buttonLabel(Entity entity) {
		if (entity != null && DkcFloorRegistry.isDkc(entity.level()))
			return Component.literal("Castle Tower");
		return Component.literal(hasRadiruCastleAccess(entity) ? "Radiru Castle" : "Demon King's Castle");
	}

	public static void unlock(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			boolean changed = false;
			if (capability.dkc_cleared < 20 && capability.dkc_unlocked <= 0) {
				capability.dkc_unlocked = 1;
				changed = true;
			}
			if (capability.dailysecrettrans != 0) {
				capability.dailysecrettrans = 0;
				changed = true;
			}
			if (changed)
				capability.syncPlayerVariables(entity);
		});
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}
}
