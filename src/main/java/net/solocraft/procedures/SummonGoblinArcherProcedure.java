package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SummonGoblinArcherProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		ShadowSummonProcedureHelper.execute(world, x, y, z, entity, "goblin_archer");
	}
}
