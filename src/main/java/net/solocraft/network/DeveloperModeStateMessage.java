package net.solocraft.network;

import net.solocraft.SololevelingMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Mirrors only the current player's developer-preview entitlement to their
 * client. Gameplay checks always read the server-side persisted flag.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class DeveloperModeStateMessage {
	private static volatile boolean clientEnabled;

	private final boolean enabled;

	public DeveloperModeStateMessage(boolean enabled) {
		this.enabled = enabled;
	}

	public DeveloperModeStateMessage(FriendlyByteBuf buffer) {
		this.enabled = buffer.readBoolean();
	}

	public static void buffer(DeveloperModeStateMessage message,
			FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.enabled);
	}

	public static void handler(DeveloperModeStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> clientEnabled = message.enabled);
		context.setPacketHandled(true);
	}

	public static boolean isClientEnabled() {
		return clientEnabled;
	}

	public static void sync(ServerPlayer player, boolean enabled) {
		if (player == null || player.connection == null)
			return;
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new DeveloperModeStateMessage(enabled));
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DeveloperModeStateMessage.class,
				DeveloperModeStateMessage::buffer,
				DeveloperModeStateMessage::new,
				DeveloperModeStateMessage::handler,
				NetworkDirection.PLAY_TO_CLIENT);
	}
}
