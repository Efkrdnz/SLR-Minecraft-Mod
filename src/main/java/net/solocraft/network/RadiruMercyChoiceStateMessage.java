package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.RadiruMercyChoiceScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Opens or closes Esil's Floor 15 mercy decision on the client. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class RadiruMercyChoiceStateMessage {
	private final boolean open;

	public RadiruMercyChoiceStateMessage(boolean open) {
		this.open = open;
	}

	public RadiruMercyChoiceStateMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean());
	}

	public static void buffer(RadiruMercyChoiceStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
	}

	public static void handler(RadiruMercyChoiceStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> RadiruMercyChoiceScreen.handleServerState(message.open)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(RadiruMercyChoiceStateMessage.class,
				RadiruMercyChoiceStateMessage::buffer, RadiruMercyChoiceStateMessage::new,
				RadiruMercyChoiceStateMessage::handler);
	}
}
