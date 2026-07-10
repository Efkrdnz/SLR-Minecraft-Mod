package net.solocraft.procedures;

import org.checkerframework.checker.units.qual.s;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;

public class DKCFloorDetectorProcedure {
	// detects current floor and section based on position relative to origin
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		CompoundTag data = entity.getPersistentData();
		double originZ = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).dkc_z;
		double playerZ = entity.getZ();
		double relativeZ = playerZ - originZ;
		int currentFloor = (int) Math.floor(relativeZ / 200.0) + 1;
		if (currentFloor < 1)
			currentFloor = 1;
		if (currentFloor > 20)
			currentFloor = 20;
		double localZ = relativeZ % 200.0;
		if (localZ < 0)
			localZ += 200.0;
		// match your placement offsets: middle starts at +56, right starts at +133
		String section = "left";
		if (localZ >= 56 && localZ < 133)
			section = "middle";
		else if (localZ >= 133)
			section = "right";
		double oldFloor = data.getDouble("dkc_current_floor");
		if (oldFloor != currentFloor) {
			data.putDouble("dkc_previous_floor", oldFloor);
			data.putDouble("dkc_current_floor", currentFloor);
			data.putBoolean("dkc_floor_just_changed", true);
			// despawn previous floor demons for players
			if (entity instanceof net.minecraft.world.entity.player.Player && entity.level() instanceof ServerLevel serverLevel) {
				despawnDemonsFromFloor(serverLevel, (int) oldFloor, entity.getStringUUID());
			}
		} else {
			data.putBoolean("dkc_floor_just_changed", false);
		}
		data.putString("dkc_current_section", section);
	}

	private static void despawnDemonsFromFloor(ServerLevel level, int floor, String ownerUuid) {
		if (floor <= 0)
			return;
		for (net.solocraft.entity.DemonEntity demon : level.getEntitiesOfClass(net.solocraft.entity.DemonEntity.class, new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
			int demonFloor = (int) demon.getPersistentData().getDouble("dkc_floor_number");
			String demonOwner = demon.getPersistentData().getString("dkc_spawned_by");
			if (demonFloor == floor && ownerUuid.equals(demonOwner)) {
				demon.discard();
			}
		}
	}

	public static int getCurrentFloor(Entity entity) {
		if (entity == null)
			return 1;
		int v = (int) entity.getPersistentData().getDouble("dkc_current_floor");
		return v <= 0 ? 1 : v;
	}

	public static String getCurrentSection(Entity entity) {
		if (entity == null)
			return "left";
		String s = entity.getPersistentData().getString("dkc_current_section");
		return (s == null || s.isEmpty()) ? "left" : s;
	}
}
