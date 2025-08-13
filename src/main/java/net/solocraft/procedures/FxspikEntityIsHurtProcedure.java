package net.solocraft.procedures;

import net.solocraft.entity.FxspikEntity;

import net.minecraft.world.entity.Entity;

public class FxspikEntityIsHurtProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof FxspikEntity) {
			((FxspikEntity) entity).setAnimation("hit");
		}
	}
}
