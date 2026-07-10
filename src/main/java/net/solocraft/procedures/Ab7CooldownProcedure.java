package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class Ab7CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (CooldownManager.isOnCooldown(entity, "Shadowstep") || CooldownManager.isOnCooldown(entity, "Detection")
				|| CooldownManager.isOnCooldown(entity, "Ground Slam")
				|| CooldownManager.isOnCooldown(entity, "Shield Bash")
				|| CooldownManager.isOnCooldown(entity, "Physical Buff")) {
			return true;
		}
		return false;
	}
}
