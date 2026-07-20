package net.solocraft.procedures;

import net.solocraft.util.MageSpellRuntime;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class MagicMissilesShootProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		MageSpellRuntime.startMagicMissiles(world, entity);
	}
}
