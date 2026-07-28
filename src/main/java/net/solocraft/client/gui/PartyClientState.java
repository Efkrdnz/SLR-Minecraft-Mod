package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.PartyScreen;
import net.solocraft.network.PartyActionMessage;
import net.solocraft.network.PartyRequestMessage;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client mirror of the server-authoritative party snapshot.
 *
 * <p>The screen reads immutable records from here. Incoming snapshots rebuild
 * only its widgets, leaving the open animation and current screen intact.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, value = Dist.CLIENT)
public final class PartyClientState {
	private static Snapshot snapshot = Snapshot.empty();
	private static boolean received;

	private PartyClientState() {
	}

	public static Snapshot snapshot() {
		return snapshot;
	}

	public static boolean hasSnapshot() {
		return received;
	}

	public static void requestSnapshot() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getConnection() == null)
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(new PartyRequestMessage(false));
	}

	public static void refresh() {
		requestSnapshot();
	}

	public static void sendAction(String action) {
		sendAction(action, new CompoundTag());
	}

	public static void sendAction(String action, CompoundTag payload) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.getConnection() == null)
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(
				new PartyActionMessage(action == null ? "" : action,
						payload == null ? new CompoundTag() : payload));
	}

	public static void applySnapshot(boolean open, CompoundTag data) {
		Minecraft minecraft = Minecraft.getInstance();
		Runnable update = () -> {
			snapshot = Snapshot.from(data == null ? new CompoundTag() : data.copy());
			received = true;
			if (minecraft.screen instanceof PartyScreen partyScreen)
				partyScreen.onPartyStateChanged(snapshot);
			else if (open && minecraft.player != null)
				minecraft.setScreen(new PartyScreen(
						!SystemPlayerAccess.hasSystem(minecraft.player)));
		};
		if (minecraft.isSameThread())
			update.run();
		else
			minecraft.execute(update);
	}

	public static void clear() {
		snapshot = Snapshot.empty();
		received = false;
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		clear();
	}

	public record Snapshot(
			boolean inParty,
			UUID partyId,
			String partyName,
			UUID leaderId,
			String leaderName,
			boolean discoverable,
			int maxMembers,
			boolean glowEnabled,
			int glowColor,
			String notice,
			List<Member> members,
			List<NearbyParty> nearby,
			List<JoinRequest> requests) {

		private static Snapshot empty() {
			return new Snapshot(false, null, "", null, "", true, 8, true,
					0x55D8FF, "", List.of(), List.of(), List.of());
		}

		private static Snapshot from(CompoundTag tag) {
			List<Member> members = new ArrayList<>();
			ListTag memberTags = tag.getList("Members", Tag.TAG_COMPOUND);
			for (int index = 0; index < memberTags.size(); index++) {
				CompoundTag member = memberTags.getCompound(index);
				members.add(new Member(
						readUuid(member, "Id"),
						clean(member.getString("Name"), 32),
						member.getBoolean("Online"),
						Math.max(0, member.getInt("Level")),
						clean(member.getString("Rank"), 16),
						member.getBoolean("Leader")));
			}

			List<NearbyParty> nearby = new ArrayList<>();
			ListTag nearbyTags = tag.getList("Nearby", Tag.TAG_COMPOUND);
			for (int index = 0; index < nearbyTags.size(); index++) {
				CompoundTag party = nearbyTags.getCompound(index);
				nearby.add(new NearbyParty(
						readUuid(party, "PartyId"),
						clean(party.getString("Name"), 32),
						clean(party.getString("LeaderName"), 32),
						Math.max(0, party.getInt("Members")),
						Math.max(1, party.getInt("MaxMembers")),
						Math.max(0, party.getInt("Distance")),
						party.getBoolean("Available"),
						party.getBoolean("Requested")));
			}

			List<JoinRequest> requests = new ArrayList<>();
			ListTag requestTags = tag.getList("Requests", Tag.TAG_COMPOUND);
			for (int index = 0; index < requestTags.size(); index++) {
				CompoundTag request = requestTags.getCompound(index);
				requests.add(new JoinRequest(
						readUuid(request, "Id"),
						clean(request.getString("Name"), 32),
						request.getBoolean("Online")));
			}

			return new Snapshot(
					tag.getBoolean("InParty"),
					readUuid(tag, "PartyId"),
					clean(tag.getString("PartyName"), 40),
					readUuid(tag, "LeaderId"),
					clean(tag.getString("LeaderName"), 32),
					tag.getBoolean("Discoverable"),
					Math.max(1, tag.getInt("MaxMembers")),
					tag.getBoolean("GlowEnabled"),
					tag.getInt("GlowColor") & 0xFFFFFF,
					clean(tag.getString("Notice"), 120),
					List.copyOf(members),
					List.copyOf(nearby),
					List.copyOf(requests));
		}
	}

	public record Member(UUID id, String name, boolean online, int level, String rank,
			boolean leader) {
	}

	public record NearbyParty(UUID id, String name, String leaderName, int members,
			int maxMembers, int distance, boolean available, boolean requested) {
	}

	public record JoinRequest(UUID id, String name, boolean online) {
	}

	private static UUID readUuid(CompoundTag tag, String key) {
		return tag.hasUUID(key) ? tag.getUUID(key) : null;
	}

	private static String clean(String value, int maximumLength) {
		if (value == null || value.isEmpty())
			return "";
		StringBuilder cleaned = new StringBuilder(Math.min(value.length(), maximumLength));
		for (int index = 0; index < value.length() && cleaned.length() < maximumLength; index++) {
			char character = value.charAt(index);
			if (character >= 32 && character != 127 && character != '\u00A7')
				cleaned.append(character);
		}
		return cleaned.toString();
	}
}
