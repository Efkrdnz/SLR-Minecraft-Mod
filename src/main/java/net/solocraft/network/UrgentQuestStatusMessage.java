package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.UrgentQuestClientState;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class UrgentQuestStatusMessage {
	private final boolean active;
	private final String title;
	private final String objective;
	private final String kind;
	private final int progress;
	private final int target;
	private final int remainingSeconds;

	public UrgentQuestStatusMessage(boolean active, String title, String objective, String kind, int progress, int target, int remainingSeconds) {
		this.active = active;
		this.title = title;
		this.objective = objective;
		this.kind = kind;
		this.progress = progress;
		this.target = target;
		this.remainingSeconds = remainingSeconds;
	}

	public UrgentQuestStatusMessage(FriendlyByteBuf buffer) {
		this.active = buffer.readBoolean();
		this.title = buffer.readUtf(128);
		this.objective = buffer.readUtf(256);
		this.kind = buffer.readUtf(32);
		this.progress = buffer.readVarInt();
		this.target = buffer.readVarInt();
		this.remainingSeconds = buffer.readInt();
	}

	public static void buffer(UrgentQuestStatusMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.active);
		buffer.writeUtf(message.title, 128);
		buffer.writeUtf(message.objective, 256);
		buffer.writeUtf(message.kind, 32);
		buffer.writeVarInt(message.progress);
		buffer.writeVarInt(message.target);
		buffer.writeInt(message.remainingSeconds);
	}

	public static void handler(UrgentQuestStatusMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			if (!context.getDirection().getReceptionSide().isServer())
				UrgentQuestClientState.update(message.active, message.title, message.objective, message.kind, message.progress, message.target, message.remainingSeconds);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UrgentQuestStatusMessage.class, UrgentQuestStatusMessage::buffer, UrgentQuestStatusMessage::new, UrgentQuestStatusMessage::handler);
	}
}
