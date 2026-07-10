package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class Ab5CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (CooldownManager.isOnCooldown(entity, "Cross Strike")
				|| CooldownManager.isOnCooldown(entity, "Critical Strike")
				|| CooldownManager.isOnCooldown(entity, "Protection Mark")
				|| CooldownManager.isOnCooldown(entity, "Protection Mark")
				|| CooldownManager.isOnCooldown(entity, "Blessing Mark")) {
			return true;
		}
		return false;
	}
}
