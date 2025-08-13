
package net.solocraft.network;

import net.solocraft.world.inventory.ShadowExchangeSETMenu;
import net.solocraft.procedures.ExchangeTP7Procedure;
import net.solocraft.procedures.ExchangeTP6Procedure;
import net.solocraft.procedures.ExchangeTP5Procedure;
import net.solocraft.procedures.ExchangeTP4Procedure;
import net.solocraft.procedures.ExchangeTP3Procedure;
import net.solocraft.procedures.ExchangeTP2Procedure;
import net.solocraft.procedures.ExchangeTP1Procedure;
import net.solocraft.procedures.ExchangeCancel7Procedure;
import net.solocraft.procedures.ExchangeCancel6Procedure;
import net.solocraft.procedures.ExchangeCancel5Procedure;
import net.solocraft.procedures.ExchangeCancel4Procedure;
import net.solocraft.procedures.ExchangeCancel3Procedure;
import net.solocraft.procedures.ExchangeCancel2Procedure;
import net.solocraft.procedures.ExchangeCancel1Procedure;
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
public class ShadowExchangeSETButtonMessage {
	private final int buttonID, x, y, z;

	public ShadowExchangeSETButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShadowExchangeSETButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShadowExchangeSETButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShadowExchangeSETButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = ShadowExchangeSETMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			ExchangeTP1Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			ExchangeTP2Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			ExchangeTP3Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			ExchangeTP4Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			ExchangeTP5Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			ExchangeTP6Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			ExchangeTP7Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			ExchangeCancel1Procedure.execute(entity);
		}
		if (buttonID == 8) {

			ExchangeCancel2Procedure.execute(entity);
		}
		if (buttonID == 9) {

			ExchangeCancel3Procedure.execute(entity);
		}
		if (buttonID == 10) {

			ExchangeCancel4Procedure.execute(entity);
		}
		if (buttonID == 11) {

			ExchangeCancel5Procedure.execute(entity);
		}
		if (buttonID == 12) {

			ExchangeCancel6Procedure.execute(entity);
		}
		if (buttonID == 13) {

			ExchangeCancel7Procedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowExchangeSETButtonMessage.class, ShadowExchangeSETButtonMessage::buffer, ShadowExchangeSETButtonMessage::new, ShadowExchangeSETButtonMessage::handler);
	}
}
