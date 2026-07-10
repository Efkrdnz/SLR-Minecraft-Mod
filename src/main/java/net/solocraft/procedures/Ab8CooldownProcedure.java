package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class Ab8CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (CooldownManager.isOnCooldown(entity, "Dualwield") || CooldownManager.isOnCooldown(entity, "Lightball")
				|| CooldownManager.isOnCooldown(entity, "Sword Dance")
				|| CooldownManager.isOnCooldown(entity, "Willpower")
				|| CooldownManager.isOnCooldown(entity, "Haste Buff")) {
			return true;
		}
		return false;
	}
}
