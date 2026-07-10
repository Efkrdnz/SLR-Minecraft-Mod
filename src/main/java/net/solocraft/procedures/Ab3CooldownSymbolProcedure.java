package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class Ab3CooldownSymbolProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (CooldownManager.isOnCooldown(entity, "Fireball") || CooldownManager.isOnCooldown(entity, "Slash Dash")
				|| CooldownManager.isOnCooldown(entity, "Stealth") || CooldownManager.isOnCooldown(entity, "Heal Beam")
				|| CooldownManager.isOnCooldown(entity, "Tank Leap")) {
			return true;
		}
		return false;
	}
}
