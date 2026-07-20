package net.solocraft.dungeon.runtime.layout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.List;

/** Compact, serialization-friendly view of a complete or partial layout. */
public record DungeonLayoutPreview(
		boolean success,
		DungeonLayoutPlanner.PlanFailure failure,
		String message,
		ResourceLocation dungeonId,
		long dataRevision,
		long seed,
		DungeonLayoutTopology topology,
		int targetRoomCount,
		List<RoomView> rooms,
		List<ConnectionView> connections,
		Diagnostics diagnostics) {

	public DungeonLayoutPreview {
		message = message == null ? "" : message;
		rooms = List.copyOf(rooms);
		connections = List.copyOf(connections);
		diagnostics = diagnostics == null ? Diagnostics.empty() : diagnostics;
	}

	public record Bounds(BlockPos min, BlockPos max) {
	}

	public record RoomView(int index, int depth, String placementKey, ResourceLocation definitionId,
			ResourceLocation structureId, String role, Rotation rotation, Bounds bounds,
			List<SocketView> sockets, List<MarkerView> markers) {
		public RoomView {
			role = role == null ? "normal" : role;
			sockets = List.copyOf(sockets);
			markers = List.copyOf(markers);
		}
	}

	public record SocketView(String id, String type, boolean required, boolean connected,
			Direction facing, Bounds opening) {
	}

	public record MarkerView(String id, String type, String group, BlockPos position) {
		public MarkerView {
			group = group == null ? "" : group;
		}
	}

	public record ConnectionView(int sourceRoom, String sourceSocket, int targetRoom,
			String targetSocket, Bounds carvedBounds) {
	}

	public record Diagnostics(int nodesVisited, boolean searchBudgetExhausted,
			List<DiagnosticCount> counts, List<DiagnosticSample> samples) {
		public Diagnostics {
			counts = List.copyOf(counts);
			samples = List.copyOf(samples);
		}

		public static Diagnostics empty() {
			return new Diagnostics(0, false, List.of(), List.of());
		}
	}

	public record DiagnosticCount(DungeonLayoutPlanner.DiagnosticCode code, int count,
			String description) {
	}

	public record DiagnosticSample(DungeonLayoutPlanner.DiagnosticCode code, String detail,
			@Nullable ResourceLocation candidateRoom, @Nullable Rotation rotation,
			@Nullable Bounds candidateBounds, int conflictingRoom) {
	}
}
