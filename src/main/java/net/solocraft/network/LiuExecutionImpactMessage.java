package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.screens.LiuExecutionImpactRenderer;

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
public final class LiuExecutionImpactMessage {
	private final int durationTicks;
	private final int primaryColor;
	private final int secondaryColor;

	public LiuExecutionImpactMessage(int durationTicks, int primaryColor, int secondaryColor) {
		this.durationTicks = durationTicks;
		this.primaryColor = primaryColor;
		this.secondaryColor = secondaryColor;
	}

	public LiuExecutionImpactMessage(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(), buffer.readInt(), buffer.readInt());
	}

	public static void buffer(LiuExecutionImpactMessage message, FriendlyByteBuf buffer) {
		buffer.writeVarInt(message.durationTicks);
		buffer.writeInt(message.primaryColor);
		buffer.writeInt(message.secondaryColor);
	}

	public static void handler(LiuExecutionImpactMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				LiuExecutionImpactRenderer.start(message.durationTicks,
						message.primaryColor, message.secondaryColor)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(LiuExecutionImpactMessage.class,
				LiuExecutionImpactMessage::buffer, LiuExecutionImpactMessage::new,
				LiuExecutionImpactMessage::handler);
	}
}
