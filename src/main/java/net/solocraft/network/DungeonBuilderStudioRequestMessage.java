package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.builder.DungeonBuilderStudioService;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Requests an authenticated, server-authoritative Studio workspace snapshot. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DungeonBuilderStudioRequestMessage {
	public DungeonBuilderStudioRequestMessage() {
	}

	public DungeonBuilderStudioRequestMessage(FriendlyByteBuf ignored) {
	}

	public static void buffer(DungeonBuilderStudioRequestMessage message, FriendlyByteBuf buffer) {
	}

	public static void handler(DungeonBuilderStudioRequestMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				DungeonBuilderStudioService.requestOpen(player);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DungeonBuilderStudioRequestMessage.class,
				DungeonBuilderStudioRequestMessage::buffer, DungeonBuilderStudioRequestMessage::new,
				DungeonBuilderStudioRequestMessage::handler);
	}
}
