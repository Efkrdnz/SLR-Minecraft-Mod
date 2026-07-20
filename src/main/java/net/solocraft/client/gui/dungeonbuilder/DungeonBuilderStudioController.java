package net.solocraft.client.gui.dungeonbuilder;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Anchor;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.AnchorKind;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.Bounds;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.LayoutDraft;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.PoolEntry;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.ProjectKind;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.RoomRole;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SocketType;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.SpawnRole;

/**
 * Transport-neutral action boundary for {@link DungeonBuilderStudioScreen}.
 * Network code can implement this interface without giving the screen direct
 * access to server data or command strings.
 */
@FunctionalInterface
public interface DungeonBuilderStudioController {
	void submit(Action action);

	default void screenClosed() {
	}

	static DungeonBuilderStudioController noop() {
		return action -> {
		};
	}

	sealed interface Action permits RequestSnapshot, SelectProject, CreateProject, DeleteProject,
			SetRoomRole, SetRoomWeight, SetProjectSettings, CaptureSnapshot,
			AssignAnchor, EditSocket, CreatePool, DeletePool, BeginPoolEntryDraft,
			UpsertPoolEntry, RemovePoolEntry, SavePoolDraft, UpdateLayout,
			SelectDungeon, NewDungeon, DeleteDungeon,
			RunSimulation, ValidateDungeon, ExportDungeon {
	}

	record RequestSnapshot() implements Action {
	}

	record SelectProject(String projectId) implements Action {
		public SelectProject {
			projectId = safe(projectId, 128);
		}
	}

	record CreateProject(String namespace, String name, ProjectKind kind) implements Action {
		public CreateProject {
			namespace = safe(namespace, 32);
			name = safe(name, 48);
			kind = kind == null ? ProjectKind.MODULE : kind;
		}
	}

	record DeleteProject(String projectId) implements Action {
		public DeleteProject {
			projectId = safe(projectId, 128);
		}
	}

	record SetRoomRole(String projectId, RoomRole role) implements Action {
		public SetRoomRole {
			projectId = safe(projectId, 128);
			role = role == null ? RoomRole.NORMAL : role;
		}
	}

	record SetRoomWeight(String projectId, int weight) implements Action {
		public SetRoomWeight {
			projectId = safe(projectId, 128);
			weight = clamp(weight, 1, 10_000);
		}
	}

	record SetProjectSettings(String projectId, Set<String> ranks, String shellBlock,
			int shellThickness) implements Action {
		public SetProjectSettings {
			projectId = safe(projectId, 128);
			LinkedHashSet<String> cleanRanks = new LinkedHashSet<>();
			if (ranks != null) {
				for (String rank : ranks) {
					if (cleanRanks.size() >= 8)
						break;
					String clean = safe(rank, 16).toUpperCase(Locale.ROOT);
					if (!clean.isBlank())
						cleanRanks.add(clean);
				}
			}
			ranks = Set.copyOf(cleanRanks);
			shellBlock = safe(shellBlock, 128);
			shellThickness = clamp(shellThickness, 0, 4);
		}
	}

	/** updateExisting=false captures the first explicit block snapshot. */
	record CaptureSnapshot(String projectId, boolean updateExisting) implements Action {
		public CaptureSnapshot {
			projectId = safe(projectId, 128);
		}
	}

	record AssignAnchor(
			String projectId,
			String anchorId,
			AnchorKind kind,
			SpawnRole spawnRole,
			Bounds triggerBounds,
			String encounterId,
			String poolId,
			boolean levelOverride,
			int minLevel,
			int maxLevel,
			boolean delayed) implements Action {
		public AssignAnchor {
			projectId = safe(projectId, 128);
			anchorId = safe(anchorId, 128);
			kind = kind == null ? AnchorKind.UNASSIGNED : kind;
			spawnRole = spawnRole == null ? SpawnRole.NONE : spawnRole;
			encounterId = safe(encounterId, 64);
			poolId = safe(poolId, 192);
			minLevel = clamp(minLevel, 1, 1_000);
			maxLevel = clamp(maxLevel, minLevel, 1_000);
		}

		public static AssignAnchor from(String projectId, Anchor anchor) {
			return new AssignAnchor(projectId, anchor.id(), anchor.kind(), anchor.spawnRole(), anchor.triggerBounds(),
					anchor.encounterId(), anchor.poolId(), anchor.levelOverride(),
					anchor.minLevel(), anchor.maxLevel(), anchor.delayed());
		}

		/** Compatibility constructor for callers that always supplied an explicit level range. */
		public AssignAnchor(String projectId, String anchorId, AnchorKind kind, SpawnRole spawnRole,
				Bounds triggerBounds, String encounterId, String poolId, int minLevel, int maxLevel, boolean delayed) {
			this(projectId, anchorId, kind, spawnRole, triggerBounds, encounterId, poolId, true,
					minLevel, maxLevel, delayed);
		}
	}

	record EditSocket(String projectId, String socketId, SocketType type, boolean required) implements Action {
		public EditSocket {
			projectId = safe(projectId, 128);
			socketId = safe(socketId, 128);
			type = type == null ? SocketType.CORRIDOR : type;
		}
	}

	record CreatePool(String requestedId) implements Action {
		public CreatePool {
			requestedId = safe(requestedId, 192);
		}
	}

	record DeletePool(String poolId) implements Action {
		public DeletePool {
			poolId = safe(poolId, 192);
		}
	}

	/** Requests the future registry/entity picker for a new draft row. */
	record BeginPoolEntryDraft(String poolId) implements Action {
		public BeginPoolEntryDraft {
			poolId = safe(poolId, 192);
		}
	}

	record UpsertPoolEntry(String poolId, PoolEntry entry) implements Action {
		public UpsertPoolEntry {
			poolId = safe(poolId, 192);
			entry = entry == null ? new PoolEntry("minecraft:zombie", 1, 0, 1, 1) : entry;
		}
	}

	record RemovePoolEntry(String poolId, String entityId) implements Action {
		public RemovePoolEntry {
			poolId = safe(poolId, 192);
			entityId = safe(entityId, 192);
		}
	}

	record SavePoolDraft(String poolId, List<PoolEntry> entries) implements Action {
		public SavePoolDraft {
			poolId = safe(poolId, 192);
			entries = entries == null ? List.of()
					: List.copyOf(entries.stream().filter(java.util.Objects::nonNull)
							.limit(DungeonBuilderStudioModel.MAX_POOL_ENTRIES).toList());
		}
	}

	record UpdateLayout(String dungeonId, LayoutDraft layout) implements Action {
		public UpdateLayout {
			dungeonId = safe(dungeonId, 192);
			layout = layout == null ? LayoutDraft.empty() : layout;
		}
	}

	record SelectDungeon(String dungeonId) implements Action {
		public SelectDungeon {
			dungeonId = safe(dungeonId, 192);
		}
	}

	record NewDungeon(String dungeonId) implements Action {
		public NewDungeon {
			dungeonId = safe(dungeonId, 192);
		}
	}

	record DeleteDungeon(String dungeonId) implements Action {
		public DeleteDungeon {
			dungeonId = safe(dungeonId, 192);
		}
	}

	record RunSimulation(String dungeonId, long seed) implements Action {
		public RunSimulation {
			dungeonId = safe(dungeonId, 192);
		}
	}

	record ValidateDungeon(String dungeonId) implements Action {
		public ValidateDungeon {
			dungeonId = safe(dungeonId, 192);
		}
	}

	record ExportDungeon(String dungeonId) implements Action {
		public ExportDungeon {
			dungeonId = safe(dungeonId, 192);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String safe(String value, int maximum) {
		String result = value == null ? "" : value.replace('\u0000', ' ').trim();
		return result.length() <= maximum ? result : result.substring(0, maximum);
	}
}
