package net.solocraft.procedures;

import net.solocraft.party.PartyService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Legacy command adapter that opens the visual party roster. */
public final class PartyMembersProcedure {
	private PartyMembersProcedure() {
	}

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity instanceof ServerPlayer player)
			PartyService.legacyOpen(player);
	}
}
