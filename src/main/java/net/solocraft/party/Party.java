package net.solocraft.party;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Persistent party identity, ordered roster, and pending join requests. */
final class Party {
	static final int MAX_MEMBERS = 8;
	static final int MAX_REQUESTS = 16;

	private final UUID id;
	private final String name;
	private final String legacyKey;
	private boolean legacyElectionOpen;
	private UUID leaderId;
	private boolean discoverable;
	private final LinkedHashMap<UUID, PartyMember> members = new LinkedHashMap<>();
	private final LinkedHashMap<UUID, PartyJoinRequest> requests = new LinkedHashMap<>();

	Party(UUID id, String name, PartyMember leader) {
		this(id, name, leader, "");
	}

	Party(UUID id, String name, PartyMember leader, String legacyKey) {
		this.id = id;
		this.name = name;
		this.legacyKey = legacyKey == null ? "" : legacyKey;
		this.legacyElectionOpen = !this.legacyKey.isEmpty();
		this.leaderId = leader.id();
		this.discoverable = true;
		members.put(leader.id(), leader);
	}

	private Party(UUID id, String name, String legacyKey, boolean legacyElectionOpen,
			UUID leaderId, boolean discoverable) {
		this.id = id;
		this.name = name;
		this.legacyKey = legacyKey == null ? "" : legacyKey;
		this.legacyElectionOpen = !this.legacyKey.isEmpty() && legacyElectionOpen;
		this.leaderId = leaderId;
		this.discoverable = discoverable;
	}

	UUID id() {
		return id;
	}

	String name() {
		return name;
	}

	String legacyKey() {
		return legacyKey;
	}

	UUID leaderId() {
		return leaderId;
	}

	boolean isLeader(UUID playerId) {
		return playerId != null && playerId.equals(leaderId);
	}

	boolean discoverable() {
		return discoverable;
	}

	void toggleDiscoverable() {
		discoverable = !discoverable;
	}

	Collection<PartyMember> members() {
		return List.copyOf(members.values());
	}

	Collection<PartyJoinRequest> requests() {
		return List.copyOf(requests.values());
	}

	PartyMember member(UUID playerId) {
		return members.get(playerId);
	}

	boolean contains(UUID playerId) {
		return members.containsKey(playerId);
	}

	boolean full() {
		return members.size() >= MAX_MEMBERS;
	}

	int size() {
		return members.size();
	}

	boolean addMember(PartyMember member) {
		if (member == null || full() || members.containsKey(member.id()))
			return false;
		members.put(member.id(), member);
		requests.remove(member.id());
		return true;
	}

	boolean removeMember(UUID playerId) {
		if (playerId == null || members.remove(playerId) == null)
			return false;
		if (playerId.equals(leaderId))
			leaderId = members.isEmpty() ? null : members.keySet().iterator().next();
		return true;
	}

	boolean transferLeader(UUID playerId) {
		if (playerId == null || playerId.equals(leaderId) || !members.containsKey(playerId))
			return false;
		leaderId = playerId;
		return true;
	}

	void preferLegacyLeader(UUID playerId) {
		if (legacyElectionOpen && playerId != null && members.containsKey(playerId)
				&& (leaderId == null || playerId.compareTo(leaderId) < 0))
			leaderId = playerId;
	}

	void settleLegacyLeadership() {
		legacyElectionOpen = false;
	}

	boolean updateMember(PartyMember update) {
		PartyMember current = update == null ? null : members.get(update.id());
		return current != null && current.update(update.name(), update.level(), update.rank());
	}

	PartyJoinRequest request(UUID playerId) {
		return requests.get(playerId);
	}

	boolean addRequest(PartyJoinRequest request) {
		if (request == null || requests.size() >= MAX_REQUESTS
				|| requests.containsKey(request.playerId()))
			return false;
		requests.put(request.playerId(), request);
		return true;
	}

	boolean removeRequest(UUID playerId) {
		return playerId != null && requests.remove(playerId) != null;
	}

	CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", id);
		tag.putString("Name", name);
		if (!legacyKey.isEmpty())
			tag.putString("LegacyKey", legacyKey);
		if (!legacyKey.isEmpty())
			tag.putBoolean("LegacyElectionOpen", legacyElectionOpen);
		if (leaderId != null)
			tag.putUUID("LeaderId", leaderId);
		tag.putBoolean("Discoverable", discoverable);

		ListTag memberTags = new ListTag();
		for (PartyMember member : members.values())
			memberTags.add(member.save());
		tag.put("Members", memberTags);

		ListTag requestTags = new ListTag();
		for (PartyJoinRequest request : requests.values())
			requestTags.add(request.save());
		tag.put("Requests", requestTags);
		return tag;
	}

	static Party load(CompoundTag tag) {
		if (tag == null || !tag.hasUUID("Id"))
			return null;
		UUID leaderId = tag.hasUUID("LeaderId") ? tag.getUUID("LeaderId") : null;
		String legacyKey = tag.getString("LegacyKey");
		Party party = new Party(tag.getUUID("Id"), tag.getString("Name"),
				legacyKey, !tag.contains("LegacyElectionOpen")
						|| tag.getBoolean("LegacyElectionOpen"), leaderId,
				!tag.contains("Discoverable") || tag.getBoolean("Discoverable"));
		ListTag memberTags = tag.getList("Members", Tag.TAG_COMPOUND);
		for (int index = 0; index < memberTags.size() && party.members.size() < MAX_MEMBERS; index++) {
			PartyMember member = PartyMember.load(memberTags.getCompound(index));
			if (member != null)
				party.members.putIfAbsent(member.id(), member);
		}
		if (party.members.isEmpty())
			return null;
		if (party.leaderId == null || !party.members.containsKey(party.leaderId))
			party.leaderId = party.members.keySet().iterator().next();

		ListTag requestTags = tag.getList("Requests", Tag.TAG_COMPOUND);
		long now = System.currentTimeMillis();
		for (int index = 0; index < requestTags.size()
				&& party.requests.size() < MAX_REQUESTS; index++) {
			PartyJoinRequest request = PartyJoinRequest.load(requestTags.getCompound(index));
			if (request != null && !request.expired(now) && !party.members.containsKey(request.playerId()))
				party.requests.putIfAbsent(request.playerId(), request);
		}
		return party;
	}
}
