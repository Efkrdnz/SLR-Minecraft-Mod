package net.solocraft.procedures;

import net.solocraft.util.GateSpawnerUtil;

import net.minecraft.world.level.LevelAccessor;

public class GetRandomPlayerProcedure {
	public static void execute(LevelAccessor world) {
		GateSpawnerUtil.spawnNearRandomOverworldPlayer(world);
	}
}
