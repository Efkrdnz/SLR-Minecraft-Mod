package net.solocraft.dungeon.builder.model;

import net.solocraft.dungeon.ProceduralDungeonRank;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** A persisted GUI-editable dungeon design, before datapack compilation. */
public record DungeonDraft(ResourceLocation id, Mode mode, Topology topology,
		List<RoomRef> rooms, Set<ProceduralDungeonRank> allowedRanks,
		int minRooms, int maxRooms, int maxDepth,
		ResourceLocation shellBlock, int shellThickness,
		List<FixedPlacement> fixedPlacements, List<FixedConnection> fixedConnections) {
	public DungeonDraft {
		if (id == null)
			throw new IllegalArgumentException("Dungeon draft id is required.");
		if (mode == null || topology == null)
			throw new IllegalArgumentException("Dungeon mode and topology are required.");
		if (shellBlock == null)
			throw new IllegalArgumentException("Shell block is required.");
		rooms = rooms == null ? List.of() : List.copyOf(rooms);
		allowedRanks = allowedRanks == null ? Set.of() : Set.copyOf(allowedRanks);
		fixedPlacements = fixedPlacements == null ? List.of() : List.copyOf(fixedPlacements);
		fixedConnections = fixedConnections == null ? List.of() : List.copyOf(fixedConnections);
	}

	public enum Mode {
		PROCEDURAL,
		FIXED
	}

	public enum Topology {
		LINEAR,
		BRANCHING
	}

	public enum PlacementRotation {
		NONE,
		CLOCKWISE_90,
		CLOCKWISE_180,
		COUNTERCLOCKWISE_90
	}

	public record RoomRef(ResourceLocation room, int weight) {
		public RoomRef {
			if (room == null)
				throw new IllegalArgumentException("Room id is required.");
		}
	}

	public record FixedPlacement(String id, ResourceLocation room, int x, int y, int z,
			PlacementRotation rotation) {
		public FixedPlacement {
			if (id == null || room == null || rotation == null)
				throw new IllegalArgumentException("Placement id, room, and rotation are required.");
		}
	}

	public record FixedConnection(String fromPlacement, String fromSocket,
			String toPlacement, String toSocket) {
		public FixedConnection {
			if (fromPlacement == null || fromSocket == null || toPlacement == null || toSocket == null)
				throw new IllegalArgumentException("Both fixed-connection endpoints are required.");
		}
	}
}
