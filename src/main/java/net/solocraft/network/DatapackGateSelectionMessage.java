package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.DatapackGateSelectionService;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.Supplier;

/** One bounded selection submitted by the datapack-gate screen. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class DatapackGateSelectionMessage {
	private static final int MAX_DUNGEON_ID_LENGTH = 192;
	private static final int MAX_RANK_LENGTH = 8;

	private final UUID gateId;
	private final long expectedRevision;
	private final String dungeonId;
	private final String rank;

	public DatapackGateSelectionMessage(UUID gateId, long expectedRevision,
			String dungeonId, String rank) {
		this.gateId = gateId == null ? new UUID(0L, 0L) : gateId;
		this.expectedRevision = Math.max(0L, expectedRevision);
		this.dungeonId = clean(dungeonId, MAX_DUNGEON_ID_LENGTH);
		this.rank = clean(rank, MAX_RANK_LENGTH);
	}

	public DatapackGateSelectionMessage(FriendlyByteBuf buffer) {
		this(buffer.readUUID(), buffer.readLong(), buffer.readUtf(MAX_DUNGEON_ID_LENGTH),
				buffer.readUtf(MAX_RANK_LENGTH));
	}

	public static void buffer(DatapackGateSelectionMessage message, FriendlyByteBuf buffer) {
		buffer.writeUUID(message.gateId);
		buffer.writeLong(message.expectedRevision);
		buffer.writeUtf(message.dungeonId, MAX_DUNGEON_ID_LENGTH);
		buffer.writeUtf(message.rank, MAX_RANK_LENGTH);
	}

	public static void handler(DatapackGateSelectionMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				DatapackGateSelectionService.select(player, message.gateId,
						message.expectedRevision, message.dungeonId, message.rank);
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DatapackGateSelectionMessage.class,
				DatapackGateSelectionMessage::buffer, DatapackGateSelectionMessage::new,
				DatapackGateSelectionMessage::handler, NetworkDirection.PLAY_TO_SERVER);
	}

	private static String clean(String value, int maximum) {
		if (value == null)
			return "";
		String clean = value.replace('\u0000', ' ').trim();
		return clean.length() <= maximum ? clean : clean.substring(0, maximum);
	}
}
