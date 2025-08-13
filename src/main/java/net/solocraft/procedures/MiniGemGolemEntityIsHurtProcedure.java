package net.solocraft.procedures;

import net.solocraft.entity.MiniGemGolemEntity;

import net.minecraft.world.entity.Entity;

public class MiniGemGolemEntityIsHurtProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof MiniGemGolemEntity) {
			((MiniGemGolemEntity) entity).setAnimation("hurt");
		}
	}
}
