package net.solocraft.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Durable entrant and exit-choice state for built-in, unscoped procedural gates.
 *
 * <p>Runtime/datapack dungeons already have {@code DungeonInstanceSavedData}.
 * Built-in procedural dungeons need this smaller receipt so Cartenon can be
 * offered only to the exact entrant set and so unloaded legacy return portals
 * cannot reappear after the alternate exit was chosen.</p>
 */
public final class ProceduralGateRunSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_procedural_gate_runs";
	private static final SavedData.Factory<ProceduralGateRunSavedData> FACTORY =
			new SavedData.Factory<>(ProceduralGateRunSavedData::new, ProceduralGateRunSavedData::load);
	private static final int MAX_RUNS = 2_048;
	private static final int MAX_PARTICIPANTS = 64;
	private static final int MAX_TAG_LENGTH = 128;

	private final Map<String, Run> runs = new LinkedHashMap<>();

	public enum ExitDecision {
		UNDECIDED,
		RETURN_PORTAL,
		CARTENON
	}

	public record RunView(Set<UUID> participants, ResourceKey<Level> dimension,
			boolean authoritativeRoster, @Nullable BlockPos returnAnchor,
			ExitDecision decision, @Nullable BlockPos exit) {
		public RunView {
			participants = Set.copyOf(participants);
		}
	}

	public record ReturnRequest(String dungeonTag, ResourceKey<Level> dimension,
			BlockPos exit) {
	}

	public static ProceduralGateRunSavedData get(MinecraftServer server) {
		if (server == null)
			throw new IllegalArgumentException("A server is required.");
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public Optional<RunView> run(String dungeonTag) {
		Run run = runs.get(cleanTag(dungeonTag));
		return run == null ? Optional.empty() : Optional.of(run.view());
	}

	public void recordEntrant(String dungeonTag, UUID playerId,
			ResourceKey<Level> dimension, long gameTime) {
		String clean = cleanTag(dungeonTag);
		if (clean.isEmpty() || playerId == null || dimension == null)
			return;
		Run run = runs.get(clean);
		if (run == null) {
			makeRoom();
			run = new Run(clean, dimension);
			run.authoritativeRoster = true;
			runs.put(clean, run);
		}
		if (run.decision != ExitDecision.UNDECIDED
				|| run.participants.contains(playerId)
				|| run.participants.size() >= MAX_PARTICIPANTS)
			return;
		run.dimension = dimension;
		run.participants.add(playerId);
		run.updatedGameTime = Math.max(0L, gameTime);
		setDirty();
	}

	/**
	 * Rolls back a player whose delayed gate entry was invalidated before the
	 * dungeon became active. Other participants and decided runs are retained.
	 */
	public void removeEntrant(String dungeonTag, UUID playerId) {
		String clean = cleanTag(dungeonTag);
		Run run = clean.isEmpty() || playerId == null ? null : runs.get(clean);
		if (run == null || !run.participants.remove(playerId))
			return;
		if (run.participants.isEmpty()
				&& run.decision == ExitDecision.UNDECIDED)
			runs.remove(clean);
		setDirty();
	}

	/**
	 * Migrates an already-active pre-feature run without claiming that the online
	 * player list is its complete roster. Such a run can suppress its legacy entry
	 * portal but will safely fall back to a normal boss exit instead of Cartenon.
	 */
	public void recordLegacyRun(String dungeonTag, ResourceKey<Level> dimension,
			long gameTime) {
		String clean = cleanTag(dungeonTag);
		if (clean.isEmpty() || dimension == null || runs.containsKey(clean))
			return;
		makeRoom();
		Run run = new Run(clean, dimension);
		run.updatedGameTime = Math.max(0L, gameTime);
		runs.put(clean, run);
		setDirty();
	}

	/**
	 * Persists the entrance-side return point before the boss is defeated.
	 * Recording an anchor does not complete the gate or make the portal usable.
	 */
	public void recordReturnAnchor(String dungeonTag, ResourceKey<Level> dimension,
			BlockPos returnAnchor, long gameTime) {
		Run run = getOrCreate(dungeonTag, dimension);
		if (run == null || returnAnchor == null
				|| run.decision != ExitDecision.UNDECIDED)
			return;
		BlockPos immutableAnchor = returnAnchor.immutable();
		boolean changed = !run.dimension.equals(dimension)
				|| !immutableAnchor.equals(run.returnAnchor);
		run.dimension = dimension;
		run.returnAnchor = immutableAnchor;
		run.updatedGameTime = Math.max(0L, gameTime);
		if (changed)
			setDirty();
	}

	public void chooseReturnPortal(String dungeonTag, ResourceKey<Level> dimension,
			BlockPos exit, long gameTime) {
		Run run = getOrCreate(dungeonTag, dimension);
		if (run == null || exit == null)
			return;
		if (run.decision != ExitDecision.UNDECIDED)
			return;
		run.dimension = dimension;
		run.decision = ExitDecision.RETURN_PORTAL;
		run.exit = exit.immutable();
		run.updatedGameTime = Math.max(0L, gameTime);
		setDirty();
	}

	public void chooseCartenon(String dungeonTag, ResourceKey<Level> dimension,
			long gameTime) {
		Run run = getOrCreate(dungeonTag, dimension);
		if (run == null)
			return;
		if (run.decision != ExitDecision.UNDECIDED)
			return;
		run.dimension = dimension;
		run.decision = ExitDecision.CARTENON;
		run.exit = null;
		run.updatedGameTime = Math.max(0L, gameTime);
		setDirty();
	}

	public List<ReturnRequest> returnRequests() {
		return runs.values().stream()
				.filter(run -> run.decision == ExitDecision.RETURN_PORTAL
						&& run.exit != null)
				.map(run -> new ReturnRequest(run.dungeonTag, run.dimension, run.exit))
				.toList();
	}

	@Override
	@Nonnull
	public CompoundTag save(@Nonnull CompoundTag root, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Run run : runs.values()) {
			CompoundTag tag = new CompoundTag();
			tag.putString("DungeonTag", run.dungeonTag);
			tag.putString("Dimension", run.dimension.location().toString());
			tag.putBoolean("AuthoritativeRoster", run.authoritativeRoster);
			tag.putString("Decision", run.decision.name());
			tag.putLong("UpdatedGameTime", run.updatedGameTime);
			if (run.returnAnchor != null)
				tag.put("ReturnAnchor", NbtUtils.writeBlockPos(run.returnAnchor));
			if (run.exit != null)
				tag.put("Exit", NbtUtils.writeBlockPos(run.exit));
			ListTag participants = new ListTag();
			for (UUID playerId : run.participants) {
				CompoundTag participant = new CompoundTag();
				participant.putUUID("Id", playerId);
				participants.add(participant);
			}
			tag.put("Participants", participants);
			list.add(tag);
		}
		root.put("Runs", list);
		return root;
	}

	private static ProceduralGateRunSavedData load(CompoundTag root, HolderLookup.Provider registries) {
		ProceduralGateRunSavedData data = new ProceduralGateRunSavedData();
		ListTag list = root.getList("Runs", Tag.TAG_COMPOUND);
		int limit = Math.min(MAX_RUNS, list.size());
		for (int index = 0; index < limit; index++) {
			CompoundTag tag = list.getCompound(index);
			String dungeonTag = cleanTag(tag.getString("DungeonTag"));
			ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
			if (dungeonTag.isEmpty() || dimensionId == null)
				continue;
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
			Run run = new Run(dungeonTag, dimension);
			run.authoritativeRoster = tag.getBoolean("AuthoritativeRoster");
			try {
				run.decision = ExitDecision.valueOf(tag.getString("Decision"));
			} catch (IllegalArgumentException ignored) {
				run.decision = ExitDecision.UNDECIDED;
			}
			run.updatedGameTime = Math.max(0L, tag.getLong("UpdatedGameTime"));
			if (tag.contains("ReturnAnchor", Tag.TAG_COMPOUND))
				run.returnAnchor = NbtUtils.readBlockPos(tag, "ReturnAnchor").orElse(null);
			if (tag.contains("Exit", Tag.TAG_COMPOUND))
				run.exit = NbtUtils.readBlockPos(tag, "Exit").orElse(null);
			ListTag participants = tag.getList("Participants", Tag.TAG_COMPOUND);
			int participantLimit = Math.min(MAX_PARTICIPANTS, participants.size());
			for (int participantIndex = 0; participantIndex < participantLimit; participantIndex++) {
				CompoundTag participant = participants.getCompound(participantIndex);
				if (participant.hasUUID("Id"))
					run.participants.add(participant.getUUID("Id"));
			}
			data.runs.putIfAbsent(dungeonTag, run);
		}
		if (list.size() > limit)
			data.setDirty();
		return data;
	}

	@Nullable
	private Run getOrCreate(String dungeonTag, ResourceKey<Level> dimension) {
		String clean = cleanTag(dungeonTag);
		if (clean.isEmpty() || dimension == null)
			return null;
		Run run = runs.get(clean);
		if (run != null)
			return run;
		makeRoom();
		run = new Run(clean, dimension);
		runs.put(clean, run);
		return run;
	}

	private void makeRoom() {
		if (runs.size() < MAX_RUNS)
			return;
		runs.values().stream()
				.min(Comparator.comparingLong(run -> run.updatedGameTime))
				.ifPresent(oldest -> runs.remove(oldest.dungeonTag));
	}

	private static String cleanTag(String dungeonTag) {
		if (dungeonTag == null)
			return "";
		String clean = dungeonTag.trim();
		return clean.length() <= MAX_TAG_LENGTH ? clean : "";
	}

	private static final class Run {
		private final String dungeonTag;
		private final LinkedHashSet<UUID> participants = new LinkedHashSet<>();
		private ResourceKey<Level> dimension;
		private boolean authoritativeRoster;
		@Nullable
		private BlockPos returnAnchor;
		private ExitDecision decision = ExitDecision.UNDECIDED;
		@Nullable
		private BlockPos exit;
		private long updatedGameTime;

		private Run(String dungeonTag, ResourceKey<Level> dimension) {
			this.dungeonTag = dungeonTag;
			this.dimension = dimension;
		}

		private RunView view() {
			return new RunView(participants, dimension, authoritativeRoster,
					returnAnchor, decision, exit);
		}
	}
}
