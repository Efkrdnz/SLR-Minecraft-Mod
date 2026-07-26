package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcSpatialLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Floor identity is derived from the player's assigned spatial cell. */
public class DKCFloorDetectorProcedure {
	public static void execute(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		int currentFloor = DkcSpatialLayout.floor(player);
		if (currentFloor == 0)
			return;
		CompoundTag data = entity.getPersistentData();
		int oldFloor = (int) data.getDouble("dkc_current_floor");
		if (oldFloor != currentFloor) {
			data.putDouble("dkc_previous_floor", oldFloor);
			data.putDouble("dkc_current_floor", currentFloor);
			data.putBoolean("dkc_floor_just_changed", true);
		}

		BlockPos origin = DkcFloorBuilder.origin(player, currentFloor);
		double localZ = entity.getZ() - origin.getZ();
		String section;
		if (currentFloor == 1)
			section = localZ < 72.0D ? "arrival" : localZ < 152.0D ? "courtyard" : "tower";
		else
			section = localZ < 24.0D ? "arrival" : "district";
		data.putString("dkc_current_section", section);
	}

	public static int getCurrentFloor(Entity entity) {
		if (entity instanceof ServerPlayer player)
			return DkcSpatialLayout.floor(player);
		if (entity == null)
			return 0;
		int stored = (int) entity.getPersistentData().getDouble("dkc_current_floor");
		return stored <= 0 ? 0 : Math.min(DkcFloorRegistry.LAST_FLOOR, stored);
	}

	public static String getCurrentSection(Entity entity) {
		if (entity == null)
			return "arrival";
		String section = entity.getPersistentData().getString("dkc_current_section");
		return section.isEmpty() ? "arrival" : section;
	}
}
