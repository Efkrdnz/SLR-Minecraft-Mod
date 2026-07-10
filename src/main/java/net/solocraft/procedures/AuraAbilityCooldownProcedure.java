package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class AuraAbilityCooldownProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (CooldownManager.getRemainingSeconds(entity, "aura") > 0) {
			return "" + Math.round(CooldownManager.getRemainingSeconds(entity, "aura"));
		}
		return "";
	}
}
