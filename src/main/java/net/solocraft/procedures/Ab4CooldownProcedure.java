package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class Ab4CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (CooldownManager.isOnCooldown(entity, "Water Slash") || CooldownManager.isOnCooldown(entity, "Slash Fury")
				|| CooldownManager.isOnCooldown(entity, "Murderious Intent") || CooldownManager.isOnCooldown(entity, "Overheal")
				|| CooldownManager.isOnCooldown(entity, "Proximity Trap") || CooldownManager.isOnCooldown(entity, "Reinforcement")) {
			return true;
		}
		return false;
	}
}
