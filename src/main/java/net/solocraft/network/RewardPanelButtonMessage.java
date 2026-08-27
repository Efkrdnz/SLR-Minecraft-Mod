
package net.solocraft.network;

import net.solocraft.procedures.RewardCollectButtonProcedure;
import net.solocraft.SololevelingMod;

import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class RewardPanelButtonMessage {
	private final int buttonID, x, y, z;

	public RewardPanelButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public RewardPanelButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(RewardPanelButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(RewardPanelButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		if (!(entity instanceof ServerPlayer player))
			return;
		// The modern System Rewards screen is client-only, so it does not have the
		// legacy RewardPanelMenu open. Slot claims are still safe here: the server
		// always claims only the sender's own reward slot.
		if (buttonID >= 100) {
			RewardCollectButtonProcedure.execute(player, buttonID - 99);
			return;
		}
		// Keep the original menu-presence check for the generated legacy screen.
		if (!(player.containerMenu instanceof net.solocraft.world.inventory.RewardPanelMenu))
			return;
		if (buttonID == 0) {
			RewardCollectButtonProcedure.execute(player, 2);
		}
		if (buttonID == 1) {
			RewardCollectButtonProcedure.execute(player, 1);
		}
		if (buttonID == 2) {
			RewardCollectButtonProcedure.execute(player, 3);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(RewardPanelButtonMessage.class,
				RewardPanelButtonMessage::buffer, RewardPanelButtonMessage::new,
				RewardPanelButtonMessage::handler, NetworkDirection.PLAY_TO_SERVER);
	}
}
