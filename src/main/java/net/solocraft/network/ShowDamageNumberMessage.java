package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.procedures.RenderDamageNumberProcedure;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
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
		context.enqueueWork(() -> {
			if (!context.getDirection().getReceptionSide().isServer()) {
				RenderDamageNumberProcedure.addNumber(message.x, message.y, message.z, message.amount, message.color);
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShowDamageNumberMessage.class, ShowDamageNumberMessage::buffer, ShowDamageNumberMessage::new, ShowDamageNumberMessage::handler);
	}
}
