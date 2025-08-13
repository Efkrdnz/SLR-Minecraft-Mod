package net.solocraft.procedures;

import net.solocraft.entity.StatueOfGodEntity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class StatueOfGodOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putString("state", "throne");
		if (entity instanceof StatueOfGodEntity _datEntSetI)
			_datEntSetI.getEntityData().set(StatueOfGodEntity.DATA_default_x, (int) entity.getX());
		if (entity instanceof StatueOfGodEntity _datEntSetI)
			_datEntSetI.getEntityData().set(StatueOfGodEntity.DATA_default_y, (int) entity.getY());
		if (entity instanceof StatueOfGodEntity _datEntSetI)
			_datEntSetI.getEntityData().set(StatueOfGodEntity.DATA_default_z, (int) entity.getZ());
		((Mob) entity).setNoAi(true);
	}
}
