package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VesselSelectionMessage {
	private final String type;
	private final String identity;

	public VesselSelectionMessage(String type, String identity) {
		this.type = type == null ? "" : type;
		this.identity = identity == null ? "" : identity;
	}

	public VesselSelectionMessage(FriendlyByteBuf buffer) {
		this(buffer.readUtf(16), buffer.readUtf(48));
	}

	public static void buffer(VesselSelectionMessage message, FriendlyByteBuf buffer) {
		buffer.writeUtf(message.type, 16);
		buffer.writeUtf(message.identity, 48);
	}

	public static void handler(VesselSelectionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				JobChangeQuestManager.selectVessel(player, message.type, message.identity);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(VesselSelectionMessage.class, VesselSelectionMessage::buffer,
				VesselSelectionMessage::new, VesselSelectionMessage::handler);
	}
}
