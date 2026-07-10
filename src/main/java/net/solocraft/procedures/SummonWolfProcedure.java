package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public class SummonWolfProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		ShadowSummonProcedureHelper.execute(world, x, y, z, entity, "wolf");
	}
}
