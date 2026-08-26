package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.HunterEvaluationManager;
import net.solocraft.util.HunterEvaluationRules;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.Supplier;

/** Client intent packet. Results and evaluator coordinates are never accepted. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class HunterEvaluationActionMessage {
	private final UUID sessionId;
	private final int actionId;

	public HunterEvaluationActionMessage(UUID sessionId,
			HunterEvaluationRules.Action action) {
		this.sessionId = sessionId == null ? new UUID(0L, 0L) : sessionId;
		this.actionId = action == null ? -1 : action.ordinal();
	}

	public HunterEvaluationActionMessage(FriendlyByteBuf buffer) {
		this.sessionId = buffer.readUUID();
		this.actionId = buffer.readVarInt();
	}

	public static void buffer(HunterEvaluationActionMessage message,
			FriendlyByteBuf buffer) {
		buffer.writeUUID(message.sessionId);
		buffer.writeVarInt(message.actionId);
	}

	public static void handler(HunterEvaluationActionMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			HunterEvaluationRules.Action action =
					HunterEvaluationRules.Action.fromId(message.actionId);
			if (player != null && action != null)
				HunterEvaluationManager.handleAction(player,
						message.sessionId, action);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(
				HunterEvaluationActionMessage.class,
				HunterEvaluationActionMessage::buffer,
				HunterEvaluationActionMessage::new,
				HunterEvaluationActionMessage::handler,
				NetworkDirection.PLAY_TO_SERVER);
	}
}
