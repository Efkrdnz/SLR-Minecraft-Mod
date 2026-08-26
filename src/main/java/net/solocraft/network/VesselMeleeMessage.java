package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.api.vessel.VesselMeleeAttackEvent;
import net.solocraft.api.vessel.VesselState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.solocraft.network.compat.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * "I left-clicked while a contributed form had claimed the attack."
 *
 * <p>Carries only the form id, and the server re-checks that the form is
 * actually active before telling anyone. The client decides that a button was
 * pressed; it does not get to decide that a transformation was in effect.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class VesselMeleeMessage {
	/** Long enough for a namespaced form id, short enough to be cheap to reject. */
	private static final int MAX_FORM_ID_LENGTH = 64;

	private final String formId;

	public VesselMeleeMessage(String formId) {
		this.formId = formId == null ? "" : formId;
	}

	public VesselMeleeMessage(FriendlyByteBuf buffer) {
		this.formId = buffer.readUtf(MAX_FORM_ID_LENGTH);
	}

	public static void buffer(VesselMeleeMessage message, FriendlyByteBuf buffer) {
		buffer.writeUtf(message.formId, MAX_FORM_ID_LENGTH);
	}

	public static void handler(VesselMeleeMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || message.formId.isBlank())
				return;
			// The claim is only real if the form is genuinely active server-side.
			if (!VesselState.isFormActive(player, message.formId))
				return;
			NeoForge.EVENT_BUS.post(new VesselMeleeAttackEvent(player, message.formId));
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(VesselMeleeMessage.class, VesselMeleeMessage::buffer,
				VesselMeleeMessage::new, VesselMeleeMessage::handler);
	}
}
