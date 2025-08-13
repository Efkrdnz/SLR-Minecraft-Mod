
package net.solocraft.network;

import net.solocraft.world.inventory.StorePotionNewMenu;
import net.solocraft.procedures.OpenStoreGUIProcedure;
import net.solocraft.procedures.BuyPotionMP3Procedure;
import net.solocraft.procedures.BuyPotionMP2Procedure;
import net.solocraft.procedures.BuyPotionMP1Procedure;
import net.solocraft.procedures.BuyPotionHP3Procedure;
import net.solocraft.procedures.BuyPotionHP2Procedure;
import net.solocraft.procedures.BuyPotionHP1Procedure;
import net.solocraft.procedures.BuyPotionFTG3Procedure;
import net.solocraft.procedures.BuyPotionFTG2Procedure;
import net.solocraft.procedures.BuyPotionFTG1Procedure;
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
public class StorePotionNewButtonMessage {
	private final int buttonID, x, y, z;

	public StorePotionNewButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public StorePotionNewButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(StorePotionNewButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(StorePotionNewButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = StorePotionNewMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			OpenStoreGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			BuyPotionHP1Procedure.execute(entity);
		}
		if (buttonID == 2) {

			BuyPotionHP2Procedure.execute(entity);
		}
		if (buttonID == 3) {

			BuyPotionHP3Procedure.execute(entity);
		}
		if (buttonID == 4) {

			BuyPotionMP1Procedure.execute(entity);
		}
		if (buttonID == 5) {

			BuyPotionMP2Procedure.execute(entity);
		}
		if (buttonID == 6) {

			BuyPotionMP3Procedure.execute(entity);
		}
		if (buttonID == 7) {

			BuyPotionFTG1Procedure.execute(entity);
		}
		if (buttonID == 8) {

			BuyPotionFTG2Procedure.execute(entity);
		}
		if (buttonID == 9) {

			BuyPotionFTG3Procedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(StorePotionNewButtonMessage.class, StorePotionNewButtonMessage::buffer, StorePotionNewButtonMessage::new, StorePotionNewButtonMessage::handler);
	}
}
