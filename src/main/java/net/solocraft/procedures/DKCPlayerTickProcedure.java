package net.solocraft.procedures;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;

public class DKCPlayerTickProcedure {
	// main tick procedure
	public static void execute(LevelAccessor world, Entity entity) {
		if (world == null || entity == null)
			return;
		DKCFloorQuestStarterProcedure.execute(world, entity);
		// delayed spawn and periodic missing demon checks
		DKCDemonSpawnerProcedure.checkDelayedSpawn(world, entity);
		// extra failsafe
		checkAndRespawnMissingDemons(world, entity);
	}

	// checks if demons are missing and respawns them as failsafe
	private static void checkAndRespawnMissingDemons(LevelAccessor world, Entity entity) {
		if (!(entity instanceof Player player))
			return;
		if (!(world instanceof ServerLevel serverLevel))
			return;
		// rate-limit to every 10s, offset from checkDelayedSpawn (which runs at %200==0)
		if (serverLevel.getGameTime() % 200 != 101)
			return;
		int currentFloor = DKCFloorDetectorProcedure.getCurrentFloor(player);
		// demon floors only
		if (currentFloor < 2 || currentFloor > 19)
			return;
		CompoundTag data = player.getPersistentData();
		if (!data.getBoolean("dkc_floor_" + currentFloor + "_spawned"))
			return;
		if (!data.getBoolean("dkc_floor_" + currentFloor + "_initial_spawned"))
			return; // still in grace period, checkDelayedSpawn handles this
		if (data.getBoolean("dkc_floor_" + currentFloor + "_complete"))
			return;
		double killed = data.getDouble("dkc_floor_" + currentFloor + "_killed");
		double required = data.getDouble("dkc_floor_" + currentFloor + "_required");
		if (killed >= required)
			return;
		// scan full level — demons can spawn anywhere in the 200-block floor
		int demonsInFloor = 0;
		for (net.solocraft.entity.DemonEntity demon : serverLevel.getEntitiesOfClass(net.solocraft.entity.DemonEntity.class,
				new AABB(player.getX() - 200, player.getY() - 40, player.getZ() - 200, player.getX() + 200, player.getY() + 40, player.getZ() + 200))) {
			int demonFloor = (int) demon.getPersistentData().getDouble("dkc_floor_number");
			String demonOwner = demon.getPersistentData().getString("dkc_spawned_by");
			if (demonFloor == currentFloor && player.getStringUUID().equals(demonOwner)) {
				demonsInFloor++;
			}
		}
		if (demonsInFloor == 0) {
			int toRespawn = (int) (required - killed);
			if (!player.level().isClientSide()) {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[DKC] No demons detected — respawning " + toRespawn), false);
			}
			DKCDemonSpawnerProcedure.respawnDemons(world, player, currentFloor, toRespawn);
		}
	}
}
