package net.solocraft.dungeon.runtime;

import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.WorldBounds;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bounded world-edit helpers for the final protective dungeon shell and its
 * explicitly authored connection openings.
 *
 * <p>Room and connection bounds are inclusive interior volumes. Shell blocks
 * are written only outside those volumes. Any shell candidate contained by any
 * declared volume is skipped, so a connection that overlaps a room opening is
 * never sealed by the room's shell.</p>
 */
public final class DungeonShellBuilder {
	public static final int DEFAULT_MAX_CHANGED_BLOCKS = 250_000;
	public static final int ABSOLUTE_MAX_CHANGED_BLOCKS = 2_000_000;
	public static final int DEFAULT_MAX_CARVED_BLOCKS = 4_096;
	public static final int ABSOLUTE_MAX_CARVED_BLOCKS = 32_768;

	private static final int MAX_THICKNESS = 4;
	private static final int MAX_CARVE_AXIS = 64;
	private static final long MAX_RAW_SHELL_CANDIDATES = 8_000_000L;
	private static final int QUIET_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
			| Block.UPDATE_SUPPRESS_DROPS;

	private DungeonShellBuilder() {
	}

	public static ShellResult buildShell(ServerLevel level, Collection<WorldBounds> rooms,
			Collection<WorldBounds> carvedConnections, ResourceLocation shellBlockId, int thickness) {
		return buildShell(level, rooms, carvedConnections, shellBlockId, thickness,
				DEFAULT_MAX_CHANGED_BLOCKS);
	}

	public static ShellResult buildShell(ServerLevel level, Collection<WorldBounds> rooms,
			Collection<WorldBounds> carvedConnections, ResourceLocation shellBlockId, int thickness,
			int maxChangedBlocks) {
		if (shellBlockId == null)
			return ShellResult.failure(ErrorCode.INVALID_ARGUMENT, "A shell block ID is required.");
		Block block = ForgeRegistries.BLOCKS.getValue(shellBlockId);
		if (block == null)
			return ShellResult.failure(ErrorCode.UNKNOWN_BLOCK, "Unknown shell block " + shellBlockId + ".");
		return buildShell(level, rooms, carvedConnections, block.defaultBlockState(), thickness,
				maxChangedBlocks);
	}

	public static ShellResult buildShell(ServerLevel level, Collection<WorldBounds> rooms,
			Collection<WorldBounds> carvedConnections, BlockState shellState, int thickness) {
		return buildShell(level, rooms, carvedConnections, shellState, thickness,
				DEFAULT_MAX_CHANGED_BLOCKS);
	}

	/**
	 * Builds a shell after a complete preflight. No blocks are changed when bounds,
	 * build height, candidate count, or the requested change cap is invalid.
	 */
	public static ShellResult buildShell(ServerLevel level, Collection<WorldBounds> rooms,
			Collection<WorldBounds> carvedConnections, BlockState shellState, int thickness,
			int maxChangedBlocks) {
		return processShell(level, rooms, carvedConnections, shellState, thickness, maxChangedBlocks, true);
	}

	/** Performs the same full shell scan and safety checks without changing blocks. */
	public static ShellResult preflightShell(ServerLevel level, Collection<WorldBounds> rooms,
			Collection<WorldBounds> carvedConnections, BlockState shellState, int thickness) {
		return processShell(level, rooms, carvedConnections, shellState, thickness,
				DEFAULT_MAX_CHANGED_BLOCKS, false);
	}

	private static ShellResult processShell(ServerLevel level, Collection<WorldBounds> rooms,
			Collection<WorldBounds> carvedConnections, BlockState shellState, int thickness,
			int maxChangedBlocks, boolean applyChanges) {
		if (level == null || rooms == null || carvedConnections == null || shellState == null)
			return ShellResult.failure(ErrorCode.INVALID_ARGUMENT,
					"Level, room bounds, connection bounds, and shell state are required.");
		if (thickness < 0 || thickness > MAX_THICKNESS)
			return ShellResult.failure(ErrorCode.INVALID_THICKNESS,
					"Shell thickness must be between 0 and " + MAX_THICKNESS + ".");
		if (maxChangedBlocks < 0 || maxChangedBlocks > ABSOLUTE_MAX_CHANGED_BLOCKS)
			return ShellResult.failure(ErrorCode.INVALID_CHANGE_CAP,
					"Shell change cap must be between 0 and " + ABSOLUTE_MAX_CHANGED_BLOCKS + ".");
		if (shellState.isAir())
			return ShellResult.failure(ErrorCode.INVALID_SHELL_STATE,
					"Air cannot be used as a protective shell block.");

		BoundsResult boundsResult = collectBounds(rooms, carvedConnections);
		if (!boundsResult.success)
			return ShellResult.failure(boundsResult.error, boundsResult.message);
		List<WorldBounds> allVolumes = boundsResult.bounds;

		long rawCandidateEstimate = 0L;
		List<WorldBounds> expandedVolumes = new ArrayList<>(allVolumes.size());
		for (WorldBounds volume : allVolumes) {
			ValidationResult validation = validateBounds(level, volume, thickness);
			if (!validation.success)
				return ShellResult.failure(validation.error, validation.message);
			WorldBounds expanded = expand(volume, thickness);
			expandedVolumes.add(expanded);
			rawCandidateEstimate = saturatedAdd(rawCandidateEstimate,
					shellCandidateVolume(expanded, volume));
			if (rawCandidateEstimate > MAX_RAW_SHELL_CANDIDATES)
				return ShellResult.failure(ErrorCode.TOO_MANY_CANDIDATES,
						"Shell preflight exceeds the hard candidate limit of "
								+ MAX_RAW_SHELL_CANDIDATES + " blocks.");
		}

		if (thickness == 0)
			return ShellResult.success("Shell thickness is zero; no blocks changed.",
					0L, 0L, 0L, 0L, 0L);

		LongOpenHashSet candidates = new LongOpenHashSet((int) Math.min(rawCandidateEstimate,
				ABSOLUTE_MAX_CHANGED_BLOCKS));
		long skippedInside = 0L;
		for (int i = 0; i < allVolumes.size(); i++) {
			WorldBounds interior = allVolumes.get(i);
			WorldBounds expanded = expandedVolumes.get(i);
			for (int x = expanded.min().getX(); x <= expanded.max().getX(); x++) {
				for (int y = expanded.min().getY(); y <= expanded.max().getY(); y++) {
					for (int z = expanded.min().getZ(); z <= expanded.max().getZ(); z++) {
						if (contains(interior, x, y, z))
							continue;
						if (containsAny(allVolumes, x, y, z)) {
							skippedInside++;
							continue;
						}
						candidates.add(BlockPos.asLong(x, y, z));
					}
				}
			}
		}

		LongArrayList changes = new LongArrayList(Math.min(candidates.size(), maxChangedBlocks));
		long alreadyMatching = 0L;
		long protectedBlockEntities = 0L;
		LongIterator candidateIterator = candidates.iterator();
		while (candidateIterator.hasNext()) {
			long packed = candidateIterator.nextLong();
			BlockPos position = BlockPos.of(packed);
			BlockState existing = level.getBlockState(position);
			if (existing.equals(shellState)) {
				alreadyMatching++;
				continue;
			}
			if (level.getBlockEntity(position) != null) {
				protectedBlockEntities++;
				continue;
			}
			if (changes.size() >= maxChangedBlocks)
				return ShellResult.failure(ErrorCode.CHANGE_LIMIT_EXCEEDED,
						"Shell requires more than the allowed " + maxChangedBlocks + " block changes.",
						candidates.size(), 0L, alreadyMatching, skippedInside, 0L);
			changes.add(packed);
		}
		if (protectedBlockEntities > 0L)
			return ShellResult.failure(ErrorCode.PROTECTED_BLOCK_ENTITY,
					"Protective shell would overwrite " + protectedBlockEntities
							+ " block entities; no shell blocks were changed.", candidates.size(), 0L,
					alreadyMatching, skippedInside, 0L);
		if (!applyChanges)
			return ShellResult.success("Protective shell preflight passed.", candidates.size(),
					0L, alreadyMatching, skippedInside, 0L);

		long changed = 0L;
		long failedWrites = 0L;
		for (int i = 0; i < changes.size(); i++) {
			BlockPos position = BlockPos.of(changes.getLong(i));
			if (level.setBlock(position, shellState, QUIET_UPDATE_FLAGS))
				changed++;
			else
				failedWrites++;
		}
		if (failedWrites > 0L)
			return ShellResult.failure(ErrorCode.WRITE_FAILED,
					"Some protective shell blocks could not be written.", candidates.size(), changed,
					alreadyMatching, skippedInside, failedWrites);
		return ShellResult.success("Protective dungeon shell built.", candidates.size(), changed,
				alreadyMatching, skippedInside, 0L);
	}

	public static CarveResult carveConnection(ServerLevel level, WorldBounds connection) {
		return carveConnection(level, connection, DEFAULT_MAX_CARVED_BLOCKS);
	}

	/**
	 * Performs the complete connection validation without changing the world.
	 *
	 * <p>The conservative change-budget check assumes every position may become
	 * non-air after room templates are placed. This makes a successful preflight
	 * remain valid when doorway wall blocks are added later in the same commit.</p>
	 */
	public static CarveResult preflightConnection(ServerLevel level, WorldBounds connection) {
		return preflightConnection(level, connection, DEFAULT_MAX_CARVED_BLOCKS);
	}

	public static CarveResult preflightConnection(ServerLevel level, WorldBounds connection,
			int maxChangedBlocks) {
		return processConnection(level, connection, maxChangedBlocks, false, true);
	}

	/**
	 * Clears exactly one explicitly supplied inclusive connection volume. The full
	 * edit is preflighted, and block entities cause rejection rather than silent
	 * deletion of authored data.
	 */
	public static CarveResult carveConnection(ServerLevel level, WorldBounds connection,
			int maxChangedBlocks) {
		return processConnection(level, connection, maxChangedBlocks, true, false);
	}

	private static CarveResult processConnection(ServerLevel level, WorldBounds connection,
			int maxChangedBlocks, boolean applyChanges, boolean conservativeChangeBudget) {
		if (level == null || connection == null)
			return CarveResult.failure(ErrorCode.INVALID_ARGUMENT,
					"A level and explicit connection volume are required.");
		if (maxChangedBlocks < 0 || maxChangedBlocks > ABSOLUTE_MAX_CARVED_BLOCKS)
			return CarveResult.failure(ErrorCode.INVALID_CHANGE_CAP,
					"Connection carve cap must be between 0 and " + ABSOLUTE_MAX_CARVED_BLOCKS + ".");
		ValidationResult validation = validateBounds(level, connection, 0);
		if (!validation.success)
			return CarveResult.failure(validation.error, validation.message);

		long volume = volumeOf(connection);
		if (connection.sizeX() > MAX_CARVE_AXIS || connection.sizeY() > MAX_CARVE_AXIS
				|| connection.sizeZ() > MAX_CARVE_AXIS || volume > ABSOLUTE_MAX_CARVED_BLOCKS)
			return CarveResult.failure(ErrorCode.CARVE_VOLUME_TOO_LARGE,
					"Connection carving is limited to " + MAX_CARVE_AXIS + " blocks per axis and "
							+ ABSOLUTE_MAX_CARVED_BLOCKS + " blocks total.", volume, 0L, 0L, 0L, 0L);
		if (conservativeChangeBudget && volume > maxChangedBlocks)
			return CarveResult.failure(ErrorCode.CHANGE_LIMIT_EXCEEDED,
					"Connection could require " + volume + " block changes after room placement, above the allowed "
							+ maxChangedBlocks + ".", volume, 0L, 0L, 0L, 0L);

		LongArrayList changes = new LongArrayList((int) Math.min(volume, maxChangedBlocks));
		long alreadyAir = 0L;
		long protectedBlockEntities = 0L;
		for (int x = connection.min().getX(); x <= connection.max().getX(); x++) {
			for (int y = connection.min().getY(); y <= connection.max().getY(); y++) {
				for (int z = connection.min().getZ(); z <= connection.max().getZ(); z++) {
					BlockPos position = new BlockPos(x, y, z);
					if (level.getBlockState(position).isAir()) {
						alreadyAir++;
						continue;
					}
					if (level.getBlockEntity(position) != null) {
						protectedBlockEntities++;
						continue;
					}
					if (changes.size() >= maxChangedBlocks)
						return CarveResult.failure(ErrorCode.CHANGE_LIMIT_EXCEEDED,
								"Connection requires more than the allowed " + maxChangedBlocks + " block changes.",
								volume, 0L, alreadyAir, protectedBlockEntities, 0L);
					changes.add(position.asLong());
				}
			}
		}
		if (protectedBlockEntities > 0L)
			return CarveResult.failure(ErrorCode.PROTECTED_BLOCK_ENTITY,
					"Connection contains " + protectedBlockEntities
							+ " block entities; nothing was carved to protect their data.",
					volume, 0L, alreadyAir, protectedBlockEntities, 0L);
		if (!applyChanges)
			return CarveResult.success("Connection preflight passed.", volume, 0L, alreadyAir,
					protectedBlockEntities, 0L);

		long changed = 0L;
		long failedWrites = 0L;
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int i = 0; i < changes.size(); i++) {
			if (level.setBlock(BlockPos.of(changes.getLong(i)), air, QUIET_UPDATE_FLAGS))
				changed++;
			else
				failedWrites++;
		}
		if (failedWrites > 0L)
			return CarveResult.failure(ErrorCode.WRITE_FAILED,
					"Some connection blocks could not be cleared.", volume, changed, alreadyAir,
					protectedBlockEntities, failedWrites);
		return CarveResult.success("Connection carved.", volume, changed, alreadyAir,
				protectedBlockEntities, 0L);
	}

	private static BoundsResult collectBounds(Collection<WorldBounds> rooms,
			Collection<WorldBounds> connections) {
		List<WorldBounds> all = new ArrayList<>(rooms.size() + connections.size());
		for (WorldBounds room : rooms) {
			if (room == null)
				return BoundsResult.failure(ErrorCode.INVALID_BOUNDS, "Room bounds cannot contain null entries.");
			all.add(room);
		}
		for (WorldBounds connection : connections) {
			if (connection == null)
				return BoundsResult.failure(ErrorCode.INVALID_BOUNDS,
						"Connection bounds cannot contain null entries.");
			all.add(connection);
		}
		if (all.isEmpty())
			return BoundsResult.failure(ErrorCode.EMPTY_VOLUMES,
					"At least one room or connection volume is required.");
		return BoundsResult.success(all);
	}

	private static ValidationResult validateBounds(ServerLevel level, WorldBounds bounds, int expansion) {
		long minX = (long) bounds.min().getX() - expansion;
		long minY = (long) bounds.min().getY() - expansion;
		long minZ = (long) bounds.min().getZ() - expansion;
		long maxX = (long) bounds.max().getX() + expansion;
		long maxY = (long) bounds.max().getY() + expansion;
		long maxZ = (long) bounds.max().getZ() + expansion;
		if (minX < Integer.MIN_VALUE || minZ < Integer.MIN_VALUE || maxX > Integer.MAX_VALUE
				|| maxZ > Integer.MAX_VALUE)
			return ValidationResult.failure(ErrorCode.INVALID_BOUNDS,
					"Expanded bounds exceed supported block coordinates.");
		if (minY < level.getMinBuildHeight() || maxY >= level.getMaxBuildHeight())
			return ValidationResult.failure(ErrorCode.OUT_OF_BUILD_HEIGHT,
					"Bounds and shell must stay between Y=" + level.getMinBuildHeight() + " and Y="
							+ (level.getMaxBuildHeight() - 1) + ".");

		BlockPos expandedMin = new BlockPos((int) minX, (int) minY, (int) minZ);
		BlockPos expandedMax = new BlockPos((int) maxX, (int) maxY, (int) maxZ);
		if (!level.getWorldBorder().isWithinBounds(expandedMin)
				|| !level.getWorldBorder().isWithinBounds(expandedMax))
			return ValidationResult.failure(ErrorCode.OUTSIDE_WORLD_BORDER,
					"Dungeon shell bounds must remain inside the world border.");
		return ValidationResult.valid();
	}

	private static WorldBounds expand(WorldBounds bounds, int amount) {
		return new WorldBounds(bounds.min().offset(-amount, -amount, -amount),
				bounds.max().offset(amount, amount, amount));
	}

	private static boolean containsAny(List<WorldBounds> bounds, int x, int y, int z) {
		for (WorldBounds volume : bounds) {
			if (contains(volume, x, y, z))
				return true;
		}
		return false;
	}

	private static boolean contains(WorldBounds bounds, int x, int y, int z) {
		return x >= bounds.min().getX() && x <= bounds.max().getX()
				&& y >= bounds.min().getY() && y <= bounds.max().getY()
				&& z >= bounds.min().getZ() && z <= bounds.max().getZ();
	}

	private static long volumeOf(WorldBounds bounds) {
		long x = (long) bounds.max().getX() - bounds.min().getX() + 1L;
		long y = (long) bounds.max().getY() - bounds.min().getY() + 1L;
		long z = (long) bounds.max().getZ() - bounds.min().getZ() + 1L;
		return saturatedMultiply(saturatedMultiply(x, y), z);
	}

	private static long shellCandidateVolume(WorldBounds expanded, WorldBounds interior) {
		long expandedVolume = volumeOf(expanded);
		if (expandedVolume == Long.MAX_VALUE)
			return Long.MAX_VALUE;
		long interiorVolume = volumeOf(interior);
		return Math.max(0L, expandedVolume - interiorVolume);
	}

	private static long saturatedMultiply(long first, long second) {
		if (first <= 0L || second <= 0L)
			return 0L;
		if (first > Long.MAX_VALUE / second)
			return Long.MAX_VALUE;
		return first * second;
	}

	private static long saturatedAdd(long first, long second) {
		if (first > Long.MAX_VALUE - second)
			return Long.MAX_VALUE;
		return first + second;
	}

	public enum ErrorCode {
		NONE,
		INVALID_ARGUMENT,
		INVALID_THICKNESS,
		INVALID_CHANGE_CAP,
		INVALID_SHELL_STATE,
		UNKNOWN_BLOCK,
		INVALID_BOUNDS,
		EMPTY_VOLUMES,
		OUT_OF_BUILD_HEIGHT,
		OUTSIDE_WORLD_BORDER,
		TOO_MANY_CANDIDATES,
		CHANGE_LIMIT_EXCEEDED,
		CARVE_VOLUME_TOO_LARGE,
		PROTECTED_BLOCK_ENTITY,
		WRITE_FAILED
	}

	public record ShellResult(boolean success, ErrorCode error, String message,
			long candidatePositions, long changedBlocks, long alreadyMatching,
			long skippedInsideVolumes, long failedWrites) {
		private static ShellResult success(String message, long candidates, long changed,
				long matching, long skipped, long failed) {
			return new ShellResult(true, ErrorCode.NONE, message, candidates, changed,
					matching, skipped, failed);
		}

		private static ShellResult failure(ErrorCode error, String message) {
			return failure(error, message, 0L, 0L, 0L, 0L, 0L);
		}

		private static ShellResult failure(ErrorCode error, String message, long candidates,
				long changed, long matching, long skipped, long failed) {
			return new ShellResult(false, error, message, candidates, changed, matching,
					skipped, failed);
		}
	}

	public record CarveResult(boolean success, ErrorCode error, String message,
			long volume, long changedBlocks, long alreadyAir, long protectedBlockEntities,
			long failedWrites) {
		private static CarveResult success(String message, long volume, long changed,
				long air, long protectedEntities, long failed) {
			return new CarveResult(true, ErrorCode.NONE, message, volume, changed, air,
					protectedEntities, failed);
		}

		private static CarveResult failure(ErrorCode error, String message) {
			return failure(error, message, 0L, 0L, 0L, 0L, 0L);
		}

		private static CarveResult failure(ErrorCode error, String message, long volume,
				long changed, long air, long protectedEntities, long failed) {
			return new CarveResult(false, error, message, volume, changed, air,
					protectedEntities, failed);
		}
	}

	private record BoundsResult(boolean success, ErrorCode error, String message,
			List<WorldBounds> bounds) {
		private static BoundsResult success(List<WorldBounds> bounds) {
			return new BoundsResult(true, ErrorCode.NONE, "", bounds);
		}

		private static BoundsResult failure(ErrorCode error, String message) {
			return new BoundsResult(false, error, message, List.of());
		}
	}

	private record ValidationResult(boolean success, ErrorCode error, String message) {
		private static ValidationResult valid() {
			return new ValidationResult(true, ErrorCode.NONE, "");
		}

		private static ValidationResult failure(ErrorCode error, String message) {
			return new ValidationResult(false, error, message);
		}
	}
}
