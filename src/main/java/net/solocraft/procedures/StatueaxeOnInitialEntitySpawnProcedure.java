package net.solocraft.procedures;

import net.solocraft.SololevelingMod;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class StatueaxeOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putString("state", "idle");
		if (false) {
			SololevelingMod.LOGGER.info(entity);
		}
		((Mob) entity).setNoAi(true);
	}
}
