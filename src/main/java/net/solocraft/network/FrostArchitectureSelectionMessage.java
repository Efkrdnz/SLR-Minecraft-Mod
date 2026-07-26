package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.FrostArchitectureManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FrostArchitectureSelectionMessage {
	private final int blueprintId;

	public FrostArchitectureSelectionMessage(int blueprintId) {
		this.blueprintId = blueprintId;
	}

	public FrostArchitectureSelectionMessage(FriendlyByteBuf buffer) {
		this.blueprintId = buffer.readVarInt();
	}

	public static void buffer(FrostArchitectureSelectionMessage message, FriendlyByteBuf buffer) {
		buffer.writeVarInt(message.blueprintId);
	}

	public static void handler(FrostArchitectureSelectionMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null && message.blueprintId >= 0 && message.blueprintId < 6)
				FrostArchitectureManager.castSelection(player, message.blueprintId);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(FrostArchitectureSelectionMessage.class,
				FrostArchitectureSelectionMessage::buffer,
				FrostArchitectureSelectionMessage::new,
				FrostArchitectureSelectionMessage::handler,
				NetworkDirection.PLAY_TO_SERVER);
	}
}
