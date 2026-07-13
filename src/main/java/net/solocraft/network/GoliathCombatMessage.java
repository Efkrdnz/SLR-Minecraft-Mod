package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.GoliathCombatManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GoliathCombatMessage {
	public GoliathCombatMessage() {
	}

	public GoliathCombatMessage(FriendlyByteBuf buffer) {
	}

	public static void buffer(GoliathCombatMessage message, FriendlyByteBuf buffer) {
	}

	public static void handler(GoliathCombatMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				GoliathCombatManager.enhancedStrike(player);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(GoliathCombatMessage.class, GoliathCombatMessage::buffer, GoliathCombatMessage::new, GoliathCombatMessage::handler);
	}
}
