package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

/** Single source of truth for whether an entity has awakened as a System Player. */
public final class SystemPlayerAccess {
	private SystemPlayerAccess() {
	}

	public static boolean hasSystem(Entity entity) {
		if (entity == null)
			return false;
		if (DungeonBuilderMode.isActive(entity.level()))
			return false;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.Player)
				.orElse(false);
	}
}
