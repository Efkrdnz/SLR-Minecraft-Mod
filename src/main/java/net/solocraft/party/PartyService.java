package net.solocraft.party;

import net.solocraft.SololevelingMod;
import net.solocraft.network.PartyStateMessage;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative operations and snapshots for the party interface. */
@EventBusSubscriber
public final class PartyService {
	public static final int DEFAULT_GLOW_COLOR = 0x55D8FF;

	private static final double NEARBY_RANGE = 96.0D;
	private static final double NEARBY_RANGE_SQR = NEARBY_RANGE * NEARBY_RANGE;
	private static final long REQUEST_LIFETIME_MS = 120_000L;
	private static final int MAX_REQUESTS_PER_PLAYER = 4;
	private static final int MAX_NEARBY_PARTIES = 24;
	private static final int MAX_ACTIONS_PER_SECOND = 6;
	private static final Map<UUID, ArrayDeque<Long>> ACTION_TICKS = new HashMap<>();
	private static final Map<UUID, Long> SNAPSHOT_TICKS = new HashMap<>();
	private static final Map<RequestMutationKey, Long> REQUEST_MUTATION_TICKS = new HashMap<>();

	private PartyService() {
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		reconcilePlayer(player);
		sendSnapshot(player, false, "");
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (false || event.getServer().overworld().getGameTime() % 20L != 0L)
			return;
		long gameTime = event.getServer().overworld().getGameTime();
		REQUEST_MUTATION_TICKS.values().removeIf(tick ->
				gameTime < tick || gameTime - tick >= 20L);
		PartySavedData.get(event.getServer().overworld())
				.pruneExpiredRequests(System.currentTimeMillis());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTION_TICKS.clear();
		SNAPSHOT_TICKS.clear();
		REQUEST_MUTATION_TICKS.clear();
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID playerId = event.getEntity().getUUID();
		ACTION_TICKS.remove(playerId);
		SNAPSHOT_TICKS.remove(playerId);
		REQUEST_MUTATION_TICKS.keySet().removeIf(key -> key.playerId().equals(playerId));
	}

	/** Handles a bounded action after the network layer has resolved the sender. */
	public static void handleAction(ServerPlayer player, String action, CompoundTag payload) {
		if (player == null || player.hasDisconnected() || !allowAction(player))
			return;
		String safeAction = action == null ? "" : action.trim();
		CompoundTag safePayload = payload == null ? new CompoundTag() : payload;
		switch (safeAction) {
			case "create" -> create(player, safePayload.getString("Name"), false);
			case "request_join" -> requestJoin(player, readUuid(safePayload, "PartyId"), false);
			case "cancel_request" -> cancelRequest(player, readUuid(safePayload, "PartyId"), false);
			case "accept_request" -> acceptRequest(player, readUuid(safePayload, "PlayerId"));
			case "deny_request" -> denyRequest(player, readUuid(safePayload, "PlayerId"));
			case "kick_member" -> kickMember(player, readUuid(safePayload, "PlayerId"));
			case "transfer_leader" -> transferLeader(player, readUuid(safePayload, "PlayerId"));
			case "leave" -> leave(player, false);
			case "disband" -> disband(player);
			case "toggle_discoverable" -> toggleDiscoverable(player);
			case "set_glow" -> setGlow(player, safePayload.getBoolean("Enabled"),
					safePayload.getInt("Color"));
			default -> sendSnapshot(player, false, "That party action is unavailable.");
		}
	}

	/** Handles GUI refresh/open requests without allowing packet polling every tick. */
	public static void requestSnapshot(ServerPlayer player, boolean open) {
		if (player == null || player.hasDisconnected())
			return;
		long now = player.serverLevel().getGameTime();
		Long previous = SNAPSHOT_TICKS.get(player.getUUID());
		if (previous != null && now >= previous && now - previous < 5L)
			return;
		SNAPSHOT_TICKS.put(player.getUUID(), now);
		sendSnapshot(player, open, "");
	}

	/**
	 * Sends the complete view permitted for this player. Passwords and private
	 * parties are never serialized.
	 */
	public static void sendSnapshot(ServerPlayer player, boolean open, String notice) {
		if (player == null || player.hasDisconnected())
			return;
		reconcilePlayer(player);
		PartySavedData data = PartySavedData.get(player.serverLevel());
		long now = System.currentTimeMillis();

		CompoundTag snapshot = new CompoundTag();
		Party ownParty = data.partyForPlayer(player.getUUID());
		refreshOnlineProfiles(player.getServer(), data, ownParty);
		snapshot.putBoolean("InParty", ownParty != null);
		snapshot.putInt("MaxMembers", Party.MAX_MEMBERS);
		snapshot.putBoolean("GlowEnabled", data.glowEnabled(player.getUUID()));
		snapshot.putInt("GlowColor", data.glowColor(player.getUUID()));
		snapshot.putString("Notice", cleanNotice(notice));

		ListTag memberTags = new ListTag();
		ListTag requestTags = new ListTag();
		if (ownParty != null) {
			snapshot.putUUID("PartyId", ownParty.id());
			snapshot.putString("PartyName", ownParty.name());
			snapshot.putUUID("LeaderId", ownParty.leaderId());
			snapshot.putString("LeaderName", memberName(ownParty, ownParty.leaderId()));
			snapshot.putBoolean("Discoverable", ownParty.discoverable());
			for (PartyMember member : ownParty.members())
				memberTags.add(memberSnapshot(player.getServer(), ownParty, member));
			if (ownParty.isLeader(player.getUUID())) {
				for (PartyJoinRequest request : ownParty.requests())
					requestTags.add(requestSnapshot(player.getServer(), request));
			}
		} else {
			snapshot.putString("PartyName", "");
			snapshot.putString("LeaderName", "");
			snapshot.putBoolean("Discoverable", true);
		}
		snapshot.put("Members", memberTags);
		snapshot.put("Requests", requestTags);
		snapshot.put("Nearby", nearbySnapshot(player, data, ownParty, now));

		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new PartyStateMessage(open, snapshot));
	}

	/** Returns online members in stable roster order, including the viewer. */
	public static List<ServerPlayer> onlineMembers(ServerPlayer viewer) {
		if (viewer == null || viewer.getServer() == null)
			return List.of();
		reconcilePlayer(viewer);
		Party party = PartySavedData.get(viewer.serverLevel()).partyForPlayer(viewer.getUUID());
		if (party == null)
			return List.of(viewer);
		List<ServerPlayer> result = new ArrayList<>();
		for (PartyMember member : party.members()) {
			ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(member.id());
			if (online != null && !online.hasDisconnected())
				result.add(online);
		}
		return List.copyOf(result);
	}

	public static boolean glowEnabled(ServerPlayer viewer) {
		return viewer != null && PartySavedData.get(viewer.serverLevel())
				.glowEnabled(viewer.getUUID());
	}

	public static int glowColor(ServerPlayer viewer) {
		return viewer == null ? DEFAULT_GLOW_COLOR : PartySavedData.get(viewer.serverLevel())
				.glowColor(viewer.getUUID());
	}

	/**
	 * Reconciles the persistent roster with the legacy player string. Existing
	 * worlds are migrated when each player next logs in.
	 */
	public static void reconcilePlayer(ServerPlayer player) {
		if (player == null || player.hasDisconnected())
			return;
		PartySavedData data = PartySavedData.get(player.serverLevel());
		Party party = data.partyForPlayer(player.getUUID());
		if (party != null) {
			data.markKnownPlayer(player.getUUID());
			PartyMember profile = memberFromPlayer(player);
			if (party.updateMember(profile))
				data.markChanged();
			mirrorLegacyParty(player, party.name());
			return;
		}

		if (data.isKnownPlayer(player.getUUID())) {
			mirrorLegacyParty(player, "");
			return;
		}

		String legacyName = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.party).orElse("");
		if (legacyName == null || legacyName.isBlank()) {
			data.markKnownPlayer(player.getUUID());
			mirrorLegacyParty(player, "");
			return;
		}

		String legacyKey = canonicalLegacyKey(legacyName);
		if (data.isRetiredLegacyKey(legacyKey)) {
			data.markKnownPlayer(player.getUUID());
			mirrorLegacyParty(player, "");
			return;
		}
		Party matching = data.partyByLegacyKey(legacyKey);
		boolean membershipChanged = false;
		if (matching == null) {
			String migratedName = uniqueMigratedName(data, legacyName, legacyKey);
			matching = data.createMigrated(migratedName, legacyKey,
					memberFromPlayer(player));
			membershipChanged = matching != null;
		} else if (!matching.full() && data.addMember(matching, memberFromPlayer(player))) {
			matching.preferLegacyLeader(player.getUUID());
			data.markChanged();
			membershipChanged = true;
		}
		data.markKnownPlayer(player.getUUID());

		if (matching != null && matching.contains(player.getUUID())) {
			mirrorLegacyParty(player, matching.name());
			if (membershipChanged)
				refreshPartyMembers(player.getServer(), matching, player.getUUID());
		} else {
			mirrorLegacyParty(player, "");
		}
	}

	/** Compatibility route for the old command procedure. */
	public static void legacyCreate(ServerPlayer player, String name) {
		create(player, name, true);
	}

	/** Compatibility route; old passwords are intentionally not retained. */
	public static void legacyRequestJoin(ServerPlayer player, String name) {
		if (player == null)
			return;
		PartySavedData data = PartySavedData.get(player.serverLevel());
		Party target = data.partyByName(sanitizePartyName(name));
		if (target == null) {
			sendSnapshot(player, true, "No discoverable party has that name.");
			return;
		}
		requestJoin(player, target.id(), true);
	}

	public static void legacyLeave(ServerPlayer player) {
		leave(player, true);
	}

	public static void legacyOpen(ServerPlayer player) {
		sendSnapshot(player, true, "");
	}

	private static void create(ServerPlayer player, String requestedName, boolean open) {
		reconcilePlayer(player);
		PartySavedData data = PartySavedData.get(player.serverLevel());
		if (data.partyForPlayer(player.getUUID()) != null) {
			sendSnapshot(player, open, "Leave your current party before creating another.");
			return;
		}
		String name = sanitizePartyName(requestedName);
		if (name.isEmpty()) {
			sendSnapshot(player, open,
					"Party names must be 3-24 letters, numbers, spaces, '-', '_' or apostrophes.");
			return;
		}
		if (data.partyByName(name) != null) {
			sendSnapshot(player, open, "That party name is already in use.");
			return;
		}
		Party party = data.create(name, memberFromPlayer(player));
		if (party == null) {
			sendSnapshot(player, open, "The party could not be created.");
			return;
		}
		data.removeRequestsForPlayer(player.getUUID());
		mirrorLegacyParty(player, party.name());
		PartyHighlightManager.syncNow(player);
		sendSnapshot(player, open, "Party created.");
	}

	private static void requestJoin(ServerPlayer player, UUID partyId, boolean open) {
		reconcilePlayer(player);
		PartySavedData data = PartySavedData.get(player.serverLevel());
		long now = System.currentTimeMillis();
		if (data.partyForPlayer(player.getUUID()) != null) {
			sendSnapshot(player, open, "Leave your current party before requesting another.");
			return;
		}
		Party party = data.party(partyId);
		if (party == null || !party.discoverable()) {
			sendSnapshot(player, open, "That party is no longer available.");
			return;
		}
		data.pruneExpiredRequests(party, now);
		if (!allowRequestMutation(player, party.id()))
			return;
		if (!isPartyNearby(player, party)) {
			sendSnapshot(player, open, "Move within 96 blocks of an online party member.");
			return;
		}
		if (party.full()) {
			sendSnapshot(player, open, "That party is full.");
			return;
		}
		if (party.request(player.getUUID()) != null) {
			sendSnapshot(player, open, "Your request is already pending.");
			return;
		}
		long requestCount = data.requestedParties(player.getUUID()).size();
		if (requestCount >= MAX_REQUESTS_PER_PLAYER) {
			sendSnapshot(player, open, "Cancel an existing request before sending another.");
			return;
		}
		if (!data.addRequest(party, new PartyJoinRequest(player.getUUID(),
				player.getGameProfile().getName(), now + REQUEST_LIFETIME_MS))) {
			sendSnapshot(player, open, "That party is not accepting more requests.");
			return;
		}
		ServerPlayer leader = player.getServer().getPlayerList().getPlayer(party.leaderId());
		if (leader != null)
			sendSnapshot(leader, false, "A new join request is waiting.");
		sendSnapshot(player, open, "Join request sent.");
	}

	private static void cancelRequest(ServerPlayer player, UUID partyId, boolean open) {
		PartySavedData data = PartySavedData.get(player.serverLevel());
		Party party = data.party(partyId);
		if (party != null && !allowRequestMutation(player, party.id()))
			return;
		if (party == null || !data.removeRequest(party, player.getUUID())) {
			sendSnapshot(player, open, "That request is no longer pending.");
			return;
		}
		ServerPlayer leader = player.getServer().getPlayerList().getPlayer(party.leaderId());
		if (leader != null)
			sendSnapshot(leader, false, "");
		sendSnapshot(player, open, "Join request cancelled.");
	}

	private static void acceptRequest(ServerPlayer leader, UUID playerId) {
		PartySavedData data = PartySavedData.get(leader.serverLevel());
		Party party = data.partyForPlayer(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			sendSnapshot(leader, false, "Only the party leader can accept requests.");
			return;
		}
		data.pruneExpiredRequests(party, System.currentTimeMillis());
		PartyJoinRequest request = party.request(playerId);
		if (request == null) {
			sendSnapshot(leader, false, "That request is no longer pending.");
			return;
		}
		if (party.full()) {
			sendSnapshot(leader, false, "The party is full.");
			return;
		}
		if (data.partyForPlayer(playerId) != null) {
			data.removeRequest(party, playerId);
			sendSnapshot(leader, false, "That player joined another party.");
			return;
		}

		ServerPlayer joining = leader.getServer().getPlayerList().getPlayer(playerId);
		PartyMember member = joining == null
				? new PartyMember(playerId, request.playerName(), 0, "Unranked")
				: memberFromPlayer(joining);
		if (!data.addMember(party, member)) {
			sendSnapshot(leader, false, "That request could not be accepted.");
			return;
		}
		party.settleLegacyLeadership();
		data.markKnownPlayer(playerId);
		data.removeRequestsForPlayer(playerId);
		data.markChanged();
		if (joining != null)
			mirrorLegacyParty(joining, party.name());
		if (joining == null)
			refreshPartyMembers(leader.getServer(), party, leader.getUUID());
		else
			refreshPartyMembers(leader.getServer(), party, leader.getUUID(), joining.getUUID());
		sendSnapshot(leader, false, request.playerName() + " joined the party.");
		if (joining != null)
			sendSnapshot(joining, false, "Your request to join " + party.name() + " was accepted.");
	}

	private static void denyRequest(ServerPlayer leader, UUID playerId) {
		PartySavedData data = PartySavedData.get(leader.serverLevel());
		Party party = data.partyForPlayer(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			sendSnapshot(leader, false, "Only the party leader can deny requests.");
			return;
		}
		PartyJoinRequest request = party.request(playerId);
		if (request == null || !data.removeRequest(party, playerId)) {
			sendSnapshot(leader, false, "That request is no longer pending.");
			return;
		}
		party.settleLegacyLeadership();
		ServerPlayer requester = leader.getServer().getPlayerList().getPlayer(playerId);
		if (requester != null)
			sendSnapshot(requester, false, "Your request to join " + party.name() + " was declined.");
		sendSnapshot(leader, false, "Join request declined.");
	}

	private static void kickMember(ServerPlayer leader, UUID playerId) {
		PartySavedData data = PartySavedData.get(leader.serverLevel());
		Party party = data.partyForPlayer(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			sendSnapshot(leader, false, "Only the party leader can remove members.");
			return;
		}
		if (playerId == null || playerId.equals(leader.getUUID()) || !party.contains(playerId)) {
			sendSnapshot(leader, false, "That member cannot be removed.");
			return;
		}
		String memberName = memberName(party, playerId);
		List<UUID> remaining = party.members().stream().map(PartyMember::id)
				.filter(id -> !id.equals(playerId)).toList();
		data.removeMember(party, playerId);
		party.settleLegacyLeadership();
		ServerPlayer removed = leader.getServer().getPlayerList().getPlayer(playerId);
		if (removed != null) {
			mirrorLegacyParty(removed, "");
			PartyHighlightManager.clearNow(removed);
			sendSnapshot(removed, false, "You were removed from " + party.name() + ".");
		}
		refreshPlayers(leader.getServer(), remaining);
		sendSnapshot(leader, false, memberName + " was removed.");
	}

	private static void transferLeader(ServerPlayer leader, UUID playerId) {
		PartySavedData data = PartySavedData.get(leader.serverLevel());
		Party party = data.partyForPlayer(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			sendSnapshot(leader, false, "Only the party leader can transfer leadership.");
			return;
		}
		ServerPlayer successor = playerId == null ? null
				: leader.getServer().getPlayerList().getPlayer(playerId);
		if (successor == null || successor.hasDisconnected()) {
			sendSnapshot(leader, false, "Leadership can only transfer to an online member.");
			return;
		}
		if (!party.transferLeader(playerId)) {
			sendSnapshot(leader, false, "Choose another current party member.");
			return;
		}
		party.settleLegacyLeadership();
		data.markChanged();
		refreshPartyMembers(leader.getServer(), party);
		sendSnapshot(leader, false, "Leadership transferred to " + memberName(party, playerId) + ".");
	}

	private static void leave(ServerPlayer player, boolean open) {
		PartySavedData data = PartySavedData.get(player.serverLevel());
		Party party = data.partyForPlayer(player.getUUID());
		if (party == null) {
			mirrorLegacyParty(player, "");
			sendSnapshot(player, open, "You are not in a party.");
			return;
		}
		boolean disbanded = party.size() == 1;
		if (party.isLeader(player.getUUID()))
			party.settleLegacyLeadership();
		UUID onlineSuccessor = party.isLeader(player.getUUID())
				? party.members().stream()
						.map(PartyMember::id)
						.filter(id -> !id.equals(player.getUUID()))
						.filter(id -> {
							ServerPlayer candidate = player.getServer().getPlayerList().getPlayer(id);
							return candidate != null && !candidate.hasDisconnected();
						})
						.findFirst().orElse(null)
				: null;
		data.removeMember(party, player.getUUID());
		if (onlineSuccessor != null && !onlineSuccessor.equals(party.leaderId()))
			party.transferLeader(onlineSuccessor);
		if (disbanded)
			data.remove(party.id());
		else
			data.markChanged();
		mirrorLegacyParty(player, "");
		PartyHighlightManager.clearNow(player);
		if (!disbanded)
			refreshPartyMembers(player.getServer(), party);
		sendSnapshot(player, open, disbanded ? "Party disbanded." : "You left the party.");
	}

	private static void disband(ServerPlayer leader) {
		PartySavedData data = PartySavedData.get(leader.serverLevel());
		Party party = data.partyForPlayer(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			sendSnapshot(leader, false, "Only the party leader can disband the party.");
			return;
		}
		List<UUID> members = party.members().stream().map(PartyMember::id).toList();
		String name = party.name();
		data.remove(party.id());
		for (UUID memberId : members) {
			ServerPlayer member = leader.getServer().getPlayerList().getPlayer(memberId);
			if (member == null)
				continue;
			mirrorLegacyParty(member, "");
			PartyHighlightManager.clearNow(member);
			sendSnapshot(member, false,
					memberId.equals(leader.getUUID()) ? "Party disbanded."
							: name + " was disbanded.");
		}
	}

	private static void toggleDiscoverable(ServerPlayer leader) {
		PartySavedData data = PartySavedData.get(leader.serverLevel());
		Party party = data.partyForPlayer(leader.getUUID());
		if (party == null || !party.isLeader(leader.getUUID())) {
			sendSnapshot(leader, false, "Only the party leader can change visibility.");
			return;
		}
		party.toggleDiscoverable();
		party.settleLegacyLeadership();
		data.markChanged();
		refreshPartyMembers(leader.getServer(), party, leader.getUUID());
		sendSnapshot(leader, false,
				party.discoverable() ? "Party is now discoverable." : "Party is now private.");
	}

	private static void setGlow(ServerPlayer player, boolean enabled, int color) {
		PartySavedData data = PartySavedData.get(player.serverLevel());
		data.setGlow(player.getUUID(), enabled, color);
		PartyHighlightManager.syncNow(player);
		sendSnapshot(player, false, "");
	}

	private static ListTag nearbySnapshot(ServerPlayer viewer, PartySavedData data,
			Party ownParty, long now) {
		Map<UUID, Double> nearestByParty = new HashMap<>();
		for (ServerPlayer online : viewer.getServer().getPlayerList().getPlayers()) {
			if (online == viewer || online.hasDisconnected()
					|| !online.level().dimension().equals(viewer.level().dimension()))
				continue;
			Party party = data.partyForPlayer(online.getUUID());
			if (party == null || party == ownParty)
				continue;
			nearestByParty.merge(party.id(), viewer.distanceToSqr(online), Math::min);
		}

		Set<UUID> candidateIds = new LinkedHashSet<>();
		for (Map.Entry<UUID, Double> entry : nearestByParty.entrySet()) {
			if (entry.getValue() <= NEARBY_RANGE_SQR)
				candidateIds.add(entry.getKey());
		}
		for (Party requested : data.requestedParties(viewer.getUUID()))
			candidateIds.add(requested.id());

		List<NearbyParty> nearby = new ArrayList<>();
		for (UUID partyId : candidateIds) {
			Party party = data.party(partyId);
			if (party == null || party == ownParty)
				continue;
			PartyJoinRequest request = party.request(viewer.getUUID());
			boolean requested = request != null && !request.expired(now);
			double nearestSqr = nearestByParty.getOrDefault(party.id(), Double.MAX_VALUE);
			boolean available = party.discoverable() && !party.full()
					&& nearestSqr <= NEARBY_RANGE_SQR;
			if (!requested && !available)
				continue;
			nearby.add(new NearbyParty(party,
					available ? Math.sqrt(nearestSqr) : -1.0D,
					requested, available));
		}
		nearby.sort((first, second) -> {
			int requestStatus = Boolean.compare(second.requested(), first.requested());
			if (requestStatus != 0)
				return requestStatus;
			int availability = Boolean.compare(second.available(), first.available());
			if (availability != 0)
				return availability;
			int distance = Double.compare(first.distance(), second.distance());
			return distance != 0 ? distance
					: first.party().name().compareToIgnoreCase(second.party().name());
		});

		ListTag tags = new ListTag();
		for (NearbyParty entry : nearby.stream().limit(MAX_NEARBY_PARTIES).toList()) {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("PartyId", entry.party().id());
			tag.putString("Name", entry.party().name());
			tag.putString("LeaderName",
					memberName(entry.party(), entry.party().leaderId()));
			tag.putInt("Members", entry.party().size());
			tag.putInt("MaxMembers", Party.MAX_MEMBERS);
			tag.putDouble("Distance", entry.distance());
			tag.putBoolean("Requested", entry.requested());
			tag.putBoolean("Available", entry.available());
			tags.add(tag);
		}
		return tags;
	}

	private static CompoundTag memberSnapshot(MinecraftServer server, Party party,
			PartyMember member) {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", member.id());
		tag.putString("Name", member.name());
		tag.putBoolean("Online", server.getPlayerList().getPlayer(member.id()) != null);
		tag.putInt("Level", member.level());
		tag.putString("Rank", member.rank());
		tag.putBoolean("Leader", party.isLeader(member.id()));
		return tag;
	}

	private static CompoundTag requestSnapshot(MinecraftServer server,
			PartyJoinRequest request) {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", request.playerId());
		tag.putString("Name", request.playerName());
		tag.putBoolean("Online",
				server.getPlayerList().getPlayer(request.playerId()) != null);
		return tag;
	}

	private static void refreshOnlineProfiles(MinecraftServer server, PartySavedData data,
			Party party) {
		if (party == null)
			return;
		boolean changed = false;
		for (PartyMember member : party.members()) {
			ServerPlayer online = server.getPlayerList().getPlayer(member.id());
			if (online != null)
				changed |= party.updateMember(memberFromPlayer(online));
		}
		if (changed)
			data.markChanged();
	}

	private static void refreshPartyMembers(MinecraftServer server, Party party,
			UUID... skipSnapshots) {
		List<UUID> skipped = new ArrayList<>();
		if (skipSnapshots != null) {
			for (UUID playerId : skipSnapshots) {
				if (playerId != null)
					skipped.add(playerId);
			}
		}
		for (PartyMember member : party.members()) {
			ServerPlayer online = server.getPlayerList().getPlayer(member.id());
			if (online == null)
				continue;
			PartyHighlightManager.syncNow(online);
			if (!skipped.contains(member.id()))
				sendSnapshot(online, false, "");
		}
	}

	private static void refreshPlayers(MinecraftServer server, List<UUID> playerIds) {
		for (UUID playerId : playerIds) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player == null)
				continue;
			PartyHighlightManager.syncNow(player);
			sendSnapshot(player, false, "");
		}
	}

	private static PartyMember memberFromPlayer(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables variables = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		int level = Math.max(0, (int) Math.floor(variables.Level));
		String rank = variables.ranking == null || variables.ranking.isBlank()
				? "Unranked" : variables.ranking;
		return new PartyMember(player.getUUID(), player.getGameProfile().getName(), level, rank);
	}

	private static void mirrorLegacyParty(ServerPlayer player, String name) {
		String value = name == null ? "" : name;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(variables -> {
					if (value.equals(variables.party))
						return;
					variables.party = value;
					variables.syncPlayerVariables(player);
				});
	}

	private static String memberName(Party party, UUID playerId) {
		PartyMember member = party == null ? null : party.member(playerId);
		return member == null ? "Unknown" : member.name();
	}

	private static UUID readUuid(CompoundTag payload, String key) {
		if (payload == null || !payload.hasUUID(key))
			return null;
		try {
			return payload.getUUID(key);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static boolean isPartyNearby(ServerPlayer viewer, Party party) {
		if (viewer == null || party == null || viewer.getServer() == null)
			return false;
		for (PartyMember member : party.members()) {
			ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(member.id());
			if (online != null && !online.hasDisconnected()
					&& online.level().dimension().equals(viewer.level().dimension())
					&& viewer.distanceToSqr(online) <= NEARBY_RANGE_SQR)
				return true;
		}
		return false;
	}

	private static boolean allowAction(ServerPlayer player) {
		long now = player.serverLevel().getGameTime();
		ArrayDeque<Long> ticks = ACTION_TICKS.computeIfAbsent(player.getUUID(),
				ignored -> new ArrayDeque<>());
		while (!ticks.isEmpty() && (now < ticks.peekFirst() || now - ticks.peekFirst() >= 20L))
			ticks.removeFirst();
		if (ticks.size() >= MAX_ACTIONS_PER_SECOND)
			return false;
		ticks.addLast(now);
		return true;
	}

	private static boolean allowRequestMutation(ServerPlayer player, UUID partyId) {
		if (partyId == null)
			return false;
		long now = player.serverLevel().getGameTime();
		RequestMutationKey key = new RequestMutationKey(player.getUUID(), partyId);
		Long previous = REQUEST_MUTATION_TICKS.get(key);
		if (previous != null && now >= previous && now - previous < 10L)
			return false;
		REQUEST_MUTATION_TICKS.put(key, now);
		return true;
	}

	static String sanitizePartyName(String input) {
		if (input == null)
			return "";
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC).trim();
		StringBuilder result = new StringBuilder();
		boolean previousSpace = false;
		boolean hasLetterOrDigit = false;
		for (int offset = 0; offset < normalized.length();) {
			int codePoint = normalized.codePointAt(offset);
			offset += Character.charCount(codePoint);
			if (Character.isWhitespace(codePoint)) {
				if (!previousSpace && !result.isEmpty()) {
					result.append(' ');
					previousSpace = true;
				}
				continue;
			}
			if (!Character.isLetterOrDigit(codePoint) && codePoint != '-'
					&& codePoint != '_' && codePoint != '\'')
				continue;
			if (result.length() + Character.charCount(codePoint) > 24)
				break;
			result.appendCodePoint(codePoint);
			hasLetterOrDigit |= Character.isLetterOrDigit(codePoint);
			previousSpace = false;
		}
		String clean = result.toString().trim();
		return hasLetterOrDigit && clean.length() >= 3 && clean.length() <= 24
				? clean : "";
	}

	private static String migrateLegacyName(String legacyName) {
		String clean = sanitizePartyName(legacyName);
		if (!clean.isEmpty())
			return clean;
		String stripped = legacyName == null ? "" : legacyName.replaceAll("[^A-Za-z0-9]", "");
		if (!stripped.isEmpty()) {
			String candidate = stripped.substring(0, Math.min(18, stripped.length())) + " Party";
			clean = sanitizePartyName(candidate);
			if (!clean.isEmpty())
				return clean;
		}
		return "Legacy " + Integer.toUnsignedString(
				legacyName == null ? 0 : legacyName.toLowerCase(Locale.ROOT).hashCode(), 36);
	}

	private static String canonicalLegacyKey(String legacyName) {
		String canonical = Normalizer.normalize(legacyName == null ? "" : legacyName,
				Normalizer.Form.NFKC).trim().toUpperCase(Locale.ROOT);
		if (canonical.length() <= 96)
			return canonical;
		return canonical.substring(0, 80) + "#"
				+ Integer.toUnsignedString(canonical.hashCode(), 36).toUpperCase(Locale.ROOT);
	}

	private static String uniqueMigratedName(PartySavedData data, String legacyName,
			String legacyKey) {
		String base = migrateLegacyName(legacyName);
		if (data.partyByName(base) == null)
			return base;
		String token = Integer.toUnsignedString(legacyKey.hashCode(), 36)
				.toUpperCase(Locale.ROOT);
		token = token.substring(Math.max(0, token.length() - 6));
		for (int attempt = 0; attempt < 1000; attempt++) {
			String suffix = "-" + token + (attempt == 0 ? "" : Integer.toString(attempt));
			int baseLength = Math.max(3, 24 - suffix.length());
			String stem = base.substring(0, Math.min(base.length(), baseLength)).trim();
			if (!stem.isEmpty() && Character.isHighSurrogate(stem.charAt(stem.length() - 1)))
				stem = stem.substring(0, stem.length() - 1);
			String candidate = sanitizePartyName(stem + suffix);
			if (!candidate.isEmpty() && data.partyByName(candidate) == null)
				return candidate;
		}
		return "Legacy-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private static String cleanNotice(String notice) {
		if (notice == null)
			return "";
		String clean = notice.replace('\n', ' ').replace('\r', ' ').trim();
		return clean.substring(0, Math.min(160, clean.length()));
	}

	private record NearbyParty(Party party, double distance, boolean requested,
			boolean available) {
	}

	private record RequestMutationKey(UUID playerId, UUID partyId) {
	}
}
