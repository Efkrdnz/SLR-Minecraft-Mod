package net.solocraft.dungeon.data;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Small immutable value types shared by the dungeon datapack schema. */
public final class DungeonDataTypes {
	private DungeonDataTypes() {
	}

	public enum DungeonKind {
		PRESET,
		PROCEDURAL,
		FIXED;

		static DungeonKind parse(String value) {
			if ("fixed".equalsIgnoreCase(value) || "fixed_layout".equalsIgnoreCase(value))
				return FIXED;
			return "procedural".equalsIgnoreCase(value) || "module_pool".equalsIgnoreCase(value)
					? PROCEDURAL : PRESET;
		}
	}

	/** Authored procedural graph shape. Fixed layouts carry their own exact graph. */
	public enum DungeonTopology {
		LINEAR,
		BRANCHING
	}

	public enum RoomRole {
		START,
		NORMAL,
		JUNCTION,
		DEAD_END,
		TREASURE,
		BOSS,
		CAP,
		CORRIDOR,
		STAIR;

		static RoomRole parse(String value) {
			String normalized = value == null ? "normal" : value.toLowerCase(Locale.ROOT).replace('-', '_');
			return switch (normalized) {
				case "start", "entry", "entrance" -> START;
				case "junction", "branch" -> JUNCTION;
				case "dead_end", "deadend" -> DEAD_END;
				case "treasure", "reward" -> TREASURE;
				case "boss", "boss_room" -> BOSS;
				case "cap", "wall_cap" -> CAP;
				case "corridor", "hall" -> CORRIDOR;
				case "stair", "stairs" -> STAIR;
				default -> NORMAL;
			};
		}
	}

	public enum SelectorKind {
		ENTITY,
		TAG
	}

	public enum ModifierOperation {
		ADD,
		REMOVE
	}

	public enum Severity {
		WARNING,
		ERROR
	}

	public record Int3(int x, int y, int z) {
		public boolean isPositive() {
			return x > 0 && y > 0 && z > 0;
		}

		public boolean contains(Int3 position) {
			return position.x >= 0 && position.y >= 0 && position.z >= 0
					&& position.x < x && position.y < y && position.z < z;
		}
	}

	public record Bounds3(Int3 min, Int3 max) {
		public Bounds3 {
			int minX = Math.min(min.x, max.x);
			int minY = Math.min(min.y, max.y);
			int minZ = Math.min(min.z, max.z);
			int maxX = Math.max(min.x, max.x);
			int maxY = Math.max(min.y, max.y);
			int maxZ = Math.max(min.z, max.z);
			min = new Int3(minX, minY, minZ);
			max = new Int3(maxX, maxY, maxZ);
		}

		public boolean inside(Int3 size) {
			return size.contains(min) && size.contains(max);
		}
	}

	public record IntRange(int min, int max) {
		public IntRange {
			if (min > max) {
				int swap = min;
				min = max;
				max = swap;
			}
		}

		public boolean contains(int value) {
			return value >= min && value <= max;
		}

		public int random(RandomSource random) {
			if (min == max)
				return min;
			long width = (long) max - min + 1L;
			return (int) (min + Math.floorMod(random.nextLong(), width));
		}
	}

	public record Region(String id, String type, Bounds3 bounds) {
	}

	/**
	 * A connector opening. The opening is the authored wall plane and carveDepth
	 * controls how many blocks are opened outward/inward when a connection is used.
	 */
	public record Socket(String id, String type, Bounds3 opening, Direction facing,
			boolean required, int carveDepth) {
		/** Returns 0 for a wall-plane socket, 1 for the preferred inside-wall anchor. */
		public int insetFromBoundary(Int3 size) {
			return switch (facing) {
				case WEST -> opening.min.x;
				case EAST -> size.x - 1 - opening.max.x;
				case DOWN -> opening.min.y;
				case UP -> size.y - 1 - opening.max.y;
				case NORTH -> opening.min.z;
				case SOUTH -> size.z - 1 - opening.max.z;
			};
		}
	}

	public record Marker(String id, String type, String group, Int3 position) {
		public boolean belongsTo(String idOrGroup) {
			return id.equals(idOrGroup) || (!group.isBlank() && group.equals(idOrGroup));
		}
	}

	public record EncounterWave(String id, String markerGroup, ResourceLocation mobPool,
			IntRange count, int delayTicks, boolean boss, Optional<IntRange> levelRange) {
		public EncounterWave {
			levelRange = levelRange == null ? Optional.empty() : levelRange;
		}
	}

	public record Encounter(String id, Optional<String> triggerRegion, List<EncounterWave> waves,
			List<String> lockSockets) {
		public Encounter {
			triggerRegion = triggerRegion == null ? Optional.empty() : triggerRegion;
			waves = List.copyOf(waves);
			lockSockets = List.copyOf(lockSockets);
		}
	}

	public record EntitySelector(SelectorKind kind, ResourceLocation id) {
		public String key() {
			return (kind == SelectorKind.TAG ? "#" : "") + id;
		}
	}

	/**
	 * eligibleLevel controls whether an entry can be selected for a dungeon.
	 * spawnLevel controls the Level value assigned to the selected entity.
	 * baseXp overrides the Solo Leveling base XP reward; global/player/difficulty
	 * multipliers still apply. An empty value uses the role/level fallback.
	 */
	public record MobPoolEntry(EntitySelector selector, int weight, Optional<String> requiredMod,
			Optional<IntRange> eligibleLevel, Optional<IntRange> spawnLevel,
			Optional<Integer> baseXp) {
		public MobPoolEntry {
			requiredMod = requiredMod == null ? Optional.empty() : requiredMod;
			eligibleLevel = eligibleLevel == null ? Optional.empty() : eligibleLevel;
			spawnLevel = spawnLevel == null ? Optional.empty() : spawnLevel;
			baseXp = baseXp == null ? Optional.empty() : baseXp;
		}

		public boolean eligibleAt(int dungeonLevel) {
			return eligibleLevel.isEmpty() || eligibleLevel.get().contains(dungeonLevel);
		}
	}

	public record MobPoolModifier(ResourceLocation id, ResourceLocation target,
			ModifierOperation operation, List<MobPoolEntry> entries, List<EntitySelector> selectors) {
		public MobPoolModifier {
			entries = List.copyOf(entries);
			selectors = List.copyOf(selectors);
		}
	}

	public record WeightedRoom(ResourceLocation room, int weight) {
	}

	/** Exact module placement in a fixed dungeon layout. Position is relative to the layout origin. */
	public record FixedRoomPlacement(String id, ResourceLocation room, Int3 position, Rotation rotation) {
	}

	/** Exact socket-to-socket edge in a fixed dungeon layout. */
	public record FixedRoomConnection(String fromRoom, String fromSocket, String toRoom, String toSocket) {
	}

	public record LevelRule(String source, IntRange range, int variance) {
	}

	public record ShellSettings(boolean enabled, ResourceLocation block, int thickness,
			boolean coverFloor, boolean coverCeiling) {
	}

	public record ValidationIssue(Severity severity, ResourceLocation resource, String message) {
	}
}
