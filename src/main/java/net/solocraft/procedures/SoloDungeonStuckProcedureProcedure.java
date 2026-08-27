package net.solocraft.procedures;

import net.minecraft.server.level.ServerPlayer;

/** Compatibility entry point for the legacy permission-free recovery command. */
public final class SoloDungeonStuckProcedureProcedure {
	private SoloDungeonStuckProcedureProcedure() {
	}

	public static boolean execute(ServerPlayer player) {
		return DungeonDimensionPlayerLeavesDimensionProcedure.emergencyExit(player);
	}
}
