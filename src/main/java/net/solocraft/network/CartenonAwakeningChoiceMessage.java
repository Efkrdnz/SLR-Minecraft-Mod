package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.CartenonTempleManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class CartenonAwakeningChoiceMessage {
	private final boolean accept;

	public CartenonAwakeningChoiceMessage(boolean accept) {
		this.accept = accept;
	}

	public CartenonAwakeningChoiceMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean());
	}

	public static void buffer(CartenonAwakeningChoiceMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.accept);
	}

	public static void handler(CartenonAwakeningChoiceMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				CartenonTempleManager.resolveAwakeningChoice(player, message.accept);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(CartenonAwakeningChoiceMessage.class,
				CartenonAwakeningChoiceMessage::buffer, CartenonAwakeningChoiceMessage::new,
				CartenonAwakeningChoiceMessage::handler);
	}
}
