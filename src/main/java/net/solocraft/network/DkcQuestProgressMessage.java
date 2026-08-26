package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.DkcQuestProgressClientState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Small S2C snapshot used only while the player is holding the quest-info key. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class DkcQuestProgressMessage {
	private final boolean active;
	private final int floor;
	private final int cleared;
	private final String floorName;
	private final String phase;
	private final String objective;
	private final String detail;
	private final int progress;
	private final int target;

	public DkcQuestProgressMessage(boolean active, int floor, int cleared, String floorName,
			String phase, String objective, String detail, int progress, int target) {
		this.active = active;
		this.floor = floor;
		this.cleared = cleared;
		this.floorName = floorName;
		this.phase = phase;
		this.objective = objective;
		this.detail = detail;
		this.progress = progress;
		this.target = target;
	}

	public DkcQuestProgressMessage(FriendlyByteBuf buffer) {
		active = buffer.readBoolean();
		floor = buffer.readVarInt();
		cleared = buffer.readVarInt();
		floorName = buffer.readUtf(64);
		phase = buffer.readUtf(24);
		objective = buffer.readUtf(256);
		detail = buffer.readUtf(256);
		progress = buffer.readVarInt();
		target = buffer.readVarInt();
	}

	public static void buffer(DkcQuestProgressMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.active);
		buffer.writeVarInt(message.floor);
		buffer.writeVarInt(message.cleared);
		buffer.writeUtf(message.floorName, 64);
		buffer.writeUtf(message.phase, 24);
		buffer.writeUtf(message.objective, 256);
		buffer.writeUtf(message.detail, 256);
		buffer.writeVarInt(message.progress);
		buffer.writeVarInt(message.target);
	}

	public static void handler(DkcQuestProgressMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DkcQuestProgressClientState.update(message.active,
				message.floor, message.cleared, message.floorName, message.phase,
				message.objective, message.detail, message.progress, message.target));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DkcQuestProgressMessage.class,
				DkcQuestProgressMessage::buffer, DkcQuestProgressMessage::new,
				DkcQuestProgressMessage::handler, NetworkDirection.PLAY_TO_CLIENT);
	}
}
