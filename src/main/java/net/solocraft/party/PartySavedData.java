package net.solocraft.party;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Overworld-owned persistent storage for multiplayer parties and viewer preferences. */
public final class PartySavedData extends SavedData {
	private static final String DATA_NAME = "solocraft_parties";
	private static final SavedData.Factory<PartySavedData> FACTORY =
			new SavedData.Factory<>(PartySavedData::new, PartySavedData::load);

	private final Map<UUID, Party> parties = new LinkedHashMap<>();
	private final Map<UUID, UUID> playerParties = new LinkedHashMap<>();
	private final Map<String, UUID> partyNames = new LinkedHashMap<>();
	private final Map<String, UUID> legacyParties = new LinkedHashMap<>();
	private final Map<UUID, Set<UUID>> outgoingRequests = new LinkedHashMap<>();
	private final Map<UUID, GlowPreference> glowPreferences = new LinkedHashMap<>();
	private final Set<UUID> knownPlayers = new LinkedHashSet<>();
	private final Set<String> retiredLegacyKeys = new LinkedHashSet<>();

	public static PartySavedData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	Party party(UUID id) {
		return id == null ? null : parties.get(id);
	}

	Party partyForPlayer(UUID playerId) {
		if (playerId == null)
			return null;
		UUID partyId = playerParties.get(playerId);
		Party party = partyId == null ? null : parties.get(partyId);
		return party != null && party.contains(playerId) ? party : null;
	}

	Party partyByName(String name) {
		if (name == null)
			return null;
		String key = name.trim().toLowerCase(Locale.ROOT);
		return parties.get(partyNames.get(key));
	}

	Party partyByLegacyKey(String legacyKey) {
		if (legacyKey == null || legacyKey.isEmpty())
			return null;
		return parties.get(legacyParties.get(legacyKey));
	}

	Party create(String name, PartyMember leader) {
		if (name == null || leader == null || partyByName(name) != null
				|| partyForPlayer(leader.id()) != null)
			return null;
		Party party = new Party(UUID.randomUUID(), name, leader);
		parties.put(party.id(), party);
		indexParty(party);
		knownPlayers.add(leader.id());
		setDirty();
		return party;
	}

	Party createMigrated(String name, String legacyKey, PartyMember leader) {
		if (name == null || legacyKey == null || legacyKey.isEmpty() || leader == null
				|| partyByName(name) != null || partyByLegacyKey(legacyKey) != null
				|| retiredLegacyKeys.contains(legacyKey)
				|| partyForPlayer(leader.id()) != null)
			return null;
		Party party = new Party(UUID.randomUUID(), name, leader, legacyKey);
		parties.put(party.id(), party);
		indexParty(party);
		knownPlayers.add(leader.id());
		setDirty();
		return party;
	}

	boolean isKnownPlayer(UUID playerId) {
		return playerId != null && knownPlayers.contains(playerId);
	}

	void markKnownPlayer(UUID playerId) {
		if (playerId != null && knownPlayers.add(playerId))
			setDirty();
	}

	boolean remove(UUID partyId) {
		if (partyId == null)
			return false;
		Party removed = parties.remove(partyId);
		if (removed == null)
			return false;
		unindexParty(removed);
		if (!removed.legacyKey().isEmpty())
			retiredLegacyKeys.add(removed.legacyKey());
		setDirty();
		return true;
	}

	boolean isRetiredLegacyKey(String legacyKey) {
		return legacyKey != null && retiredLegacyKeys.contains(legacyKey);
	}

	boolean addMember(Party party, PartyMember member) {
		if (party == null || member == null || parties.get(party.id()) != party
				|| playerParties.containsKey(member.id()) || !party.addMember(member))
			return false;
		unindexRequest(member.id(), party.id());
		playerParties.put(member.id(), party.id());
		knownPlayers.add(member.id());
		setDirty();
		return true;
	}

	boolean addRequest(Party party, PartyJoinRequest request) {
		if (party == null || request == null || parties.get(party.id()) != party
				|| !party.addRequest(request))
			return false;
		outgoingRequests.computeIfAbsent(request.playerId(), ignored -> new LinkedHashSet<>())
				.add(party.id());
		setDirty();
		return true;
	}

	boolean removeRequest(Party party, UUID playerId) {
		if (party == null || playerId == null || parties.get(party.id()) != party
				|| !party.removeRequest(playerId))
			return false;
		unindexRequest(playerId, party.id());
		setDirty();
		return true;
	}

	List<Party> requestedParties(UUID playerId) {
		Set<UUID> ids = playerId == null ? null : outgoingRequests.get(playerId);
		if (ids == null || ids.isEmpty())
			return List.of();
		return ids.stream().map(parties::get).filter(java.util.Objects::nonNull).toList();
	}

	boolean removeMember(Party party, UUID playerId) {
		if (party == null || playerId == null || parties.get(party.id()) != party
				|| !party.removeMember(playerId))
			return false;
		playerParties.remove(playerId, party.id());
		knownPlayers.add(playerId);
		setDirty();
		return true;
	}

	boolean removeRequestsForPlayer(UUID playerId) {
		Set<UUID> requestedIds = playerId == null ? null : outgoingRequests.get(playerId);
		if (requestedIds == null || requestedIds.isEmpty())
			return false;
		boolean changed = false;
		for (UUID partyId : List.copyOf(requestedIds)) {
			Party party = parties.get(partyId);
			if (party != null)
				changed |= party.removeRequest(playerId);
			unindexRequest(playerId, partyId);
		}
		if (changed)
			setDirty();
		return changed;
	}

	boolean pruneExpiredRequests(long now) {
		boolean changed = false;
		for (Party party : parties.values())
			changed |= pruneExpiredRequests(party, now);
		if (changed)
			setDirty();
		return changed;
	}

	boolean pruneExpiredRequests(Party party, long now) {
		if (party == null || parties.get(party.id()) != party)
			return false;
		boolean changed = false;
		for (PartyJoinRequest request : List.copyOf(party.requests())) {
			if (request.expired(now) && party.removeRequest(request.playerId())) {
				unindexRequest(request.playerId(), party.id());
				changed = true;
			}
		}
		if (changed)
			setDirty();
		return changed;
	}

	boolean glowEnabled(UUID playerId) {
		GlowPreference preference = glowPreferences.get(playerId);
		return preference == null || preference.enabled();
	}

	int glowColor(UUID playerId) {
		GlowPreference preference = glowPreferences.get(playerId);
		return preference == null ? PartyService.DEFAULT_GLOW_COLOR : preference.color();
	}

	void setGlow(UUID playerId, boolean enabled, int color) {
		if (playerId == null)
			return;
		GlowPreference next = new GlowPreference(enabled, color & 0xFFFFFF);
		if (next.equals(glowPreferences.get(playerId)))
			return;
		glowPreferences.put(playerId, next);
		setDirty();
	}

	void markChanged() {
		setDirty();
	}

	@Nonnull
	@Override
	public CompoundTag save(@Nonnull CompoundTag root, HolderLookup.Provider registries) {
		ListTag partyTags = new ListTag();
		for (Party party : parties.values())
			partyTags.add(party.save());
		root.put("Parties", partyTags);

		ListTag preferenceTags = new ListTag();
		for (Map.Entry<UUID, GlowPreference> entry : glowPreferences.entrySet()) {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("PlayerId", entry.getKey());
			tag.putBoolean("Enabled", entry.getValue().enabled());
			tag.putInt("Color", entry.getValue().color());
			preferenceTags.add(tag);
		}
		root.put("GlowPreferences", preferenceTags);

		ListTag knownPlayerTags = new ListTag();
		for (UUID playerId : knownPlayers) {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", playerId);
			knownPlayerTags.add(tag);
		}
		root.put("KnownPlayers", knownPlayerTags);

		ListTag retiredKeyTags = new ListTag();
		for (String legacyKey : retiredLegacyKeys) {
			CompoundTag tag = new CompoundTag();
			tag.putString("Key", legacyKey);
			retiredKeyTags.add(tag);
		}
		root.put("RetiredLegacyKeys", retiredKeyTags);
		return root;
	}

	private static PartySavedData load(CompoundTag root, HolderLookup.Provider registries) {
		PartySavedData data = new PartySavedData();
		ListTag partyTags = root.getList("Parties", Tag.TAG_COMPOUND);
		for (int index = 0; index < partyTags.size(); index++) {
			Party party = Party.load(partyTags.getCompound(index));
			if (party != null && !data.parties.containsKey(party.id())
					&& data.partyByName(party.name()) == null
					&& (party.legacyKey().isEmpty()
							|| data.partyByLegacyKey(party.legacyKey()) == null)
					&& party.members().stream().noneMatch(member ->
							data.partyForPlayer(member.id()) != null)) {
				data.parties.put(party.id(), party);
				data.indexParty(party);
				party.members().forEach(member -> data.knownPlayers.add(member.id()));
			}
		}

		ListTag preferenceTags = root.getList("GlowPreferences", Tag.TAG_COMPOUND);
		for (int index = 0; index < preferenceTags.size(); index++) {
			CompoundTag tag = preferenceTags.getCompound(index);
			if (tag.hasUUID("PlayerId"))
				data.glowPreferences.put(tag.getUUID("PlayerId"),
						new GlowPreference(!tag.contains("Enabled") || tag.getBoolean("Enabled"),
								tag.getInt("Color") & 0xFFFFFF));
		}
		ListTag knownPlayerTags = root.getList("KnownPlayers", Tag.TAG_COMPOUND);
		for (int index = 0; index < knownPlayerTags.size(); index++) {
			CompoundTag tag = knownPlayerTags.getCompound(index);
			if (tag.hasUUID("Id"))
				data.knownPlayers.add(tag.getUUID("Id"));
		}
		ListTag retiredKeyTags = root.getList("RetiredLegacyKeys", Tag.TAG_COMPOUND);
		for (int index = 0; index < retiredKeyTags.size(); index++) {
			String legacyKey = retiredKeyTags.getCompound(index).getString("Key");
			if (!legacyKey.isEmpty())
				data.retiredLegacyKeys.add(legacyKey);
		}
		return data;
	}

	private void indexParty(Party party) {
		partyNames.put(party.name().trim().toLowerCase(Locale.ROOT), party.id());
		if (!party.legacyKey().isEmpty())
			legacyParties.put(party.legacyKey(), party.id());
		for (PartyMember member : party.members())
			playerParties.put(member.id(), party.id());
		for (PartyJoinRequest request : party.requests()) {
			outgoingRequests.computeIfAbsent(request.playerId(),
					ignored -> new LinkedHashSet<>()).add(party.id());
		}
	}

	private void unindexParty(Party party) {
		partyNames.remove(party.name().trim().toLowerCase(Locale.ROOT), party.id());
		if (!party.legacyKey().isEmpty())
			legacyParties.remove(party.legacyKey(), party.id());
		for (PartyMember member : party.members())
			playerParties.remove(member.id(), party.id());
		for (PartyJoinRequest request : party.requests())
			unindexRequest(request.playerId(), party.id());
	}

	private void unindexRequest(UUID playerId, UUID partyId) {
		Set<UUID> ids = outgoingRequests.get(playerId);
		if (ids == null)
			return;
		ids.remove(partyId);
		if (ids.isEmpty())
			outgoingRequests.remove(playerId);
	}

	private record GlowPreference(boolean enabled, int color) {
	}
}
