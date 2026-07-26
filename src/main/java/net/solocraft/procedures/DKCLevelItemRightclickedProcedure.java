package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Creative test tool for advancing or rewinding DKC access state. */
public class DKCLevelItemRightclickedProcedure {
	public static void execute(Entity entity) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		int delta = player.isShiftKeyDown() ? -1 : 1;
		int cleared = Math.max(0, Math.min(DkcFloorRegistry.LAST_FLOOR, (int) vars.dkc_cleared + delta));
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dkc_cleared = cleared;
			capability.dkc_started = cleared > 0 || capability.dkc_started;
			capability.syncPlayerVariables(player);
		});
		DkcRadiruManager.normalizeDebugProgress(player, cleared);
		DkcRunSavedData.get(player.server).setDebugProgress(player, cleared);
		DkcRadiruManager.reconcileDebugProgress(player, cleared);
	}

	/** Sets one exact floor as a fresh test attempt, then moves the target there. */
	public static boolean setCurrentFloor(ServerPlayer player, int floor) {
		if (player == null || player.server == null || floor < DkcFloorRegistry.FIRST_FLOOR
				|| floor > DkcFloorRegistry.LAST_FLOOR)
			return false;

		int cleared = floor - 1;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dkc_started = true;
			capability.dkc_cleared = cleared;
			capability.syncPlayerVariables(player);
		});
		resetFloorAttempt(player, floor);
		DkcRadiruManager.normalizeDebugProgress(player, cleared);
		DkcRunSavedData.get(player.server).setDebugProgress(player, cleared);
		DkcFloorBuilder.debugTeleportToFloor(player, floor, () -> {
			DkcFloorBuilder.resetEncounterForDebug(player, floor);
			DkcRadiruManager.reconcileDebugProgress(player, cleared);
			player.sendSystemMessage(Component.literal("DKC test state set to Floor " + floor + " - "
					+ DkcFloorRegistry.name(floor)).withStyle(ChatFormatting.LIGHT_PURPLE));
		});
		return true;
	}

	private static void resetFloorAttempt(ServerPlayer player, int floor) {
		CompoundTag data = player.getPersistentData();
		String prefix = "dkc_floor_" + floor;
		for (String suffix : new String[] {"_spawned", "_initial_spawned", "_spawning", "_complete",
				"_killed", "_required", "_demon_count", "_knight_count", "_miniboss_spawned",
				"_spawn_retry_after", "_enter_time"})
			data.remove(prefix + suffix);
		data.remove(prefix + "_boss_defeated");
		if (floor == DkcFloorRegistry.LAST_FLOOR) {
			data.remove("dkc_floor_20_baran_defeated");
			data.remove("dkc_floor_20_kaiselin_defeated");
			data.remove("dkc_floor_20_kaisel_soul_spawned");
		}
		DKCDemonSpawnerProcedure.invalidateAttempt(player, floor);
		ServerLevel dkc = player.server.getLevel(DkcFloorRegistry.SHARED_DIMENSION);
		if (dkc != null)
			DKCDemonSpawnerProcedure.discardOwnedWave(dkc, player, floor);
	}
}
