package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.solocraft.util.CooldownManager;

public class Ab9CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null) return false;
		return CooldownManager.isOnCooldown(entity, "aura");
	}
}
