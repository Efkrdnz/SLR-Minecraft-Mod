package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.util.CurseMageSpellManager;
import net.solocraft.util.CurseType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Commits the curse chosen on the radial wheel. */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class CurseSelectionMessage {
	private final int curseId;

	public CurseSelectionMessage(int curseId) {
		this.curseId = curseId;
	}

	public CurseSelectionMessage(FriendlyByteBuf buffer) {
		this.curseId = buffer.readVarInt();
	}

	public static void buffer(CurseSelectionMessage message, FriendlyByteBuf buffer) {
		buffer.writeVarInt(message.curseId);
	}

	public static void handler(CurseSelectionMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || message.curseId < 0
					|| message.curseId >= CurseType.values().length)
				return;
			CurseType curse = CurseType.byId(message.curseId);
			// The client builds the wheel from the player's unlocked curses, but the
			// server must not trust that list; a crafted packet could otherwise arm a
			// curse the player has not earned.
			if (!CurseMageSpellManager.unlockedCurses(player).contains(curse))
				return;
			CurseMageSpellManager.setArmedCurse(player, curse);
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(CurseSelectionMessage.class,
				CurseSelectionMessage::buffer,
				CurseSelectionMessage::new,
				CurseSelectionMessage::handler,
				NetworkDirection.PLAY_TO_SERVER);
	}
}
