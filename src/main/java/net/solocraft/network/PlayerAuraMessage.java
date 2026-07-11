package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.aura.ClientPlayerAuraManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerAuraMessage {
	public static final byte SET = 0;
	public static final byte CLEAR = 1;
	public static final byte BURST = 2;

	private final int entityId;
	private final String auraId;
	private final byte action;
	private final int duration;
	private final float intensity;

	public PlayerAuraMessage(int entityId, String auraId, byte action, int duration, float intensity) {
		this.entityId = entityId;
		this.auraId = auraId == null ? "" : auraId;
		this.action = action;
		this.duration = duration;
		this.intensity = intensity;
	}

	public PlayerAuraMessage(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(), buffer.readUtf(64), buffer.readByte(), buffer.readVarInt(), buffer.readFloat());
	}

	public static void buffer(PlayerAuraMessage message, FriendlyByteBuf buffer) {
		buffer.writeVarInt(message.entityId);
		buffer.writeUtf(message.auraId, 64);
		buffer.writeByte(message.action);
		buffer.writeVarInt(message.duration);
		buffer.writeFloat(message.intensity);
	}

	public static void handler(PlayerAuraMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				ClientPlayerAuraManager.handle(message.entityId, message.auraId, message.action, message.duration, message.intensity)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PlayerAuraMessage.class, PlayerAuraMessage::buffer,
				PlayerAuraMessage::new, PlayerAuraMessage::handler);
	}
}
