package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.procedures.RenderDamageNumberProcedure;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.DistExecutor;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ShowDamageNumberMessage {
	private final double x;
	private final double y;
	private final double z;
	private final float amount;
	private final int color;

	public ShowDamageNumberMessage(double x, double y, double z, float amount, int color) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.amount = amount;
		this.color = color;
	}

	public ShowDamageNumberMessage(FriendlyByteBuf buffer) {
		this.x = buffer.readDouble();
		this.y = buffer.readDouble();
		this.z = buffer.readDouble();
		this.amount = buffer.readFloat();
		this.color = buffer.readInt();
	}

	public static void buffer(ShowDamageNumberMessage message, FriendlyByteBuf buffer) {
		buffer.writeDouble(message.x);
		buffer.writeDouble(message.y);
		buffer.writeDouble(message.z);
		buffer.writeFloat(message.amount);
		buffer.writeInt(message.color);
	}

	public static void handler(ShowDamageNumberMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		if (context.getDirection().getReceptionSide().isClient()) {
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> RenderDamageNumberProcedure.addNumber(message.x, message.y,
							message.z, message.amount, message.color)));
		}
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShowDamageNumberMessage.class,
				ShowDamageNumberMessage::buffer, ShowDamageNumberMessage::new,
				ShowDamageNumberMessage::handler, NetworkDirection.PLAY_TO_CLIENT);
	}
}
