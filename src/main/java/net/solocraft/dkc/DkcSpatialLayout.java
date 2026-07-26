package net.solocraft.dkc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Coordinate layout for all private Demon King's Castle runs in the shared
 * DKC level. Each persistent player slot owns one sector containing a 5 x 4
 * grid of 2,048-block floor cells.
 */
public final class DkcSpatialLayout {
	public static final String ACTIVE_RUN_TAG = "dkc_inside_castle";
	public static final int CELL_SIZE = 2_048;
	public static final int FLOOR_COLUMNS = 5;
	public static final int FLOOR_ROWS = 4;
	public static final int SECTOR_COLUMNS = 64;
	public static final int SECTOR_STRIDE = 16_384;
	public static final int BASE_X = 4_000_000;
	public static final int BASE_Z = 4_000_000;
	public static final int MAX_SECTOR_ROWS = 64;
	public static final int MAX_SLOTS = SECTOR_COLUMNS * MAX_SECTOR_ROWS;

	private static final int HALF_CELL = CELL_SIZE / 2;
	private static final Location OUTSIDE = new Location(-1, 0);

	private DkcSpatialLayout() {
	}

	public static BlockPos floorOrigin(int slot, int floor) {
		validateSlot(slot);
		validateFloor(floor);
		int sectorColumn = slot % SECTOR_COLUMNS;
		int sectorRow = slot / SECTOR_COLUMNS;
		int floorIndex = floor - 1;
		int floorColumn = floorIndex % FLOOR_COLUMNS;
		int floorRow = floorIndex / FLOOR_COLUMNS;
		return new BlockPos(
				BASE_X + sectorColumn * SECTOR_STRIDE + floorColumn * CELL_SIZE,
				DkcFloorRegistry.BUILD_Y,
				BASE_Z + sectorRow * SECTOR_STRIDE + floorRow * CELL_SIZE);
	}

	/** Client/server convenience for choosing the visual floor at a position. */
	public static int floorAt(BlockPos position) {
		if (position == null)
			return 0;
		long packed = packedLocation(position.getX(), position.getZ());
		return packed == 0L ? 0 : (int) (packed & 0xFFL);
	}

	public static int slotAt(BlockPos position) {
		if (position == null)
			return -1;
		long packed = packedLocation(position.getX(), position.getZ());
		return packed == 0L ? -1 : (int) (packed >>> 8) - 1;
	}

	/**
	 * Returns an authoritative floor only when the player occupies the spatial
	 * cell assigned to their persistent run slot in the shared DKC dimension.
	 */
	public static int floor(ServerPlayer player) {
		if (player == null || player.server == null || !DkcFloorRegistry.isSharedDkc(player.level()))
			return 0;
		int slot = DkcRunSavedData.get(player.server).slot(player);
		long packed = packedLocation(player.getX(), player.getZ());
		return packed != 0L && (int) (packed >>> 8) - 1 == slot ? (int) (packed & 0xFFL) : 0;
	}

	public static boolean isPlayerInFloor(ServerPlayer player, int floor) {
		return validFloor(floor) && floor(player) == floor;
	}

	/** Validates both an entity's coordinates and the persistent slot of its owner. */
	public static boolean isEntityInOwnedFloor(Entity entity, UUID owner, int floor) {
		if (entity == null || owner == null || !validFloor(floor)
				|| !DkcFloorRegistry.isSharedDkc(entity.level()) || entity.getServer() == null)
			return false;
		int slot = DkcRunSavedData.get(entity.getServer()).slot(owner);
		if (slot < 0)
			return false;
		long packed = packedLocation(entity.getX(), entity.getZ());
		return packed != 0L && (int) (packed >>> 8) - 1 == slot && (int) (packed & 0xFFL) == floor;
	}

	public static boolean isInsideSlotFloor(int slot, int floor, BlockPos position) {
		if (!validSlot(slot) || !validFloor(floor) || position == null)
			return false;
		long packed = packedLocation(position.getX(), position.getZ());
		return packed != 0L && (int) (packed >>> 8) - 1 == slot && (int) (packed & 0xFFL) == floor;
	}

	public static Location locate(double x, double z) {
		long packed = packedLocation(x, z);
		return packed == 0L ? OUTSIDE : new Location((int) (packed >>> 8) - 1, (int) (packed & 0xFFL));
	}

	private static long packedLocation(double x, double z) {
		long blockX = (long) Math.floor(x);
		long blockZ = (long) Math.floor(z);
		long firstSectorMinX = (long) BASE_X - HALF_CELL;
		long firstSectorMinZ = (long) BASE_Z - HALF_CELL;
		int sectorColumn = (int) Math.floorDiv(blockX - firstSectorMinX, (long) SECTOR_STRIDE);
		int sectorRow = (int) Math.floorDiv(blockZ - firstSectorMinZ, (long) SECTOR_STRIDE);
		if (sectorColumn < 0 || sectorColumn >= SECTOR_COLUMNS
				|| sectorRow < 0 || sectorRow >= MAX_SECTOR_ROWS)
			return 0L;

		long sectorBaseX = (long) BASE_X + sectorColumn * (long) SECTOR_STRIDE;
		long sectorBaseZ = (long) BASE_Z + sectorRow * (long) SECTOR_STRIDE;
		int floorColumn = (int) Math.floorDiv(blockX - sectorBaseX + HALF_CELL, (long) CELL_SIZE);
		int floorRow = (int) Math.floorDiv(blockZ - sectorBaseZ + HALF_CELL, (long) CELL_SIZE);
		if (floorColumn < 0 || floorColumn >= FLOOR_COLUMNS || floorRow < 0 || floorRow >= FLOOR_ROWS)
			return 0L;

		int floor = floorRow * FLOOR_COLUMNS + floorColumn + 1;
		int slot = sectorRow * SECTOR_COLUMNS + sectorColumn;
		return ((long) (slot + 1) << 8) | floor;
	}

	public static boolean validSlot(int slot) {
		return slot >= 0 && slot < MAX_SLOTS;
	}

	private static boolean validFloor(int floor) {
		return floor >= DkcFloorRegistry.FIRST_FLOOR && floor <= DkcFloorRegistry.LAST_FLOOR;
	}

	private static void validateSlot(int slot) {
		if (!validSlot(slot))
			throw new IllegalArgumentException("DKC slot must be between 0 and " + (MAX_SLOTS - 1) + ": " + slot);
	}

	private static void validateFloor(int floor) {
		if (!validFloor(floor))
			throw new IllegalArgumentException("DKC floor must be between 1 and 20: " + floor);
	}

	public record Location(int slot, int floor) {
		public boolean isInside() {
			return slot >= 0 && floor > 0;
		}
	}
}
