package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.party.PartyService;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.Supplier;

/** One party action whose actor is always resolved from the network sender. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PartyActionMessage {
	private static final int MAX_ACTION_LENGTH = 32;

	private final String action;
	private final CompoundTag payload;

	public PartyActionMessage(String action, CompoundTag payload) {
		String safeAction = action == null ? "" : action;
		this.action = safeAction.substring(0, Math.min(MAX_ACTION_LENGTH, safeAction.length()));
		this.payload = boundedPayload(payload);
	}

	public PartyActionMessage(FriendlyByteBuf buffer) {
		this.action = buffer.readUtf(MAX_ACTION_LENGTH);
		CompoundTag read = new CompoundTag();
		read.putString("Name", buffer.readUtf(24));
		readUuid(buffer, read, "PartyId");
		readUuid(buffer, read, "PlayerId");
		read.putBoolean("Enabled", buffer.readBoolean());
		read.putInt("Color", buffer.readInt() & 0xFFFFFF);
		this.payload = read;
	}

	public static void buffer(PartyActionMessage message, FriendlyByteBuf buffer) {
		buffer.writeUtf(message.action, MAX_ACTION_LENGTH);
		buffer.writeUtf(message.payload.getString("Name"), 24);
		writeUuid(buffer, message.payload, "PartyId");
		writeUuid(buffer, message.payload, "PlayerId");
		buffer.writeBoolean(message.payload.getBoolean("Enabled"));
		buffer.writeInt(message.payload.getInt("Color") & 0xFFFFFF);
	}

	public static void handler(PartyActionMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				PartyService.handleAction(player, message.action, message.payload);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PartyActionMessage.class,
				PartyActionMessage::buffer, PartyActionMessage::new,
				PartyActionMessage::handler, NetworkDirection.PLAY_TO_SERVER);
	}

	private static CompoundTag boundedPayload(CompoundTag source) {
		CompoundTag bounded = new CompoundTag();
		if (source == null)
			return bounded;
		String name = source.getString("Name");
		bounded.putString("Name", name.substring(0, Math.min(24, name.length())));
		copyUuid(source, bounded, "PartyId");
		copyUuid(source, bounded, "PlayerId");
		bounded.putBoolean("Enabled", source.getBoolean("Enabled"));
		bounded.putInt("Color", source.getInt("Color") & 0xFFFFFF);
		return bounded;
	}

	private static void copyUuid(CompoundTag source, CompoundTag target, String key) {
		if (source.hasUUID(key))
			target.putUUID(key, source.getUUID(key));
	}

	private static void writeUuid(FriendlyByteBuf buffer, CompoundTag source, String key) {
		boolean present = source.hasUUID(key);
		buffer.writeBoolean(present);
		if (present)
			buffer.writeUUID(source.getUUID(key));
	}

	private static void readUuid(FriendlyByteBuf buffer, CompoundTag target, String key) {
		if (buffer.readBoolean()) {
			UUID value = buffer.readUUID();
			target.putUUID(key, value);
		}
	}
}
