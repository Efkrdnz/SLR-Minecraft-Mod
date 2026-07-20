package net.solocraft.client.gui.dungeonbuilder;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioController.*;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioModel.*;
import net.solocraft.network.DungeonBuilderStudioActionMessage;
import net.solocraft.network.DungeonBuilderStudioRequestMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Client snapshot codec and transport controller for Dungeon Builder Studio. */
public final class DungeonBuilderStudioClient {
	private static DungeonBuilderStudioModel model = DungeonBuilderStudioModel.empty();
	private static final DungeonBuilderStudioController CONTROLLER = new NetworkController();

	private DungeonBuilderStudioClient() {
	}

	public static void requestOpen() {
		model = DungeonBuilderStudioModel.loadingState();
		SololevelingMod.PACKET_HANDLER.sendToServer(new DungeonBuilderStudioRequestMessage());
	}

	public static void handleState(boolean open, CompoundTag state) {
		model = decode(state == null ? new CompoundTag() : state);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof DungeonBuilderStudioScreen studio) {
			studio.updateModel(model);
		} else if (open) {
			minecraft.setScreen(new DungeonBuilderStudioScreen(model, CONTROLLER));
		}
	}

	public static DungeonBuilderStudioModel model() {
		return model;
	}

	private static DungeonBuilderStudioModel decode(CompoundTag root) {
		List<Project> projects = new ArrayList<>();
		ListTag projectTags = root.getList("Projects", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_PROJECTS, projectTags.size()); index++) {
			CompoundTag projectTag = projectTags.getCompound(index);
			Project previous = model.project(projectTag.getString("Id")).orElse(null);
			projects.add(project(projectTag, previous));
		}
		List<MobPool> pools = new ArrayList<>();
		ListTag poolTags = root.getList("Pools", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_POOLS, poolTags.size()); index++)
			pools.add(pool(poolTags.getCompound(index)));
		List<DraftSummary> dungeonDrafts = new ArrayList<>();
		ListTag draftTags = root.getList("DungeonDrafts", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_DUNGEON_DRAFTS, draftTags.size()); index++) {
			CompoundTag draft = draftTags.getCompound(index);
			dungeonDrafts.add(new DraftSummary(draft.getString("Id"),
					enumValue(LayoutMode.class, draft.getString("Mode"), LayoutMode.PROCEDURAL),
					enumValue(Topology.class, draft.getString("Topology"), Topology.LINEAR),
					draft.getInt("RoomCount"), draft.getInt("PlacementCount")));
		}
		CompoundTag noticeTag = root.getCompound("Notice");
		Notice notice = new Notice(enumValue(Severity.class, noticeTag.getString("Severity"), Severity.INFO),
				noticeTag.getString("Message"));
		return new DungeonBuilderStudioModel(root.getLong("Revision"), root.getString("SelectedProjectId"),
				root.getString("SelectedPoolId"), root.getString("DungeonId"), false,
				projects, pools, layout(root.getCompound("Layout"), projects), simulation(root.getCompound("Simulation")),
				validation(root.getCompound("Validation")), notice, dungeonDrafts);
	}

	private static Project project(CompoundTag tag, Project previous) {
		Bounds bounds = tag.contains("Bounds", Tag.TAG_COMPOUND)
				? bounds(tag.getCompound("Bounds")) : null;
		boolean detailed = tag.getBoolean("Detailed");
		List<FootprintCell> footprint = new ArrayList<>();
		int width = Math.max(0, Math.min(48, tag.getInt("FootprintWidth")));
		int depth = Math.max(0, Math.min(48, tag.getInt("FootprintDepth")));
		int[] colors = tag.getIntArray("FootprintColors");
		for (int z = 0; z < depth; z++) {
			for (int x = 0; x < width; x++) {
				int cell = z * width + x;
				if (cell < colors.length && colors[cell] != 0)
					footprint.add(new FootprintCell(x, z, colors[cell]));
			}
		}
		List<Socket> sockets = new ArrayList<>();
		ListTag socketTags = tag.getList("Sockets", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_SOCKETS, socketTags.size()); index++) {
			CompoundTag socket = socketTags.getCompound(index);
			sockets.add(new Socket(socket.getString("Id"), point(socket.getCompound("Position")),
					enumValue(Facing.class, socket.getString("Facing"), Facing.NORTH),
					enumValue(SocketType.class, socket.getString("Type"), SocketType.CORRIDOR),
					socket.getBoolean("Required"), socket.getInt("OpeningWidth"),
					socket.getInt("OpeningHeight")));
		}
		List<Anchor> anchors = new ArrayList<>();
		ListTag anchorTags = tag.getList("Anchors", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_ANCHORS, anchorTags.size()); index++) {
			CompoundTag anchor = anchorTags.getCompound(index);
			AnchorKind kind = enumValue(AnchorKind.class, anchor.getString("Kind"), AnchorKind.UNASSIGNED);
			SpawnRole role = switch (kind) {
				case MOB_SPAWN -> SpawnRole.NORMAL;
				case ELITE_SPAWN -> SpawnRole.ELITE;
				case BOSS_SPAWN -> SpawnRole.BOSS;
				default -> SpawnRole.NONE;
			};
			Bounds triggerBounds = anchor.contains("Bounds", Tag.TAG_COMPOUND)
					? bounds(anchor.getCompound("Bounds")) : null;
			anchors.add(new Anchor(anchor.getString("Id"), kind, role,
					point(anchor.getCompound("Position")), triggerBounds, anchor.getString("EncounterId"),
					anchor.getString("PoolId"), anchor.getBoolean("LevelOverride"),
					nonZero(anchor.getInt("MinLevel"), 1),
					nonZero(anchor.getInt("MaxLevel"), 1), anchor.getBoolean("Delayed")));
		}
		if (!detailed && previous != null) {
			footprint = new ArrayList<>(previous.footprint());
			sockets = new ArrayList<>(previous.sockets());
			anchors = new ArrayList<>(previous.anchors());
		}
		return new Project(tag.getString("Id"), tag.getString("Name"),
				enumValue(ProjectKind.class, tag.getString("Kind"), ProjectKind.MODULE),
				enumValue(RoomRole.class, tag.getString("Role"), RoomRole.NORMAL),
				nonZero(tag.getInt("Weight"), 1), new LinkedHashSet<>(strings(tag, "Ranks", 8)),
				tag.getString("ShellBlock"), tag.getInt("ShellThickness"), bounds,
				tag.getBoolean("SnapshotCaptured"), tag.getBoolean("SnapshotOutdated"),
				tag.getLong("SnapshotRevision"), footprint, sockets, anchors,
				tag.getInt("Errors"), tag.getInt("Warnings"));
	}

	private static MobPool pool(CompoundTag tag) {
		List<PoolEntry> entries = new ArrayList<>();
		ListTag values = tag.getList("Entries", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_POOL_ENTRIES, values.size()); index++) {
			CompoundTag value = values.getCompound(index);
			entries.add(new PoolEntry(enumValue(SelectorKind.class, value.getString("SelectorKind"), SelectorKind.ENTITY),
					value.getString("Selector"), nonZero(value.getInt("Weight"), 1),
					value.getString("RequiredMod"), range(value, "Eligible"), range(value, "Spawn"),
					new OptionalXp(value.contains("Xp", Tag.TAG_INT), Math.max(0, value.getInt("Xp")))));
		}
		return new MobPool(tag.getString("Id"), false, entries);
	}

	private static LayoutDraft layout(CompoundTag tag, List<Project> projects) {
		List<String> enabled = new ArrayList<>();
		List<RoomWeight> weights = new ArrayList<>();
		ListTag roomTags = tag.getList("Rooms", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_PROJECTS, roomTags.size()); index++) {
			CompoundTag room = roomTags.getCompound(index);
			enabled.add(room.getString("ProjectId"));
			weights.add(new RoomWeight(room.getString("ProjectId"), nonZero(room.getInt("Weight"), 1)));
		}
		List<LayoutNode> nodes = new ArrayList<>();
		ListTag nodeTags = tag.getList("Nodes", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_LAYOUT_ROOMS, nodeTags.size()); index++) {
			CompoundTag node = nodeTags.getCompound(index);
			Project project = projects.stream().filter(value -> value.id().equals(node.getString("ProjectId")))
					.findFirst().orElse(null);
			int rotation = rotationDegrees(node.getString("Rotation"));
			int roomWidth = project == null || project.bounds() == null ? 1 : project.bounds().width();
			int roomDepth = project == null || project.bounds() == null ? 1 : project.bounds().depth();
			if (rotation == 90 || rotation == 270) {
				int swap = roomWidth;
				roomWidth = roomDepth;
				roomDepth = swap;
			}
			nodes.add(new LayoutNode(node.getString("Id"), node.getString("ProjectId"),
					project == null ? RoomRole.NORMAL : project.role(), node.getInt("X"), node.getInt("Y"),
					node.getInt("Z"), roomWidth, roomDepth, rotation, false));
		}
		List<LayoutConnection> connections = new ArrayList<>();
		ListTag connectionTags = tag.getList("Connections", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_CONNECTIONS, connectionTags.size()); index++) {
			CompoundTag connection = connectionTags.getCompound(index);
			connections.add(new LayoutConnection(connection.getString("FromNode"), connection.getString("FromSocket"),
					connection.getString("ToNode"), connection.getString("ToSocket")));
		}
		return new LayoutDraft(enumValue(LayoutMode.class, tag.getString("Mode"), LayoutMode.PROCEDURAL),
				enumValue(Topology.class, tag.getString("Topology"), Topology.LINEAR),
				nonZero(tag.getInt("MinRooms"), 3), nonZero(tag.getInt("MaxRooms"), 8),
				nonZero(tag.getInt("MaxDepth"), 16), new LinkedHashSet<>(strings(tag, "Ranks", 8)),
				tag.getString("ShellBlock"), tag.getInt("ShellThickness"), enabled, weights, nodes, connections);
	}

	private static Simulation simulation(CompoundTag tag) {
		List<SimRoom> rooms = new ArrayList<>();
		ListTag roomTags = tag.getList("Rooms", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_LAYOUT_ROOMS, roomTags.size()); index++) {
			CompoundTag room = roomTags.getCompound(index);
			rooms.add(new SimRoom(room.getString("Id"), room.getString("ProjectId"),
					enumValue(RoomRole.class, room.getString("Role"), RoomRole.NORMAL),
					room.getInt("X"), room.getInt("Z"), nonZero(room.getInt("Width"), 1),
					nonZero(room.getInt("Depth"), 1), room.getInt("Rotation")));
		}
		List<SimConnection> connections = new ArrayList<>();
		ListTag connectionTags = tag.getList("Connections", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_CONNECTIONS, connectionTags.size()); index++) {
			CompoundTag connection = connectionTags.getCompound(index);
			connections.add(new SimConnection(connection.getString("FromRoom"), connection.getString("ToRoom")));
		}
		return new Simulation(tag.getLong("Seed"),
				enumValue(SimulationStatus.class, tag.getString("Status"), SimulationStatus.IDLE),
				tag.getString("Message"), tag.getInt("Attempts"), rooms, connections);
	}

	private static ValidationSummary validation(CompoundTag tag) {
		List<ValidationIssue> issues = new ArrayList<>();
		ListTag issueTags = tag.getList("Issues", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(DungeonBuilderStudioModel.MAX_ISSUES, issueTags.size()); index++) {
			CompoundTag issue = issueTags.getCompound(index);
			issues.add(new ValidationIssue(enumValue(Severity.class, issue.getString("Severity"), Severity.INFO),
					issue.getString("Code"), issue.getString("Message"), issue.getString("ProjectId"),
					issue.getString("ElementId")));
		}
		return new ValidationSummary(tag.getBoolean("HasRun"), tag.getInt("Errors"), tag.getInt("Warnings"), issues);
	}

	private static CompoundTag encode(Action action) {
		CompoundTag tag = new CompoundTag();
		if (action instanceof CreateProject value) {
			tag.putString("Namespace", value.namespace());
			tag.putString("Name", value.name());
			tag.putString("Kind", value.kind().name());
		} else if (action instanceof DeleteProject value) {
			tag.putString("ProjectId", value.projectId());
		} else if (action instanceof SelectDungeon value) {
			tag.putString("DungeonId", value.dungeonId());
		} else if (action instanceof NewDungeon value) {
			tag.putString("DungeonId", value.dungeonId());
		} else if (action instanceof DeleteDungeon value) {
			tag.putString("DungeonId", value.dungeonId());
		} else if (action instanceof SelectProject value) {
			tag.putString("ProjectId", value.projectId());
		} else if (action instanceof SetRoomRole value) {
			tag.putString("ProjectId", value.projectId());
			tag.putString("Role", value.role().name());
		} else if (action instanceof SetRoomWeight value) {
			tag.putString("ProjectId", value.projectId());
			tag.putInt("Weight", value.weight());
		} else if (action instanceof SetProjectSettings value) {
			tag.putString("ProjectId", value.projectId());
			tag.put("Ranks", stringTags(value.ranks()));
			tag.putString("ShellBlock", value.shellBlock());
			tag.putInt("ShellThickness", value.shellThickness());
		} else if (action instanceof CaptureSnapshot value) {
			tag.putString("ProjectId", value.projectId());
			tag.putBoolean("UpdateExisting", value.updateExisting());
		} else if (action instanceof AssignAnchor value) {
			tag.putString("ProjectId", value.projectId());
			tag.putString("AnchorId", value.anchorId());
			tag.putString("AnchorKind", value.kind().name());
			tag.putString("SpawnRole", value.spawnRole().name());
			tag.putString("EncounterId", value.encounterId());
			tag.putString("PoolId", value.poolId());
			tag.putBoolean("LevelOverride", value.levelOverride());
			tag.putInt("MinLevel", value.minLevel());
			tag.putInt("MaxLevel", value.maxLevel());
			tag.putBoolean("Delayed", value.delayed());
		} else if (action instanceof EditSocket value) {
			tag.putString("ProjectId", value.projectId());
			tag.putString("SocketId", value.socketId());
			tag.putString("SocketType", value.type().name());
			tag.putBoolean("Required", value.required());
		} else if (action instanceof CreatePool value) {
			tag.putString("PoolId", value.requestedId());
		} else if (action instanceof DeletePool value) {
			tag.putString("PoolId", value.poolId());
		} else if (action instanceof BeginPoolEntryDraft value) {
			tag.putString("PoolId", value.poolId());
		} else if (action instanceof UpsertPoolEntry value) {
			List<PoolEntry> entries = new ArrayList<>(model.pool(value.poolId()).map(MobPool::entries).orElse(List.of()));
			entries.removeIf(entry -> entry.selectorLabel().equals(value.entry().selectorLabel()));
			entries.add(value.entry());
			putPool(tag, value.poolId(), entries);
		} else if (action instanceof RemovePoolEntry value) {
			List<PoolEntry> entries = new ArrayList<>(model.pool(value.poolId()).map(MobPool::entries).orElse(List.of()));
			entries.removeIf(entry -> entry.selectorLabel().equals(value.entityId()));
			putPool(tag, value.poolId(), entries);
		} else if (action instanceof SavePoolDraft value) {
			putPool(tag, value.poolId(), value.entries());
		} else if (action instanceof UpdateLayout value) {
			putLayout(tag, value.dungeonId(), value.layout());
		} else if (action instanceof RunSimulation value) {
			tag.putString("DungeonId", value.dungeonId());
			tag.putLong("Seed", value.seed());
		} else if (action instanceof ValidateDungeon value) {
			tag.putString("DungeonId", value.dungeonId());
		} else if (action instanceof ExportDungeon value) {
			tag.putString("DungeonId", value.dungeonId());
		}
		return tag;
	}

	private static void putPool(CompoundTag tag, String poolId, List<PoolEntry> entries) {
		tag.putString("PoolId", poolId);
		ListTag list = new ListTag();
		for (PoolEntry entry : entries) {
			CompoundTag value = new CompoundTag();
			value.putString("Selector", entry.selectorId());
			value.putBoolean("Tag", entry.selectorKind() == SelectorKind.TAG);
			value.putInt("Weight", entry.weight());
			value.putString("RequiredMod", entry.requiredMod());
			if (entry.eligibleLevel().present()) {
				value.putInt("EligibleMin", entry.eligibleLevel().min());
				value.putInt("EligibleMax", entry.eligibleLevel().max());
			}
			if (entry.spawnLevel().present()) {
				value.putInt("SpawnMin", entry.spawnLevel().min());
				value.putInt("SpawnMax", entry.spawnLevel().max());
			}
			if (entry.baseXp().present())
				value.putInt("Xp", entry.baseXp().value());
			list.add(value);
		}
		tag.put("Entries", list);
	}

	private static void putLayout(CompoundTag tag, String dungeonId, LayoutDraft layout) {
		tag.putString("DungeonId", dungeonId);
		tag.putString("Mode", layout.mode().name());
		tag.putString("Topology", layout.topology().name());
		tag.putInt("MinRooms", layout.minRooms());
		tag.putInt("MaxRooms", layout.maxRooms());
		tag.putInt("MaxDepth", layout.maxDepth());
		tag.putString("ShellBlock", layout.shellBlock());
		tag.putInt("ShellThickness", layout.shellThickness());
		tag.put("Ranks", stringTags(layout.ranks()));
		ListTag rooms = new ListTag();
		for (String projectId : layout.enabledProjectIds()) {
			CompoundTag room = new CompoundTag();
			room.putString("ProjectId", projectId);
			room.putInt("Weight", layout.roomWeights().stream()
					.filter(value -> value.projectId().equals(projectId)).findFirst()
					.map(RoomWeight::weight).orElse(1));
			rooms.add(room);
		}
		tag.put("Rooms", rooms);
		ListTag nodes = new ListTag();
		for (LayoutNode node : layout.nodes()) {
			CompoundTag value = new CompoundTag();
			value.putString("Id", node.id());
			value.putString("ProjectId", node.projectId());
			value.putInt("X", node.x());
			value.putInt("Y", node.y());
			value.putInt("Z", node.z());
			value.putString("Rotation", rotationName(node.rotation()));
			nodes.add(value);
		}
		tag.put("Nodes", nodes);
		ListTag connections = new ListTag();
		for (LayoutConnection connection : layout.connections()) {
			CompoundTag value = new CompoundTag();
			value.putString("FromNode", connection.fromNodeId());
			value.putString("FromSocket", connection.fromSocketId());
			value.putString("ToNode", connection.toNodeId());
			value.putString("ToSocket", connection.toSocketId());
			connections.add(value);
		}
		tag.put("Connections", connections);
	}

	private static String actionName(Action action) {
		if (action instanceof RequestSnapshot) return "request_snapshot";
		if (action instanceof CreateProject) return "create_project";
		if (action instanceof DeleteProject) return "delete_project";
		if (action instanceof SelectDungeon) return "select_dungeon";
		if (action instanceof NewDungeon) return "new_dungeon";
		if (action instanceof DeleteDungeon) return "delete_dungeon";
		if (action instanceof SelectProject) return "select_project";
		if (action instanceof SetRoomRole) return "set_room_role";
		if (action instanceof SetRoomWeight) return "set_room_weight";
		if (action instanceof SetProjectSettings) return "set_project_settings";
		if (action instanceof CaptureSnapshot) return "capture_snapshot";
		if (action instanceof AssignAnchor) return "assign_anchor";
		if (action instanceof EditSocket) return "edit_socket";
		if (action instanceof CreatePool) return "create_pool";
		if (action instanceof DeletePool) return "delete_pool";
		if (action instanceof BeginPoolEntryDraft) return "begin_pool_entry_draft";
		if (action instanceof UpsertPoolEntry) return "upsert_pool_entry";
		if (action instanceof RemovePoolEntry) return "remove_pool_entry";
		if (action instanceof SavePoolDraft) return "save_pool_draft";
		if (action instanceof UpdateLayout) return "update_layout";
		if (action instanceof RunSimulation) return "run_simulation";
		if (action instanceof ValidateDungeon) return "validate_dungeon";
		if (action instanceof ExportDungeon) return "export_dungeon";
		return "unknown";
	}

	private static Point point(CompoundTag tag) {
		return new Point(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
	}

	private static Bounds bounds(CompoundTag tag) {
		return new Bounds(point(tag.getCompound("Min")), point(tag.getCompound("Max")));
	}

	private static LevelRange range(CompoundTag tag, String prefix) {
		boolean present = tag.contains(prefix + "Min", Tag.TAG_INT) && tag.contains(prefix + "Max", Tag.TAG_INT);
		return new LevelRange(present, nonZero(tag.getInt(prefix + "Min"), 1),
				nonZero(tag.getInt(prefix + "Max"), 1));
	}

	private static List<String> strings(CompoundTag tag, String key, int maximum) {
		ListTag list = tag.getList(key, Tag.TAG_STRING);
		List<String> result = new ArrayList<>();
		for (int index = 0; index < Math.min(maximum, list.size()); index++)
			result.add(list.getString(index));
		return result;
	}

	private static ListTag stringTags(Set<String> values) {
		ListTag tags = new ListTag();
		values.forEach(value -> tags.add(StringTag.valueOf(value)));
		return tags;
	}

	private static int rotationDegrees(String value) {
		return switch (value) {
			case "CLOCKWISE_90" -> 90;
			case "CLOCKWISE_180" -> 180;
			case "COUNTERCLOCKWISE_90" -> 270;
			default -> 0;
		};
	}

	private static String rotationName(int degrees) {
		return switch (Math.floorMod(degrees, 360)) {
			case 90 -> "CLOCKWISE_90";
			case 180 -> "CLOCKWISE_180";
			case 270 -> "COUNTERCLOCKWISE_90";
			default -> "NONE";
		};
	}

	private static int nonZero(int value, int fallback) {
		return value == 0 ? fallback : value;
	}

	private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
		try {
			return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}

	private static final class NetworkController implements DungeonBuilderStudioController {
		@Override
		public void submit(Action action) {
			if (action instanceof RequestSnapshot) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new DungeonBuilderStudioRequestMessage());
				return;
			}
			SololevelingMod.PACKET_HANDLER.sendToServer(new DungeonBuilderStudioActionMessage(
					model.revision(), actionName(action), encode(action)));
		}
	}
}
