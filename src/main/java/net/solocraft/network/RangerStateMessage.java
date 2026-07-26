package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.RangerClientState;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/** Compact server-to-client state used only by the Ranger aiming HUD. */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RangerStateMessage {
	public final boolean quiverActive;
	public final int chargeStage;
	public final int maximumStage;
	public final float lockProgress;
	public final boolean locked;
	public final int fivefoldCharges;
	public final boolean hawkeye;
	public final boolean hyperFocus;

	public RangerStateMessage(boolean quiverActive, int chargeStage, int maximumStage,
			float lockProgress, boolean locked, int fivefoldCharges,
			boolean hawkeye, boolean hyperFocus) {
		this.quiverActive = quiverActive;
		this.chargeStage = chargeStage;
		this.maximumStage = maximumStage;
		this.lockProgress = lockProgress;
		this.locked = locked;
		this.fivefoldCharges = fivefoldCharges;
		this.hawkeye = hawkeye;
		this.hyperFocus = hyperFocus;
	}

	private RangerStateMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readFloat(), buffer.readBoolean(), buffer.readVarInt(),
				buffer.readBoolean(), buffer.readBoolean());
	}

	private static void encode(RangerStateMessage message, FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.quiverActive);
		buffer.writeVarInt(message.chargeStage);
		buffer.writeVarInt(message.maximumStage);
		buffer.writeFloat(message.lockProgress);
		buffer.writeBoolean(message.locked);
		buffer.writeVarInt(message.fivefoldCharges);
		buffer.writeBoolean(message.hawkeye);
		buffer.writeBoolean(message.hyperFocus);
	}

	private static void handle(RangerStateMessage message, Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> RangerClientState.update(message));
		context.get().setPacketHandled(true);
	}

	@SubscribeEvent
	public static void register(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(RangerStateMessage.class,
				RangerStateMessage::encode, RangerStateMessage::new, RangerStateMessage::handle,
				NetworkDirection.PLAY_TO_CLIENT);
	}
}
