package net.solocraft.procedures;

import net.solocraft.dkc.DkcRunSavedData;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

public class PointSetProcedure {
	public static void execute(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		DkcRunSavedData.get(player.server).getOrCreate(player);
	}
}
