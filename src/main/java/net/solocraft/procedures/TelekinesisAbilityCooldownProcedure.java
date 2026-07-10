package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class TelekinesisAbilityCooldownProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (CooldownManager.getRemainingSeconds(entity, "telekinesis") > 0) {
			return "" + Math.round(CooldownManager.getRemainingSeconds(entity, "telekinesis"));
		}
		return "";
	}
}
