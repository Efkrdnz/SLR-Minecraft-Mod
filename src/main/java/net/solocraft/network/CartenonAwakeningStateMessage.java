package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.CartenonAwakeningScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CartenonAwakeningStateMessage {
	private final boolean open;

	public CartenonAwakeningStateMessage(boolean open) {
		this.open = open;
	}

	public CartenonAwakeningStateMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean());
	}

	public static void buffer(CartenonAwakeningStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
	}

	public static void handler(CartenonAwakeningStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> CartenonAwakeningScreen.handleServerState(message.open)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(CartenonAwakeningStateMessage.class,
				CartenonAwakeningStateMessage::buffer, CartenonAwakeningStateMessage::new,
				CartenonAwakeningStateMessage::handler);
	}
}
