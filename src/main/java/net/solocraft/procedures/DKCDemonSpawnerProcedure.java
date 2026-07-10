package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.DemonEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

public class DKCDemonSpawnerProcedure {
	private static final ResourceKey<net.minecraft.world.level.Level> DKC_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling", "dungeon_dimension_dkc"));

	// demon count and knight count per floor
	// floors 2-10: all demons, no knights
	// floors 11-19: mixed — knights increase each floor, demons decrease
	private record FloorComposition(int demons, int knights) {
		int total() { return demons + knights; }
	}

	private static FloorComposition getFloorComposition(int floor) {
		return switch (floor) {
			case 2  -> new FloorComposition(10,  0);
			case 3  -> new FloorComposition(15,  0);
			case 4  -> new FloorComposition(20,  0);
			case 5  -> new FloorComposition(25,  0);
			case 6  -> new FloorComposition(28,  0);
			case 7  -> new FloorComposition(32,  0);
			case 8  -> new FloorComposition(36,  0);
			case 9  -> new FloorComposition(40,  0);
			case 10 -> new FloorComposition(50,  0); // 50 demons → Vulcan spawns
			case 11 -> new FloorComposition(20,  5);
			case 12 -> new FloorComposition(19,  7);
			case 13 -> new FloorComposition(18,  9);
			case 14 -> new FloorComposition(16, 12);
			case 15 -> new FloorComposition(15, 14);
			case 16 -> new FloorComposition(14, 16);
			case 17 -> new FloorComposition(14, 18);
			case 18 -> new FloorComposition(14, 20);
			case 19 -> new FloorComposition(13, 25);
			default -> new FloorComposition(10,  0);
		};
	}

	// called when entering a new floor — sets up kill tracking data
	public static void execute(LevelAccessor world, Entity entity) {
		if (world == null || entity == null || !(entity instanceof Player))
			return;
		Player player = (Player) entity;
		if (!(world instanceof Level level) || level.dimension() != DKC_DIMENSION)
			return;
		CompoundTag data = player.getPersistentData();
		// only trigger for survival/adventure players
		if (player instanceof ServerPlayer serverPlayer) {
			net.minecraft.world.level.GameType gameMode = serverPlayer.gameMode.getGameModeForPlayer();
			if (gameMode != net.minecraft.world.level.GameType.SURVIVAL && gameMode != net.minecraft.world.level.GameType.ADVENTURE)
				return;
		}
		int currentFloor = DKCFloorDetectorProcedure.getCurrentFloor(player);
		// skip boss-only floors
		if (currentFloor == 1 || currentFloor == 20)
			return;
		// check if floor just changed
		if (!data.getBoolean("dkc_floor_just_changed"))
			return;
		// check if already spawned for this floor
		if (data.getBoolean("dkc_floor_" + currentFloor + "_spawned"))
			return;
		FloorComposition comp = getFloorComposition(currentFloor);
		data.putDouble("dkc_floor_" + currentFloor + "_required", comp.total());
		data.putDouble("dkc_floor_" + currentFloor + "_killed", 0);
		data.putInt("dkc_floor_" + currentFloor + "_demon_count", comp.demons());
		data.putInt("dkc_floor_" + currentFloor + "_knight_count", comp.knights());
		data.putBoolean("dkc_floor_" + currentFloor + "_spawned", true);
		data.putBoolean("dkc_floor_" + currentFloor + "_spawning", true);
		data.putLong("dkc_floor_" + currentFloor + "_enter_time",
				world instanceof Level _level ? _level.getGameTime() : 0);
	}

	// ── Spawn helpers ────────────────────────────────────────────────────────

	private static BlockPos findSpawnPos(ServerLevel serverLevel, double spawnX, double spawnY, double spawnZ) {
		BlockPos testPos = BlockPos.containing(spawnX, spawnY, spawnZ);
		// search down
		for (int i = 0; i < 20; i++) {
			BlockPos below = testPos.below();
			if (!serverLevel.isEmptyBlock(below) && serverLevel.isEmptyBlock(testPos) && serverLevel.isEmptyBlock(testPos.above()))
				return testPos;
			testPos = below;
		}
		// search up from original
		testPos = BlockPos.containing(spawnX, spawnY, spawnZ);
		for (int i = 0; i < 20; i++) {
			BlockPos below = testPos.below();
			if (!serverLevel.isEmptyBlock(below) && serverLevel.isEmptyBlock(testPos) && serverLevel.isEmptyBlock(testPos.above()))
				return testPos;
			testPos = testPos.above();
		}
		return null;
	}

	private static void spawnDemonWave(LevelAccessor world, Player player, int floor, int count) {
		if (count <= 0 || !(world instanceof ServerLevel serverLevel))
			return;
		double originX = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).dkc_x;
		double originY = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).dkc_y;
		double originZ = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).dkc_z;
		double floorStartZ = originZ + ((floor - 1) * 200);
		double floorStartX = originX - 100;
		int spawnedCount = 0;
		int attempts = 0;
		while (spawnedCount < count && attempts < count * 3) {
			attempts++;
			BlockPos pos = findSpawnPos(serverLevel,
					floorStartX + (Math.random() * 200),
					originY + 3,
					floorStartZ + (Math.random() * 200));
			if (pos == null) continue;
			DemonEntity demon = SololevelingModEntities.DEMON.get().spawn(serverLevel, pos, MobSpawnType.SPAWNER);
			if (demon != null) {
				demon.getPersistentData().putDouble("dkc_floor_number", floor);
				demon.getPersistentData().putString("dkc_spawned_by", player.getStringUUID());
				demon.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, false, false));
				demon.setTarget(player);
				spawnedCount++;
			}
		}
		System.out.println("[DKC] Floor " + floor + " - Spawned " + spawnedCount + "/" + count + " demons");
	}

	private static void spawnKnightWave(LevelAccessor world, Player player, int floor, int count) {
		if (count <= 0 || !(world instanceof ServerLevel serverLevel))
			return;
		double originX = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).dkc_x;
		double originY = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).dkc_y;
		double originZ = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables())).dkc_z;
		double floorStartZ = originZ + ((floor - 1) * 200);
		double floorStartX = originX - 100;
		int spawnedCount = 0;
		int attempts = 0;
		while (spawnedCount < count && attempts < count * 3) {
			attempts++;
			BlockPos pos = findSpawnPos(serverLevel,
					floorStartX + (Math.random() * 200),
					originY + 3,
					floorStartZ + (Math.random() * 200));
			if (pos == null) continue;
			DemonKnightEntity knight = SololevelingModEntities.DEMON_KNIGHT.get().spawn(serverLevel, pos, MobSpawnType.SPAWNER);
			if (knight != null) {
				knight.randomizeVariant();
				knight.getPersistentData().putDouble("dkc_floor_number", floor);
				knight.getPersistentData().putString("dkc_spawned_by", player.getStringUUID());
				knight.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, false, false));
				knight.setTarget(player);
				spawnedCount++;
			}
		}
		System.out.println("[DKC] Floor " + floor + " - Spawned " + spawnedCount + "/" + count + " demon knights");
	}

	// ── Delayed spawn + respawn check ────────────────────────────────────────

	public static void checkDelayedSpawn(LevelAccessor world, Entity entity) {
		if (!(world instanceof Level _level) || !(entity instanceof Player player))
			return;
		if (_level.dimension() != DKC_DIMENSION)
			return;
		if (!(world instanceof ServerLevel serverLevel))
			return;
		int currentFloor = DKCFloorDetectorProcedure.getCurrentFloor(player);
		if (currentFloor < 2 || currentFloor > 19)
			return;
		CompoundTag data = player.getPersistentData();
		if (!data.getBoolean("dkc_floor_" + currentFloor + "_spawned"))
			return;
		if (data.getBoolean("dkc_floor_" + currentFloor + "_complete"))
			return;
		long currentTime = _level.getGameTime();
		long enterTime = data.getLong("dkc_floor_" + currentFloor + "_enter_time");
		boolean initialSpawned = data.getBoolean("dkc_floor_" + currentFloor + "_initial_spawned");
		// initial spawn after 15 seconds (300 ticks)
		if (!initialSpawned && currentTime - enterTime >= 300) {
			data.putBoolean("dkc_floor_" + currentFloor + "_initial_spawned", true);
			data.putBoolean("dkc_floor_" + currentFloor + "_spawning", false);
			int demonCount = data.getInt("dkc_floor_" + currentFloor + "_demon_count");
			int knightCount = data.getInt("dkc_floor_" + currentFloor + "_knight_count");
			spawnDemonWave(world, player, currentFloor, demonCount);
			spawnKnightWave(world, player, currentFloor, knightCount);
			System.out.println("[DKC] Floor " + currentFloor + " - Initial spawn: "
					+ demonCount + " demons + " + knightCount + " knights");
			return;
		}
		// check every 10 seconds if all enemies are gone
		if (initialSpawned && currentTime % 200 == 0) {
			double killed = data.getDouble("dkc_floor_" + currentFloor + "_killed");
			double required = data.getDouble("dkc_floor_" + currentFloor + "_required");
			if (killed >= required) return;
			// count surviving demons and knights
			int demonsAlive = 0;
			int knightsAlive = 0;
			for (net.minecraft.world.entity.Entity ent : serverLevel.getAllEntities()) {
				String owner = ent.getPersistentData().getString("dkc_spawned_by");
				int entFloor = (int) ent.getPersistentData().getDouble("dkc_floor_number");
				if (entFloor == currentFloor && owner.equals(player.getStringUUID())) {
					if (ent instanceof DemonEntity) demonsAlive++;
					else if (ent instanceof DemonKnightEntity) knightsAlive++;
				}
			}
			if (demonsAlive == 0 && knightsAlive == 0) {
				// respawn proportionally to what's still needed
				int remaining = (int) (required - killed);
				int origDemons = data.getInt("dkc_floor_" + currentFloor + "_demon_count");
				int origKnights = data.getInt("dkc_floor_" + currentFloor + "_knight_count");
				int origTotal = origDemons + origKnights;
				int respawnDemons = origTotal > 0 ? Math.round((float) origDemons / origTotal * remaining) : remaining;
				int respawnKnights = remaining - respawnDemons;
				System.out.println("[DKC RESPAWN] Floor " + currentFloor + " - Respawning "
						+ respawnDemons + " demons + " + respawnKnights + " knights");
				spawnDemonWave(world, player, currentFloor, respawnDemons);
				spawnKnightWave(world, player, currentFloor, respawnKnights);
			}
		}
	}

	public static void checkWaveSpawn(LevelAccessor world, Entity entity) {
		// reserved for future wave logic
	}

	public static void respawnDemons(LevelAccessor world, Player player, int floor, int count) {
		spawnDemonWave(world, player, floor, count);
	}
}
