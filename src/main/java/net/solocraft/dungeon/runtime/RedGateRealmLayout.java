package net.solocraft.dungeon.runtime;

import net.solocraft.world.dimension.rift.RiftTerritory;

import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Stable spatial layout for independently unloadable Red Gate cells in the
 * shared snow dungeon realm.
 *
 * <p>Territory is encoded in the X lane and the reusable instance slot is
 * encoded in Z. The region is deliberately shifted away from the historical
 * snow-dungeon origin so old generated terrain cannot overlap new arenas.</p>
 */
public final class RedGateRealmLayout {
	public static final int REGION_ORIGIN = 1_048_576;
	public static final int CELL_SIZE = 1_024;
	public static final int MAX_SLOTS_PER_TERRITORY = DungeonInstanceSavedData.MAX_INSTANCES;
	private static final int LANE_RECOVERY_RADIUS = CELL_SIZE / 3;

	private RedGateRealmLayout() {
	}

	public static BlockPos center(RiftTerritory territory, int slot, int y) {
		if (territory == null)
			throw new IllegalArgumentException("A Red Gate territory is required.");
		if (slot < 0 || slot >= MAX_SLOTS_PER_TERRITORY)
			throw new IllegalArgumentException("Red Gate cell slot is outside the supported range.");
		return new BlockPos(REGION_ORIGIN + territory.ordinal() * CELL_SIZE, y,
				REGION_ORIGIN + slot * CELL_SIZE);
	}

	/** Recovers the encoded cell from an instance's persisted center. */
	public static Optional<Cell> cellAt(BlockPos center) {
		if (center == null)
			return Optional.empty();
		long deltaX = (long) center.getX() - REGION_ORIGIN;
		long deltaZ = (long) center.getZ() - REGION_ORIGIN;
		long lane = Math.floorDiv(deltaX + CELL_SIZE / 2L, CELL_SIZE);
		long slot = Math.floorDiv(deltaZ + CELL_SIZE / 2L, CELL_SIZE);
		if (lane < 0L || lane >= RiftTerritory.values().length
				|| slot < 0L || slot >= MAX_SLOTS_PER_TERRITORY)
			return Optional.empty();
		long expectedX = REGION_ORIGIN + lane * CELL_SIZE;
		long expectedZ = REGION_ORIGIN + slot * CELL_SIZE;
		if (Math.abs((long) center.getX() - expectedX) > LANE_RECOVERY_RADIUS
				|| Math.abs((long) center.getZ() - expectedZ) > LANE_RECOVERY_RADIUS)
			return Optional.empty();
		return Optional.of(new Cell(RiftTerritory.byIndex((int) lane), (int) slot));
	}

	/**
	 * Resolves the territory around a client camera or bounded arena entity.
	 * Z is unnecessary because each X lane always belongs to one territory.
	 */
	public static Optional<RiftTerritory> territoryAtX(double x) {
		long blockX = (long) Math.floor(x);
		long deltaX = blockX - REGION_ORIGIN;
		long lane = Math.floorDiv(deltaX + CELL_SIZE / 2L, CELL_SIZE);
		if (lane < 0L || lane >= RiftTerritory.values().length)
			return Optional.empty();
		long expectedX = REGION_ORIGIN + lane * CELL_SIZE;
		if (Math.abs(blockX - expectedX) > LANE_RECOVERY_RADIUS)
			return Optional.empty();
		return Optional.of(RiftTerritory.byIndex((int) lane));
	}

	public record Cell(RiftTerritory territory, int slot) {
	}
}
