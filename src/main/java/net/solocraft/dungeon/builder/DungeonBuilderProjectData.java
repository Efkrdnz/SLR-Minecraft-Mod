package net.solocraft.dungeon.builder;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.dungeon.builder.model.BuilderMobPool;
import net.solocraft.dungeon.builder.model.BuilderWorkspaceSnapshot;
import net.solocraft.dungeon.builder.model.DungeonDraft;
import net.solocraft.dungeon.builder.model.RoomSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative authoring state for Dungeon Builder worlds.
 *
 * <p>Projects live in overworld SavedData instead of wand NBT so copied tools,
 * reconnects, and multiple builders cannot corrupt each other's work.</p>
 */
public final class DungeonBuilderProjectData extends SavedData {
	private static final String DATA_NAME = "sololeveling_dungeon_builder_projects";
	private static final int SCHEMA_VERSION = 6;
	private static final int MAX_PROJECTS_PER_BUILDER = 128;
	public static final int MAX_MOB_POOLS_PER_BUILDER = 128;
	public static final int MAX_MOB_POOL_ENTRIES = 256;
	public static final int MAX_TOTAL_MOB_POOL_ENTRIES = 2_048;
	public static final int MAX_DUNGEON_DRAFTS_PER_BUILDER = 64;
	public static final int MAX_DRAFT_ROOM_REFS = 256;
	public static final int MAX_FIXED_PLACEMENTS = 64;
	public static final int MAX_FIXED_CONNECTIONS = 128;
	public static final int MAX_FIXED_COORDINATE = 1_000_000;
	public static final int MAX_PROJECT_MARKERS = 256;
	public static final int MAX_PROJECT_SOCKETS = 64;
	public static final int MAX_PROJECT_REGIONS = 64;
	public static final int MAX_PROJECT_ENCOUNTERS = 128;
	private static final int MAX_NAMESPACE_LENGTH = 32;
	private static final int MAX_NAME_LENGTH = 48;
	private static final int MAX_LOCAL_ID_LENGTH = 64;
	public static final int MAX_STRUCTURE_AXIS = 48;
	public static final long MAX_STRUCTURE_VOLUME = 48L * 48L * 48L;
	public static final int MAX_ENCOUNTER_LEVEL = 1_000;
	public static final int MAX_POOL_WEIGHT = 1_000_000;
	public static final int MAX_BASE_XP = 1_000_000;
	private final Map<UUID, Workspace> workspaces = new HashMap<>();

	public static DungeonBuilderProjectData get(ServerLevel level) {
		ServerLevel storageLevel = level.getServer().overworld();
		return storageLevel.getDataStorage().computeIfAbsent(
				DungeonBuilderProjectData::load, DungeonBuilderProjectData::new, DATA_NAME);
	}

	/** Current server-authoritative edit revision for this player's workspace. */
	public long revision(ServerPlayer player) {
		return workspace(player).revision;
	}

	/**
	 * Marks an accepted external edit (for example an atomic snapshot-file write)
	 * and returns the new monotonically increasing workspace revision.
	 */
	public long touch(ServerPlayer player) {
		return touch(workspace(player));
	}

	/** Immutable catalog view intended for GUI/network snapshot adapters. */
	public BuilderWorkspaceSnapshot workspaceSnapshot(ServerPlayer player) {
		Workspace workspace = workspace(player);
		return new BuilderWorkspaceSnapshot(workspace.revision, workspace.activeId,
				workspace.projects.keySet().stream().sorted().toList(),
				workspace.mobPools.values().stream()
						.sorted(Comparator.comparing(pool -> pool.id().toString())).toList(),
				workspace.dungeonDrafts.values().stream()
						.sorted(Comparator.comparing(draft -> draft.id().toString())).toList());
	}

	public Project project(ServerPlayer player) {
		Workspace workspace = workspace(player);
		Project active = workspace.active();
		if (active != null)
			return active;
		Project project = new Project(defaultNamespace(player), "my_dungeon", ProjectKind.PRESET);
		workspace.addAndSelect(project);
		bindProject(workspace, project);
		touch(workspace);
		return project;
	}

	public ProjectResult createProject(ServerPlayer player, String namespace, String name, ProjectKind kind) {
		String problem = authorIdProblem(namespace, "namespace", MAX_NAMESPACE_LENGTH);
		if (problem != null)
			return ProjectResult.failure(problem);
		problem = authorIdProblem(name, "name", MAX_NAME_LENGTH);
		if (problem != null)
			return ProjectResult.failure(problem);
		String cleanNamespace = namespace;
		String cleanName = name;
		String id = projectId(cleanNamespace, cleanName);
		Workspace workspace = workspace(player);
		if (workspace.projects.containsKey(id))
			return ProjectResult.failure("Project " + id + " already exists. Select it instead.");
		if (workspace.projects.size() >= MAX_PROJECTS_PER_BUILDER)
			return ProjectResult.failure("This workspace already has the maximum of " + MAX_PROJECTS_PER_BUILDER + " projects.");
		Project project = new Project(cleanNamespace, cleanName, kind);
		if (kind == ProjectKind.MODULE)
			workspace.projects.values().stream()
					.filter(existing -> existing.kind == ProjectKind.MODULE && existing.namespace.equals(cleanNamespace))
					.findFirst().ifPresent(existing -> project.allowedRanks = EnumSet.copyOf(existing.allowedRanks));
		workspace.addAndSelect(project);
		bindProject(workspace, project);
		touch(workspace);
		return ProjectResult.success(project, "Created and selected " + id + ".");
	}

	public ProjectResult selectProject(ServerPlayer player, String namespace, String name) {
		Workspace workspace = workspaces.get(player.getUUID());
		String problem = authorIdProblem(namespace, "namespace", MAX_NAMESPACE_LENGTH);
		if (problem != null)
			return ProjectResult.failure(problem);
		problem = authorIdProblem(name, "name", MAX_NAME_LENGTH);
		if (problem != null)
			return ProjectResult.failure(problem);
		String id = projectId(namespace, name);
		if (workspace == null || !workspace.projects.containsKey(id))
			return ProjectResult.failure("No project named " + id + " exists in your workspace.");
		workspace.activeId = id;
		bindProject(workspace, workspace.projects.get(id));
		touch(workspace);
		return ProjectResult.success(workspace.projects.get(id), "Selected " + id + ".");
	}

	public ProjectResult resetActiveProject(ServerPlayer player) {
		Workspace workspace = workspaces.get(player.getUUID());
		Project active = workspace == null ? null : workspace.active();
		if (active == null)
			return ProjectResult.failure("There is no active project to reset.");
		Project replacement = new Project(active.namespace, active.name, active.kind);
		workspace.addAndSelect(replacement);
		bindProject(workspace, replacement);
		touch(workspace);
		return ProjectResult.success(replacement, "Reset " + replacement.id() + " to an empty project.");
	}

	public ProjectResult deleteProject(ServerPlayer player, String namespace, String name) {
		Workspace workspace = workspaces.get(player.getUUID());
		String problem = authorIdProblem(namespace, "namespace", MAX_NAMESPACE_LENGTH);
		if (problem != null)
			return ProjectResult.failure(problem);
		problem = authorIdProblem(name, "name", MAX_NAME_LENGTH);
		if (problem != null)
			return ProjectResult.failure(problem);
		String id = projectId(namespace, name);
		if (workspace == null || !workspace.projects.containsKey(id))
			return ProjectResult.failure("No project named " + id + " exists in your workspace.");
		ResourceLocation projectResource = ResourceLocation.tryParse(id);
		if (projectResource != null && workspace.dungeonDrafts.values().stream().anyMatch(draft ->
				draft.rooms().stream().anyMatch(ref -> ref.room().equals(projectResource))
						|| draft.fixedPlacements().stream().anyMatch(placement -> placement.room().equals(projectResource))))
			return ProjectResult.failure("Project " + id
					+ " is referenced by a dungeon draft. Remove it from that draft before deleting the room.");
		workspace.projects.remove(id);
		if (id.equals(workspace.activeId))
			workspace.activeId = workspace.projects.keySet().stream().findFirst().orElse("");
		touch(workspace);
		return ProjectResult.success(workspace.active(), "Deleted project " + id + ". Exported datapacks were not removed.");
	}

	public List<Project> projects(ServerPlayer player) {
		project(player);
		Workspace workspace = workspaces.get(player.getUUID());
		bindWorkspace(workspace);
		return workspace.projects.values().stream().sorted(Comparator.comparing(Project::id)).toList();
	}

	public boolean isActive(ServerPlayer player, Project project) {
		Workspace workspace = workspaces.get(player.getUUID());
		return workspace != null && project.id().equals(workspace.activeId);
	}

	private Workspace workspace(ServerPlayer player) {
		Workspace workspace = workspaces.computeIfAbsent(player.getUUID(), uuid -> new Workspace());
		bindWorkspace(workspace);
		return workspace;
	}

	private void bindWorkspace(@Nullable Workspace workspace) {
		if (workspace != null)
			workspace.projects.values().forEach(project -> bindProject(workspace, project));
	}

	private void bindProject(Workspace workspace, Project project) {
		project.bindMutationHook(() -> touch(workspace));
	}

	private long touch(Workspace workspace) {
		if (workspace.revision < Long.MAX_VALUE)
			workspace.revision++;
		setDirty();
		return workspace.revision;
	}

	public List<BuilderMobPool> mobPools(ServerPlayer player) {
		return workspace(player).mobPools.values().stream()
				.sorted(Comparator.comparing(pool -> pool.id().toString())).toList();
	}

	public Optional<BuilderMobPool> mobPool(ServerPlayer player, ResourceLocation id) {
		if (id == null)
			return Optional.empty();
		return Optional.ofNullable(workspace(player).mobPools.get(id));
	}

	/** Creates or atomically replaces a GUI-authored mob pool. */
	public MutationResult upsertMobPool(ServerPlayer player, BuilderMobPool pool) {
		if (pool == null)
			return MutationResult.failure("Mob pool is required.", revision(player));
		Workspace workspace = workspace(player);
		String problem = mobPoolProblem(pool);
		if (problem != null)
			return MutationResult.failure(problem, workspace.revision);
		if (!workspace.mobPools.containsKey(pool.id())
				&& workspace.mobPools.size() >= MAX_MOB_POOLS_PER_BUILDER)
			return MutationResult.failure("This workspace already has the maximum of "
					+ MAX_MOB_POOLS_PER_BUILDER + " mob pools.", workspace.revision);
		long otherEntries = workspace.mobPools.values().stream()
				.filter(existing -> !existing.id().equals(pool.id()))
				.mapToLong(existing -> existing.entries().size()).sum();
		if (otherEntries + pool.entries().size() > MAX_TOTAL_MOB_POOL_ENTRIES)
			return MutationResult.failure("Studio workspaces support at most "
					+ MAX_TOTAL_MOB_POOL_ENTRIES
					+ " total mob-pool entries so workspace snapshots remain network-safe.", workspace.revision);
		workspace.mobPools.put(pool.id(), pool);
		long revision = touch(workspace);
		return MutationResult.success("Saved mob pool " + pool.id() + ".", revision);
	}

	public MutationResult deleteMobPool(ServerPlayer player, ResourceLocation id) {
		Workspace workspace = workspace(player);
		if (id == null || !workspace.mobPools.containsKey(id))
			return MutationResult.failure("No authored mob pool named " + id + " exists.", workspace.revision);
		String value = id.toString();
		boolean referenced = workspace.projects.values().stream().anyMatch(project ->
				project.defaultMobPool.equals(value) || project.bossMobPool.equals(value)
						|| project.encounters.values().stream().anyMatch(encounter -> encounter.pool.equals(value)));
		if (referenced)
			return MutationResult.failure("Mob pool " + id
					+ " is still referenced by a room or encounter. Reassign those references first.", workspace.revision);
		workspace.mobPools.remove(id);
		long revision = touch(workspace);
		return MutationResult.success("Deleted mob pool " + id + ".", revision);
	}

	public List<DungeonDraft> dungeonDrafts(ServerPlayer player) {
		return workspace(player).dungeonDrafts.values().stream()
				.sorted(Comparator.comparing(draft -> draft.id().toString())).toList();
	}

	public Optional<DungeonDraft> dungeonDraft(ServerPlayer player, ResourceLocation id) {
		if (id == null)
			return Optional.empty();
		return Optional.ofNullable(workspace(player).dungeonDrafts.get(id));
	}

	/** Creates or atomically replaces a GUI-authored dungeon design. */
	public MutationResult upsertDungeonDraft(ServerPlayer player, DungeonDraft draft) {
		if (draft == null)
			return MutationResult.failure("Dungeon draft is required.", revision(player));
		Workspace workspace = workspace(player);
		String problem = dungeonDraftProblem(workspace, draft);
		if (problem != null)
			return MutationResult.failure(problem, workspace.revision);
		if (!workspace.dungeonDrafts.containsKey(draft.id())
				&& workspace.dungeonDrafts.size() >= MAX_DUNGEON_DRAFTS_PER_BUILDER)
			return MutationResult.failure("This workspace already has the maximum of "
					+ MAX_DUNGEON_DRAFTS_PER_BUILDER + " dungeon drafts.", workspace.revision);
		workspace.dungeonDrafts.put(draft.id(), draft);
		long revision = touch(workspace);
		return MutationResult.success("Saved dungeon draft " + draft.id() + ".", revision);
	}

	public MutationResult deleteDungeonDraft(ServerPlayer player, ResourceLocation id) {
		Workspace workspace = workspace(player);
		if (id == null || workspace.dungeonDrafts.remove(id) == null)
			return MutationResult.failure("No dungeon draft named " + id + " exists.", workspace.revision);
		long revision = touch(workspace);
		return MutationResult.success("Deleted dungeon draft " + id + ".", revision);
	}

	private static String defaultNamespace(ServerPlayer player) {
		return sanitizeId(player.getGameProfile().getName(), "builder", MAX_NAMESPACE_LENGTH);
	}

	public static String sanitizeId(String value, String fallback) {
		return sanitizeId(value, fallback, MAX_NAME_LENGTH);
	}

	private static String sanitizeId(String value, String fallback, int maxLength) {
		String sanitized = value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9_.-]", "_")
				.replaceAll("_+", "_")
				.replaceAll("^[._-]+|[._-]+$", "");
		if (sanitized.length() > maxLength)
			sanitized = sanitized.substring(0, maxLength);
		if (sanitized.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?"))
			sanitized = fallback;
		return sanitized.isBlank() ? fallback : sanitized;
	}

	@Nullable
	private static String authorIdProblem(String value, String label, int maxLength) {
		if (value == null || value.isBlank())
			return "Project " + label + " cannot be empty.";
		if (value.length() > maxLength)
			return "Project " + label + " is too long (maximum " + maxLength + " characters).";
		if (!value.matches("[a-z0-9][a-z0-9_.-]*") || value.endsWith("."))
			return "Project " + label + " must use lowercase letters, numbers, _, - or . and start with a letter or number.";
		String lower = value.toLowerCase(Locale.ROOT);
		if (lower.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?"))
			return "Project " + label + " uses a reserved file name.";
		return null;
	}

	@Nonnull
	@Override
	public CompoundTag save(@Nonnull CompoundTag tag) {
		tag.putInt("SchemaVersion", SCHEMA_VERSION);
		ListTag workspaceList = new ListTag();
		workspaces.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			CompoundTag workspaceTag = entry.getValue().save();
			workspaceTag.putUUID("Owner", entry.getKey());
			workspaceList.add(workspaceTag);
		});
		tag.put("Workspaces", workspaceList);
		return tag;
	}

	private static DungeonBuilderProjectData load(CompoundTag tag) {
		DungeonBuilderProjectData data = new DungeonBuilderProjectData();
		// Presence-tested fields below are authoritative; the version is retained for
		// diagnostics and future migrations without rejecting older saves.
		int schemaVersion = tag.contains("SchemaVersion", Tag.TAG_INT) ? tag.getInt("SchemaVersion") : 1;
		if (tag.contains("Workspaces", Tag.TAG_LIST)) {
			ListTag workspaceList = tag.getList("Workspaces", Tag.TAG_COMPOUND);
			for (int i = 0; i < workspaceList.size(); i++) {
				CompoundTag workspaceTag = workspaceList.getCompound(i);
				if (workspaceTag.hasUUID("Owner"))
					data.workspaces.put(workspaceTag.getUUID("Owner"), Workspace.load(workspaceTag));
			}
			data.workspaces.values().forEach(data::bindWorkspace);
			return data;
		}

		// Schema 1 stored one active project directly per owner. Import it without data loss.
		ListTag projectList = tag.getList("Projects", Tag.TAG_COMPOUND);
		for (int i = 0; i < projectList.size(); i++) {
			CompoundTag projectTag = projectList.getCompound(i);
			if (projectTag.hasUUID("Owner")) {
				Workspace workspace = data.workspaces.computeIfAbsent(projectTag.getUUID("Owner"), uuid -> new Workspace());
				workspace.addAndSelect(Project.load(projectTag));
			}
		}
		data.workspaces.values().forEach(data::bindWorkspace);
		return data;
	}

	public record ProjectResult(boolean success, String message, @Nullable Project project) {
		private static ProjectResult success(@Nullable Project project, String message) {
			return new ProjectResult(true, message, project);
		}

		private static ProjectResult failure(String message) {
			return new ProjectResult(false, message, null);
		}
	}

	public record MutationResult(boolean success, String message, long revision) {
		private static MutationResult success(String message, long revision) {
			return new MutationResult(true, message, revision);
		}

		private static MutationResult failure(String message, long revision) {
			return new MutationResult(false, message, revision);
		}
	}

	private static final class Workspace {
		private final Map<String, Project> projects = new LinkedHashMap<>();
		private final Map<ResourceLocation, BuilderMobPool> mobPools = new LinkedHashMap<>();
		private final Map<ResourceLocation, DungeonDraft> dungeonDrafts = new LinkedHashMap<>();
		private String activeId = "";
		private long revision;

		@Nullable
		private Project active() {
			return projects.get(activeId);
		}

		private void addAndSelect(Project project) {
			activeId = project.id();
			projects.put(activeId, project);
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Active", activeId);
			tag.putLong("Revision", revision);
			ListTag projectList = new ListTag();
			projects.values().stream().sorted(Comparator.comparing(Project::id))
					.forEach(project -> projectList.add(project.save()));
			tag.put("Projects", projectList);
			tag.put("MobPools", saveList(mobPools.values().stream()
					.sorted(Comparator.comparing(pool -> pool.id().toString()))
					.map(DungeonBuilderProjectData::saveMobPool).toList()));
			tag.put("DungeonDrafts", saveList(dungeonDrafts.values().stream()
					.sorted(Comparator.comparing(draft -> draft.id().toString()))
					.map(DungeonBuilderProjectData::saveDungeonDraft).toList()));
			return tag;
		}

		private static Workspace load(CompoundTag tag) {
			Workspace workspace = new Workspace();
			workspace.revision = tag.contains("Revision", Tag.TAG_LONG)
					? Math.max(0L, tag.getLong("Revision")) : 0L;
			ListTag projectList = tag.getList("Projects", Tag.TAG_COMPOUND);
			for (int i = 0; i < projectList.size() && workspace.projects.size() < MAX_PROJECTS_PER_BUILDER; i++) {
				Project project = Project.load(projectList.getCompound(i));
				workspace.projects.put(project.id(), project);
			}
			String savedActive = tag.getString("Active");
			workspace.activeId = workspace.projects.containsKey(savedActive)
					? savedActive
					: workspace.projects.keySet().stream().findFirst().orElse("");
			if (tag.contains("MobPools", Tag.TAG_LIST)) {
				int totalPoolEntries = 0;
				for (CompoundTag poolTag : readList(tag, "MobPools")) {
					try {
						BuilderMobPool pool = loadMobPool(poolTag);
						if (workspace.mobPools.size() < MAX_MOB_POOLS_PER_BUILDER
								&& !workspace.mobPools.containsKey(pool.id())
								&& mobPoolProblem(pool) == null
								&& totalPoolEntries + pool.entries().size() <= MAX_TOTAL_MOB_POOL_ENTRIES) {
							workspace.mobPools.put(pool.id(), pool);
							totalPoolEntries += pool.entries().size();
						} else if (totalPoolEntries + pool.entries().size() > MAX_TOTAL_MOB_POOL_ENTRIES) {
							SololevelingMod.LOGGER.warn("Skipped Dungeon Builder mob pool {} while loading: "
									+ "the workspace exceeds the {}-entry safety limit.",
									pool.id(), MAX_TOTAL_MOB_POOL_ENTRIES);
						}
					} catch (RuntimeException ignored) {
						// A malformed optional catalog entry must not discard room projects.
					}
				}
			}
			if (tag.contains("DungeonDrafts", Tag.TAG_LIST)) {
				for (CompoundTag draftTag : readList(tag, "DungeonDrafts")) {
					try {
						DungeonDraft draft = loadDungeonDraft(draftTag);
						if (workspace.dungeonDrafts.size() < MAX_DUNGEON_DRAFTS_PER_BUILDER
								&& dungeonDraftStructuralProblem(draft) == null)
							workspace.dungeonDrafts.put(draft.id(), draft);
					} catch (RuntimeException ignored) {
						// Presence-tested, entry-isolated migration.
					}
				}
			}
			return workspace;
		}
	}

	public enum ProjectKind {
		PRESET,
		MODULE;

		public static ProjectKind parse(String value) {
			return "module".equalsIgnoreCase(value) ? MODULE : PRESET;
		}
	}

	/** How a procedural assembler is allowed to use a room module. */
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

		public static RoomRole parse(String value) {
			if (value != null) {
				try {
					return valueOf(value.toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException ignored) {
				}
			}
			return NORMAL;
		}
	}

	public enum Severity {
		ERROR,
		WARNING
	}

	public record Issue(Severity severity, String message) {
	}

	public record Bounds(BlockPos min, BlockPos max) {
		public Bounds {
			BlockPos first = min;
			BlockPos second = max;
			min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
			max = new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
		}

		public boolean contains(BlockPos pos) {
			return pos.getX() >= min.getX() && pos.getX() <= max.getX()
					&& pos.getY() >= min.getY() && pos.getY() <= max.getY()
					&& pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
		}

		public BlockPos size() {
			return new BlockPos(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
		}

		public long volume() {
			BlockPos size = size();
			return (long) size.getX() * size.getY() * size.getZ();
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.put("Min", NbtUtils.writeBlockPos(min));
			tag.put("Max", NbtUtils.writeBlockPos(max));
			return tag;
		}

		private static Bounds load(CompoundTag tag) {
			return new Bounds(NbtUtils.readBlockPos(tag.getCompound("Min")), NbtUtils.readBlockPos(tag.getCompound("Max")));
		}
	}

	public record Region(String id, String type, Bounds bounds, String group) {
		private CompoundTag save() {
			CompoundTag tag = bounds.save();
			tag.putString("Id", id);
			tag.putString("Type", type);
			tag.putString("Group", group);
			return tag;
		}

		private static Region load(CompoundTag tag) {
			String group = tag.contains("Group", Tag.TAG_STRING) ? tag.getString("Group") : "default";
			return new Region(tag.getString("Id"), tag.getString("Type"), Bounds.load(tag), group);
		}
	}

	public record Marker(String id, String type, BlockPos position, String group) {
		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Id", id);
			tag.putString("Type", type);
			tag.put("Position", NbtUtils.writeBlockPos(position));
			tag.putString("Group", group);
			return tag;
		}

		private static Marker load(CompoundTag tag) {
			String group = tag.contains("Group", Tag.TAG_STRING) ? tag.getString("Group") : "default";
			return new Marker(tag.getString("Id"), tag.getString("Type"), NbtUtils.readBlockPos(tag.getCompound("Position")), group);
		}
	}

	/** Shared behavior for all spawn markers and trigger regions with the same logical ID. */
	public record Encounter(String id, String pool, int minLevel, int maxLevel, boolean delayed) {
		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Id", id);
			tag.putString("Pool", pool);
			tag.putInt("MinLevel", minLevel);
			tag.putInt("MaxLevel", maxLevel);
			tag.putBoolean("Delayed", delayed);
			return tag;
		}

		private static Encounter load(CompoundTag tag) {
			return new Encounter(sanitizeId(tag.getString("Id"), "default", MAX_LOCAL_ID_LENGTH), tag.getString("Pool"),
					Math.max(1, tag.getInt("MinLevel")), Math.max(1, tag.getInt("MaxLevel")),
					tag.contains("Delayed", Tag.TAG_BYTE) && tag.getBoolean("Delayed"));
		}
	}

	public record Socket(String id, String type, Bounds opening, Direction facing, boolean required) {
		private CompoundTag save() {
			CompoundTag tag = opening.save();
			tag.putString("Id", id);
			tag.putString("Type", type);
			tag.putString("Facing", facing.getName());
			tag.putBoolean("Required", required);
			return tag;
		}

		private static Socket load(CompoundTag tag) {
			Direction facing = Direction.byName(tag.getString("Facing"));
			String type = tag.getString("Type");
			if (type.equals("stair_up") || type.equals("stair_down"))
				type = "stair";
			return new Socket(tag.getString("Id"), type, Bounds.load(tag), facing == null ? Direction.NORTH : facing, tag.getBoolean("Required"));
		}
	}

	private record Pending(String kind, BlockPos first, @Nullable Direction facing) {
		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Kind", kind);
			tag.put("First", NbtUtils.writeBlockPos(first));
			if (facing != null)
				tag.putString("Facing", facing.getName());
			return tag;
		}

		private static Pending load(CompoundTag tag) {
			Direction facing = tag.contains("Facing") ? Direction.byName(tag.getString("Facing")) : null;
			return new Pending(tag.getString("Kind"), NbtUtils.readBlockPos(tag.getCompound("First")), facing);
		}
	}

	public static final class Project {
		private static final int UNDO_LIMIT = 32;
		public enum SpawnRole {
			NORMAL("mob_spawn"),
			ELITE("elite_spawn"),
			BOSS("boss_spawn");

			private final String markerType;

			SpawnRole(String markerType) {
				this.markerType = markerType;
			}

			public String markerType() {
				return markerType;
			}
		}

		private String namespace;
		private String name;
		private ProjectKind kind;
		private RoomRole roomRole;
		private String activeEncounterGroup;
		private String defaultMobPool;
		private String bossMobPool;
		private EnumSet<ProceduralDungeonRank> allowedRanks;
		private int roomWeight;
		private String shellBlock;
		private int shellThickness;
		@Nullable private Bounds structureBounds;
		@Nullable private BlockPos origin;
		@Nullable private Pending pending;
		@Nullable private RoomSnapshot roomSnapshot;
		@Nullable private transient Runnable mutationHook;
		private final List<Region> regions = new ArrayList<>();
		private final List<Marker> markers = new ArrayList<>();
		private final List<Socket> sockets = new ArrayList<>();
		private final Map<String, Encounter> encounters = new LinkedHashMap<>();
		private final Deque<ProjectState> undoHistory = new ArrayDeque<>();

		private Project(String namespace, String name, ProjectKind kind) {
			this.namespace = namespace;
			this.name = name;
			this.kind = kind;
			this.roomRole = RoomRole.NORMAL;
			this.activeEncounterGroup = "default";
			this.defaultMobPool = namespace + ":" + name + "_default";
			this.bossMobPool = namespace + ":" + name + "_boss";
			this.allowedRanks = EnumSet.allOf(ProceduralDungeonRank.class);
			this.roomWeight = 1;
			this.shellBlock = "minecraft:bedrock";
			this.shellThickness = 1;
		}

		public String namespace() {
			return namespace;
		}

		public String id() {
			return projectId(namespace, name);
		}

		public String name() {
			return name;
		}

		public ProjectKind kind() {
			return kind;
		}

		public RoomRole roomRole() {
			return roomRole;
		}

		public String activeEncounterGroup() {
			return activeEncounterGroup;
		}

		public String defaultMobPool() {
			return defaultMobPool;
		}

		public String bossMobPool() {
			return bossMobPool;
		}

		public Set<ProceduralDungeonRank> allowedRanks() {
			return Set.copyOf(allowedRanks);
		}

		public int roomWeight() {
			return roomWeight;
		}

		public String shellBlock() {
			return shellBlock;
		}

		public int shellThickness() {
			return shellThickness;
		}

		public Optional<RoomSnapshot> roomSnapshot() {
			return Optional.ofNullable(roomSnapshot);
		}

		@Nullable
		public Bounds structureBounds() {
			return structureBounds;
		}

		@Nullable
		public BlockPos origin() {
			return origin;
		}

		public List<Region> regions() {
			return List.copyOf(regions);
		}

		public List<Marker> markers() {
			return List.copyOf(markers);
		}

		/** Configured markers only; generic GUI spawn anchors are authoring metadata. */
		public List<Marker> markersForExport() {
			return markers.stream().filter(marker -> !marker.type.equals("spawn_point")).toList();
		}

		public List<Socket> sockets() {
			return List.copyOf(sockets);
		}

		public List<Encounter> encounters() {
			return List.copyOf(encounters.values());
		}

		private void bindMutationHook(Runnable hook) {
			mutationHook = hook;
		}

		private void changed() {
			if (mutationHook != null)
				mutationHook.run();
		}

		public String setRoomRole(RoomRole role) {
			if (role == null)
				return "Room role is required.";
			rememberState();
			roomRole = role;
			pending = null;
			changed();
			return "Room role set to " + role.name().toLowerCase(Locale.ROOT) + ".";
		}

		public String selectEncounterGroup(String group) {
			String clean = sanitizeId(group, "default", MAX_LOCAL_ID_LENGTH);
			if (!clean.equals(group))
				return "Encounter group IDs must use lowercase letters, numbers, _, - or .";
			rememberState();
			activeEncounterGroup = clean;
			pending = null;
			changed();
			return "Active encounter group set to " + clean + ". New spawn markers and trigger regions will use it.";
		}

		public String configureEncounter(String group, String pool, int minLevel, int maxLevel) {
			String clean = sanitizeId(group, "default", MAX_LOCAL_ID_LENGTH);
			if (!clean.equals(group))
				return "Encounter group IDs must use lowercase letters, numbers, _, - or .";
			if (!isResourceId(pool))
				return "Mob or mob-pool ID must be a resource ID such as minecraft:zombie or mypack:goblins.";
			boolean automaticLevel = maxLevel == Integer.MAX_VALUE;
			if (minLevel < 1 || (!automaticLevel && (maxLevel < minLevel || maxLevel > MAX_ENCOUNTER_LEVEL)))
				return "Encounter levels must be 1-" + MAX_ENCOUNTER_LEVEL
						+ ", or use the automatic dungeon/pool level setting.";
			if (!encounters.containsKey(clean) && encounters.size() >= MAX_PROJECT_ENCOUNTERS)
				return "This room already has the maximum of " + MAX_PROJECT_ENCOUNTERS + " encounters.";
			rememberState();
			Encounter previous = encounters.get(clean);
			encounters.put(clean, new Encounter(clean, pool, minLevel, maxLevel,
					previous != null && previous.delayed));
			activeEncounterGroup = clean;
			pending = null;
			changed();
			return "Encounter " + clean + " now uses " + pool + (automaticLevel
					? " with automatic dungeon/pool levels."
					: " at levels " + minLevel + "-" + maxLevel + ".");
		}

		/** Switches an existing encounter between immediate and trigger-delayed activation. */
		public String configureEncounterActivation(String encounterId, boolean delayed) {
			Encounter encounter = encounters.get(encounterId);
			if (encounter == null)
				return "No encounter named " + encounterId + " exists.";
			if (delayed && regions.stream().noneMatch(region -> region.type.equals("trigger_region")
					&& region.group.equals(encounterId)))
				return "Encounter " + encounterId
						+ " cannot be delayed until it has a matching Trigger Region.";
			if (encounter.delayed == delayed)
				return "Encounter " + encounterId + " is already "
						+ (delayed ? "trigger-delayed." : "automatic.");
			rememberState();
			encounters.put(encounterId, new Encounter(encounter.id, encounter.pool,
					encounter.minLevel, encounter.maxLevel, delayed));
			pending = null;
			changed();
			return "Encounter " + encounterId + " activation set to "
					+ (delayed ? "delayed trigger." : "automatic spawn.");
		}

		/** Assigns one authored trigger volume to an encounter without renaming unrelated encounters. */
		public String assignTriggerRegion(String regionId, String encounterId) {
			String clean = sanitizeId(encounterId, "default", MAX_LOCAL_ID_LENGTH);
			if (!clean.equals(encounterId))
				return "Encounter group IDs must use lowercase letters, numbers, _, - or .";
			int regionIndex = -1;
			for (int index = 0; index < regions.size(); index++) {
				Region region = regions.get(index);
				if (region.id.equals(regionId) && region.type.equals("trigger_region")) {
					regionIndex = index;
					break;
				}
			}
			if (regionIndex < 0)
				return "No Trigger Region named " + regionId + " exists.";
			if (regions.stream().anyMatch(region -> region.type.equals("trigger_region")
					&& region.group.equals(clean) && !region.id.equals(regionId)))
				return "Encounter " + clean + " already has a Trigger Region.";
			if (!encounters.containsKey(clean) && encounters.size() >= MAX_PROJECT_ENCOUNTERS)
				return "This room already has the maximum of " + MAX_PROJECT_ENCOUNTERS + " encounters.";

			rememberState();
			Region previousRegion = regions.get(regionIndex);
			String previousGroup = previousRegion.group;
			regions.set(regionIndex, new Region(previousRegion.id, previousRegion.type,
					previousRegion.bounds, clean));
			ensureEncounter(clean, false);
			Encounter target = encounters.get(clean);
			encounters.put(clean, new Encounter(target.id, target.pool, target.minLevel,
					target.maxLevel, true));
			if (!previousGroup.equals(clean) && regions.stream().noneMatch(region ->
					region.type.equals("trigger_region") && region.group.equals(previousGroup))) {
				Encounter previous = encounters.get(previousGroup);
				if (previous != null && previous.delayed)
					encounters.put(previousGroup, new Encounter(previous.id, previous.pool,
							previous.minLevel, previous.maxLevel, false));
			}
			activeEncounterGroup = clean;
			pending = null;
			changed();
			return "Assigned Trigger Region " + regionId + " to encounter " + clean + ".";
		}

		public String setDefaultMobPool(String pool, boolean boss) {
			if (!isResourceId(pool))
				return "Mob pool must be a resource ID such as mypack:goblins.";
			rememberState();
			String previous = boss ? bossMobPool : defaultMobPool;
			if (boss)
				bossMobPool = pool;
			else
				defaultMobPool = pool;
			encounters.replaceAll((id, encounter) -> {
				boolean bossGroup = markers.stream().anyMatch(marker -> marker.group.equals(id)
						&& marker.type.equals("boss_spawn"));
				return encounter.pool.equals(previous) && bossGroup == boss
						? new Encounter(encounter.id, pool, encounter.minLevel, encounter.maxLevel, encounter.delayed)
						: encounter;
			});
			pending = null;
			changed();
			return (boss ? "Boss" : "Default") + " mob pool set to " + pool
					+ ". Existing auto-configured " + (boss ? "boss" : "normal") + " groups were updated.";
		}

		public String setRoomWeight(int weight) {
			if (weight < 1 || weight > 10000)
				return "Room weight must be from 1 to 10000.";
			rememberState();
			roomWeight = weight;
			changed();
			return "Room generation weight set to " + weight + ".";
		}

		public String setAllowedRanks(Set<ProceduralDungeonRank> ranks) {
			if (ranks == null || ranks.isEmpty())
				return "Select at least one dungeon rank.";
			rememberState();
			allowedRanks = EnumSet.copyOf(ranks);
			pending = null;
			changed();
			return "Allowed dungeon ranks set to " + rankText(allowedRanks) + ".";
		}

		public String setShell(String block, int thickness) {
			if (!isResourceId(block))
				return "Shell block must be a resource ID such as minecraft:bedrock.";
			ResourceLocation blockId = ResourceLocation.tryParse(block);
			if (blockId == null || !ForgeRegistries.BLOCKS.containsKey(blockId)
					|| ForgeRegistries.BLOCKS.getValue(blockId) == Blocks.AIR)
				return "Shell block " + block + " is not a loaded, solid block.";
			if (thickness < 0 || thickness > 4)
				return "Shell thickness must be from 0 (disabled) to 4.";
			rememberState();
			shellBlock = block;
			shellThickness = thickness;
			changed();
			return thickness == 0 ? "Protective shell disabled." : "Protective shell set to " + thickness + " layer(s) of " + block + ".";
		}

		public String setRoomSnapshot(RoomSnapshot snapshot) {
			String problem = roomSnapshotProblem(snapshot, structureBounds);
			if (problem != null)
				return problem;
			rememberState();
			roomSnapshot = snapshot;
			pending = null;
			changed();
			return "Saved room snapshot " + snapshot.structureKey() + ".";
		}

		public String clearRoomSnapshot() {
			if (roomSnapshot == null)
				return "This project has no saved room snapshot.";
			rememberState();
			roomSnapshot = null;
			pending = null;
			changed();
			return "Cleared the saved room snapshot metadata.";
		}

		@Nullable
		public BlockPos pendingPosition() {
			return pending == null ? null : pending.first;
		}

		public String selectStructureCorner(BlockPos pos) {
			if (!isPending("structure_bounds")) {
				pending = new Pending("structure_bounds", pos.immutable(), null);
				changed();
				return "Structure corner 1 set to " + shortPos(pos) + ". Select the opposite corner.";
			}
			rememberState();
			Bounds previous = structureBounds;
			structureBounds = new Bounds(pending.first, pos);
			boolean invalidatedSnapshot = roomSnapshot != null && !structureBounds.equals(previous);
			if (invalidatedSnapshot)
				roomSnapshot = null;
			pending = null;
			BlockPos size = structureBounds.size();
			changed();
			return "Structure bounds saved: " + size.getX() + " x " + size.getY() + " x " + size.getZ() + "."
					+ (invalidatedSnapshot ? " The previous room snapshot was invalidated." : "");
		}

		public String selectRegionCorner(String type, BlockPos pos) {
			String pendingKind = "region:" + type;
			if (!isPending(pendingKind)) {
				boolean replacesExisting = type.equals("room")
						? regions.stream().anyMatch(region -> region.type.equals("room"))
						: type.equals("trigger_region") && regions.stream().anyMatch(region ->
								region.type.equals("trigger_region") && region.group.equals(activeEncounterGroup));
				if (regions.size() >= MAX_PROJECT_REGIONS && !replacesExisting)
					return "This room already has the maximum of " + MAX_PROJECT_REGIONS + " regions.";
				if (type.equals("trigger_region") && !encounters.containsKey(activeEncounterGroup)
						&& encounters.size() >= MAX_PROJECT_ENCOUNTERS)
					return "This room already has the maximum of " + MAX_PROJECT_ENCOUNTERS + " encounters.";
				pending = new Pending(pendingKind, pos.immutable(), null);
				changed();
				return display(type) + " corner 1 set to " + shortPos(pos) + ". Select the opposite corner.";
			}
			rememberState();
			Bounds bounds = new Bounds(pending.first, pos);
			int replacedRooms = 0;
			if (type.equals("room")) {
				replacedRooms = (int) regions.stream().filter(region -> region.type.equals("room")).count();
				regions.removeIf(region -> region.type.equals("room"));
			}
			String id = nextId(type, regions.stream().map(Region::id).toList());
			String group = type.equals("trigger_region") ? activeEncounterGroup : "";
			if (type.equals("trigger_region"))
				regions.removeIf(region -> region.type.equals("trigger_region") && region.group.equals(group));
			regions.add(new Region(id, type, bounds, group));
			if (type.equals("trigger_region")) {
				ensureEncounter(group, false);
				Encounter encounter = encounters.get(group);
				encounters.put(group, new Encounter(encounter.id, encounter.pool, encounter.minLevel,
						encounter.maxLevel, true));
			}
			pending = null;
			changed();
			return display(type) + " " + id + " saved."
					+ (type.equals("trigger_region")
							? " This optional encounter will wait until a participant enters this area."
							: "")
					+ (replacedRooms > 0 ? " Replaced " + replacedRooms + " previous Room Bounds selection"
							+ (replacedRooms == 1 ? "." : "s.") : "");
		}

		public String setOrigin(BlockPos pos) {
			rememberState();
			origin = pos.immutable();
			pending = null;
			changed();
			return "Structure origin set to " + shortPos(pos) + ".";
		}

		public String selectSocketCorner(String type, boolean required, BlockPos pos, Direction facing) {
			String pendingKind = "socket:" + type + ":" + required;
			if (!isPending(pendingKind)) {
				if (sockets.size() >= MAX_PROJECT_SOCKETS)
					return "This room already has the maximum of " + MAX_PROJECT_SOCKETS + " sockets.";
				pending = new Pending(pendingKind, pos.immutable(), facing);
				changed();
				return display(type) + " socket corner 1 set. Select the opposite corner on the same plane.";
			}
			Direction socketFacing = pending.facing == null ? facing : pending.facing;
			if (!samePlane(pending.first, pos, socketFacing))
				return "The second socket corner must be on the same " + socketFacing.getAxis().getName() + " plane.";
			rememberState();
			Bounds opening = new Bounds(pending.first, pos);
			String id = nextId(type + "_socket", sockets.stream().map(Socket::id).toList());
			sockets.add(new Socket(id, type, opening, socketFacing, required));
			pending = null;
			changed();
			return display(type) + " socket " + id + " saved facing " + socketFacing.getName() + ".";
		}

		public String addMarker(String type, BlockPos pos) {
			boolean replacesExisting = (type.equals("exit") || type.equals("return_portal"))
					? markers.stream().anyMatch(marker -> marker.type.equals("exit") || marker.type.equals("return_portal"))
					: isUniqueMarker(type) && markers.stream().anyMatch(marker -> marker.type.equals(type));
			if (markers.size() >= MAX_PROJECT_MARKERS && !replacesExisting)
				return "This room already has the maximum of " + MAX_PROJECT_MARKERS + " markers.";
			if (isEncounterMarker(type) && !encounters.containsKey(activeEncounterGroup)
					&& encounters.size() >= MAX_PROJECT_ENCOUNTERS)
				return "This room already has the maximum of " + MAX_PROJECT_ENCOUNTERS + " encounters.";
			rememberState();
			if (type.equals("exit") || type.equals("return_portal"))
				markers.removeIf(marker -> marker.type.equals("exit") || marker.type.equals("return_portal"));
			else if (isUniqueMarker(type))
				markers.removeIf(marker -> marker.type.equals(type));
			String id = nextId(type, markers.stream().map(Marker::id).toList());
			String group = "";
			if (isEncounterMarker(type)) {
				if (type.equals("boss_spawn") && activeEncounterGroup.equals("default")) {
					boolean defaultHasSpawn = markers.stream().anyMatch(marker -> isEncounterMarker(marker.type)
							&& marker.group.equals("default"));
					if (!defaultHasSpawn) {
						for (int index = 0; index < regions.size(); index++) {
							Region region = regions.get(index);
							if (region.type.equals("trigger_region") && region.group.equals("default"))
								regions.set(index, new Region(region.id, region.type, region.bounds, "boss"));
						}
						encounters.remove("default");
					}
					activeEncounterGroup = "boss";
				}
				group = activeEncounterGroup;
			}
			markers.add(new Marker(id, type, pos.immutable(), group));
			if (isEncounterMarker(type))
				ensureEncounter(group, type.equals("boss_spawn"));
			pending = null;
			changed();
			return display(type) + " marker " + id + " set at " + shortPos(pos)
					+ (group.isBlank() ? "." : " in encounter group " + group + ".");
		}

		/** Assigns or reassigns a stable generic/legacy spawn marker to an encounter. */
		public String configureSpawnPoint(String markerId, SpawnRole role, String encounterId) {
			if (role == null)
				return "Spawn role is required.";
			String clean = sanitizeId(encounterId, "default", MAX_LOCAL_ID_LENGTH);
			if (!clean.equals(encounterId))
				return "Encounter IDs must use lowercase letters, numbers, _, - or .";
			int markerIndex = markerIndex(markerId);
			if (markerIndex < 0)
				return "No marker named " + markerId + " exists in this room.";
			Marker marker = markers.get(markerIndex);
			if (!isAssignableSpawnMarker(marker.type))
				return "Marker " + markerId + " is not an encounter spawn point.";
			if (!encounters.containsKey(clean) && encounters.size() >= MAX_PROJECT_ENCOUNTERS)
				return "This room already has the maximum of " + MAX_PROJECT_ENCOUNTERS + " encounters.";

			rememberState();
			markers.set(markerIndex, new Marker(marker.id, role.markerType, marker.position, clean));
			ensureEncounter(clean, role == SpawnRole.BOSS);
			activeEncounterGroup = clean;
			pending = null;
			changed();
			return "Assigned " + markerId + " as a " + role.name().toLowerCase(Locale.ROOT)
					+ " spawn in encounter " + clean + ".";
		}

		/** Keeps the marker position but returns it to an unconfigured GUI anchor. */
		public String clearSpawnPointAssignment(String markerId) {
			int markerIndex = markerIndex(markerId);
			if (markerIndex < 0)
				return "No marker named " + markerId + " exists in this room.";
			Marker marker = markers.get(markerIndex);
			if (!isAssignableSpawnMarker(marker.type))
				return "Marker " + markerId + " is not an encounter spawn point.";
			if (marker.type.equals("spawn_point") && marker.group.isBlank())
				return "Spawn point " + markerId + " is already unassigned.";
			rememberState();
			markers.set(markerIndex, new Marker(marker.id, "spawn_point", marker.position, ""));
			pending = null;
			changed();
			return "Spawn point " + markerId + " is now unassigned.";
		}

		/** Changes socket behavior without changing its stable ID or authored geometry. */
		public String configureSocket(String socketId, String type, boolean required) {
			if (!type.equals("corridor") && !type.equals("stair"))
				return "Socket type must be corridor or stair.";
			int socketIndex = socketIndex(socketId);
			if (socketIndex < 0)
				return "No socket named " + socketId + " exists in this room.";
			Socket socket = sockets.get(socketIndex);
			if (type.equals("corridor") && socket.facing.getAxis().isVertical())
				return "A vertical socket cannot be configured as a corridor.";
			if (type.equals("stair") && socket.facing.getAxis().isHorizontal())
				return "A horizontal socket cannot be configured as a stair.";
			long requiredWithoutTarget = sockets.stream().filter(Socket::required)
					.filter(existing -> !existing.id.equals(socketId)).count();
			if (required && requiredWithoutTarget >= 2)
				return "Schema v2 supports at most two required sockets per room.";
			rememberState();
			sockets.set(socketIndex, new Socket(socket.id, type, socket.opening, socket.facing, required));
			pending = null;
			changed();
			return "Socket " + socketId + " is now " + (required ? "required " : "optional ") + type + ".";
		}

		/** Renames the encounter and every marker/trigger reference as one edit. */
		public String renameEncounter(String oldId, String newId) {
			String clean = sanitizeId(newId, "default", MAX_LOCAL_ID_LENGTH);
			if (!clean.equals(newId))
				return "Encounter IDs must use lowercase letters, numbers, _, - or .";
			Encounter encounter = encounters.get(oldId);
			if (encounter == null)
				return "No encounter named " + oldId + " exists.";
			if (oldId.equals(newId))
				return "Encounter " + oldId + " already has that name.";
			if (encounters.containsKey(newId))
				return "Encounter " + newId + " already exists.";
			rememberState();
			LinkedHashMap<String, Encounter> renamed = new LinkedHashMap<>();
			for (Encounter value : encounters.values())
				renamed.put(value.id.equals(oldId) ? newId : value.id,
						value.id.equals(oldId) ? new Encounter(newId, value.pool, value.minLevel,
								value.maxLevel, value.delayed) : value);
			encounters.clear();
			encounters.putAll(renamed);
			for (int index = 0; index < markers.size(); index++) {
				Marker marker = markers.get(index);
				if (marker.group.equals(oldId))
					markers.set(index, new Marker(marker.id, marker.type, marker.position, newId));
			}
			for (int index = 0; index < regions.size(); index++) {
				Region region = regions.get(index);
				if (region.group.equals(oldId))
					regions.set(index, new Region(region.id, region.type, region.bounds, newId));
			}
			if (activeEncounterGroup.equals(oldId))
				activeEncounterGroup = newId;
			pending = null;
			changed();
			return "Renamed encounter " + oldId + " to " + newId + ".";
		}

		/** Deletes encounter configuration and safely returns its spawn markers to unassigned anchors. */
		public String deleteEncounter(String id) {
			if (!encounters.containsKey(id))
				return "No encounter named " + id + " exists.";
			rememberState();
			encounters.remove(id);
			for (int index = 0; index < markers.size(); index++) {
				Marker marker = markers.get(index);
				if (marker.group.equals(id) && isAssignableSpawnMarker(marker.type))
					markers.set(index, new Marker(marker.id, "spawn_point", marker.position, ""));
			}
			regions.removeIf(region -> region.type.equals("trigger_region") && region.group.equals(id));
			if (activeEncounterGroup.equals(id))
				activeEncounterGroup = "default";
			pending = null;
			changed();
			return "Deleted encounter " + id + "; its spawn positions are now unassigned.";
		}

		public void cancelPending() {
			if (pending != null) {
				pending = null;
				changed();
			}
		}

		public String undoLast() {
			if (pending != null) {
				pending = null;
				changed();
				return "Canceled the unfinished two-point selection.";
			}
			ProjectState state = undoHistory.pollLast();
			if (state == null)
				return "Nothing to undo in this play session.";
			structureBounds = state.structureBounds;
			origin = state.origin;
			pending = state.pending;
			regions.clear();
			regions.addAll(state.regions);
			markers.clear();
			markers.addAll(state.markers);
			sockets.clear();
			sockets.addAll(state.sockets);
			roomRole = state.roomRole;
			activeEncounterGroup = state.activeEncounterGroup;
			defaultMobPool = state.defaultMobPool;
			bossMobPool = state.bossMobPool;
			allowedRanks = EnumSet.copyOf(state.allowedRanks);
			roomWeight = state.roomWeight;
			shellBlock = state.shellBlock;
			shellThickness = state.shellThickness;
			roomSnapshot = state.roomSnapshot;
			encounters.clear();
			state.encounters.forEach(encounter -> encounters.put(encounter.id(), encounter));
			changed();
			return "Undid the most recent authoring action.";
		}

		public String eraseNearest(BlockPos pos) {
			EraseTarget nearest = null;
			for (int index = 0; index < markers.size(); index++) {
				Marker marker = markers.get(index);
				nearest = nearer(nearest, new EraseTarget("marker", index,
						distanceSquared(pos, marker.position), marker.id));
			}
			for (int index = 0; index < sockets.size(); index++) {
				Socket socket = sockets.get(index);
				nearest = nearer(nearest, new EraseTarget("socket", index,
						distanceSquared(pos, socket.opening), socket.id));
			}
			for (int index = 0; index < regions.size(); index++) {
				Region region = regions.get(index);
				nearest = nearer(nearest, new EraseTarget("region", index,
						distanceSquared(pos, region.bounds), region.id));
			}
			if (nearest == null || nearest.distance > 64.0D)
				return "No marker, socket, or region was found within 8 blocks.";

			rememberState();
			switch (nearest.collection) {
				case "marker" -> markers.remove(nearest.index);
				case "socket" -> sockets.remove(nearest.index);
				case "region" -> {
					Region removed = regions.remove(nearest.index);
					if (removed.type.equals("trigger_region") && regions.stream().noneMatch(region ->
							region.type.equals("trigger_region") && region.group.equals(removed.group))) {
						Encounter encounter = encounters.get(removed.group);
						if (encounter != null)
							encounters.put(removed.group, new Encounter(encounter.id, encounter.pool,
									encounter.minLevel, encounter.maxLevel, false));
					}
				}
				default -> throw new IllegalStateException("Unknown erase target " + nearest.collection);
			}
			pending = null;
			changed();
			return "Removed " + nearest.collection + " " + nearest.id + ".";
		}

		public List<Issue> validate() {
			List<Issue> issues = new ArrayList<>();
			if (structureBounds == null) {
				issues.add(new Issue(Severity.ERROR, "Select the structure bounds with the Surveyor Wand."));
				return issues;
			}
			BlockPos size = structureBounds.size();
			if (size.getX() > MAX_STRUCTURE_AXIS || size.getY() > MAX_STRUCTURE_AXIS
					|| size.getZ() > MAX_STRUCTURE_AXIS || structureBounds.volume() > MAX_STRUCTURE_VOLUME)
				issues.add(new Issue(Severity.ERROR, "Structure is too large for safe single-tick capture. Keep each export within 48 blocks per axis (48 x 48 x 48 maximum)."));
			if (origin != null && !structureBounds.contains(origin))
				issues.add(new Issue(Severity.ERROR, "The structure origin is outside the selected bounds."));
			if (roomSnapshot != null) {
				String snapshotProblem = roomSnapshotProblem(roomSnapshot, structureBounds);
				if (snapshotProblem != null)
					issues.add(new Issue(Severity.ERROR, snapshotProblem));
			}

			for (Region region : regions)
				if (!structureBounds.contains(region.bounds.min) || !structureBounds.contains(region.bounds.max))
					issues.add(new Issue(Severity.ERROR, "Region " + region.id + " extends outside the structure bounds."));
			for (Marker marker : markers)
				if (!structureBounds.contains(marker.position))
					issues.add(new Issue(Severity.ERROR, "Marker " + marker.id + " is outside the structure bounds."));
			for (Socket socket : sockets)
				if (!structureBounds.contains(socket.opening.min) || !structureBounds.contains(socket.opening.max))
					issues.add(new Issue(Severity.ERROR, "Socket " + socket.id + " extends outside the structure bounds."));
				else if (!isOnBoundaryOrOneInside(socket, structureBounds))
					issues.add(new Issue(Severity.ERROR, "Socket " + socket.id + " must be on the indicated wall or exactly one block inside it."));
				else if (socket.type.equals("corridor") && socket.facing.getAxis().isVertical())
					issues.add(new Issue(Severity.ERROR, "Corridor socket " + socket.id + " must face horizontally."));
				else if (socket.type.equals("stair") && socket.facing.getAxis().isHorizontal())
					issues.add(new Issue(Severity.ERROR, "Stair socket " + socket.id + " must face up or down."));
			if (sockets.stream().filter(Socket::required).count() > 2)
				issues.add(new Issue(Severity.ERROR, "Schema v2 supports at most two required sockets per room. Leave extra junction branches optional."));

			if (kind == ProjectKind.PRESET) {
				if (regions.stream().noneMatch(region -> region.type.equals("room")))
					issues.add(new Issue(Severity.ERROR, "Preset dungeons require one Room Bounds volume."));
				if (!hasMarker("player_start"))
					issues.add(new Issue(Severity.ERROR, "Preset dungeons require one Player Start marker."));
				if (!hasMarker("exit") && !hasMarker("return_portal"))
					issues.add(new Issue(Severity.ERROR, "Preset dungeons require an Exit or Return Portal marker."));
				if (!hasMarker("boss_spawn"))
					issues.add(new Issue(Severity.ERROR, "Preset dungeons require one Boss Spawn marker so the run can complete."));
			} else {
				long roomRegionCount = regions.stream().filter(region -> region.type.equals("room")).count();
				if (roomRegionCount == 0)
					issues.add(new Issue(Severity.ERROR, "This module needs one Room Bounds volume. One module project represents exactly one room."));
				else if (roomRegionCount > 1)
					issues.add(new Issue(Severity.ERROR, "This module contains " + roomRegionCount
							+ " Room Bounds volumes. One module project represents exactly one room; select Room Bounds once more to replace the old selections."));
				if (roomRole == RoomRole.CAP)
					issues.add(new Issue(Severity.ERROR, "CAP modules are reserved for a future schema version. Leave unused sockets as solid authored walls instead."));
				if (sockets.isEmpty())
					issues.add(new Issue(Severity.ERROR, "Procedural modules require at least one socket."));
				if ((roomRole == RoomRole.START || roomRole == RoomRole.BOSS || roomRole == RoomRole.CAP) && sockets.size() != 1)
					issues.add(new Issue(Severity.ERROR, display(roomRole.name()) + " modules must have exactly one socket so they remain terminal rooms."));
				if ((roomRole == RoomRole.NORMAL || roomRole == RoomRole.CORRIDOR) && sockets.size() < 2)
					issues.add(new Issue(Severity.ERROR, display(roomRole.name())
							+ " modules need at least two sockets: one entrance and one exit."));
				if (roomRole == RoomRole.NORMAL && sockets.size() > 2)
					issues.add(new Issue(Severity.WARNING, "A normal module usually has two sockets. Use the junction role when this room is intended to branch."));
				if (roomRole == RoomRole.START && !hasMarker("player_start"))
					issues.add(new Issue(Severity.ERROR, "Start modules require a Player Start marker."));
				if (roomRole == RoomRole.START && !hasMarker("exit") && !hasMarker("return_portal"))
					issues.add(new Issue(Severity.ERROR, "Start modules require an Exit or Return Portal marker for safe dungeon return."));
				if (roomRole == RoomRole.BOSS && !hasMarker("boss_spawn"))
					issues.add(new Issue(Severity.ERROR, "Boss modules require a Boss Spawn marker."));
			}
			for (Marker marker : markers) {
				if (isEncounterMarker(marker.type) && !encounters.containsKey(marker.group))
					issues.add(new Issue(Severity.ERROR, "Marker " + marker.id + " refers to missing encounter group " + marker.group + "."));
			}
			for (Region region : regions) {
				if (region.type.equals("trigger_region") && !encounters.containsKey(region.group))
					issues.add(new Issue(Severity.ERROR, "Trigger " + region.id + " refers to missing encounter group " + region.group + "."));
			}
			for (Encounter encounter : encounters.values()) {
				boolean hasSpawn = markers.stream().anyMatch(marker -> isEncounterMarker(marker.type) && marker.group.equals(encounter.id));
				if (!hasSpawn)
					issues.add(new Issue(Severity.WARNING, "Encounter group " + encounter.id
							+ " has no assigned spawn points and will not be exported."));
				if (encounter.minLevel < 1 || encounter.maxLevel < encounter.minLevel
						|| (encounter.maxLevel != Integer.MAX_VALUE && encounter.maxLevel > MAX_ENCOUNTER_LEVEL))
					issues.add(new Issue(Severity.ERROR, "Encounter group " + encounter.id + " must use levels 1-"
							+ MAX_ENCOUNTER_LEVEL + ", or the automatic dungeon-level setting."));
				if (encounter.delayed && regions.stream().noneMatch(region -> region.type.equals("trigger_region")
						&& region.group.equals(encounter.id)))
					issues.add(new Issue(Severity.ERROR, "Delayed encounter group " + encounter.id
							+ " has no Trigger Region. Place one or erase/recreate the encounter as automatic."));
				boolean hasBoss = markers.stream().anyMatch(marker -> marker.group.equals(encounter.id)
						&& marker.type.equals("boss_spawn"));
				boolean hasNonBoss = markers.stream().anyMatch(marker -> marker.group.equals(encounter.id)
						&& (marker.type.equals("mob_spawn") || marker.type.equals("elite_spawn")));
				if (hasBoss && hasNonBoss)
					issues.add(new Issue(Severity.ERROR, "Encounter group " + encounter.id
							+ " mixes boss and normal spawn points. Assign them to separate encounters."));
			}
			long unassignedSpawns = markers.stream().filter(marker -> marker.type.equals("spawn_point")).count();
			if (unassignedSpawns > 0)
				issues.add(new Issue(Severity.WARNING, unassignedSpawns + " spawn point"
						+ (unassignedSpawns == 1 ? " is" : "s are")
						+ " not configured yet. Assign an encounter in the Dungeon Builder GUI; unassigned points are not exported."));
			if (!hasMarker("mob_spawn") && !hasMarker("elite_spawn") && !hasMarker("boss_spawn")
					&& unassignedSpawns == 0)
				issues.add(new Issue(Severity.WARNING, "This structure has no encounter spawn markers."));
			if (pending != null)
				issues.add(new Issue(Severity.WARNING, "An unfinished two-point selection is still active."));
			return issues;
		}

		public long errorCount() {
			return validate().stream().filter(issue -> issue.severity == Severity.ERROR).count();
		}

		public String summary() {
			String boundsText = structureBounds == null ? "not selected" : shortPos(structureBounds.min) + " to " + shortPos(structureBounds.max);
			String roleText = kind == ProjectKind.MODULE ? "/" + roomRole.name().toLowerCase(Locale.ROOT) : "";
			return namespace + ":" + name + " [" + kind.name().toLowerCase(Locale.ROOT) + roleText + "] | ranks "
					+ rankText(allowedRanks) + " | bounds " + boundsText
					+ " | group " + activeEncounterGroup + " | " + regions.size() + " regions | " + sockets.size() + " sockets | " + markers.size() + " markers";
		}

		private boolean hasMarker(String type) {
			return markers.stream().anyMatch(marker -> marker.type.equals(type));
		}

		private void rememberState() {
			undoHistory.addLast(new ProjectState(structureBounds, origin, pending,
					List.copyOf(regions), List.copyOf(markers), List.copyOf(sockets), roomRole,
					activeEncounterGroup, defaultMobPool, bossMobPool, Set.copyOf(allowedRanks), roomWeight, shellBlock,
					shellThickness, roomSnapshot, List.copyOf(encounters.values())));
			while (undoHistory.size() > UNDO_LIMIT)
				undoHistory.removeFirst();
		}

		private boolean isPending(String kind) {
			return pending != null && pending.kind.equals(kind);
		}

		private static boolean isUniqueMarker(String type) {
			return type.equals("player_start") || type.equals("return_portal") || type.equals("boss_spawn");
		}

		private static boolean isEncounterMarker(String type) {
			return type.equals("mob_spawn") || type.equals("elite_spawn") || type.equals("boss_spawn");
		}

		private static boolean isAssignableSpawnMarker(String type) {
			return type.equals("spawn_point") || isEncounterMarker(type);
		}

		private int markerIndex(String id) {
			for (int index = 0; index < markers.size(); index++)
				if (markers.get(index).id.equals(id))
					return index;
			return -1;
		}

		private int socketIndex(String id) {
			for (int index = 0; index < sockets.size(); index++)
				if (sockets.get(index).id.equals(id))
					return index;
			return -1;
		}

		private void ensureEncounter(String group, boolean boss) {
			encounters.computeIfAbsent(group,
					id -> new Encounter(id, boss ? bossMobPool : defaultMobPool, 1, Integer.MAX_VALUE, false));
		}

		private static boolean samePlane(BlockPos first, BlockPos second, Direction facing) {
			return switch (facing.getAxis()) {
				case X -> first.getX() == second.getX();
				case Y -> first.getY() == second.getY();
				case Z -> first.getZ() == second.getZ();
			};
		}

		private static boolean isOnBoundaryOrOneInside(Socket socket, Bounds structureBounds) {
			return switch (socket.facing) {
				case WEST -> socket.opening.min.getX() == structureBounds.min.getX() || socket.opening.min.getX() == structureBounds.min.getX() + 1;
				case EAST -> socket.opening.max.getX() == structureBounds.max.getX() || socket.opening.max.getX() == structureBounds.max.getX() - 1;
				case DOWN -> socket.opening.min.getY() == structureBounds.min.getY() || socket.opening.min.getY() == structureBounds.min.getY() + 1;
				case UP -> socket.opening.max.getY() == structureBounds.max.getY() || socket.opening.max.getY() == structureBounds.max.getY() - 1;
				case NORTH -> socket.opening.min.getZ() == structureBounds.min.getZ() || socket.opening.min.getZ() == structureBounds.min.getZ() + 1;
				case SOUTH -> socket.opening.max.getZ() == structureBounds.max.getZ() || socket.opening.max.getZ() == structureBounds.max.getZ() - 1;
			};
		}

		private static EraseTarget nearer(@Nullable EraseTarget current, EraseTarget candidate) {
			return current == null || eraseScore(candidate) < eraseScore(current) ? candidate : current;
		}

		private static double eraseScore(EraseTarget target) {
			int priority = switch (target.collection) {
				case "marker" -> 0;
				case "socket" -> 1;
				default -> 2;
			};
			return target.distance + priority * 16.0D;
		}

		private static double distanceSquared(BlockPos first, BlockPos second) {
			long dx = (long) first.getX() - second.getX();
			long dy = (long) first.getY() - second.getY();
			long dz = (long) first.getZ() - second.getZ();
			return (double) dx * dx + (double) dy * dy + (double) dz * dz;
		}

		private static double distanceSquared(BlockPos pos, Bounds bounds) {
			long dx = Math.max(Math.max((long) bounds.min.getX() - pos.getX(), 0L), (long) pos.getX() - bounds.max.getX());
			long dy = Math.max(Math.max((long) bounds.min.getY() - pos.getY(), 0L), (long) pos.getY() - bounds.max.getY());
			long dz = Math.max(Math.max((long) bounds.min.getZ() - pos.getZ(), 0L), (long) pos.getZ() - bounds.max.getZ());
			return (double) dx * dx + (double) dy * dy + (double) dz * dz;
		}

		private CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Namespace", namespace);
			tag.putString("Name", name);
			tag.putString("Kind", kind.name());
			tag.putString("RoomRole", roomRole.name());
			tag.putString("ActiveEncounterGroup", activeEncounterGroup);
			tag.putString("DefaultMobPool", defaultMobPool);
			tag.putString("BossMobPool", bossMobPool);
			tag.putString("AllowedRanks", allowedRanks.stream().map(Enum::name)
					.collect(java.util.stream.Collectors.joining(",")));
			tag.putInt("RoomWeight", roomWeight);
			tag.putString("ShellBlock", shellBlock);
			tag.putInt("ShellThickness", shellThickness);
			if (structureBounds != null)
				tag.put("StructureBounds", structureBounds.save());
			if (origin != null)
				tag.put("Origin", NbtUtils.writeBlockPos(origin));
			if (pending != null)
				tag.put("Pending", pending.save());
			if (roomSnapshot != null)
				tag.put("RoomSnapshot", saveRoomSnapshot(roomSnapshot));
			tag.put("Regions", saveList(regions.stream().map(Region::save).toList()));
			tag.put("Markers", saveList(markers.stream().map(Marker::save).toList()));
			tag.put("Sockets", saveList(sockets.stream().map(Socket::save).toList()));
			tag.put("Encounters", saveList(encounters.values().stream().map(Encounter::save).toList()));
			return tag;
		}

		private static Project load(CompoundTag tag) {
			Project project = new Project(sanitizeId(tag.getString("Namespace"), "builder", MAX_NAMESPACE_LENGTH),
					sanitizeId(tag.getString("Name"), "my_dungeon", MAX_NAME_LENGTH), ProjectKind.parse(tag.getString("Kind")));
			project.roomRole = RoomRole.parse(tag.getString("RoomRole"));
			project.activeEncounterGroup = tag.contains("ActiveEncounterGroup", Tag.TAG_STRING)
					? sanitizeId(tag.getString("ActiveEncounterGroup"), "default", MAX_LOCAL_ID_LENGTH) : "default";
			if (tag.contains("DefaultMobPool", Tag.TAG_STRING) && isResourceId(tag.getString("DefaultMobPool")))
				project.defaultMobPool = tag.getString("DefaultMobPool");
			if (tag.contains("BossMobPool", Tag.TAG_STRING) && isResourceId(tag.getString("BossMobPool")))
				project.bossMobPool = tag.getString("BossMobPool");
			if (tag.contains("AllowedRanks", Tag.TAG_STRING)) {
				EnumSet<ProceduralDungeonRank> loadedRanks = EnumSet.noneOf(ProceduralDungeonRank.class);
				for (String value : tag.getString("AllowedRanks").split(","))
					ProceduralDungeonRank.tryParse(value.trim()).ifPresent(loadedRanks::add);
				if (!loadedRanks.isEmpty())
					project.allowedRanks = loadedRanks;
			}
			project.roomWeight = tag.contains("RoomWeight", Tag.TAG_INT) ? Math.max(1, tag.getInt("RoomWeight")) : 1;
			if (tag.contains("ShellBlock", Tag.TAG_STRING) && isResourceId(tag.getString("ShellBlock")))
				project.shellBlock = tag.getString("ShellBlock");
			project.shellThickness = tag.contains("ShellThickness", Tag.TAG_INT)
					? Math.max(0, Math.min(4, tag.getInt("ShellThickness"))) : 1;
			if (tag.contains("StructureBounds", Tag.TAG_COMPOUND))
				project.structureBounds = Bounds.load(tag.getCompound("StructureBounds"));
			if (tag.contains("Origin", Tag.TAG_COMPOUND))
				project.origin = NbtUtils.readBlockPos(tag.getCompound("Origin"));
			if (tag.contains("Pending", Tag.TAG_COMPOUND))
				project.pending = Pending.load(tag.getCompound("Pending"));
			readList(tag, "Regions").forEach(element -> project.regions.add(Region.load(element)));
			readList(tag, "Markers").forEach(element -> {
				Marker marker = Marker.load(element);
				project.markers.add(marker.type.equals("spawn_point")
						? new Marker(marker.id, marker.type, marker.position, "") : marker);
			});
			readList(tag, "Sockets").forEach(element -> project.sockets.add(Socket.load(element)));
			readList(tag, "Encounters").forEach(element -> {
				Encounter encounter = Encounter.load(element);
				project.encounters.put(encounter.id(), encounter);
			});
			if (tag.contains("RoomSnapshot", Tag.TAG_COMPOUND)) {
				try {
					RoomSnapshot snapshot = loadRoomSnapshot(tag.getCompound("RoomSnapshot"));
					if (roomSnapshotProblem(snapshot, project.structureBounds) == null)
						project.roomSnapshot = snapshot;
				} catch (RuntimeException ignored) {
					// An invalid optional snapshot must not discard legacy project metadata.
				}
			}
			boolean legacyDefaultBoss = project.markers.stream().anyMatch(marker -> marker.type.equals("boss_spawn")
					&& marker.group.equals("default"));
			boolean legacyDefaultNormal = project.markers.stream().anyMatch(marker -> (marker.type.equals("mob_spawn")
					|| marker.type.equals("elite_spawn")) && marker.group.equals("default"));
			if (legacyDefaultBoss) {
				for (int index = 0; index < project.markers.size(); index++) {
					Marker marker = project.markers.get(index);
					if (marker.type.equals("boss_spawn") && marker.group.equals("default"))
						project.markers.set(index, new Marker(marker.id, marker.type, marker.position, "boss"));
				}
				if (!legacyDefaultNormal) {
					for (int index = 0; index < project.regions.size(); index++) {
						Region region = project.regions.get(index);
						if (region.type.equals("trigger_region") && region.group.equals("default"))
							project.regions.set(index, new Region(region.id, region.type, region.bounds, "boss"));
					}
					project.encounters.remove("default");
				}
			}
			for (Marker marker : project.markers)
				if (isEncounterMarker(marker.type))
					project.ensureEncounter(marker.group, marker.type.equals("boss_spawn"));
			for (Region region : project.regions)
				if (region.type.equals("trigger_region"))
					project.ensureEncounter(region.group, false);
			return project;
		}

		private record ProjectState(@Nullable Bounds structureBounds, @Nullable BlockPos origin,
				@Nullable Pending pending, List<Region> regions, List<Marker> markers,
				List<Socket> sockets, RoomRole roomRole, String activeEncounterGroup,
				String defaultMobPool, String bossMobPool, Set<ProceduralDungeonRank> allowedRanks,
				int roomWeight, String shellBlock,
				int shellThickness, @Nullable RoomSnapshot roomSnapshot, List<Encounter> encounters) {
		}

		private record EraseTarget(String collection, int index, double distance, String id) {
		}
	}

	@Nullable
	private static String mobPoolProblem(BuilderMobPool pool) {
		if (pool == null || pool.id() == null)
			return "Mob pool and its resource ID are required.";
		if (pool.id().toString().length() > 192)
			return "Mob-pool resource IDs may contain at most 192 characters.";
		if (!safeResourceNamespace(pool.id().getNamespace()) || !safeResourcePath(pool.id().getPath()))
			return "Mob-pool IDs must use filesystem-safe namespace and path segments.";
		if (pool.entries().size() > MAX_MOB_POOL_ENTRIES)
			return "Mob pool " + pool.id() + " has more than " + MAX_MOB_POOL_ENTRIES + " entries.";
		for (int index = 0; index < pool.entries().size(); index++) {
			BuilderMobPool.Entry entry = pool.entries().get(index);
			if (entry == null || entry.selectorKind() == null || entry.selector() == null)
				return "Mob-pool entry " + (index + 1) + " is missing its selector.";
			if (entry.selector().toString().length() > 192)
				return "Mob-pool entry " + (index + 1) + " has an excessively long selector.";
			if (entry.selectorKind() == BuilderMobPool.SelectorKind.ENTITY
					&& entry.selector().toString().equals("minecraft:player"))
				return "minecraft:player cannot be used as a dungeon mob.";
			if (entry.weight() < 1 || entry.weight() > MAX_POOL_WEIGHT)
				return "Mob-pool entry " + (index + 1) + " weight must be 1-" + MAX_POOL_WEIGHT + ".";
			if (entry.requiredMod().isPresent()
					&& !entry.requiredMod().get().matches("[a-z][a-z0-9_-]{1,63}"))
				return "Mob-pool entry " + (index + 1) + " has an invalid required mod ID.";
			if (entry.eligibleLevel().isPresent()
					&& !rangeInside(entry.eligibleLevel().get(), 0, MAX_ENCOUNTER_LEVEL))
				return "Mob-pool entry " + (index + 1) + " eligible levels must be 0-"
						+ MAX_ENCOUNTER_LEVEL + ".";
			if (entry.spawnLevel().isPresent()
					&& !rangeInside(entry.spawnLevel().get(), 1, MAX_ENCOUNTER_LEVEL))
				return "Mob-pool entry " + (index + 1) + " spawn levels must be 1-"
						+ MAX_ENCOUNTER_LEVEL + ".";
			if (entry.baseXp().isPresent()
					&& (entry.baseXp().get() < 0 || entry.baseXp().get() > MAX_BASE_XP))
				return "Mob-pool entry " + (index + 1) + " XP must be 0-" + MAX_BASE_XP + ".";
		}
		return null;
	}

	private static boolean rangeInside(BuilderMobPool.LevelRange range, int minimum, int maximum) {
		return range != null && range.min() >= minimum && range.max() >= range.min() && range.max() <= maximum;
	}

	@Nullable
	private static String dungeonDraftStructuralProblem(DungeonDraft draft) {
		if (draft == null || draft.id() == null || draft.mode() == null || draft.topology() == null)
			return "Dungeon draft ID, mode, and topology are required.";
		if (draft.id().toString().length() > 192)
			return "Dungeon-draft resource IDs may contain at most 192 characters.";
		if (!safeResourceNamespace(draft.id().getNamespace()) || !safeResourcePath(draft.id().getPath()))
			return "Dungeon-draft IDs must use filesystem-safe namespace and path segments.";
		if (draft.rooms().size() > MAX_DRAFT_ROOM_REFS)
			return "A dungeon draft may reference at most " + MAX_DRAFT_ROOM_REFS + " room modules.";
		if (draft.allowedRanks().isEmpty())
			return "A dungeon draft must allow at least one rank.";
		if (draft.mode() == DungeonDraft.Mode.PROCEDURAL
				&& (draft.minRooms() < 3 || draft.maxRooms() < draft.minRooms() || draft.maxRooms() > 64))
			return "Dungeon room counts must be 3-64 and max must be at least min.";
		if (draft.mode() == DungeonDraft.Mode.PROCEDURAL
				&& draft.topology() == DungeonDraft.Topology.BRANCHING && draft.minRooms() < 4)
			return "Branching dungeons need a minimum of at least 4 rooms so one room can form a real branch.";
		if (draft.maxDepth() < 1 || draft.maxDepth() > 64)
			return "Dungeon max depth must be 1-64.";
		if (draft.shellBlock() == null)
			return "A dungeon shell block is required.";
		if (draft.shellThickness() < 0 || draft.shellThickness() > 4)
			return "Dungeon shell thickness must be 0-4.";
		if (draft.fixedPlacements().size() > MAX_FIXED_PLACEMENTS)
			return "A fixed dungeon may contain at most " + MAX_FIXED_PLACEMENTS + " placements.";
		if (draft.fixedConnections().size() > MAX_FIXED_CONNECTIONS)
			return "A fixed dungeon may contain at most " + MAX_FIXED_CONNECTIONS + " connections.";

		Set<ResourceLocation> roomIds = new HashSet<>();
		for (DungeonDraft.RoomRef room : draft.rooms()) {
			if (room == null || room.room() == null)
				return "Every room reference needs a resource ID.";
			if (!safeResourceNamespace(room.room().getNamespace()) || !safeResourcePath(room.room().getPath()))
				return "Room reference " + room.room() + " is not filesystem-safe.";
			if (!roomIds.add(room.room()))
				return "Dungeon draft contains duplicate room reference " + room.room() + ".";
			if (room.weight() < 1 || room.weight() > MAX_POOL_WEIGHT)
				return "Room " + room.room() + " weight must be 1-" + MAX_POOL_WEIGHT + ".";
		}

		Set<String> placementIds = new HashSet<>();
		for (DungeonDraft.FixedPlacement placement : draft.fixedPlacements()) {
			if (placement == null || placement.room() == null || placement.rotation() == null)
				return "Every fixed placement needs an ID, room, and rotation.";
			if (!safeResourceNamespace(placement.room().getNamespace())
					|| !safeResourcePath(placement.room().getPath()))
				return "Fixed placement room " + placement.room() + " is not filesystem-safe.";
			String idProblem = localIdProblem(placement.id(), "placement");
			if (idProblem != null)
				return idProblem;
			if (!placementIds.add(placement.id()))
				return "Duplicate fixed placement ID " + placement.id() + ".";
			if (Math.abs((long) placement.x()) > MAX_FIXED_COORDINATE
					|| Math.abs((long) placement.y()) > MAX_FIXED_COORDINATE
					|| Math.abs((long) placement.z()) > MAX_FIXED_COORDINATE)
				return "Fixed placement " + placement.id() + " is outside the supported coordinate range.";
		}
		for (DungeonDraft.FixedConnection connection : draft.fixedConnections()) {
			if (connection == null)
				return "Fixed connections cannot be null.";
			String problem = localIdProblem(connection.fromPlacement(), "connection placement");
			if (problem == null)
				problem = localIdProblem(connection.toPlacement(), "connection placement");
			if (problem == null)
				problem = localIdProblem(connection.fromSocket(), "connection socket");
			if (problem == null)
				problem = localIdProblem(connection.toSocket(), "connection socket");
			if (problem != null)
				return problem;
			if (connection.fromPlacement().equals(connection.toPlacement()))
				return "A fixed connection cannot connect a placement to itself.";
		}
		return null;
	}

	@Nullable
	private static String dungeonDraftProblem(Workspace workspace, DungeonDraft draft) {
		String structural = dungeonDraftStructuralProblem(draft);
		if (structural != null)
			return structural;
		if (!ForgeRegistries.BLOCKS.containsKey(draft.shellBlock())
				|| ForgeRegistries.BLOCKS.getValue(draft.shellBlock()) == Blocks.AIR)
			return "Shell block " + draft.shellBlock() + " is not a loaded, solid block.";

		for (DungeonDraft.RoomRef ref : draft.rooms()) {
			Project project = workspace.projects.get(ref.room().toString());
			if (project == null || project.kind != ProjectKind.MODULE)
				return "Referenced room " + ref.room() + " is not a module in this workspace.";
		}
		Map<String, DungeonDraft.FixedPlacement> placements = new LinkedHashMap<>();
		for (DungeonDraft.FixedPlacement placement : draft.fixedPlacements()) {
			Project project = workspace.projects.get(placement.room().toString());
			if (project == null || project.kind != ProjectKind.MODULE)
				return "Fixed placement " + placement.id() + " references missing module "
						+ placement.room() + ".";
			placements.put(placement.id(), placement);
		}
		Set<String> usedEndpoints = new HashSet<>();
		for (DungeonDraft.FixedConnection connection : draft.fixedConnections()) {
			DungeonDraft.FixedPlacement from = placements.get(connection.fromPlacement());
			DungeonDraft.FixedPlacement to = placements.get(connection.toPlacement());
			if (from == null || to == null)
				return "Fixed connection references a placement that does not exist.";
			Project fromProject = workspace.projects.get(from.room().toString());
			Project toProject = workspace.projects.get(to.room().toString());
			if (fromProject == null || fromProject.sockets.stream()
					.noneMatch(socket -> socket.id.equals(connection.fromSocket())))
				return "Fixed connection references missing socket " + connection.fromSocket()
						+ " on " + from.id() + ".";
			if (toProject == null || toProject.sockets.stream()
					.noneMatch(socket -> socket.id.equals(connection.toSocket())))
				return "Fixed connection references missing socket " + connection.toSocket()
						+ " on " + to.id() + ".";
			String firstEndpoint = connection.fromPlacement() + "\u0000" + connection.fromSocket();
			String secondEndpoint = connection.toPlacement() + "\u0000" + connection.toSocket();
			if (!usedEndpoints.add(firstEndpoint) || !usedEndpoints.add(secondEndpoint))
				return "A fixed-layout socket cannot be used by more than one connection.";
		}
		return null;
	}

	@Nullable
	private static String localIdProblem(String value, String label) {
		if (value == null || value.isBlank())
			return "Fixed " + label + " ID cannot be empty.";
		if (value.length() > MAX_LOCAL_ID_LENGTH)
			return "Fixed " + label + " ID may contain at most " + MAX_LOCAL_ID_LENGTH + " characters.";
		if (!value.matches("[a-z0-9][a-z0-9_.-]*") || value.endsWith("."))
			return "Fixed " + label + " IDs must use lowercase letters, numbers, _, - or .";
		return null;
	}

	private static boolean safeResourceNamespace(String namespace) {
		return safeFilesystemSegment(namespace);
	}

	private static boolean safeResourcePath(String path) {
		if (path == null || path.isBlank() || path.startsWith("/") || path.endsWith("/"))
			return false;
		for (String segment : path.split("/", -1))
			if (!safeFilesystemSegment(segment))
				return false;
		return true;
	}

	private static boolean safeFilesystemSegment(String segment) {
		if (segment == null || segment.isBlank() || segment.equals(".") || segment.equals("..")
				|| segment.endsWith("."))
			return false;
		return !segment.toLowerCase(Locale.ROOT)
				.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?");
	}

	@Nullable
	private static String roomSnapshotProblem(@Nullable RoomSnapshot snapshot, @Nullable Bounds bounds) {
		if (snapshot == null)
			return "Room snapshot metadata is required.";
		if (bounds == null)
			return "Select structure bounds before saving a room snapshot.";
		BlockPos size = snapshot.size();
		if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1
				|| size.getX() > MAX_STRUCTURE_AXIS || size.getY() > MAX_STRUCTURE_AXIS
				|| size.getZ() > MAX_STRUCTURE_AXIS)
			return "Room snapshot size must be 1-" + MAX_STRUCTURE_AXIS + " blocks on each axis.";
		if (!size.equals(bounds.size()) || !snapshot.captureMin().equals(bounds.min()))
			return "Room snapshot capture bounds no longer match this project's structure bounds.";
		if (snapshot.structureKey().toString().length() > 192)
			return "Room snapshot structure key is too long.";
		if (!snapshot.checksum().matches("(?i)[0-9a-f]{32,128}"))
			return "Room snapshot checksum must be a 32-128 character hexadecimal digest.";
		if (snapshot.capturedAt() < 0L || snapshot.metadataRevision() < 0L)
			return "Room snapshot timestamps and revisions cannot be negative.";
		return null;
	}

	private static CompoundTag saveRoomSnapshot(RoomSnapshot snapshot) {
		CompoundTag tag = new CompoundTag();
		tag.putString("StructureKey", snapshot.structureKey().toString());
		tag.put("Size", NbtUtils.writeBlockPos(snapshot.size()));
		tag.put("CaptureMin", NbtUtils.writeBlockPos(snapshot.captureMin()));
		tag.putString("Checksum", snapshot.checksum());
		tag.putLong("CapturedAt", snapshot.capturedAt());
		tag.putLong("MetadataRevision", snapshot.metadataRevision());
		return tag;
	}

	private static RoomSnapshot loadRoomSnapshot(CompoundTag tag) {
		ResourceLocation structureKey = ResourceLocation.tryParse(tag.getString("StructureKey"));
		if (structureKey == null || !tag.contains("Size", Tag.TAG_COMPOUND)
				|| !tag.contains("CaptureMin", Tag.TAG_COMPOUND))
			throw new IllegalArgumentException("Malformed room snapshot metadata");
		return new RoomSnapshot(structureKey, NbtUtils.readBlockPos(tag.getCompound("Size")),
				NbtUtils.readBlockPos(tag.getCompound("CaptureMin")), tag.getString("Checksum"),
				tag.contains("CapturedAt", Tag.TAG_LONG) ? tag.getLong("CapturedAt") : 0L,
				tag.contains("MetadataRevision", Tag.TAG_LONG) ? tag.getLong("MetadataRevision") : 0L);
	}

	private static CompoundTag saveMobPool(BuilderMobPool pool) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", pool.id().toString());
		ListTag entries = new ListTag();
		for (BuilderMobPool.Entry entry : pool.entries()) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.putString("SelectorKind", entry.selectorKind().name());
			entryTag.putString("Selector", entry.selector().toString());
			entryTag.putInt("Weight", entry.weight());
			entry.requiredMod().ifPresent(value -> entryTag.putString("RequiredMod", value));
			entry.eligibleLevel().ifPresent(value -> entryTag.put("EligibleLevel", saveRange(value)));
			entry.spawnLevel().ifPresent(value -> entryTag.put("SpawnLevel", saveRange(value)));
			entry.baseXp().ifPresent(value -> entryTag.putInt("BaseXp", value));
			entries.add(entryTag);
		}
		tag.put("Entries", entries);
		return tag;
	}

	private static BuilderMobPool loadMobPool(CompoundTag tag) {
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
		if (id == null)
			throw new IllegalArgumentException("Malformed mob-pool ID");
		List<BuilderMobPool.Entry> entries = new ArrayList<>();
		if (tag.contains("Entries", Tag.TAG_LIST)) {
			ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
			for (int index = 0; index < list.size() && entries.size() < MAX_MOB_POOL_ENTRIES; index++) {
				try {
					CompoundTag entry = list.getCompound(index);
					BuilderMobPool.SelectorKind kind = BuilderMobPool.SelectorKind.valueOf(
							entry.getString("SelectorKind").toUpperCase(Locale.ROOT));
					ResourceLocation selector = ResourceLocation.tryParse(entry.getString("Selector"));
					if (selector == null)
						continue;
					entries.add(new BuilderMobPool.Entry(kind, selector,
							entry.contains("Weight", Tag.TAG_INT) ? entry.getInt("Weight") : 1,
							entry.contains("RequiredMod", Tag.TAG_STRING)
									? Optional.of(entry.getString("RequiredMod")) : Optional.empty(),
							entry.contains("EligibleLevel", Tag.TAG_COMPOUND)
									? Optional.of(loadRange(entry.getCompound("EligibleLevel"))) : Optional.empty(),
							entry.contains("SpawnLevel", Tag.TAG_COMPOUND)
									? Optional.of(loadRange(entry.getCompound("SpawnLevel"))) : Optional.empty(),
							entry.contains("BaseXp", Tag.TAG_INT)
									? Optional.of(entry.getInt("BaseXp")) : Optional.empty()));
				} catch (RuntimeException ignored) {
					// Preserve all other entries when one optional integration is malformed.
				}
			}
		}
		return new BuilderMobPool(id, entries);
	}

	private static CompoundTag saveRange(BuilderMobPool.LevelRange range) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Min", range.min());
		tag.putInt("Max", range.max());
		return tag;
	}

	private static BuilderMobPool.LevelRange loadRange(CompoundTag tag) {
		return new BuilderMobPool.LevelRange(tag.getInt("Min"), tag.getInt("Max"));
	}

	private static CompoundTag saveDungeonDraft(DungeonDraft draft) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", draft.id().toString());
		tag.putString("Mode", draft.mode().name());
		tag.putString("Topology", draft.topology().name());
		tag.putString("AllowedRanks", java.util.Arrays.stream(ProceduralDungeonRank.values())
				.filter(draft.allowedRanks()::contains).map(Enum::name)
				.collect(java.util.stream.Collectors.joining(",")));
		tag.putInt("MinRooms", draft.minRooms());
		tag.putInt("MaxRooms", draft.maxRooms());
		tag.putInt("MaxDepth", draft.maxDepth());
		tag.putString("ShellBlock", draft.shellBlock().toString());
		tag.putInt("ShellThickness", draft.shellThickness());
		ListTag rooms = new ListTag();
		for (DungeonDraft.RoomRef room : draft.rooms()) {
			CompoundTag roomTag = new CompoundTag();
			roomTag.putString("Room", room.room().toString());
			roomTag.putInt("Weight", room.weight());
			rooms.add(roomTag);
		}
		tag.put("Rooms", rooms);
		ListTag placements = new ListTag();
		for (DungeonDraft.FixedPlacement placement : draft.fixedPlacements()) {
			CompoundTag placementTag = new CompoundTag();
			placementTag.putString("Id", placement.id());
			placementTag.putString("Room", placement.room().toString());
			placementTag.putInt("X", placement.x());
			placementTag.putInt("Y", placement.y());
			placementTag.putInt("Z", placement.z());
			placementTag.putString("Rotation", placement.rotation().name());
			placements.add(placementTag);
		}
		tag.put("FixedPlacements", placements);
		ListTag connections = new ListTag();
		for (DungeonDraft.FixedConnection connection : draft.fixedConnections()) {
			CompoundTag connectionTag = new CompoundTag();
			connectionTag.putString("FromPlacement", connection.fromPlacement());
			connectionTag.putString("FromSocket", connection.fromSocket());
			connectionTag.putString("ToPlacement", connection.toPlacement());
			connectionTag.putString("ToSocket", connection.toSocket());
			connections.add(connectionTag);
		}
		tag.put("FixedConnections", connections);
		return tag;
	}

	private static DungeonDraft loadDungeonDraft(CompoundTag tag) {
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
		if (id == null)
			throw new IllegalArgumentException("Malformed dungeon-draft ID");
		DungeonDraft.Mode mode = enumOrDefault(DungeonDraft.Mode.class, tag.getString("Mode"),
				DungeonDraft.Mode.PROCEDURAL);
		DungeonDraft.Topology topology = enumOrDefault(DungeonDraft.Topology.class,
				tag.getString("Topology"), DungeonDraft.Topology.LINEAR);
		EnumSet<ProceduralDungeonRank> ranks = EnumSet.noneOf(ProceduralDungeonRank.class);
		if (tag.contains("AllowedRanks", Tag.TAG_STRING))
			for (String value : tag.getString("AllowedRanks").split(","))
				ProceduralDungeonRank.tryParse(value.trim()).ifPresent(ranks::add);
		if (ranks.isEmpty())
			ranks = EnumSet.allOf(ProceduralDungeonRank.class);
		ResourceLocation shell = tag.contains("ShellBlock", Tag.TAG_STRING)
				? ResourceLocation.tryParse(tag.getString("ShellBlock")) : null;
		if (shell == null)
			shell = new ResourceLocation("minecraft", "bedrock");
		List<DungeonDraft.RoomRef> rooms = new ArrayList<>();
		for (CompoundTag roomTag : readList(tag, "Rooms")) {
			if (rooms.size() >= MAX_DRAFT_ROOM_REFS)
				break;
			ResourceLocation room = ResourceLocation.tryParse(roomTag.getString("Room"));
			if (room != null)
				rooms.add(new DungeonDraft.RoomRef(room,
						roomTag.contains("Weight", Tag.TAG_INT) ? roomTag.getInt("Weight") : 1));
		}
		List<DungeonDraft.FixedPlacement> placements = new ArrayList<>();
		for (CompoundTag placementTag : readList(tag, "FixedPlacements")) {
			if (placements.size() >= MAX_FIXED_PLACEMENTS)
				break;
			ResourceLocation room = ResourceLocation.tryParse(placementTag.getString("Room"));
			if (room != null)
				placements.add(new DungeonDraft.FixedPlacement(placementTag.getString("Id"), room,
						placementTag.getInt("X"), placementTag.getInt("Y"), placementTag.getInt("Z"),
						enumOrDefault(DungeonDraft.PlacementRotation.class, placementTag.getString("Rotation"),
								DungeonDraft.PlacementRotation.NONE)));
		}
		List<DungeonDraft.FixedConnection> connections = new ArrayList<>();
		for (CompoundTag connectionTag : readList(tag, "FixedConnections")) {
			if (connections.size() >= MAX_FIXED_CONNECTIONS)
				break;
			connections.add(new DungeonDraft.FixedConnection(connectionTag.getString("FromPlacement"),
					connectionTag.getString("FromSocket"), connectionTag.getString("ToPlacement"),
					connectionTag.getString("ToSocket")));
		}
		return new DungeonDraft(id, mode, topology, rooms, ranks,
				tag.contains("MinRooms", Tag.TAG_INT) ? tag.getInt("MinRooms") : 3,
				tag.contains("MaxRooms", Tag.TAG_INT) ? tag.getInt("MaxRooms") : 3,
				tag.contains("MaxDepth", Tag.TAG_INT) ? tag.getInt("MaxDepth") : 3,
				shell, tag.contains("ShellThickness", Tag.TAG_INT) ? tag.getInt("ShellThickness") : 1,
				placements, connections);
	}

	private static <E extends Enum<E>> E enumOrDefault(Class<E> type, String value, E fallback) {
		if (value != null) {
			try {
				return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return fallback;
	}

	private static ListTag saveList(List<CompoundTag> entries) {
		ListTag list = new ListTag();
		entries.forEach(list::add);
		return list;
	}

	private static List<CompoundTag> readList(CompoundTag tag, String key) {
		ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
		List<CompoundTag> result = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++)
			result.add(list.getCompound(i));
		return result;
	}

	private static String nextId(String base, List<String> existing) {
		String cleanBase = sanitizeId(base, "marker");
		for (int index = 1; index < Integer.MAX_VALUE; index++) {
			String candidate = cleanBase + "_" + index;
			if (!existing.contains(candidate))
				return candidate;
		}
		return cleanBase;
	}

	private static String projectId(String namespace, String name) {
		return namespace + ":" + name;
	}

	private static boolean isResourceId(String value) {
		return value != null && ResourceLocation.tryParse(value) != null;
	}

	private static String display(String id) {
		String value = id.replace('_', ' ');
		return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static String rankText(Set<ProceduralDungeonRank> ranks) {
		if (ranks.size() == ProceduralDungeonRank.values().length)
			return "all";
		return java.util.Arrays.stream(ProceduralDungeonRank.values()).filter(ranks::contains)
				.map(Enum::name).collect(java.util.stream.Collectors.joining(","));
	}

	private static String shortPos(BlockPos pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}
}
