package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.TitleManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TitleSelectionMessage {
	private final int titleId;

	public TitleSelectionMessage(int titleId) {
		this.titleId = titleId;
	}

	public TitleSelectionMessage(FriendlyByteBuf buffer) {
		this.titleId = buffer.readInt();
	}

	public static void buffer(TitleSelectionMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.titleId);
	}

	public static void handler(TitleSelectionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null) {
				TitleManager.selectTitle(player, message.titleId);
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(TitleSelectionMessage.class, TitleSelectionMessage::buffer, TitleSelectionMessage::new, TitleSelectionMessage::handler);
	}
}
