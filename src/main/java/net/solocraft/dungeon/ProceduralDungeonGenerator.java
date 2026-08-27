package net.solocraft.dungeon;

import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.init.SololevelingModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;

import net.solocraft.SololevelingMod;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class ProceduralDungeonGenerator {
	private static final int EMPTY = 0;
	private static final int FLOOR = 1;
	private static final int WALL = 2;
	private ProceduralDungeonGenerator() {
	}

	public static ProceduralDungeonResult generate(ServerLevel level, BlockPos origin,
			ProceduralDungeonSettings settings, Entity owner) {
		return generate(level, origin, settings, owner, true);
	}

	/**
	 * Generates a built-in procedural dungeon.
	 *
	 * @param spawnInitialReturnPortal whether to place the return portal in the
	 *                                 generated entry room
	 */
	public static ProceduralDungeonResult generate(ServerLevel level, BlockPos origin,
			ProceduralDungeonSettings settings, Entity owner, boolean spawnInitialReturnPortal) {
		RandomSource random = level.getRandom();
		int spacing = settings.rank.maxRoomSize + 16;
		int gridSize = Math.max(112, settings.targetRooms * spacing + 72);
		int[][] grid = new int[gridSize][gridSize];
		boolean[][] roomCells = new boolean[gridSize][gridSize];
		boolean[][] corridorCells = new boolean[gridSize][gridSize];

		DungeonLayout layout = placeLayout(random, gridSize, settings);
		List<DungeonRoom> rooms = layout.rooms();
		markRooms(grid, roomCells, rooms);
		markCorridors(grid, corridorCells, layout.connections(), settings, random);
		GridBounds bounds = usedBounds(rooms, gridSize);
		wrapWalls(grid, bounds);

		DungeonRoom entry = rooms.get(0);
		DungeonRoom boss = rooms.stream().filter(room -> room.type == DungeonRoom.Type.BOSS)
				.findFirst().orElse(rooms.get(rooms.size() - 1));
		int baseY = Math.max(6, origin.getY() - settings.rank.interiorHeight - 12);
		int offsetX = origin.getX() - entry.centerX();
		int offsetZ = origin.getZ() - entry.centerZ();

		buildShell(level, grid, bounds, baseY, offsetX, offsetZ, settings, random);
		buildBedrockBacking(level, grid, bounds, baseY, offsetX, offsetZ, settings);
		decorate(level, grid, roomCells, corridorCells, bounds, rooms, baseY, offsetX, offsetZ,
				settings, random);
		BlockPos portalPos = returnPortalPosition(entry, baseY, offsetX, offsetZ);
		if (spawnInitialReturnPortal)
			spawnReturnPortal(level, portalPos, owner);
		int spawned = spawnEncounters(level, rooms, boss, baseY, offsetX, offsetZ, settings,
				owner, random, !spawnInitialReturnPortal);

		return new ProceduralDungeonResult(
				new BlockPos(offsetX + entry.centerX(), baseY + 1, offsetZ + entry.centerZ()),
				portalPos,
				new BlockPos(offsetX + boss.centerX(), baseY + 1, offsetZ + boss.centerZ()),
				rooms.size(), spawned);
	}

	private static DungeonLayout placeLayout(RandomSource random, int gridSize,
			ProceduralDungeonSettings settings) {
		List<DungeonRoom> rooms = new ArrayList<>();
		List<DungeonConnection> connections = new ArrayList<>();
		int entryWidth = randomOdd(random, settings.rank.minRoomSize + 2,
				Math.min(settings.rank.maxRoomSize, settings.rank.minRoomSize + 6));
		int entryLength = randomOdd(random, settings.rank.minRoomSize + 2,
				Math.min(settings.rank.maxRoomSize, settings.rank.minRoomSize + 6));
		DungeonRoom entry = new DungeonRoom(gridSize / 2 - entryWidth / 2,
				gridSize / 2 - entryLength / 2, entryWidth, entryLength,
				DungeonRoom.Type.ENTRY, random.nextBoolean()
						? DungeonRoom.Shape.CHAMFERED : DungeonRoom.Shape.RECTANGLE);
		rooms.add(entry);

		int branchTarget = Math.min(5, Math.max(1, settings.targetRooms / 5
				+ (settings.complexity >= 8 ? 1 : 0)));
		int mainTarget = Math.max(5, settings.targetRooms - branchTarget);
		DungeonRoom previous = entry;
		int previousDirection = random.nextInt(4);

		for (int index = 1; index < mainTarget - 1; index++) {
			DungeonRoom.Type type = index > 2 && random.nextFloat() < 0.10F
					? DungeonRoom.Type.TREASURE : DungeonRoom.Type.NORMAL;
			int preferred = chooseNextDirection(random, previousDirection);
			DungeonRoom candidate = attachRoom(random, previous, rooms, gridSize, settings,
					type, preferred);
			if (candidate == null)
				candidate = attachToAnyRoom(random, rooms, gridSize, settings, type, false);
			if (candidate == null)
				break;
			rooms.add(candidate);
			connections.add(new DungeonConnection(previous, candidate));
			previousDirection = directionBetween(previous, candidate);
			previous = candidate;
		}

		DungeonRoom boss = attachRoom(random, previous, rooms, gridSize, settings,
				DungeonRoom.Type.BOSS, chooseNextDirection(random, previousDirection));
		if (boss == null)
			boss = attachToAnyRoom(random, rooms, gridSize, settings,
					DungeonRoom.Type.BOSS, true);
		if (boss == null) {
			int size = settings.rank.bossRoomSize;
			boss = new DungeonRoom(previous.gx + previous.width + 8,
					previous.centerZ() - size / 2, size, size,
					DungeonRoom.Type.BOSS, DungeonRoom.Shape.CHAMFERED);
		}
		rooms.add(boss);
		connections.add(new DungeonConnection(previous, boss));

		int attempts = 0;
		while (rooms.size() < settings.targetRooms && attempts++ < settings.targetRooms * 12) {
			List<DungeonRoom> branchAnchors = rooms.stream()
					.filter(room -> room.type != DungeonRoom.Type.ENTRY
							&& room.type != DungeonRoom.Type.BOSS)
					.toList();
			if (branchAnchors.isEmpty())
				break;
			DungeonRoom anchor = branchAnchors.get(random.nextInt(branchAnchors.size()));
			DungeonRoom.Type type = random.nextFloat() < 0.38F
					? DungeonRoom.Type.TREASURE : DungeonRoom.Type.NORMAL;
			DungeonRoom branch = attachRoom(random, anchor, rooms, gridSize, settings,
					type, random.nextInt(4));
			if (branch != null) {
				rooms.add(branch);
				connections.add(new DungeonConnection(anchor, branch));
			}
		}
		return new DungeonLayout(List.copyOf(rooms), List.copyOf(connections));
	}

	private static DungeonRoom attachToAnyRoom(RandomSource random, List<DungeonRoom> rooms,
			int gridSize, ProceduralDungeonSettings settings, DungeonRoom.Type type,
			boolean preferLateRooms) {
		for (int attempt = 0; attempt < rooms.size() * 4; attempt++) {
			int index = preferLateRooms
					? Math.max(0, rooms.size() - 1 - attempt % Math.min(rooms.size(), 5))
					: random.nextInt(rooms.size());
			DungeonRoom anchor = rooms.get(index);
			if (anchor.type == DungeonRoom.Type.BOSS)
				continue;
			DungeonRoom candidate = attachRoom(random, anchor, rooms, gridSize, settings,
					type, random.nextInt(4));
			if (candidate != null)
				return candidate;
		}
		return null;
	}

	private static DungeonRoom attachRoom(RandomSource random, DungeonRoom anchor,
			List<DungeonRoom> rooms, int gridSize, ProceduralDungeonSettings settings,
			DungeonRoom.Type type, int preferredDirection) {
		for (int attempt = 0; attempt < 40; attempt++) {
			int direction = attempt < 4 ? Math.floorMod(preferredDirection + attempt, 4)
					: random.nextInt(4);
			int width;
			int length;
			if (type == DungeonRoom.Type.BOSS) {
				width = settings.rank.bossRoomSize;
				length = Math.max(settings.rank.minRoomSize,
						settings.rank.bossRoomSize + (random.nextBoolean() ? 2 : -2));
			} else {
				width = randomOdd(random, settings.rank.minRoomSize, settings.rank.maxRoomSize);
				length = randomOdd(random, settings.rank.minRoomSize, settings.rank.maxRoomSize);
			}
			DungeonRoom.Shape shape = chooseShape(random, type, settings);
			int gap = 6 + random.nextInt(7);
			int jitter = random.nextInt(11) - 5;
			int gx;
			int gz;
			switch (direction) {
				case 0 -> {
					gx = anchor.gx + anchor.width + gap;
					gz = anchor.centerZ() - length / 2 + jitter;
				}
				case 1 -> {
					gx = anchor.gx - gap - width;
					gz = anchor.centerZ() - length / 2 + jitter;
				}
				case 2 -> {
					gx = anchor.centerX() - width / 2 + jitter;
					gz = anchor.gz + anchor.length + gap;
				}
				default -> {
					gx = anchor.centerX() - width / 2 + jitter;
					gz = anchor.gz - gap - length;
				}
			}
			DungeonRoom candidate = new DungeonRoom(gx, gz, width, length, type, shape);
			if (insideGrid(candidate, gridSize)
					&& rooms.stream().noneMatch(room -> candidate.overlaps(room, 3)))
				return candidate;
		}
		return null;
	}

	private static DungeonRoom.Shape chooseShape(RandomSource random, DungeonRoom.Type type,
			ProceduralDungeonSettings settings) {
		float roll = random.nextFloat();
		if (type == DungeonRoom.Type.ENTRY)
			return roll < 0.55F ? DungeonRoom.Shape.CHAMFERED : DungeonRoom.Shape.RECTANGLE;
		if (type == DungeonRoom.Type.BOSS)
			return roll < 0.48F ? DungeonRoom.Shape.ROUND
					: roll < 0.86F ? DungeonRoom.Shape.CHAMFERED : DungeonRoom.Shape.RECTANGLE;
		if (type == DungeonRoom.Type.TREASURE)
			return roll < 0.42F ? DungeonRoom.Shape.ROUND
					: roll < 0.78F ? DungeonRoom.Shape.CHAMFERED : DungeonRoom.Shape.RECTANGLE;
		if (roll < 0.34F)
			return DungeonRoom.Shape.RECTANGLE;
		if (roll < 0.60F)
			return DungeonRoom.Shape.CHAMFERED;
		if (roll < 0.82F)
			return DungeonRoom.Shape.ROUND;
		return settings.rank.numericRank >= 2 || settings.complexity >= 5
				? DungeonRoom.Shape.CROSS : DungeonRoom.Shape.RECTANGLE;
	}

	private static int chooseNextDirection(RandomSource random, int previous) {
		float roll = random.nextFloat();
		if (roll < 0.34F)
			return previous;
		if (roll < 0.67F)
			return previous < 2 ? 2 + random.nextInt(2) : random.nextInt(2);
		return Math.floorMod(previous + 2, 4);
	}

	private static int directionBetween(DungeonRoom first, DungeonRoom second) {
		int dx = second.centerX() - first.centerX();
		int dz = second.centerZ() - first.centerZ();
		if (Math.abs(dx) >= Math.abs(dz))
			return dx >= 0 ? 0 : 1;
		return dz >= 0 ? 2 : 3;
	}

	private static int randomOdd(RandomSource random, int minimum, int maximum) {
		int min = Math.min(minimum, maximum);
		int max = Math.max(minimum, maximum);
		int value = min + random.nextInt(Math.max(1, max - min + 1));
		if ((value & 1) == 0)
			value = value < max ? value + 1 : value - 1;
		return Math.max(5, value);
	}

	private static boolean insideGrid(DungeonRoom room, int gridSize) {
		return room.gx > 6 && room.gz > 6
				&& room.gx + room.width < gridSize - 6
				&& room.gz + room.length < gridSize - 6;
	}

	private static void markRooms(int[][] grid, boolean[][] roomCells,
			List<DungeonRoom> rooms) {
		for (DungeonRoom room : rooms) {
			for (int x = 0; x < room.width; x++) {
				for (int z = 0; z < room.length; z++) {
					if (!room.containsLocal(x, z))
						continue;
					int gx = room.gx + x;
					int gz = room.gz + z;
					grid[gx][gz] = FLOOR;
					roomCells[gx][gz] = true;
				}
			}
		}
	}

	private static void markCorridors(int[][] grid, boolean[][] corridorCells,
			List<DungeonConnection> connections, ProceduralDungeonSettings settings,
			RandomSource random) {
		for (DungeonConnection connection : connections) {
			int ax = connection.first().centerX();
			int az = connection.first().centerZ();
			int bx = connection.second().centerX();
			int bz = connection.second().centerZ();
			int radius = Math.max(2, settings.rank.corridorWidth / 2
					+ random.nextInt(3) - 1);
			boolean dogleg = settings.complexity >= 5 && random.nextFloat() < 0.24F
					&& Math.abs(ax - bx) > 12 && Math.abs(az - bz) > 12;
			if (dogleg) {
				int midpointX = Mth.clamp((ax + bx) / 2 + random.nextInt(9) - 4,
						Math.min(ax, bx), Math.max(ax, bx));
				carveHorizontal(grid, corridorCells, ax, midpointX, az, radius);
				carveVertical(grid, corridorCells, az, bz, midpointX, radius);
				carveHorizontal(grid, corridorCells, midpointX, bx, bz, radius);
				carveJunction(grid, corridorCells, midpointX, az, radius + 1);
				carveJunction(grid, corridorCells, midpointX, bz, radius + 1);
			} else if (random.nextBoolean()) {
				carveHorizontal(grid, corridorCells, ax, bx, az, radius);
				carveVertical(grid, corridorCells, az, bz, bx, radius);
				if (random.nextFloat() < 0.55F)
					carveJunction(grid, corridorCells, bx, az, radius + 1);
			} else {
				carveVertical(grid, corridorCells, az, bz, ax, radius);
				carveHorizontal(grid, corridorCells, ax, bx, bz, radius);
				if (random.nextFloat() < 0.55F)
					carveJunction(grid, corridorCells, ax, bz, radius + 1);
			}
		}
	}

	private static void carveHorizontal(int[][] grid, boolean[][] corridorCells,
			int fromX, int toX, int z, int radius) {
		for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
			for (int dz = -radius; dz <= radius; dz++)
				setFloor(grid, corridorCells, x, z + dz);
		}
	}

	private static void carveVertical(int[][] grid, boolean[][] corridorCells,
			int fromZ, int toZ, int x, int radius) {
		for (int z = Math.min(fromZ, toZ); z <= Math.max(fromZ, toZ); z++) {
			for (int dx = -radius; dx <= radius; dx++)
				setFloor(grid, corridorCells, x + dx, z);
		}
	}

	private static void carveJunction(int[][] grid, boolean[][] corridorCells,
			int centerX, int centerZ, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz <= radius * radius + radius)
					setFloor(grid, corridorCells, centerX + dx, centerZ + dz);
			}
		}
	}

	private static void setFloor(int[][] grid, boolean[][] corridorCells, int x, int z) {
		if (x < 0 || z < 0 || x >= grid.length || z >= grid[x].length)
			return;
		grid[x][z] = FLOOR;
		corridorCells[x][z] = true;
	}

	private static GridBounds usedBounds(List<DungeonRoom> rooms, int gridSize) {
		int minX = gridSize - 1;
		int minZ = gridSize - 1;
		int maxX = 0;
		int maxZ = 0;
		for (DungeonRoom room : rooms) {
			minX = Math.min(minX, room.gx);
			minZ = Math.min(minZ, room.gz);
			maxX = Math.max(maxX, room.gx + room.width - 1);
			maxZ = Math.max(maxZ, room.gz + room.length - 1);
		}
		return new GridBounds(Math.max(1, minX - 4), Math.max(1, minZ - 4),
				Math.min(gridSize - 2, maxX + 4), Math.min(gridSize - 2, maxZ + 4));
	}

	private static void wrapWalls(int[][] grid, GridBounds bounds) {
		for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
			for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
				if (grid[x][z] != EMPTY)
					continue;
				boolean nearFloor = false;
				for (int dx = -1; dx <= 1 && !nearFloor; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (grid[x + dx][z + dz] == FLOOR) {
							nearFloor = true;
							break;
						}
					}
				}
				if (nearFloor)
					grid[x][z] = WALL;
			}
		}
	}

	private static void buildShell(ServerLevel level, int[][] grid, GridBounds bounds,
			int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings,
			RandomSource random) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		for (int gx = bounds.minX(); gx <= bounds.maxX(); gx++) {
			for (int gz = bounds.minZ(); gz <= bounds.maxZ(); gz++) {
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

	private static void buildBedrockBacking(ServerLevel level, int[][] grid, GridBounds bounds,
			int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		int bottomY = baseY - 1;
		int topY = ceilingY + 1;
		BlockState backing = Blocks.BEDROCK.defaultBlockState();
		for (int gx = bounds.minX(); gx <= bounds.maxX(); gx++) {
			for (int gz = bounds.minZ(); gz <= bounds.maxZ(); gz++) {
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
		return x < 0 || z < 0 || x >= grid.length || z >= grid[x].length
				|| grid[x][z] == EMPTY;
	}

	private static void placeBackingColumn(ServerLevel level, int x, int z, int bottomY,
			int topY, BlockState backing) {
		for (int y = bottomY; y <= topY; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (level.getBlockState(pos).isAir())
				level.setBlock(pos, backing, 2);
		}
	}

	private static void decorate(ServerLevel level, int[][] grid, boolean[][] roomCells,
			boolean[][] corridorCells, GridBounds bounds, List<DungeonRoom> rooms,
			int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings,
			RandomSource random) {
		for (int index = 0; index < rooms.size(); index++) {
			DungeonRoom room = rooms.get(index);
			decorateFloorPattern(level, room, baseY, offsetX, offsetZ, settings, random);
			decorateWallBands(level, grid, room, baseY, offsetX, offsetZ, settings);
			decorateCeiling(level, room, baseY, offsetX, offsetZ, settings, random);
			placeRoomLights(level, room, baseY, offsetX, offsetZ, settings, random);
			if (index != 1) {
				decorateArchitecture(level, room, baseY, offsetX, offsetZ, settings, random);
				decorateThemeFeatures(level, room, baseY, offsetX, offsetZ, settings, random);
			}
		}
		decorateCorridors(level, roomCells, corridorCells, bounds, baseY, offsetX,
				offsetZ, settings, random);
	}

	private static void decorateFloorPattern(ServerLevel level, DungeonRoom room, int baseY,
			int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int pattern = room.type == DungeonRoom.Type.ENTRY ? 0
				: room.type == DungeonRoom.Type.BOSS ? 1 : random.nextInt(6);
		int cx = room.width / 2;
		int cz = room.length / 2;
		int ring = Math.max(3, Math.min(room.width, room.length) / 3);
		for (int x = 0; x < room.width; x++) {
			for (int z = 0; z < room.length; z++) {
				if (!room.containsLocal(x, z))
					continue;
				int dx = Math.abs(x - cx);
				int dz = Math.abs(z - cz);
				boolean accent = switch (pattern) {
					case 0 -> x == cx || z == cz;
					case 1 -> Math.abs(dx + dz - ring) <= 1 || (dx <= 1 && dz <= 1);
					case 2 -> Math.floorMod(x + z, 6) == 0;
					case 3 -> Math.floorMod(x - cx, 5) == 0;
					case 4 -> (x % 5 == 2 && z % 5 == 2);
					default -> room.isEdgeLocal(x, z) || (dx == dz && dx < ring);
				};
				if (!accent)
					continue;
				BlockState state = random.nextFloat() < 0.10F
						? settings.theme.rare.defaultBlockState()
						: settings.theme.accent.defaultBlockState();
				level.setBlock(new BlockPos(offsetX + room.gx + x, baseY,
						offsetZ + room.gz + z), state, 2);
			}
		}
	}

	private static void decorateWallBands(ServerLevel level, int[][] grid, DungeonRoom room,
			int baseY, int offsetX, int offsetZ, ProceduralDungeonSettings settings) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		for (int gx = room.gx - 1; gx <= room.gx + room.width; gx++) {
			for (int gz = room.gz - 1; gz <= room.gz + room.length; gz++) {
				if (gx < 0 || gz < 0 || gx >= grid.length || gz >= grid[gx].length
						|| grid[gx][gz] != WALL || !adjacentToRoom(room, gx, gz))
					continue;
				BlockPos lower = new BlockPos(offsetX + gx, baseY + 2, offsetZ + gz);
				level.setBlock(lower, settings.theme.accent.defaultBlockState(), 2);
				if (settings.rank.interiorHeight >= 7)
					level.setBlock(new BlockPos(offsetX + gx, ceilingY - 2, offsetZ + gz),
							settings.theme.pillar.defaultBlockState(), 2);
			}
		}
	}

	private static boolean adjacentToRoom(DungeonRoom room, int gx, int gz) {
		return room.containsGrid(gx + 1, gz) || room.containsGrid(gx - 1, gz)
				|| room.containsGrid(gx, gz + 1) || room.containsGrid(gx, gz - 1);
	}

	private static void decorateCeiling(ServerLevel level, DungeonRoom room, int baseY,
			int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		BlockState rib = settings.theme.accent.defaultBlockState();
		int variant = random.nextInt(3);
		for (int x = 0; x < room.width; x++) {
			for (int z = 0; z < room.length; z++) {
				if (!room.containsLocal(x, z))
					continue;
				boolean replace = variant == 0
						? (x == room.width / 2 || z == room.length / 2)
						: variant == 1 ? (x % 6 == 2) : (z % 6 == 2);
				if (replace)
					level.setBlock(new BlockPos(offsetX + room.gx + x, ceilingY,
							offsetZ + room.gz + z), rib, 2);
			}
		}
	}

	private static void placeRoomLights(ServerLevel level, DungeonRoom room, int baseY,
			int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int spacing = room.type == DungeonRoom.Type.BOSS ? 5 : 6;
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		for (int x = 2; x < room.width - 2; x += spacing) {
			for (int z = 2; z < room.length - 2; z += spacing) {
				if (!room.containsLocal(x, z) || random.nextFloat() > 0.72F)
					continue;
				placeCeilingLight(level, new BlockPos(offsetX + room.gx + x, ceilingY,
						offsetZ + room.gz + z), settings.theme);
			}
		}
	}

	private static void placeCeilingLight(ServerLevel level, BlockPos ceiling,
			DungeonTheme theme) {
		BlockState light = lightState(theme);
		if (light.hasProperty(LanternBlock.HANGING)) {
			// Keep the ceiling sealed; lanterns hang one block below it.
			level.setBlock(ceiling.below(), light, 2);
		} else {
			level.setBlock(ceiling, light, 2);
		}
	}

	private static void decorateArchitecture(ServerLevel level, DungeonRoom room, int baseY,
			int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		BlockState pillar = settings.theme.pillar.defaultBlockState();
		int fullHeight = settings.rank.interiorHeight;
		int[][] points = {
				{room.width / 4, room.length / 4},
				{room.width * 3 / 4, room.length / 4},
				{room.width / 4, room.length * 3 / 4},
				{room.width * 3 / 4, room.length * 3 / 4}
		};
		int variant = room.type == DungeonRoom.Type.BOSS ? 0 : random.nextInt(4);
		for (int index = 0; index < points.length; index++) {
			int localX = points[index][0];
			int localZ = points[index][1];
			if (!safeFeatureCell(room, localX, localZ))
				continue;
			int height = switch (variant) {
				case 0 -> fullHeight;
				case 1 -> Math.max(2, fullHeight / 2 + index % 2);
				case 2 -> index % 2 == 0 ? fullHeight : Math.max(2, fullHeight / 3);
				default -> 2 + random.nextInt(Math.max(1, fullHeight - 2));
			};
			placePillar(level, new BlockPos(offsetX + room.gx + localX, baseY + 1,
					offsetZ + room.gz + localZ), pillar, height);
		}
	}

	private static void decorateThemeFeatures(ServerLevel level, DungeonRoom room, int baseY,
			int offsetX, int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int featureCount = room.type == DungeonRoom.Type.BOSS ? 5
				: room.type == DungeonRoom.Type.TREASURE ? 4 : 2 + random.nextInt(3);
		for (int index = 0; index < featureCount; index++) {
			int localX = 2 + random.nextInt(Math.max(1, room.width - 4));
			int localZ = 2 + random.nextInt(Math.max(1, room.length - 4));
			if (!safeFeatureCell(room, localX, localZ))
				continue;
			BlockState state = settings.theme.pillar.defaultBlockState();
			int height = 2;
			switch (settings.theme) {
				case STONE -> {
					state = random.nextBoolean() ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
							: Blocks.COBBLESTONE.defaultBlockState();
					height = 1 + random.nextInt(2);
				}
				case ICE -> {
					state = random.nextBoolean() ? Blocks.BLUE_ICE.defaultBlockState()
							: Blocks.PACKED_ICE.defaultBlockState();
					height = 2 + random.nextInt(2);
				}
				case NETHER -> {
					state = random.nextBoolean() ? Blocks.POLISHED_BASALT.defaultBlockState()
							: Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
					height = 2 + random.nextInt(3);
				}
				case DESERT -> {
					state = random.nextBoolean() ? Blocks.CHISELED_SANDSTONE.defaultBlockState()
							: Blocks.CUT_SANDSTONE.defaultBlockState();
					height = 1 + random.nextInt(3);
				}
				case MOSSY -> {
					state = random.nextBoolean() ? Blocks.MOSS_BLOCK.defaultBlockState()
							: Blocks.OAK_LOG.defaultBlockState();
					height = 1 + random.nextInt(2);
				}
				case VOID -> {
					state = random.nextBoolean() ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
							: Blocks.GILDED_BLACKSTONE.defaultBlockState();
					height = 2 + random.nextInt(3);
				}
				case DEEPSLATE -> {
					state = random.nextBoolean() ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
							: Blocks.POLISHED_BASALT.defaultBlockState();
					height = 2 + random.nextInt(3);
				}
				case PRISMARINE -> {
					state = random.nextBoolean() ? Blocks.PRISMARINE.defaultBlockState()
							: Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
					height = 1 + random.nextInt(3);
				}
			}
			placePillar(level, new BlockPos(offsetX + room.gx + localX, baseY + 1,
					offsetZ + room.gz + localZ), state, height);
		}
	}

	private static boolean safeFeatureCell(DungeonRoom room, int localX, int localZ) {
		if (!room.containsLocal(localX, localZ))
			return false;
		int dx = Math.abs(localX - room.width / 2);
		int dz = Math.abs(localZ - room.length / 2);
		return dx > 2 && dz > 2;
	}

	private static void decorateCorridors(ServerLevel level, boolean[][] roomCells,
			boolean[][] corridorCells, GridBounds bounds, int baseY, int offsetX,
			int offsetZ, ProceduralDungeonSettings settings, RandomSource random) {
		int ceilingY = baseY + settings.rank.interiorHeight + 1;
		for (int gx = bounds.minX(); gx <= bounds.maxX(); gx++) {
			for (int gz = bounds.minZ(); gz <= bounds.maxZ(); gz++) {
				if (!corridorCells[gx][gz] || roomCells[gx][gz])
					continue;
				int hash = Math.floorMod(gx * 31 + gz * 17, 23);
				if (hash == 0 || hash == 1)
					level.setBlock(new BlockPos(offsetX + gx, baseY, offsetZ + gz),
							settings.theme.accent.defaultBlockState(), 2);
				if (hash == 7 && random.nextFloat() < 0.55F)
					placeCeilingLight(level, new BlockPos(offsetX + gx, ceilingY,
							offsetZ + gz), settings.theme);
			}
		}
	}

	private static void placePillar(ServerLevel level, BlockPos base, BlockState state,
			int height) {
		for (int y = 0; y < height; y++)
			level.setBlock(base.above(y), state, 2);
	}

	private static BlockPos returnPortalPosition(DungeonRoom entry, int baseY,
			int offsetX, int offsetZ) {
		return new BlockPos(offsetX + entry.centerX() - 2, baseY + 1,
				offsetZ + entry.centerZ());
	}

	private static void spawnReturnPortal(ServerLevel level, BlockPos pos, Entity owner) {
		Entity portal = SololevelingModEntities.PORTAL_12.get().spawn(level, pos,
				MobSpawnType.MOB_SUMMONED);
		if (portal != null && owner != null)
			portal.getPersistentData().putString("dungeon_tag",
					owner.getPersistentData().getString("dungeon_tag"));
	}

	private static int spawnEncounters(ServerLevel level, List<DungeonRoom> rooms,
			DungeonRoom bossRoom, int baseY, int offsetX, int offsetZ,
			ProceduralDungeonSettings settings, Entity owner, RandomSource random,
			boolean deferredReturnPortal) {
		List<MobChoice> normalTypes = normalTypes(settings.rank);
		BossChoice bossChoice = pickBoss(bossTypes(settings.rank), random);
		String dungeonTag = owner == null ? ""
				: owner.getPersistentData().getString("dungeon_tag");
		int spawned = 0;
		for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
			DungeonRoom room = rooms.get(roomIndex);
			if (room.type == DungeonRoom.Type.ENTRY
					|| room.type == DungeonRoom.Type.TREASURE
					|| room.type == DungeonRoom.Type.BOSS || roomIndex == 1)
				continue;
			int packs = packsForRoom(room, settings, random);
			for (int pack = 0; pack < packs; pack++) {
				MobChoice core = pickMob(normalTypes, random);
				int packSize = packSizeFor(settings, random);
				for (int index = 0; index < packSize; index++) {
					MobChoice choice = index > 0 && random.nextFloat() < 0.28F
							? pickMob(normalTypes, random) : core;
					BlockPos pos = spawnPoint(level, room, baseY, offsetX, offsetZ, random);
					Entity spawnedEntity = choice.type().spawn(level, pos,
							MobSpawnType.MOB_SUMMONED);
					if (spawnedEntity != null) {
						tagDungeonMob(spawnedEntity, dungeonTag, settings.rank,
								deferredReturnPortal);
						LowRankDungeonBalance.applyMobBalance(spawnedEntity,
								settings.rank);
						DungeonMobVariantScaler.applyForRank(spawnedEntity, settings.rank, random);
						spawned++;
					}
				}
			}
		}
		// Placed at the room centre, cleared to the boss's own size.
		//
		// Two ways to get this wrong, and this code has now had both. Spawning on
		// the raw centre without checking meant a pillar there left the gate with
		// no boss at all. Borrowing the ordinary mob's spawn point instead was
		// worse: that check clears a one-block column, which is honest for a
		// goblin and meaningless for a Goblin King, so the boss materialised
		// inside a wall and suffocated.
		//
		// The centre is the farthest point from every wall, so it is where a large
		// entity has the best chance. Clearing to the entity's real width and
		// height is what makes that chance a certainty.
		BlockPos bossPos = new BlockPos(offsetX + bossRoom.centerX(), baseY + 1,
				offsetZ + bossRoom.centerZ());
		clearFor(level, bossPos, bossChoice.type(), settings.rank.interiorHeight);
		Entity boss = bossChoice.type().spawn(level, bossPos, MobSpawnType.MOB_SUMMONED);
		if (boss != null) {
			tagDungeonMob(boss, dungeonTag, settings.rank, deferredReturnPortal);
			LowRankDungeonBalance.applyMobBalance(boss, settings.rank);
			applyBossLevel(boss, bossChoice, random);
			spawned++;
		} else {
			// Still nothing. Say so: a silent failure here is a gate the player
			// walks around in for ten minutes looking for a fight.
			SololevelingMod.LOGGER.error(
					"Dungeon boss {} could not be spawned in the {} boss room at {}",
					bossChoice.type(), settings.rank, bossPos);
		}
		return spawned;
	}

	/**
	 * Larger rooms can hold a second encounter, but the cap stays small so layout
	 * randomness cannot turn one generated room into an entity spike.
	 */
	private static int packsForRoom(DungeonRoom room,
			ProceduralDungeonSettings settings, RandomSource random) {
		// A/S rooms are substantially larger and appear in much longer runs. One
		// focused encounter per room keeps their challenge in elite strength and
		// mechanics instead of creating a server-heavy mob horde.
		if (settings.rank == ProceduralDungeonRank.A
				|| settings.rank == ProceduralDungeonRank.S)
			return 1;
		int area = room.width * room.length;
		int largeRoomThreshold = settings.rank.minRoomSize
				* settings.rank.minRoomSize + settings.rank.minRoomSize * 2;
		int packs = area >= largeRoomThreshold ? 2 : 1;
		if (settings.rank.numericRank >= ProceduralDungeonRank.C.numericRank
				&& settings.complexity >= 8 && random.nextFloat() < 0.25F)
			packs++;
		return Math.min(3, packs);
	}

	/** Per-pack variance makes repeat layouts less predictable while remaining bounded. */
	private static int packSizeFor(ProceduralDungeonSettings settings,
			RandomSource random) {
		boolean highRank = settings.rank == ProceduralDungeonRank.A
				|| settings.rank == ProceduralDungeonRank.S;
		int variance = highRank ? 1 : 1 + Math.min(2, settings.complexity / 4);
		int size = settings.rank.packSize + random.nextInt(variance);
		if (!highRank && settings.complexity >= 7 && random.nextFloat() < 0.35F)
			size++;
		int maximum = switch (settings.rank) {
			case E -> 3;
			case D -> 4;
			case C -> 5;
			case B -> 6;
			case A -> 2;
			case S -> 3;
		};
		return Mth.clamp(size, settings.rank.packSize, maximum);
	}

	/**
	 * Clears a pocket big enough for the entity that is about to stand in it.
	 *
	 * <p>Sized from the entity type rather than from a fixed 3x3, because the
	 * bosses differ in width and the whole point is that the one being placed
	 * fits. A generated dungeon is ours to carve, so this is cheaper and more
	 * reliable than hunting for a spot that happens to be big enough.
	 *
	 * <p>Bounded by the room's own interior height so the carve cannot reach
	 * through the ceiling.
	 */
	private static void clearFor(ServerLevel level, BlockPos centre, EntityType<?> type,
			int interiorHeight) {
		int halfWidth = Math.max(1, Mth.ceil(type.getWidth() / 2.0F));
		// Never taller than the room. An E-rank interior is four blocks and its
		// boss is four blocks, so a carve with any headroom added would take the
		// ceiling out and open the dungeon to whatever is above it.
		int height = Math.min(Math.max(2, interiorHeight),
				Math.max(2, Mth.ceil(type.getHeight())));
		for (int y = 0; y < height; y++)
			for (int dx = -halfWidth; dx <= halfWidth; dx++)
				for (int dz = -halfWidth; dz <= halfWidth; dz++) {
					BlockPos at = centre.offset(dx, y, dz);
					if (!level.getBlockState(at).isAir())
						level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
				}
	}

	private static BlockPos spawnPoint(ServerLevel level, DungeonRoom room, int baseY,
			int offsetX, int offsetZ, RandomSource random) {
		int marginX = Math.min(3, Math.max(1, room.width / 5));
		int marginZ = Math.min(3, Math.max(1, room.length / 5));
		for (int attempt = 0; attempt < 32; attempt++) {
			int localX = marginX + random.nextInt(Math.max(1, room.width - marginX * 2));
			int localZ = marginZ + random.nextInt(Math.max(1, room.length - marginZ * 2));
			if (!room.containsLocal(localX, localZ))
				continue;
			BlockPos candidate = new BlockPos(offsetX + room.gx + localX, baseY + 1,
					offsetZ + room.gz + localZ);
			boolean clear = true;
			for (int y = 0; y < 4; y++) {
				if (!level.getBlockState(candidate.above(y)).isAir()) {
					clear = false;
					break;
				}
			}
			if (clear)
				return candidate;
		}
		return new BlockPos(offsetX + room.centerX(), baseY + 1,
				offsetZ + room.centerZ());
	}

	private static void tagDungeonMob(Entity entity, String dungeonTag,
			ProceduralDungeonRank rank, boolean deferredReturnPortal) {
		if (!dungeonTag.isEmpty())
			entity.getPersistentData().putString("dungeon_tag", dungeonTag);
		if (deferredReturnPortal)
			ProceduralDungeonCompletionHandler.markProceduralMob(entity);
		entity.getPersistentData().putString(
				LowRankDungeonBalance.PROCEDURAL_RANK_TAG, rank.name());
		DungeonMobHealthCompatibilityGuard.stabilize(entity);
	}

	private static MobChoice pickMob(List<MobChoice> choices, RandomSource random) {
		int total = choices.stream().mapToInt(MobChoice::weight).sum();
		int roll = random.nextInt(Math.max(1, total));
		for (MobChoice choice : choices) {
			roll -= choice.weight();
			if (roll < 0)
				return choice;
		}
		return choices.get(choices.size() - 1);
	}

	private static BossChoice pickBoss(List<BossChoice> choices,
			RandomSource random) {
		int total = choices.stream().mapToInt(BossChoice::weight).sum();
		int roll = random.nextInt(Math.max(1, total));
		for (BossChoice choice : choices) {
			roll -= choice.weight();
			if (roll < 0)
				return choice;
		}
		return choices.get(choices.size() - 1);
	}

	/**
	 * High-rank Kaiselin rolls receive an explicit rank-band level before combat.
	 * The shared adapter adds durable boss stats and mirrors the legacy Level tag,
	 * so display, extraction, XP scaling, and combat comparisons agree.
	 */
	private static void applyBossLevel(Entity entity, BossChoice choice,
			RandomSource random) {
		if (!(entity instanceof Mob mob) || choice.maximumLevel() <= 0)
			return;
		int level = Mth.nextInt(random, choice.minimumLevel(),
				choice.maximumLevel());
		mob.getPersistentData().putString(DungeonMobLevelAdapter.ROLE_TAG,
				DungeonMobLevelAdapter.MobRole.BOSS.id());
		DungeonMobLevelAdapter.applyGenericScaling(mob, level,
				DungeonMobLevelAdapter.MobRole.BOSS);
	}

	private static List<MobChoice> normalTypes(ProceduralDungeonRank rank) {
		return switch (rank) {
			case E -> List.of(
					mob(SololevelingModEntities.GOBLIN_CLUB.get(), 62),
					mob(SololevelingModEntities.GOBLIN_ARCHER.get(), 38));
			case D -> List.of(
					mob(SololevelingModEntities.GOBLIN_CLUB.get(), 42),
					mob(SololevelingModEntities.GOBLIN_ARCHER.get(), 30),
					mob(SololevelingModEntities.GOBLIN_MAGE.get(), 18),
					mob(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 10));
			case C -> List.of(
					mob(SololevelingModEntities.GREEN_ORC.get(), 29),
					mob(SololevelingModEntities.STONE_GOLEM.get(), 24),
					mob(SololevelingModEntities.SKELETON_WARRIOR.get(), 25),
					mob(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 14),
					mob(SololevelingModEntities.GOBLIN_MAGE.get(), 8));
			case B -> List.of(
					mob(SololevelingModEntities.HIGH_ORC.get(), 28),
					mob(SololevelingModEntities.SKELETON_WARRIOR.get(), 22),
					mob(SololevelingModEntities.SKELETON_BRUTE.get(), 25),
					mob(SololevelingModEntities.GREEN_ORC.get(), 11),
					mob(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 10),
					mob(SololevelingModEntities.GOBLIN_MAGE.get(), 4));
			case A -> List.of(
					mob(SololevelingModEntities.MUTATED.get(), 29),
					mob(SololevelingModEntities.MINI_GEM_GOLEM.get(), 24),
					mob(SololevelingModEntities.STONE_GOLEM.get(), 20),
					mob(SololevelingModEntities.HIGH_ORC.get(), 16),
					mob(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 8),
					mob(SololevelingModEntities.GOBLIN_MAGE.get(), 3));
			case S -> List.of(
					mob(SololevelingModEntities.RED_ANTS.get(), 34),
					mob(SololevelingModEntities.HIGH_ORC.get(), 26),
					mob(SololevelingModEntities.DEMON_KNIGHT.get(), 26),
					mob(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 8),
					mob(SololevelingModEntities.GREEN_ORC.get(), 4),
					mob(SololevelingModEntities.GOBLIN_MAGE.get(), 2));
		};
	}

	private static MobChoice mob(EntityType<?> type, int weight) {
		return new MobChoice(type, weight);
	}

	/**
	 * Boss roster per rank.
	 *
	 * <p>E through B used to be single-entry lists, which quietly defeated the
	 * rank variety the gate spawner works hard to produce: a D-rank player rolls
	 * E or D gates, but both ranks resolved to the Goblin King, so every gate
	 * they ever cleared had the same boss. The weight of 100 on a one-item list
	 * was rolling against nothing.
	 *
	 * <p>Every alternate below is already a normal spawn at its own rank, so
	 * promoting it to boss adds variety without introducing anything the player
	 * would not already meet there. Boss scaling is applied on top by
	 * {@code DungeonMobLevelAdapter}, so these stay rank-appropriate.
	 */
	private static List<BossChoice> bossTypes(ProceduralDungeonRank rank) {
		return switch (rank) {
			case E -> List.of(
					boss(SololevelingModEntities.GOBLIN_KING.get(), 60),
					boss(SololevelingModEntities.GOBLIN_ARCHER.get(), 22),
					boss(SololevelingModEntities.GOBLIN_CLUB.get(), 18));
			case D -> List.of(
					boss(SololevelingModEntities.GOBLIN_KING.get(), 42),
					boss(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 33),
					boss(SololevelingModEntities.GOBLIN_MAGE.get(), 25));
			case C -> List.of(
					boss(SololevelingModEntities.ANCIENT_GOLEM.get(), 40),
					boss(SololevelingModEntities.STONE_GOLEM.get(), 32),
					boss(SololevelingModEntities.SKELETON_WARRIOR.get(), 28));
			case B -> List.of(
					boss(SololevelingModEntities.BARUKA.get(), 40),
					boss(SololevelingModEntities.SKELETON_BRUTE.get(), 32),
					boss(SololevelingModEntities.HIGH_ORC.get(), 28));
			case A -> List.of(
					boss(SololevelingModEntities.FUTURISTIC_GOLEM.get(), 70),
					boss(SololevelingModEntities.KAISELIN.get(), 30, 75, 89));
			case S -> List.of(
					boss(SololevelingModEntities.GEM_GOLEM.get(), 55),
					boss(SololevelingModEntities.KAISELIN.get(), 45, 100, 119));
		};
	}

	private static BossChoice boss(EntityType<?> type, int weight) {
		return boss(type, weight, 0, 0);
	}

	private static BossChoice boss(EntityType<?> type, int weight,
			int minimumLevel, int maximumLevel) {
		return new BossChoice(type, weight, minimumLevel, maximumLevel);
	}

	private static BlockState pickFloor(DungeonTheme theme, RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.88F)
			return theme.floor.defaultBlockState();
		if (roll < 0.98F)
			return theme.accent.defaultBlockState();
		return theme.rare.defaultBlockState();
	}

	private static BlockState pickWall(DungeonTheme theme, RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.72F)
			return theme.wall.defaultBlockState();
		if (roll < 0.95F)
			return theme.accent.defaultBlockState();
		return theme.rare.defaultBlockState();
	}

	private static BlockState pickCeiling(DungeonTheme theme, RandomSource random) {
		return random.nextFloat() < 0.08F
				? theme.accent.defaultBlockState() : theme.wall.defaultBlockState();
	}

	private static BlockState lightState(DungeonTheme theme) {
		BlockState state = theme.light.defaultBlockState();
		if (state.hasProperty(LanternBlock.HANGING))
			return state.setValue(LanternBlock.HANGING, true);
		return state;
	}

	private record DungeonLayout(List<DungeonRoom> rooms,
			List<DungeonConnection> connections) {
	}

	private record DungeonConnection(DungeonRoom first, DungeonRoom second) {
	}

	private record GridBounds(int minX, int minZ, int maxX, int maxZ) {
	}

	private record MobChoice(EntityType<?> type, int weight) {
	}

	private record BossChoice(EntityType<?> type, int weight,
			int minimumLevel, int maximumLevel) {
	}
}
