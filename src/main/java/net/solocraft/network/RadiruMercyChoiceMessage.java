package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcRadiruManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Client response to Esil's server-authorized Floor 15 mercy prompt. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RadiruMercyChoiceMessage {
	private final boolean spare;

	public RadiruMercyChoiceMessage(boolean spare) {
		this.spare = spare;
	}

	public RadiruMercyChoiceMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean());
	}

	public static void buffer(RadiruMercyChoiceMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.spare);
	}

	public static void handler(RadiruMercyChoiceMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				DkcRadiruManager.resolveMercyChoice(player, message.spare);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(RadiruMercyChoiceMessage.class,
				RadiruMercyChoiceMessage::buffer, RadiruMercyChoiceMessage::new,
				RadiruMercyChoiceMessage::handler);
	}
}
