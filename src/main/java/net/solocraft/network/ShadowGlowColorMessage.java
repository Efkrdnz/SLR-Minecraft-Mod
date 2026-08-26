package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.ShadowGlowManager;
import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Client request to set or clear a shadow type's outline colour.
 *
 * <p>The client sends only a type and a colour; the server re-validates the type
 * against the roster and stores it on the player, so nothing here can be used to
 * write arbitrary persistent data.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ShadowGlowColorMessage {
	private final String shadowType;
	private final int color;

	public ShadowGlowColorMessage(FriendlyByteBuf buffer) {
		this.shadowType = buffer.readUtf(24);
		this.color = buffer.readInt();
	}

	public ShadowGlowColorMessage(String shadowType, int color) {
		this.shadowType = shadowType == null ? "" : shadowType;
		this.color = color;
	}

	public static void buffer(ShadowGlowColorMessage message, FriendlyByteBuf buffer) {
		buffer.writeUtf(message.shadowType, 24);
		buffer.writeInt(message.color);
	}

	public static void handler(ShadowGlowColorMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null)
				return;
			if (!ShadowMonarchManager.customizableTypes().contains(message.shadowType))
				return;
			int color = message.color == ShadowMonarchManager.NO_GLOW
					? ShadowMonarchManager.NO_GLOW : message.color & 0xFFFFFF;
			ShadowMonarchManager.setGlowColor(player, message.shadowType, color);
			ShadowGlowManager.syncNow(player);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowGlowColorMessage.class,
				ShadowGlowColorMessage::buffer, ShadowGlowColorMessage::new,
				ShadowGlowColorMessage::handler);
	}
}
