package net.solocraft.dungeon;

import net.solocraft.init.SololevelingModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProceduralDungeonGenerator {
	private static final int EMPTY = 0;
	private static final int FLOOR = 1;
	private static final int WALL = 2;

	private ProceduralDungeonGenerator() {
	}

	public static ProceduralDungeonResult generate(ServerLevel level, BlockPos origin, ProceduralDungeonSettings settings, Entity owner) {
		RandomSource random = level.random;
		int spacing = settings.rank.maxRoomSize + 12;
		int gridSize = Math.max(96, settings.targetRooms * spacing + 64);
		int[][] grid = new int[gridSize][gridSize];
		List<DungeonRoom> rooms = placeMainRoute(random, gridSize, settings);
		markRooms(grid, rooms);
		markCorridors(grid, rooms, settings.rank.corridorWidth);
		wrapWalls(grid);

		DungeonRoom entry = rooms.get(0);
		DungeonRoom boss = rooms.stream().filter(room -> room.type == DungeonRoom.Type.BOSS).findFirst().orElse(rooms.get(rooms.size() - 1));
		int baseY = Math.max(6, origin.getY() - settings.rank.interiorHeight - 12);
		int offsetX = origin.getX() - entry.centerX();
		int offsetZ = origin.getZ() - entry.centerZ();

		buildShell(level, grid, baseY, offsetX, offsetZ, settings, random);
		buildObsidianBacking(level, grid, baseY, offsetX, offsetZ, settings);
		decorate(level, rooms, baseY, offsetX, offsetZ, settings, random);
		BlockPos portalPos = spawnReturnPortal(level, entry, baseY, offsetX, offsetZ, owner);
		int spawned = spawnEncounters(level, rooms, boss, baseY, offsetX, offsetZ, settings, owner, random);

		return new ProceduralDungeonResult(new BlockPos(offsetX + entry.centerX(), baseY + 1, offsetZ + entry.centerZ()), portalPos,
				new BlockPos(offsetX + boss.centerX(), baseY + 1, offsetZ + boss.centerZ()), rooms.size(), spawned);
	}

	private static List<DungeonRoom> placeMainRoute(RandomSource random, int gridSize, ProceduralDungeonSettings settings) {
		List<DungeonRoom> rooms = new ArrayList<>();
		int x = gridSize / 2;
		int z = gridSize / 2;
		rooms.add(new DungeonRoom(x, z, settings.rank.minRoomSize + 2, settings.rank.minRoomSize + 2, DungeonRoom.Type.ENTRY));

		for (int i = 1; i < settings.targetRooms; i++) {
			DungeonRoom previous = rooms.get(rooms.size() - 1);
			int size = randomSize(random, settings.rank.minRoomSize, settings.rank.maxRoomSize);
			DungeonRoom.Type type = DungeonRoom.Type.NORMAL;
			if (i == settings.targetRooms - 1) {
				size = settings.rank.bossRoomSize;
				type = DungeonRoom.Type.BOSS;
			} else if (i > 2 && random.nextFloat() < 0.16f) {
				type = DungeonRoom.Type.TREASURE;
			}

			int direction = random.nextInt(4);
			boolean placed = false;
			for (int attempt = 0; attempt < 12 && !placed; attempt++) {
				int distance = settings.rank.maxRoomSize + 8 + random.nextInt(8);
				int nx = previous.centerX();
				int nz = previous.centerZ();
				switch ((direction + attempt) % 4) {
					case 0 -> nx += distance;
					case 1 -> nx -= distance;
					case 2 -> nz += distance;
					default -> nz -= distance;
				}
				DungeonRoom candidate = new DungeonRoom(nx - size / 2, nz - size / 2, size, size, type);
				if (insideGrid(candidate, gridSize) && rooms.stream().noneMatch(room -> candidate.overlaps(room, 4))) {
					rooms.add(candidate);
					placed = true;
				}
			}
			if (!placed) {
				int nx = previous.centerX() + settings.rank.maxRoomSize + 12;
				DungeonRoom candidate = new DungeonRoom(nx - size / 2, previous.centerZ() - size / 2, size, size, type);
				if (insideGrid(candidate, gridSize))
					rooms.add(candidate);
			}
		}
		return rooms;
	}

	private static int randomSize(RandomSource random, int min, int max) {
		int size = min + random.nextInt(Math.max(1, max - min + 1));
		return size % 2 == 0 ? size + 1 : size;
	}

	private static boolean insideGrid(DungeonRoom room, int gridSize) {
		return room.gx > 4 && room.gz > 4 && room.gx + room.width < gridSize - 4 && room.gz + room.length < gridSize - 4;
	}

	private static void markRooms(int[][] grid, List<DungeonRoom> rooms) {
		for (DungeonRoom room : rooms) {
			for (int x = room.gx; x < room.gx + room.width; x++) {
				for (int z = room.gz; z < room.gz + room.length; z++) {
					grid[x][z] = FLOOR;
				}
			}
		}
	}

	private static void markCorridors(int[][] grid, List<DungeonRoom> rooms, int width) {
		int radius = Math.max(2, width / 2);
		for (int i = 0; i < rooms.size() - 1; i++) {
			DungeonRoom a = rooms.get(i);
			DungeonRoom b = rooms.get(i + 1);
			int ax = a.centerX();
			int az = a.centerZ();
			int bx = b.centerX();
			int bz = b.centerZ();
			for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
				for (int dz = -radius; dz <= radius; dz++)
					setFloor(grid, x, az + dz);
			}
			for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
				for (int dx = -radius; dx <= radius; dx++)
					setFloor(grid, bx + dx, z);
			}
		}
	}

	private static void setFloor(int[][] grid, int x, int z) {
		if (x >= 0 && z >= 0 && x < grid.length && z < grid[x].length)
			grid[x][z] = FLOOR;
	}

	private static void wrapWalls(int[][] grid) {
		int[][] copy = new int[grid.length][grid.length];
		for (int x = 0; x < grid.length; x++)
			System.arraycopy(grid[x], 0, copy[x], 0, grid[x].length);
		for (int x = 1; x < grid.length - 1; x++) {
			for (int z = 1; z < grid[x].length - 1; z++) {
				if (copy[x][z] != EMPTY)
					continue;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (copy[x + dx][z + dz] == FLOOR) {
							grid[x][z] = WALL;
							dx = 2;
							break;
						}
					}
				}
			}
		}
	}

	private static void buildShell(ServerLevel level, int[][] grid, int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		for (int gx = 0; gx < grid.length; gx++) {
			for (int gz = 0; gz < grid[gx].length; gz++) {
				int cell = grid[gx][gz];
				if (cell == EMPTY)
					continue;
				int x = offsetX + gx;
				int z = offsetZ + gz;
				if (cell == WALL) {
					for (int y = baseY; y <= ceilingY; y++)
						level.setBlock(new BlockPos(x, y, z), pickWall(settings.theme, random), 2);
				} else {
					level.setBlock(new BlockPos(x, baseY, z), pickFloor(settings.theme, random), 2);
					for (int y = baseY + 1; y < ceilingY; y++)
						level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
					level.setBlock(new BlockPos(x, ceilingY, z), pickCeiling(settings.theme, random), 2);
				}
			}
		}
	}

	private static void buildObsidianBacking(ServerLevel level, int[][] grid, int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		int bottomY = baseY - 1;
		int topY = ceilingY + 1;
		BlockState backing = Blocks.BEDROCK.defaultBlockState();
		for (int gx = 0; gx < grid.length; gx++) {
			for (int gz = 0; gz < grid[gx].length; gz++) {
				if (grid[gx][gz] == EMPTY)
					continue;
				int x = offsetX + gx;
				int z = offsetZ + gz;
				level.setBlock(new BlockPos(x, bottomY, z), backing, 2);
				level.setBlock(new BlockPos(x, topY, z), backing, 2);
				if (isOutside(grid, gx + 1, gz))
					placeBackingColumn(level, x + 1, z, bottomY, topY, backing);
				if (isOutside(grid, gx - 1, gz))
					placeBackingColumn(level, x - 1, z, bottomY, topY, backing);
				if (isOutside(grid, gx, gz + 1))
					placeBackingColumn(level, x, z + 1, bottomY, topY, backing);
				if (isOutside(grid, gx, gz - 1))
					placeBackingColumn(level, x, z - 1, bottomY, topY, backing);
			}
		}
	}

	private static boolean isOutside(int[][] grid, int x, int z) {
		return x < 0 || z < 0 || x >= grid.length || z >= grid[x].length || grid[x][z] == EMPTY;
	}

	private static void placeBackingColumn(ServerLevel level, int x, int z, int bottomY, int topY, BlockState backing) {
		for (int y = bottomY; y <= topY; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (level.getBlockState(pos).isAir())
				level.setBlock(pos, backing, 2);
		}
	}

	private static void decorate(ServerLevel level, List<DungeonRoom> rooms, int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		int lightY = ceilingY - 1;
		for (DungeonRoom room : rooms) {
			int x0 = offsetX + room.gx;
			int z0 = offsetZ + room.gz;
			BlockState accent = settings.theme.accent.defaultBlockState();
			BlockState pillar = settings.theme.pillar.defaultBlockState();
			BlockState light = lightState(settings.theme);
			for (int x = 2; x < room.width - 2; x += 4) {
				for (int z = 2; z < room.length - 2; z += 4) {
					if (random.nextFloat() < 0.45f || room.type != DungeonRoom.Type.NORMAL)
						level.setBlock(new BlockPos(x0 + x, lightY, z0 + z), light, 2);
				}
			}
			if (room.width >= 11) {
				placePillar(level, new BlockPos(x0 + 1, baseY + 1, z0 + 1), pillar, settings.rank.interiorHeight);
				placePillar(level, new BlockPos(x0 + room.width - 2, baseY + 1, z0 + 1), pillar, settings.rank.interiorHeight);
				placePillar(level, new BlockPos(x0 + 1, baseY + 1, z0 + room.length - 2), pillar, settings.rank.interiorHeight);
				placePillar(level, new BlockPos(x0 + room.width - 2, baseY + 1, z0 + room.length - 2), pillar, settings.rank.interiorHeight);
			}
			if (room.type == DungeonRoom.Type.ENTRY || room.type == DungeonRoom.Type.BOSS) {
				for (int dx = 1; dx < room.width - 1; dx++) {
					level.setBlock(new BlockPos(x0 + dx, baseY, z0 + room.length / 2), accent, 2);
				}
				for (int dz = 1; dz < room.length - 1; dz++) {
					level.setBlock(new BlockPos(x0 + room.width / 2, baseY, z0 + dz), accent, 2);
				}
			}
		}
	}

	private static void placePillar(ServerLevel level, BlockPos base, BlockState state, int height) {
		for (int y = 0; y < height; y++)
			level.setBlock(base.above(y), state, 2);
	}

	private static BlockPos spawnReturnPortal(ServerLevel level, DungeonRoom entry, int baseY, int offsetX, int offsetZ, Entity owner) {
		BlockPos pos = new BlockPos(offsetX + entry.centerX() - 2, baseY + 1, offsetZ + entry.centerZ());
		Entity portal = SololevelingModEntities.PORTAL_12.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
		if (portal != null && owner != null)
			portal.getPersistentData().putString("dungeon_tag", owner.getPersistentData().getString("dungeon_tag"));
		return pos;
	}

	private static int spawnEncounters(ServerLevel level, List<DungeonRoom> rooms, DungeonRoom bossRoom, int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings, Entity owner, RandomSource random) {
		List<EntityType<?>> normalTypes = normalTypes(settings.rank);
		EntityType<?> bossType = bossType(settings.rank);
		String dungeonTag = owner == null ? "" : owner.getPersistentData().getString("dungeon_tag");
		int spawned = 0;
		for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
			DungeonRoom room = rooms.get(roomIndex);
			if (room.type == DungeonRoom.Type.ENTRY || room.type == DungeonRoom.Type.TREASURE || room.type == DungeonRoom.Type.BOSS)
				continue;
			if (roomIndex == 1)
				continue;
			int packs = room.width >= 15 ? 2 : 1;
			for (int pack = 0; pack < packs; pack++) {
				for (int i = 0; i < settings.rank.packSize; i++) {
					EntityType<?> type = normalTypes.get(random.nextInt(normalTypes.size()));
					BlockPos pos = spawnPoint(room, baseY, offsetX, offsetZ, random);
					Entity spawnedEntity = type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
					if (spawnedEntity != null) {
						tagDungeonMob(spawnedEntity, dungeonTag);
						spawned++;
					}
				}
			}
		}
		Entity boss = bossType.spawn(level, new BlockPos(offsetX + bossRoom.centerX(), baseY + 1, offsetZ + bossRoom.centerZ()), MobSpawnType.MOB_SUMMONED);
		if (boss != null) {
			tagDungeonMob(boss, dungeonTag);
			spawned++;
		}
		return spawned;
	}

	private static BlockPos spawnPoint(DungeonRoom room, int baseY, int offsetX, int offsetZ, RandomSource random) {
		int margin = Math.min(3, Math.max(1, room.width / 5));
		int x = offsetX + room.gx + margin + random.nextInt(Math.max(1, room.width - margin * 2));
		int z = offsetZ + room.gz + margin + random.nextInt(Math.max(1, room.length - margin * 2));
		return new BlockPos(x, baseY + 1, z);
	}

	private static void tagDungeonMob(Entity entity, String dungeonTag) {
		if (!dungeonTag.isEmpty())
			entity.getPersistentData().putString("dungeon_tag", dungeonTag);
	}

	private static List<EntityType<?>> normalTypes(ProceduralDungeonRank rank) {
		return switch (rank) {
			case E -> List.of(SololevelingModEntities.GOBLIN_CLUB.get(), SololevelingModEntities.GOBLIN_ARCHER.get(), SololevelingModEntities.STEEL_FANGED_LYCAN.get());
			case D -> List.of(SololevelingModEntities.GOBLIN_CLUB.get(), SololevelingModEntities.GOBLIN_ARCHER.get(), SololevelingModEntities.GOBLIN_MAGE.get(), SololevelingModEntities.STEEL_FANGED_LYCAN.get());
			case C -> List.of(SololevelingModEntities.GREEN_ORC.get(), SololevelingModEntities.STONE_GOLEM.get(), SololevelingModEntities.SKELETON_WARRIOR.get());
			case B -> List.of(SololevelingModEntities.HIGH_ORC.get(), SololevelingModEntities.SKELETON_WARRIOR.get(), SololevelingModEntities.SKELETON_BRUTE.get());
			case A -> List.of(SololevelingModEntities.MUTATED.get(), SololevelingModEntities.MINI_GEM_GOLEM.get(), SololevelingModEntities.STONE_GOLEM.get());
			case S -> List.of(SololevelingModEntities.RED_ANTS.get(), SololevelingModEntities.HIGH_ORC.get(), SololevelingModEntities.DEMON_KNIGHT.get());
		};
	}

	private static EntityType<?> bossType(ProceduralDungeonRank rank) {
		return switch (rank) {
			case E, D -> SololevelingModEntities.GOBLIN_KING.get();
			case C -> SololevelingModEntities.ANCIENT_GOLEM.get();
			case B -> SololevelingModEntities.BARUKA.get();
			case A -> SololevelingModEntities.FUTURISTIC_GOLEM.get();
			case S -> SololevelingModEntities.GEM_GOLEM.get();
		};
	}

	private static BlockState pickFloor(DungeonTheme theme, RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.75f)
			return theme.floor.defaultBlockState();
		if (roll < 0.94f)
			return theme.accent.defaultBlockState();
		return theme.rare.defaultBlockState();
	}

	private static BlockState pickWall(DungeonTheme theme, RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.64f)
			return theme.wall.defaultBlockState();
		if (roll < 0.90f)
			return theme.accent.defaultBlockState();
		return theme.rare.defaultBlockState();
	}

	private static BlockState pickCeiling(DungeonTheme theme, RandomSource random) {
		return random.nextFloat() < 0.12f ? theme.accent.defaultBlockState() : theme.wall.defaultBlockState();
	}

	private static BlockState lightState(DungeonTheme theme) {
		BlockState state = theme.light.defaultBlockState();
		if (state.hasProperty(LanternBlock.HANGING))
			return state.setValue(LanternBlock.HANGING, true);
		return state;
	}
}
