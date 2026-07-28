package net.solocraft.procedures;

import net.solocraft.party.PartyService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Legacy command adapter for leaving a party. */
public final class PartyLeaveProcedure {
	private PartyLeaveProcedure() {
	}

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity instanceof ServerPlayer player)
			PartyService.legacyLeave(player);
	}
}
