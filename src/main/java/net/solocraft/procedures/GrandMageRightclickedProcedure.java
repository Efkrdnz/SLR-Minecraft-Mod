package net.solocraft.procedures;

import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.VesselManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class GrandMageRightclickedProcedure {
	public static void execute(Entity entity) {
		if (entity instanceof ServerPlayer player
				&& VesselManager.assignPlayer(player, VesselManager.RULER, "christopher_reed", true) == VesselManager.AssignmentResult.SUCCESS)
			JobChangeQuestManager.finish(player);
	}
}
