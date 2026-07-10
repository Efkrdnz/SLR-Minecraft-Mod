package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShadowCommandButtonMessage {
	private final int buttonID, x, y, z;

	public ShadowCommandButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShadowCommandButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShadowCommandButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShadowCommandButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleButtonAction(context.getSender(), message.buttonID, message.x, message.y, message.z));
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		if (entity == null)
			return;
		Level world = entity.level();
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		String command = switch (buttonID) {
			case 0 -> ShadowMonarchManager.COMMAND_DEFAULT;
			case 1 -> ShadowMonarchManager.COMMAND_PROTECT;
			case 2 -> ShadowMonarchManager.COMMAND_BERSERK;
			case 3 -> ShadowMonarchManager.COMMAND_FOLLOW;
			case 4 -> ShadowMonarchManager.COMMAND_CLEAR_DUNGEON;
			default -> "";
		};
		if (!command.isEmpty())
			ShadowMonarchManager.commandSummonedShadows(entity, command);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowCommandButtonMessage.class, ShadowCommandButtonMessage::buffer, ShadowCommandButtonMessage::new, ShadowCommandButtonMessage::handler);
	}
}
