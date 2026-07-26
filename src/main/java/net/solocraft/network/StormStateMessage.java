package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.StormClientState;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Compact server-to-client snapshot for the Storm Current passive HUD. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StormStateMessage {
	public final boolean hasAccess;
	public final int voltage;
	public final int effectiveStage;
	public final boolean overcharged;
	public final boolean rodActive;
	public final boolean tempestActive;
	public final boolean spiritualizationBonus;

	public StormStateMessage(boolean hasAccess, int voltage, int effectiveStage,
			boolean overcharged, boolean rodActive, boolean tempestActive,
			boolean spiritualizationBonus) {
		this.hasAccess = hasAccess;
		this.voltage = voltage;
		this.effectiveStage = effectiveStage;
		this.overcharged = overcharged;
		this.rodActive = rodActive;
		this.tempestActive = tempestActive;
		this.spiritualizationBonus = spiritualizationBonus;
	}

	private StormStateMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
				buffer.readBoolean());
	}

	private static void encode(StormStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.hasAccess);
		buffer.writeVarInt(message.voltage);
		buffer.writeVarInt(message.effectiveStage);
		buffer.writeBoolean(message.overcharged);
		buffer.writeBoolean(message.rodActive);
		buffer.writeBoolean(message.tempestActive);
		buffer.writeBoolean(message.spiritualizationBonus);
	}

	private static void handle(StormStateMessage message,
			Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> StormClientState.update(message));
		context.get().setPacketHandled(true);
	}

	@SubscribeEvent
	public static void register(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(StormStateMessage.class,
				StormStateMessage::encode, StormStateMessage::new,
				StormStateMessage::handle, NetworkDirection.PLAY_TO_CLIENT);
	}
}
