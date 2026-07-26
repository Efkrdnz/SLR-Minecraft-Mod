package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;

public class DKCFloorQuestStarterProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer player)
				|| level.getGameTime() % 20L != 0L)
			return;
		int floor = DkcSpatialLayout.floor(player);
		if (floor == 0)
			return;
		GameType mode = player.gameMode.getGameModeForPlayer();
		if (mode == GameType.SPECTATOR || mode == GameType.CREATIVE)
			return;

		DKCFloorDetectorProcedure.execute(player);
		CompoundTag data = player.getPersistentData();
		if (data.getBoolean("dkc_floor_" + floor + "_spawned")
				&& (DkcFloorRegistry.isBossFloor(floor) || (floor == 10 && data.getBoolean("dkc_floor_10_complete")))
				&& level.getGameTime() % 40L == 0L)
			DkcFloorBuilder.ensureBosses(player, floor);

		if (!data.getBoolean("dkc_floor_just_changed") && data.getBoolean("dkc_floor_" + floor + "_spawned"))
			return;
		startFloorQuest(world, player, floor);
		data.putBoolean("dkc_floor_just_changed", false);
	}

	private static void startFloorQuest(LevelAccessor world, ServerPlayer player, int floor) {
		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		if (!runs.isUnlocked(player, floor)) {
			notifyNegative(player, "FLOOR LOCKED", "Present the previous floor's Entry Permit.");
			return;
		}
		CompoundTag data = player.getPersistentData();
		String spawnedKey = "dkc_floor_" + floor + "_spawned";
		double cleared = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_cleared;
		if (cleared >= floor) {
			data.putBoolean(spawnedKey, true);
			return;
		}
		if (data.getBoolean(spawnedKey))
			return;

		if (floor == 1) {
			data.putBoolean(spawnedKey, true);
			DkcFloorBuilder.ensureBosses(player, floor);
			return;
		}
		if (floor == 20) {
			data.putBoolean(spawnedKey, true);
			DkcFloorBuilder.ensureBosses(player, floor);
			return;
		}

		DKCDemonSpawnerProcedure.execute(world, player);
	}

	private static void notifyNegative(ServerPlayer player, String title, String under) {
		SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D3D, 80,
				Component.literal(title).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.literal(under).withStyle(ChatFormatting.RED));
	}
}
