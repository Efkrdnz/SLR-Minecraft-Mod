package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.DatapackGateSelectionScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-filtered choices and revision for one configurable datapack gate. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class DatapackGateSelectionStateMessage {
	public static final int MAX_OPTIONS = 256;
	private static final int MAX_DUNGEON_ID_LENGTH = 192;
	private static final int MAX_KIND_LENGTH = 16;
	private static final int MAX_NOTICE_LENGTH = 192;
	private static final int MAX_RANKS = 6;
	private static final int MAX_RANK_LENGTH = 8;

	private final boolean open;
	private final UUID gateId;
	private final long revision;
	private final List<Option> options;
	private final String notice;

	public DatapackGateSelectionStateMessage(boolean open, UUID gateId, long revision,
			List<Option> options, String notice) {
		this.open = open;
		this.gateId = gateId == null ? new UUID(0L, 0L) : gateId;
		this.revision = Math.max(0L, revision);
		this.options = options == null ? List.of()
				: options.stream().filter(java.util.Objects::nonNull).limit(MAX_OPTIONS).toList();
		this.notice = clean(notice, MAX_NOTICE_LENGTH);
	}

	public DatapackGateSelectionStateMessage(FriendlyByteBuf buffer) {
		this.open = buffer.readBoolean();
		this.gateId = buffer.readUUID();
		this.revision = Math.max(0L, buffer.readLong());
		int size = readCount(buffer, MAX_OPTIONS, "datapack dungeon options");
		List<Option> decoded = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			String dungeonId = buffer.readUtf(MAX_DUNGEON_ID_LENGTH);
			String kind = buffer.readUtf(MAX_KIND_LENGTH);
			int minRooms = buffer.readVarInt();
			int maxRooms = buffer.readVarInt();
			int rankCount = readCount(buffer, MAX_RANKS, "datapack dungeon ranks");
			List<String> ranks = new ArrayList<>(rankCount);
			for (int rankIndex = 0; rankIndex < rankCount; rankIndex++)
				ranks.add(buffer.readUtf(MAX_RANK_LENGTH));
			decoded.add(new Option(dungeonId, kind, minRooms, maxRooms, ranks));
		}
		this.options = List.copyOf(decoded);
		this.notice = buffer.readUtf(MAX_NOTICE_LENGTH);
	}

	public static void buffer(DatapackGateSelectionStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
		buffer.writeUUID(message.gateId);
		buffer.writeLong(message.revision);
		buffer.writeVarInt(message.options.size());
		for (Option option : message.options) {
			buffer.writeUtf(option.dungeonId(), MAX_DUNGEON_ID_LENGTH);
			buffer.writeUtf(option.kind(), MAX_KIND_LENGTH);
			buffer.writeVarInt(option.minRooms());
			buffer.writeVarInt(option.maxRooms());
			buffer.writeVarInt(option.ranks().size());
			for (String rank : option.ranks())
				buffer.writeUtf(rank, MAX_RANK_LENGTH);
		}
		buffer.writeUtf(message.notice, MAX_NOTICE_LENGTH);
	}

	public static void handler(DatapackGateSelectionStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				DatapackGateSelectionScreen.handleServerState(message.open, message.gateId,
						message.revision, message.options, message.notice)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DatapackGateSelectionStateMessage.class,
				DatapackGateSelectionStateMessage::buffer, DatapackGateSelectionStateMessage::new,
				DatapackGateSelectionStateMessage::handler, NetworkDirection.PLAY_TO_CLIENT);
	}

	public record Option(String dungeonId, String kind, int minRooms, int maxRooms,
			List<String> ranks) {
		public Option {
			dungeonId = clean(dungeonId, MAX_DUNGEON_ID_LENGTH);
			kind = clean(kind, MAX_KIND_LENGTH).toUpperCase(Locale.ROOT);
			minRooms = Math.max(1, Math.min(64, minRooms));
			maxRooms = Math.max(minRooms, Math.min(64, maxRooms));
			Set<String> cleanRanks = new LinkedHashSet<>();
			if (ranks != null) {
				for (String rank : ranks) {
					if (cleanRanks.size() >= MAX_RANKS)
						break;
					String value = clean(rank, MAX_RANK_LENGTH).toUpperCase(Locale.ROOT);
					if (!value.isBlank())
						cleanRanks.add(value);
				}
			}
			ranks = List.copyOf(cleanRanks);
		}
	}

	private static int readCount(FriendlyByteBuf buffer, int maximum, String label) {
		int value = buffer.readVarInt();
		if (value < 0 || value > maximum)
			throw new IllegalArgumentException("Invalid " + label + " count " + value);
		return value;
	}

	private static String clean(String value, int maximum) {
		if (value == null)
			return "";
		String clean = value.replace('\u0000', ' ').trim();
		return clean.length() <= maximum ? clean : clean.substring(0, maximum);
	}
}
