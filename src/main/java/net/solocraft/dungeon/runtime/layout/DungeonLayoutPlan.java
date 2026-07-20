package net.solocraft.dungeon.runtime.layout;

import net.solocraft.dungeon.data.DungeonRoomDefinition;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.PreparedTemplate;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.WorldBounds;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical, placement-ready output of the dungeon planner. It contains no
 * world mutations; the runtime generator decides whether and when to commit it.
 */
public record DungeonLayoutPlan(List<PlannedRoom> rooms, List<PlannedConnection> connections) {
	public DungeonLayoutPlan {
		rooms = List.copyOf(rooms);
		connections = List.copyOf(connections);
	}

	public boolean runtimePlaceable() {
		return rooms.stream().allMatch(room -> room.geometry().runtimePlaceable());
	}

	public static final class PlannedRoom {
		private final int index;
		private final int depth;
		private final String placementKey;
		@Nullable private final DungeonRoomDefinition definition;
		private final ResourceLocation definitionId;
		private final DungeonRoomGeometry geometry;
		private final Set<String> usedSockets;

		public PlannedRoom(int index, int depth, DungeonRoomDefinition definition,
				PreparedTemplate prepared, Set<String> usedSockets) {
			this("room_" + index, index, depth, definition, prepared, usedSockets);
		}

		public PlannedRoom(int index, int depth, DungeonRoomDefinition definition,
				DungeonRoomGeometry geometry, Set<String> usedSockets) {
			this("room_" + index, index, depth, definition, geometry, usedSockets);
		}

		public PlannedRoom(String placementKey, int index, int depth, DungeonRoomDefinition definition,
				PreparedTemplate prepared, Set<String> usedSockets) {
			this(placementKey, index, depth, definition, DungeonRoomGeometry.loaded(prepared), usedSockets);
		}

		public PlannedRoom(String placementKey, int index, int depth, DungeonRoomDefinition definition,
				DungeonRoomGeometry geometry, Set<String> usedSockets) {
			this.index = index;
			this.depth = depth;
			this.placementKey = placementKey == null || placementKey.isBlank() ? "room_" + index : placementKey;
			this.definition = definition;
			this.definitionId = definition.id();
			this.geometry = geometry;
			this.usedSockets = Set.copyOf(usedSockets);
		}

		private PlannedRoom(ResourceLocation definitionId, PreparedTemplate prepared) {
			this.index = 0;
			this.depth = 0;
			this.placementKey = "preset";
			this.definition = null;
			this.definitionId = definitionId;
			this.geometry = DungeonRoomGeometry.loaded(prepared);
			this.usedSockets = Set.of();
		}

		public static PlannedRoom preset(ResourceLocation dungeonId, PreparedTemplate prepared) {
			return new PlannedRoom(dungeonId, prepared);
		}

		public int index() {
			return index;
		}

		public int depth() {
			return depth;
		}

		public String placementKey() {
			return placementKey;
		}

		public boolean preset() {
			return definition == null;
		}

		public DungeonRoomDefinition definition() {
			if (definition == null)
				throw new IllegalStateException("Preset room has no module definition");
			return definition;
		}

		@Nullable
		public DungeonRoomDefinition definitionOrNull() {
			return definition;
		}

		public ResourceLocation definitionId() {
			return definitionId;
		}

		public PreparedTemplate prepared() {
			return geometry.runtimeTemplate();
		}

		public DungeonRoomGeometry geometry() {
			return geometry;
		}

		public Set<String> usedSockets() {
			return usedSockets;
		}

		public PlannedRoom withUsedSocket(String socketId) {
			if (usedSockets.contains(socketId))
				return this;
			Set<String> next = new HashSet<>(usedSockets);
			next.add(socketId);
			return new PlannedRoom(placementKey, index, depth, definition(), geometry, next);
		}
	}

	public record PlannedConnection(int sourceRoom, String sourceSocket, int targetRoom,
			String targetSocket, WorldBounds bounds) {
	}
}
