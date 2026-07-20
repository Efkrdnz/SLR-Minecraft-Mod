package net.solocraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-persistent ownership for the unique Ruler and Monarch vessels. */
public final class VesselClaimSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_vessel_claims";

	private final Map<String, LinkedHashSet<UUID>> claims = new LinkedHashMap<>();

	public static VesselClaimSavedData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage()
				.computeIfAbsent(VesselClaimSavedData::load, VesselClaimSavedData::new, DATA_NAME);
	}

	/**
	 * Atomically moves a player to a claim when capacity allows. A non-positive
	 * limit means unlimited vessels of each identity.
	 */
	public boolean tryClaim(String key, UUID playerId, int limit) {
		LinkedHashSet<UUID> target = claims.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
		if (target.contains(playerId))
			return true;
		if (limit > 0 && target.size() >= limit)
			return false;
		releaseInternal(playerId);
		target = claims.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
		target.add(playerId);
		setDirty();
		return true;
	}

	/** Records an existing save without deleting a player's already-earned job. */
	public void claimExisting(String key, UUID playerId) {
		Set<UUID> target = claims.computeIfAbsent(key, ignored -> new LinkedHashSet<>());
		if (target.contains(playerId))
			return;
		releaseInternal(playerId);
		claims.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(playerId);
		setDirty();
	}

	public void release(UUID playerId) {
		if (releaseInternal(playerId))
			setDirty();
	}

	public int count(String key) {
		return claims.getOrDefault(key, new LinkedHashSet<>()).size();
	}

	private boolean releaseInternal(UUID playerId) {
		boolean changed = false;
		for (Set<UUID> owners : claims.values())
			changed |= owners.remove(playerId);
		claims.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		return changed;
	}

	@Nonnull
	@Override
	public CompoundTag save(@Nonnull CompoundTag tag) {
		ListTag entries = new ListTag();
		for (Map.Entry<String, LinkedHashSet<UUID>> claim : claims.entrySet()) {
			for (UUID owner : claim.getValue()) {
				CompoundTag entry = new CompoundTag();
				entry.putString("Key", claim.getKey());
				entry.putUUID("Owner", owner);
				entries.add(entry);
			}
		}
		tag.put("Claims", entries);
		return tag;
	}

	private static VesselClaimSavedData load(CompoundTag tag) {
		VesselClaimSavedData data = new VesselClaimSavedData();
		ListTag entries = tag.getList("Claims", Tag.TAG_COMPOUND);
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entry = entries.getCompound(i);
			if (!entry.contains("Key") || !entry.hasUUID("Owner"))
				continue;
			data.claims.computeIfAbsent(entry.getString("Key"), ignored -> new LinkedHashSet<>())
					.add(entry.getUUID("Owner"));
		}
		return data;
	}
}
