package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.PartyClientState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Server-filtered party interface state for one viewer. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class PartyStateMessage {
	private final boolean open;
	private final CompoundTag snapshot;

	public PartyStateMessage(boolean open, CompoundTag snapshot) {
		this.open = open;
		this.snapshot = snapshot == null ? new CompoundTag() : snapshot.copy();
	}

	public PartyStateMessage(FriendlyByteBuf buffer) {
		this.open = buffer.readBoolean();
		CompoundTag read = buffer.readNbt();
		this.snapshot = read == null ? new CompoundTag() : read;
	}

	public static void buffer(PartyStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
		buffer.writeNbt(message.snapshot);
	}

	public static void handler(PartyStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				PartyClientState.applySnapshot(message.open, message.snapshot)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PartyStateMessage.class,
				PartyStateMessage::buffer, PartyStateMessage::new,
				PartyStateMessage::handler, NetworkDirection.PLAY_TO_CLIENT);
	}
}
