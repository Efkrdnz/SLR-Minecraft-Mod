package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Full Studio metadata snapshot. Structure NBT is never included. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class DungeonBuilderStudioStateMessage {
	private final boolean open;
	private final CompoundTag state;

	public DungeonBuilderStudioStateMessage(boolean open, CompoundTag state) {
		this.open = open;
		this.state = state == null ? new CompoundTag() : state.copy();
	}

	public DungeonBuilderStudioStateMessage(FriendlyByteBuf buffer) {
		this.open = buffer.readBoolean();
		CompoundTag read = buffer.readNbt();
		this.state = read == null ? new CompoundTag() : read;
	}

	public static void buffer(DungeonBuilderStudioStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
		buffer.writeNbt(message.state);
	}

	public static void handler(DungeonBuilderStudioStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				DungeonBuilderStudioClient.handleState(message.open, message.state)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DungeonBuilderStudioStateMessage.class,
				DungeonBuilderStudioStateMessage::buffer, DungeonBuilderStudioStateMessage::new,
				DungeonBuilderStudioStateMessage::handler);
	}
}
