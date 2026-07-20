package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.VesselSelectionScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VesselSelectionStateMessage {
	private final boolean open;
	private final int advancementPoints;
	private final int requiredPoints;
	private final int vesselLimit;
	private final int[] claimCounts;

	public VesselSelectionStateMessage(boolean open, int advancementPoints, int requiredPoints, int vesselLimit, int[] claimCounts) {
		this.open = open;
		this.advancementPoints = advancementPoints;
		this.requiredPoints = requiredPoints;
		this.vesselLimit = vesselLimit;
		this.claimCounts = claimCounts == null ? new int[0] : claimCounts.clone();
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
	}

	public static void buffer(VesselSelectionStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
		buffer.writeVarInt(message.advancementPoints);
		buffer.writeVarInt(message.requiredPoints);
		buffer.writeInt(message.vesselLimit);
		buffer.writeVarInt(message.claimCounts.length);
		for (int count : message.claimCounts)
			buffer.writeVarInt(count);
	}

	public static void handler(VesselSelectionStateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				VesselSelectionScreen.handleServerState(message.open, message.advancementPoints,
						message.requiredPoints, message.vesselLimit, message.claimCounts)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(VesselSelectionStateMessage.class, VesselSelectionStateMessage::buffer,
				VesselSelectionStateMessage::new, VesselSelectionStateMessage::handler);
	}
}
