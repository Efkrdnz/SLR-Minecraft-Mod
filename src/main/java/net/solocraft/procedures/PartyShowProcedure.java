package net.solocraft.procedures;

import net.solocraft.party.PartyService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Legacy command adapter that opens the visual party interface. */
public final class PartyShowProcedure {
	private PartyShowProcedure() {
	}

	public static void execute(Entity entity) {
		if (entity instanceof ServerPlayer player)
			PartyService.legacyOpen(player);
	}
}
