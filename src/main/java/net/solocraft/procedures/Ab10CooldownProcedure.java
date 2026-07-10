package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class Ab10CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (CooldownManager.isOnCooldown(entity, "Backstab") || CooldownManager.isOnCooldown(entity, "Light Golem")
				|| CooldownManager.isOnCooldown(entity, "Sword of Light") || CooldownManager.isOnCooldown(entity, "Taunt")) {
			return true;
		}
		return false;
	}
}
