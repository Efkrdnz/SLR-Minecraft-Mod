package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.BeastHuntClientState;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BeastHuntStatusMessage {
	private final boolean active;
	private final String quarryName;
	private final int hunt;
	private final int openings;
	private final int combo;
	private final boolean stance;

	public BeastHuntStatusMessage(boolean active, String quarryName, int hunt, int openings,
			int combo, boolean stance) {
		this.active = active;
		this.quarryName = quarryName == null ? "" : quarryName;
		this.hunt = hunt;
		this.openings = openings;
		this.combo = combo;
		this.stance = stance;
	}

	public BeastHuntStatusMessage(FriendlyByteBuf buffer) {
		this.active = buffer.readBoolean();
		this.quarryName = buffer.readUtf(96);
		this.hunt = buffer.readVarInt();
		this.openings = buffer.readVarInt();
		this.combo = buffer.readVarInt();
		this.stance = buffer.readBoolean();
	}

	public static void buffer(BeastHuntStatusMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.active);
		buffer.writeUtf(message.quarryName, 96);
		buffer.writeVarInt(message.hunt);
		buffer.writeVarInt(message.openings);
		buffer.writeVarInt(message.combo);
		buffer.writeBoolean(message.stance);
	}

	public static void handler(BeastHuntStatusMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			if (!context.getDirection().getReceptionSide().isServer())
				BeastHuntClientState.update(message.active, message.quarryName, message.hunt,
						message.openings, message.combo, message.stance);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(BeastHuntStatusMessage.class, BeastHuntStatusMessage::buffer,
				BeastHuntStatusMessage::new, BeastHuntStatusMessage::handler);
	}
}
