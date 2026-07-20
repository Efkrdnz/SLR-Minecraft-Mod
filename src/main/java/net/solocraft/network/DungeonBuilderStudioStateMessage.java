package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.dungeonbuilder.DungeonBuilderStudioClient;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Full Studio metadata snapshot. Structure NBT is never included. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
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
