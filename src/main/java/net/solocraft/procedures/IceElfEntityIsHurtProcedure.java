package net.solocraft.procedures;

import net.solocraft.entity.IceElfEntity;

import net.minecraft.world.entity.Entity;

public class IceElfEntityIsHurtProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof IceElfEntity) {
			((IceElfEntity) entity).setAnimation("misc.hurt");
		}
	}
}
