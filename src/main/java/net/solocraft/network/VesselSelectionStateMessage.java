package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.VesselSelectionScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class VesselSelectionStateMessage {
	private final boolean open;
	private final int advancementPoints;
	private final int requiredPoints;
	private final int vesselLimit;
	private final int[] claimCounts;
	private final boolean developerMode;

	public VesselSelectionStateMessage(boolean open, int advancementPoints,
			int requiredPoints, int vesselLimit, int[] claimCounts,
			boolean developerMode) {
		this.open = open;
		this.advancementPoints = advancementPoints;
		this.requiredPoints = requiredPoints;
		this.vesselLimit = vesselLimit;
		this.claimCounts = claimCounts == null ? new int[0] : claimCounts.clone();
		this.developerMode = developerMode;
	}

	public VesselSelectionStateMessage(FriendlyByteBuf buffer) {
		this.open = buffer.readBoolean();
		this.advancementPoints = buffer.readVarInt();
		this.requiredPoints = buffer.readVarInt();
		this.vesselLimit = buffer.readInt();
		int size = Math.min(64, buffer.readVarInt());
		this.claimCounts = new int[size];
		for (int i = 0; i < size; i++)
			this.claimCounts[i] = buffer.readVarInt();
		this.developerMode = buffer.readBoolean();
	}

	public static void buffer(VesselSelectionStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
		buffer.writeVarInt(message.advancementPoints);
		buffer.writeVarInt(message.requiredPoints);
		buffer.writeInt(message.vesselLimit);
		buffer.writeVarInt(message.claimCounts.length);
		for (int count : message.claimCounts)
			buffer.writeVarInt(count);
		buffer.writeBoolean(message.developerMode);
	}

	public static void handler(VesselSelectionStateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				VesselSelectionScreen.handleServerState(message.open, message.advancementPoints,
						message.requiredPoints, message.vesselLimit,
						message.claimCounts, message.developerMode)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(VesselSelectionStateMessage.class, VesselSelectionStateMessage::buffer,
				VesselSelectionStateMessage::new, VesselSelectionStateMessage::handler);
	}
}
