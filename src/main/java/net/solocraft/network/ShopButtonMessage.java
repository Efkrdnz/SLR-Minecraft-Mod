
package net.solocraft.network;

import net.solocraft.world.inventory.ShopMenu;
import net.solocraft.procedures.ShopRefreshButtonProcedure;
import net.solocraft.procedures.OpenStoreGUIProcedure;
import net.solocraft.procedures.BuyingS6Procedure;
import net.solocraft.procedures.BuyingS5Procedure;
import net.solocraft.procedures.BuyingS4Procedure;
import net.solocraft.procedures.BuyingS3Procedure;
import net.solocraft.procedures.BuyingS2Procedure;
import net.solocraft.procedures.BuyingProcedure;
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
public class ShopButtonMessage {
	private final int buttonID, x, y, z;

	public ShopButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShopButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShopButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShopButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = ShopMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			BuyingProcedure.execute(entity);
		}
		if (buttonID == 1) {

			BuyingS2Procedure.execute(entity);
		}
		if (buttonID == 2) {

			BuyingS3Procedure.execute(entity);
		}
		if (buttonID == 3) {

			BuyingS4Procedure.execute(entity);
		}
		if (buttonID == 4) {

			BuyingS5Procedure.execute(entity);
		}
		if (buttonID == 5) {

			BuyingS6Procedure.execute(entity);
		}
		if (buttonID == 6) {

			OpenStoreGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			ShopRefreshButtonProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShopButtonMessage.class, ShopButtonMessage::buffer, ShopButtonMessage::new, ShopButtonMessage::handler);
	}
}
