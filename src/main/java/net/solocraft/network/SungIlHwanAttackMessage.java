package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.SungIlHwanCombatManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Narrow stance attack channel. Clients request no target, geometry or damage;
 * the server derives the entire line cut. The reverse mode synchronizes only the
 * stance boolean needed by the vanilla and optional combat-animation swing
 * observers.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class SungIlHwanAttackMessage {
	private static final byte ATTACK_REQUEST = 0;
	private static final byte STANCE_SYNC = 1;
	private static volatile boolean clientStanceActive;

	private final byte mode;
	private final boolean stanceActive;

	public SungIlHwanAttackMessage() {
		this(ATTACK_REQUEST, false);
	}

	private SungIlHwanAttackMessage(byte mode, boolean stanceActive) {
		this.mode = mode;
		this.stanceActive = stanceActive;
	}

	public SungIlHwanAttackMessage(FriendlyByteBuf buffer) {
		this.mode = buffer.readByte();
		this.stanceActive = buffer.readBoolean();
	}

	public static void buffer(SungIlHwanAttackMessage message,
			FriendlyByteBuf buffer) {
		buffer.writeByte(message.mode);
		buffer.writeBoolean(message.stanceActive);
	}

	public static void handler(SungIlHwanAttackMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender != null) {
				if (message.mode == ATTACK_REQUEST)
					SungIlHwanCombatManager.performAssassinLineCut(sender);
				return;
			}
			if (message.mode == STANCE_SYNC)
				clientStanceActive = message.stanceActive;
		});
		context.setPacketHandled(true);
	}

	public static boolean isClientStanceActive() {
		return clientStanceActive;
	}

	public static void syncStance(ServerPlayer player, boolean active) {
		if (player == null || player.connection == null)
			return;
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SungIlHwanAttackMessage(STANCE_SYNC, active));
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(SungIlHwanAttackMessage.class,
				SungIlHwanAttackMessage::buffer, SungIlHwanAttackMessage::new,
				SungIlHwanAttackMessage::handler);
	}
}
