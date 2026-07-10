package net.solocraft.util;

import net.solocraft.dungeon.DungeonTheme;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class GateSpawnerUtil {
	private GateSpawnerUtil() {
	}

	public static void spawnNearRandomOverworldPlayer(LevelAccessor world) {
		if (!(world instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.OVERWORLD)
			return;
		List<Entity> players = new ArrayList<>();
		for (Entity player : new ArrayList<>(world.players())) {
			if (player.level().dimension() == Level.OVERWORLD)
				players.add(player);
		}
		if (players.isEmpty())
			return;
		RandomSource random = RandomSource.create();
		Entity target = players.get(Mth.nextInt(random, 0, players.size() - 1));
		double baseX = truncate(target.getX());
		double baseZ = truncate(target.getZ());
		double randX = Mth.nextInt(random, -200, 200);
		double randZ = Mth.nextInt(random, -200, 200);
		int spawnX = (int) (baseX + randX);
		int spawnZ = (int) (baseZ + randZ);
		int spawnY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
		if (spawnY < -60) {
			delayNextGate(world);
			return;
		}
		BlockPos pos = BlockPos.containing(baseX + randX, spawnY, baseZ + randZ);
		SololevelingModVariables.MapVariables mapVars = SololevelingModVariables.MapVariables.get(world);
		if (!mapVars.firstNaturalGateSpawned) {
			Entity firstGate = spawn(serverLevel, SololevelingModEntities.PORTAL_SEWERS.get(), pos);
			if (firstGate != null) {
				mapVars.firstNaturalGateSpawned = true;
				mapVars.syncData(world);
			} else {
				delayNextGate(world);
			}
			return;
		}
		if (random.nextFloat() < 0.18F) {
			spawnProceduralGate(serverLevel, target, pos, random);
		} else if (Math.random() < 1 / 6.0F) {
			spawn(serverLevel, SololevelingModEntities.RANDOM_CAVE_LARGE.get(), pos);
		} else if (Math.random() < 1 / 6.0F) {
			spawn(serverLevel, SololevelingModEntities.PORTAL_LUSH.get(), pos);
		} else if (Math.random() < 1 / 6.0F) {
			spawn(serverLevel, SololevelingModEntities.PORTAL_ANCIENT_GOLEM.get(), pos);
		} else if (Math.random() < 1 / 6.0F) {
			spawn(serverLevel, SololevelingModEntities.PORTAL_LAB.get(), pos);
		} else if (Math.random() < 1 / 10.0F) {
			if (!SololevelingModVariables.MapVariables.get(world).RedGate) {
				spawn(serverLevel, SololevelingModEntities.RED_GATE.get(), pos);
			} else {
				delayNextGate(world);
			}
		} else if (Math.random() < 1 / 5.0F) {
			spawn(serverLevel, SololevelingModEntities.PORTAL_KARGALGANS_THRONE_ROOM.get(), pos);
		} else if (Math.random() < 1 / 5.0F) {
			spawn(serverLevel, SololevelingModEntities.PORTAL_BERU.get(), pos);
		} else {
			spawn(serverLevel, SololevelingModEntities.PORTAL_SEWERS.get(), pos);
		}
	}

	private static double truncate(double value) {
		return (float) ((int) (Math.pow(10, 2) * value)) / (float) Math.pow(10, 2);
	}

	private static Entity spawn(ServerLevel level, EntityType<?> type, BlockPos pos) {
		return type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
	}

	private static void spawnProceduralGate(ServerLevel level, Entity target, BlockPos pos, RandomSource random) {
		Entity gate = spawn(level, SololevelingModEntities.PORTAL_1.get(), pos);
		if (gate == null)
			return;
		ProceduralDungeonRank rank = rankFor(target);
		DungeonTheme theme = randomTheme(random);
		gate.getPersistentData().putBoolean("slr_procedural_gate", true);
		gate.getPersistentData().putBoolean("slr_procedural_red_gate", false);
		gate.getPersistentData().putString("slr_procedural_rank", rank.name());
		gate.getPersistentData().putString("slr_procedural_theme", theme.name());
		gate.getPersistentData().putInt("slr_procedural_complexity", complexityFor(rank, random));
	}

	private static ProceduralDungeonRank rankFor(Entity target) {
		SololevelingModVariables.PlayerVariables vars = target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		int hunterRank = Math.max(1, (int) vars.HunterRank);
		int level = (int) vars.Level;
		if (hunterRank >= 6 && level >= 100)
			return ProceduralDungeonRank.S;
		if (hunterRank >= 5 && level >= 75)
			return ProceduralDungeonRank.A;
		if (hunterRank >= 4 && level >= 50)
			return ProceduralDungeonRank.B;
		if (hunterRank >= 3 && level >= 30)
			return ProceduralDungeonRank.C;
		if (hunterRank >= 2 && level >= 15)
			return ProceduralDungeonRank.D;
		return ProceduralDungeonRank.E;
	}

	private static DungeonTheme randomTheme(RandomSource random) {
		DungeonTheme[] themes = DungeonTheme.values();
		return themes[Mth.nextInt(random, 0, themes.length - 1)];
	}

	private static int complexityFor(ProceduralDungeonRank rank, RandomSource random) {
		return switch (rank) {
			case E -> Mth.nextInt(random, 1, 3);
			case D -> Mth.nextInt(random, 2, 4);
			case C -> Mth.nextInt(random, 3, 6);
			case B -> Mth.nextInt(random, 5, 7);
			case A -> Mth.nextInt(random, 7, 9);
			case S -> Mth.nextInt(random, 8, 10);
		};
	}

	private static void delayNextGate(LevelAccessor world) {
		SololevelingModVariables.MapVariables.get(world).gatetimer = world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY) - 1;
		SololevelingModVariables.MapVariables.get(world).syncData(world);
	}
}
