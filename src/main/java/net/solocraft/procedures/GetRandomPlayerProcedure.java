package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.init.SololevelingModEntities;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.ArrayList;

public class GetRandomPlayerProcedure {
	public static void execute(LevelAccessor world) {
		List<Object> player = new ArrayList<>();
		String name = "";
		double randomNum = 0;
		double CordX = 0;
		double CordY = 0;
		double CordZ = 0;
		double RandX = 0;
		double RandZ = 0;
		if ((world instanceof Level _lvl ? _lvl.dimension() : Level.OVERWORLD) == Level.OVERWORLD) {
			player.clear();
			for (Entity entityiterator : new ArrayList<>(world.players())) {
				if ((entityiterator.level().dimension()) == Level.OVERWORLD) {
					player.add(entityiterator);
				}
			}
			if (player.size() > 0) {
				randomNum = Mth.nextInt(RandomSource.create(), 1, (int) player.size());
				name = (player.get((int) (randomNum - 1)) instanceof Entity _e ? _e : null).getDisplayName().getString();
				CordX = (float) ((int) (Math.pow(10, (Math.min(8, Math.max((2), -1)))) * ((player.get((int) (randomNum - 1)) instanceof Entity _e ? _e : null).getX())) / (float) Math.pow(10, (Math.min(8, Math.max((2), -1)))));
				CordY = (float) ((int) (Math.pow(10, (Math.min(8, Math.max((2), -1)))) * ((player.get((int) (randomNum - 1)) instanceof Entity _e ? _e : null).getY())) / (float) Math.pow(10, (Math.min(8, Math.max((2), -1)))));
				CordZ = (float) ((int) (Math.pow(10, (Math.min(8, Math.max((2), -1)))) * ((player.get((int) (randomNum - 1)) instanceof Entity _e ? _e : null).getZ())) / (float) Math.pow(10, (Math.min(8, Math.max((2), -1)))));
				RandX = Mth.nextInt(RandomSource.create(), -200, 200);
				RandZ = Mth.nextInt(RandomSource.create(), -200, 200);
				if (world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)) >= -60) {
					if (Math.random() < (1) / ((float) 6)) {
						if (world instanceof ServerLevel _level) {
							Entity entityToSpawn = SololevelingModEntities.RANDOM_CAVE_LARGE.get().spawn(_level,
									BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
							if (entityToSpawn != null) {
							}
						}
					} else {
						if (Math.random() < (1) / ((float) 6)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.PORTAL_LUSH.get().spawn(_level,
										BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
								}
							}
						} else {
							if (Math.random() < (1) / ((float) 6)) {
								if (world instanceof ServerLevel _level) {
									Entity entityToSpawn = SololevelingModEntities.PORTAL_ANCIENT_GOLEM.get().spawn(_level,
											BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
									if (entityToSpawn != null) {
									}
								}
							} else {
								if (Math.random() < (1) / ((float) 6)) {
									if (world instanceof ServerLevel _level) {
										Entity entityToSpawn = SololevelingModEntities.PORTAL_LAB.get().spawn(_level,
												BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
										if (entityToSpawn != null) {
										}
									}
								} else {
									if (Math.random() < (1) / ((float) 10)) {
										if (!SololevelingModVariables.MapVariables.get(world).RedGate) {
											if (world instanceof ServerLevel _level) {
												Entity entityToSpawn = SololevelingModEntities.RED_GATE.get().spawn(_level,
														BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
												if (entityToSpawn != null) {
												}
											}
										} else {
											SololevelingModVariables.MapVariables.get(world).gatetimer = (world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY)) - 1;
											SololevelingModVariables.MapVariables.get(world).syncData(world);
										}
									} else {
										if (Math.random() < (1) / ((float) 5)) {
											if (world instanceof ServerLevel _level) {
												Entity entityToSpawn = SololevelingModEntities.PORTAL_KARGALGANS_THRONE_ROOM.get().spawn(_level,
														BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
												if (entityToSpawn != null) {
												}
											}
										} else {
											if (Math.random() < (1) / ((float) 5)) {
												if (world instanceof ServerLevel _level) {
													Entity entityToSpawn = SololevelingModEntities.PORTAL_BERU.get().spawn(_level,
															BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
													if (entityToSpawn != null) {
													}
												}
											} else {
												if (world instanceof ServerLevel _level) {
													Entity entityToSpawn = SololevelingModEntities.PORTAL_SEWERS.get().spawn(_level,
															BlockPos.containing(CordX + RandX, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (CordX + RandX), (int) (CordZ + RandZ)), CordZ + RandZ), MobSpawnType.MOB_SUMMONED);
													if (entityToSpawn != null) {
													}
												}
											}
										}
									}
								}
							}
						}
					}
				} else {
					SololevelingModVariables.MapVariables.get(world).gatetimer = (world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY)) - 1;
					SololevelingModVariables.MapVariables.get(world).syncData(world);
				}
			}
		}
	}
}
