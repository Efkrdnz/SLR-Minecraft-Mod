package net.solocraft.procedures;

import net.solocraft.util.VesselProgressionManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Compatibility entry point retained for generated callers.
 *
 * Shadow progression used to be evaluated every player tick in this procedure.
 * It now shares the bounded, server-authoritative vessel progression manager.
 */
public final class ShadowExtractionShowProcedure {
	private ShadowExtractionShowProcedure() {
	}

	public static void execute(Entity entity) {
		if (entity instanceof ServerPlayer player)
			VesselProgressionManager.sync(player);
	}
}
