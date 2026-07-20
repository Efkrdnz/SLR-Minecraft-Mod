package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.builder.DungeonBuilderStudioService;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** A bounded typed Studio edit; the server resolves all targets from the sender. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DungeonBuilderStudioActionMessage {
	private final long expectedRevision;
	private final String action;
	private final CompoundTag payload;

	public DungeonBuilderStudioActionMessage(long expectedRevision, String action, CompoundTag payload) {
		this.expectedRevision = Math.max(0L, expectedRevision);
		this.action = action == null ? "" : action.substring(0, Math.min(40, action.length()));
		this.payload = payload == null ? new CompoundTag() : payload.copy();
	}

	public DungeonBuilderStudioActionMessage(FriendlyByteBuf buffer) {
		this.expectedRevision = Math.max(0L, buffer.readLong());
		this.action = buffer.readUtf(40);
		CompoundTag read = buffer.readNbt();
		this.payload = read == null ? new CompoundTag() : read;
	}

	public static void buffer(DungeonBuilderStudioActionMessage message, FriendlyByteBuf buffer) {
		buffer.writeLong(message.expectedRevision);
		buffer.writeUtf(message.action, 40);
		buffer.writeNbt(message.payload);
	}

	public static void handler(DungeonBuilderStudioActionMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				DungeonBuilderStudioService.apply(player, message.expectedRevision,
						message.action, message.payload);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(DungeonBuilderStudioActionMessage.class,
				DungeonBuilderStudioActionMessage::buffer, DungeonBuilderStudioActionMessage::new,
				DungeonBuilderStudioActionMessage::handler);
	}
}
