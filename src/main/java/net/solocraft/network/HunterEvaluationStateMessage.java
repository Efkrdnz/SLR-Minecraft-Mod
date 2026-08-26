package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.HunterEvaluationScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;
import java.util.function.Supplier;

/** Authoritative snapshot for the evaluator ceremony UI. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class HunterEvaluationStateMessage {
	private final boolean open;
	private final boolean forceOpen;
	private final UUID sessionId;
	private final int mode;
	private final int phase;
	private final int classId;
	private final int rank;
	private final int previousRank;
	private final int phaseDurationTicks;
	private final int remainingTicks;
	private final boolean canReroll;
	private final boolean fixedClass;
	private final int styleId;
	private final boolean canRerollStyle;

	public HunterEvaluationStateMessage(boolean open, boolean forceOpen,
			UUID sessionId, int mode, int phase, int classId, int rank,
			int previousRank, int phaseDurationTicks, int remainingTicks,
			boolean canReroll, boolean fixedClass, int styleId,
			boolean canRerollStyle) {
		this.open = open;
		this.forceOpen = forceOpen;
		this.sessionId = sessionId == null ? new UUID(0L, 0L) : sessionId;
		this.mode = mode;
		this.phase = phase;
		this.classId = classId;
		this.rank = rank;
		this.previousRank = previousRank;
		this.phaseDurationTicks = Math.max(0, phaseDurationTicks);
		this.remainingTicks = Math.max(0, remainingTicks);
		this.canReroll = canReroll;
		this.fixedClass = fixedClass;
		this.styleId = Math.max(0, styleId);
		this.canRerollStyle = canRerollStyle;
	}

	public HunterEvaluationStateMessage(FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readBoolean(), buffer.readUUID(),
				buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
				buffer.readVarInt(), buffer.readBoolean());
	}

	public static void buffer(HunterEvaluationStateMessage message,
			FriendlyByteBuf buffer) {
		buffer.writeBoolean(message.open);
		buffer.writeBoolean(message.forceOpen);
		buffer.writeUUID(message.sessionId);
		buffer.writeVarInt(message.mode);
		buffer.writeVarInt(message.phase);
		buffer.writeVarInt(message.classId);
		buffer.writeVarInt(message.rank);
		buffer.writeVarInt(message.previousRank);
		buffer.writeVarInt(message.phaseDurationTicks);
		buffer.writeVarInt(message.remainingTicks);
		buffer.writeBoolean(message.canReroll);
		buffer.writeBoolean(message.fixedClass);
		buffer.writeVarInt(message.styleId);
		buffer.writeBoolean(message.canRerollStyle);
	}

	public static void handler(HunterEvaluationStateMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> HunterEvaluationScreen.handleServerState(
						message.open, message.forceOpen, message.sessionId,
						message.mode, message.phase, message.classId,
						message.rank, message.previousRank,
						message.phaseDurationTicks, message.remainingTicks,
						message.canReroll, message.fixedClass,
						message.styleId, message.canRerollStyle)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(
				HunterEvaluationStateMessage.class,
				HunterEvaluationStateMessage::buffer,
				HunterEvaluationStateMessage::new,
				HunterEvaluationStateMessage::handler,
				NetworkDirection.PLAY_TO_CLIENT);
	}
}
