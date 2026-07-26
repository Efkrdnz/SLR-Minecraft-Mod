package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.RadiruMercyChoiceScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Opens or closes Esil's Floor 15 mercy decision on the client. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
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
