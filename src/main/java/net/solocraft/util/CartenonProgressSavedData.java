package net.solocraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent per-world discovery and instance state for the Cartenon Temple. */
public final class CartenonProgressSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_cartenon_progress";
	private static final SavedData.Factory<CartenonProgressSavedData> FACTORY =
			new SavedData.Factory<>(CartenonProgressSavedData::new, CartenonProgressSavedData::load);

	private final Map<UUID, PlayerProgress> players = new LinkedHashMap<>();
	private final Set<Integer> builtInstances = new LinkedHashSet<>();
	private int nextInstanceId = 1;

	public static CartenonProgressSavedData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	/** Records one unique normal-dungeon clear and reports whether a gate is now due. */
	public boolean recordDungeonClear(UUID playerId, String dungeonTag) {
		if (playerId == null || dungeonTag == null || dungeonTag.isBlank())
			return false;
		PlayerProgress progress = progress(playerId);
		if (progress.resolved || progress.gateOffered || !progress.dungeonTags.add(dungeonTag))
			return false;
		setDirty();
		return progress.dungeonTags.size() >= progress.targetClear;
	}

	public int allocateInstance() {
		int instanceId = Math.max(1, nextInstanceId++);
		setDirty();
		return instanceId;
	}

	public void markGateOffered(UUID playerId, int instanceId) {
		PlayerProgress progress = progress(playerId);
		progress.gateOffered = true;
		progress.instanceId = instanceId;
		setDirty();
	}

	public void cancelGateOffer(UUID playerId) {
		PlayerProgress progress = progress(playerId);
		progress.gateOffered = false;
		progress.instanceId = 0;
		setDirty();
	}

	public void associateInstance(UUID playerId, int instanceId) {
		PlayerProgress progress = progress(playerId);
		if (progress.instanceId == instanceId)
			return;
		progress.instanceId = instanceId;
		setDirty();
	}

	public int instanceFor(UUID playerId) {
		PlayerProgress progress = players.get(playerId);
		return progress == null ? 0 : progress.instanceId;
	}

	public boolean isResolved(UUID playerId) {
		PlayerProgress progress = players.get(playerId);
		return progress != null && progress.resolved;
	}

	public boolean accepted(UUID playerId) {
		PlayerProgress progress = players.get(playerId);
		return progress != null && progress.resolved && progress.accepted;
	}

	public void resolve(UUID playerId, boolean accepted) {
		PlayerProgress progress = progress(playerId);
		progress.resolved = true;
		progress.accepted = accepted;
		setDirty();
	}

	// --- The return. A second, separate visit made at the peak of the run. It
	// tracks its own instance so the finale temple can never collide with the
	// instance the player awakened in.

	public boolean isFinaleOffered(UUID playerId) {
		PlayerProgress progress = players.get(playerId);
		return progress != null && progress.finaleOffered;
	}

	public boolean isFinaleResolved(UUID playerId) {
		PlayerProgress progress = players.get(playerId);
		return progress != null && progress.finaleResolved;
	}

	public void markFinaleOffered(UUID playerId, int instanceId) {
		PlayerProgress progress = progress(playerId);
		progress.finaleOffered = true;
		progress.finaleInstanceId = Math.max(1, instanceId);
		setDirty();
	}

	/** Declining re-opens the offer; the player may return later. */
	public void cancelFinaleOffer(UUID playerId) {
		PlayerProgress progress = progress(playerId);
		progress.finaleOffered = false;
		progress.finaleInstanceId = 0;
		setDirty();
	}

	public void resolveFinale(UUID playerId) {
		PlayerProgress progress = progress(playerId);
		progress.finaleResolved = true;
		setDirty();
	}

	public int finaleInstanceFor(UUID playerId) {
		PlayerProgress progress = players.get(playerId);
		return progress == null ? 0 : progress.finaleInstanceId;
	}

	public boolean isInstanceBuilt(int instanceId) {
		return builtInstances.contains(instanceId);
	}

	public void markInstanceBuilt(int instanceId) {
		if (instanceId > 0 && builtInstances.add(instanceId))
			setDirty();
	}

	private PlayerProgress progress(UUID playerId) {
		return players.computeIfAbsent(playerId,
				id -> new PlayerProgress(1 + Math.floorMod(id.hashCode(), 3)));
	}

	@Nonnull
	@Override
	public CompoundTag save(@Nonnull CompoundTag tag, HolderLookup.Provider registries) {
		tag.putInt("NextInstance", nextInstanceId);
		tag.put("BuiltInstances", new IntArrayTag(builtInstances.stream().mapToInt(Integer::intValue).toArray()));

		ListTag playerList = new ListTag();
		for (Map.Entry<UUID, PlayerProgress> entry : players.entrySet()) {
			CompoundTag playerTag = new CompoundTag();
			playerTag.putUUID("Player", entry.getKey());
			PlayerProgress progress = entry.getValue();
			playerTag.putInt("TargetClear", progress.targetClear);
			playerTag.putBoolean("GateOffered", progress.gateOffered);
			playerTag.putBoolean("Resolved", progress.resolved);
			playerTag.putBoolean("Accepted", progress.accepted);
			playerTag.putInt("Instance", progress.instanceId);
			playerTag.putBoolean("FinaleOffered", progress.finaleOffered);
			playerTag.putBoolean("FinaleResolved", progress.finaleResolved);
			playerTag.putInt("FinaleInstance", progress.finaleInstanceId);
			ListTag clears = new ListTag();
			for (String dungeonTag : progress.dungeonTags)
				clears.add(StringTag.valueOf(dungeonTag));
			playerTag.put("DungeonTags", clears);
			playerList.add(playerTag);
		}
		tag.put("Players", playerList);
		return tag;
	}

	private static CartenonProgressSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		CartenonProgressSavedData data = new CartenonProgressSavedData();
		data.nextInstanceId = Math.max(1, tag.getInt("NextInstance"));
		for (int instanceId : tag.getIntArray("BuiltInstances")) {
			if (instanceId > 0)
				data.builtInstances.add(instanceId);
		}

		ListTag playerList = tag.getList("Players", Tag.TAG_COMPOUND);
		for (int i = 0; i < playerList.size(); i++) {
			CompoundTag playerTag = playerList.getCompound(i);
			if (!playerTag.hasUUID("Player"))
				continue;
			UUID playerId = playerTag.getUUID("Player");
			int targetClear = playerTag.contains("TargetClear", Tag.TAG_INT)
					? Math.max(1, Math.min(3, playerTag.getInt("TargetClear")))
					: 1 + Math.floorMod(playerId.hashCode(), 3);
			PlayerProgress progress = new PlayerProgress(targetClear);
			progress.gateOffered = playerTag.getBoolean("GateOffered");
			progress.resolved = playerTag.getBoolean("Resolved");
			progress.accepted = playerTag.getBoolean("Accepted");
			progress.instanceId = Math.max(0, playerTag.getInt("Instance"));
			progress.finaleOffered = playerTag.getBoolean("FinaleOffered");
			progress.finaleResolved = playerTag.getBoolean("FinaleResolved");
			progress.finaleInstanceId = Math.max(0, playerTag.getInt("FinaleInstance"));
			ListTag clears = playerTag.getList("DungeonTags", Tag.TAG_STRING);
			for (int clearIndex = 0; clearIndex < clears.size(); clearIndex++)
				progress.dungeonTags.add(clears.getString(clearIndex));
			data.players.put(playerId, progress);
		}
		return data;
	}

	private static final class PlayerProgress {
		private final int targetClear;
		private final LinkedHashSet<String> dungeonTags = new LinkedHashSet<>();
		private boolean gateOffered;
		private boolean resolved;
		private boolean accepted;
		private int instanceId;
		private boolean finaleOffered;
		private boolean finaleResolved;
		private int finaleInstanceId;

		private PlayerProgress(int targetClear) {
			this.targetClear = targetClear;
		}
	}
}
