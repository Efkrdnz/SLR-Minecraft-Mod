
package net.solocraft.network;

import net.solocraft.world.inventory.ShadowExchangeSaveMenu;
import net.solocraft.procedures.PolarBearExchangeSaveProcedure;
import net.solocraft.procedures.LycanExchangeSaveProcedure;
import net.solocraft.procedures.KnightExchangeSaveProcedure;
import net.solocraft.procedures.GoblinMageExchangeSaveProcedure;
import net.solocraft.procedures.GoblinClubExchangeSaveProcedure;
import net.solocraft.procedures.GoblinArcherExchangeSaveProcedure;
import net.solocraft.SololevelingMod;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShadowExchangeSaveButtonMessage {
	private final int buttonID, x, y, z;

	public ShadowExchangeSaveButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShadowExchangeSaveButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShadowExchangeSaveButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShadowExchangeSaveButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		Level world = entity.level();
		HashMap guistate = ShadowExchangeSaveMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			KnightExchangeSaveProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			GoblinClubExchangeSaveProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			GoblinArcherExchangeSaveProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			GoblinMageExchangeSaveProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			LycanExchangeSaveProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			PolarBearExchangeSaveProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowExchangeSaveButtonMessage.class, ShadowExchangeSaveButtonMessage::buffer, ShadowExchangeSaveButtonMessage::new, ShadowExchangeSaveButtonMessage::handler);
	}
}
