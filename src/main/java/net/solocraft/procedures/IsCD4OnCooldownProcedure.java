package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.solocraft.util.CooldownManager;

public class IsCD4OnCooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null) return false;
		return CooldownManager.isOnCooldown(entity, "job_4");
	}
}
