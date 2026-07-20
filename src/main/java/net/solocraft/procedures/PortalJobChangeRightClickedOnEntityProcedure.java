package net.solocraft.procedures;

import net.solocraft.entity.PortalJobChangeEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Legacy gate compatibility; all entry state is now owned by the Quests flow. */
public class PortalJobChangeRightClickedOnEntityProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (!(entity instanceof PortalJobChangeEntity portal) || !(sourceentity instanceof ServerPlayer player))
			return;
		String owner = portal.getEntityData().get(PortalJobChangeEntity.DATA_person_to_enter);
		if (!owner.isBlank() && !owner.equals(player.getStringUUID()))
			return;
		JobChangeQuestEntryProcedure.execute(world, player);
		if (!portal.level().isClientSide())
			portal.discard();
	}
}
