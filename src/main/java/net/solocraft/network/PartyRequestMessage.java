package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.party.PartyService;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Requests the sender's current party interface snapshot. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class PartyRequestMessage {
	private final boolean open;

	public PartyRequestMessage(boolean open) {
		this.open = open;
	}

	public PartyRequestMessage(FriendlyByteBuf buffer) {
		this.open = buffer.readBoolean();
	}

	public static void buffer(PartyRequestMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
	}

	public static void handler(PartyRequestMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				PartyService.requestSnapshot(player, message.open);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PartyRequestMessage.class,
				PartyRequestMessage::buffer, PartyRequestMessage::new,
				PartyRequestMessage::handler, NetworkDirection.PLAY_TO_SERVER);
	}
}
