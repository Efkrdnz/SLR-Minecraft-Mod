package net.solocraft.network;

import net.solocraft.client.gui.system.SystemNotificationManager;
import net.solocraft.SololevelingMod;

import net.solocraft.network.compat.DistExecutor;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.function.Supplier;

/**
 * Server → client packet that shows a System notification. Carries an accent
 * color, a hold duration (ticks) and an optional big title + small undertext
 * (presence flags select the layout). The client handler forwards to
 * {@link SystemNotificationManager}.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ShowNotificationMessage {
	private final int accentColor;
	private final int durationTicks;
	private final Component title;    // nullable
	private final Component undertext; // nullable
	private final boolean negativeSound;

	public ShowNotificationMessage(int accentColor, int durationTicks, Component title, Component undertext) {
		this(accentColor, durationTicks, title, undertext, false);
	}

	public ShowNotificationMessage(int accentColor, int durationTicks, Component title, Component undertext, boolean negativeSound) {
		this.accentColor = accentColor;
		this.durationTicks = durationTicks;
		this.title = title;
		this.undertext = undertext;
		this.negativeSound = negativeSound;
	}

	public ShowNotificationMessage(FriendlyByteBuf buffer) {
		this.accentColor = buffer.readInt();
		this.durationTicks = buffer.readInt();
		this.title = buffer.readBoolean()
				? ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer) : null;
		this.undertext = buffer.readBoolean()
				? ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer) : null;
		this.negativeSound = buffer.readBoolean();
	}

	public static void buffer(ShowNotificationMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.accentColor);
		buffer.writeInt(message.durationTicks);
		buffer.writeBoolean(message.title != null);
		if (message.title != null)
			ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, message.title);
		buffer.writeBoolean(message.undertext != null);
		if (message.undertext != null)
			ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, message.undertext);
		buffer.writeBoolean(message.negativeSound);
	}

	public static void handler(ShowNotificationMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		if (context.getDirection().getReceptionSide().isClient()) {
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
					SystemNotificationManager.INSTANCE.push(message.accentColor,
							message.durationTicks, message.title, message.undertext,
							message.negativeSound)));
		}
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShowNotificationMessage.class,
				ShowNotificationMessage::buffer, ShowNotificationMessage::new,
				ShowNotificationMessage::handler, NetworkDirection.PLAY_TO_CLIENT);
	}
}
