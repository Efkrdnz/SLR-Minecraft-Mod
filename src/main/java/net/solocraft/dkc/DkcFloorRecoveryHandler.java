package net.solocraft.dkc;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.PlayerEntryGenerationGuard;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;

/** Repairs interrupted builds and migrates players left inside the legacy tower. */
@EventBusSubscriber
public final class DkcFloorRecoveryHandler {
	private DkcFloorRecoveryHandler() {
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		int storedFloor = clamp((int) player.getPersistentData().getDouble("dkc_current_floor"));
		boolean nearLegacyDistrict = storedFloor > 0 && vars.dkc_started
				&& (vars.dkc_x != 0.0D || vars.dkc_z != 0.0D)
				&& Math.abs(player.getX() - vars.dkc_x) <= 512.0D
				&& Math.abs(player.getZ() - vars.dkc_z) <= 4_096.0D;
		boolean legacyStranded = player.getPersistentData().getBoolean(DkcSpatialLayout.ACTIVE_RUN_TAG)
				|| (vars.dungeoning && storedFloor > 0) || nearLegacyDistrict;
		if (!DkcFloorRegistry.isDkc(player.level()) && !legacyStranded)
			return;
		long entryGeneration =
				PlayerEntryGenerationGuard.capture(player);
		SololevelingMod.queueServerWork(player.server, 20, () -> {
			if (PlayerEntryGenerationGuard.isCurrent(player,
					entryGeneration))
				recover(player);
		});
	}

	private static void recover(ServerPlayer player) {
		if (!player.isAlive() || player.server == null)
			return;
		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		double legacyOriginZ = vars.dkc_z;
		int storedFloor = clamp((int) player.getPersistentData().getDouble("dkc_current_floor"));
		int legacyDimensionFloor = DkcFloorRegistry.legacyFloor(player.level().dimension());
		DkcSpatialLayout.Location location = DkcSpatialLayout.locate(player.getX(), player.getZ());

		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		DkcRunSavedData.RunState state = runs.getOrCreate(player);
		int targetFloor = location.slot() == state.slot() ? location.floor() : legacyDimensionFloor;
		if (legacyDimensionFloor == 1 && targetFloor <= 1 && legacyOriginZ != 0.0D) {
			int coordinateFloor = (int) Math.floor((player.getZ() - legacyOriginZ) / 200.0D) + 1;
			if (coordinateFloor >= 1 && coordinateFloor <= DkcFloorRegistry.LAST_FLOOR)
				targetFloor = coordinateFloor;
		}
		targetFloor = Math.max(targetFloor, storedFloor);
		if (targetFloor <= 0)
			targetFloor = 1;
		while (targetFloor > 1 && !runs.isUnlocked(player, targetFloor))
			targetFloor--;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dkc_started = true;
			// Old DKC used the generic dungeon flag. The shared realm has its own
			// containment/protection and must not leak that flag back to the Overworld.
			capability.dungeoning = false;
			capability.syncPlayerVariables(player);
		});
		DkcFloorBuilder.teleportToFloor(player, targetFloor);
	}

	private static int clamp(int floor) {
		return Math.max(0, Math.min(DkcFloorRegistry.LAST_FLOOR, floor));
	}
}
