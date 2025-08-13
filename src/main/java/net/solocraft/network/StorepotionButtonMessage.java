
package net.solocraft.network;

import net.solocraft.world.inventory.StorepotionMenu;
import net.solocraft.procedures.BuyPotionManaProcedure;
import net.solocraft.procedures.BuyPotionMana3Procedure;
import net.solocraft.procedures.BuyPotionMana2Procedure;
import net.solocraft.procedures.BuyPotionHealthProcedure;
import net.solocraft.procedures.BuyPotionHealth3Procedure;
import net.solocraft.procedures.BuyPotionHealth2Procedure;
import net.solocraft.procedures.BuyPotionFatigueProcedure;
import net.solocraft.procedures.BuyPotionFatigue3Procedure;
import net.solocraft.procedures.BuyPotionFatigue2Procedure;
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
public class StorepotionButtonMessage {
	private final int buttonID, x, y, z;

	public StorepotionButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public StorepotionButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(StorepotionButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(StorepotionButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = StorepotionMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			BuyPotionManaProcedure.execute(entity);
		}
		if (buttonID == 1) {

			BuyPotionHealthProcedure.execute(entity);
		}
		if (buttonID == 2) {

			BuyPotionFatigueProcedure.execute(entity);
		}
		if (buttonID == 3) {

			BuyPotionMana2Procedure.execute(entity);
		}
		if (buttonID == 4) {

			BuyPotionMana3Procedure.execute(entity);
		}
		if (buttonID == 5) {

			BuyPotionHealth2Procedure.execute(entity);
		}
		if (buttonID == 6) {

			BuyPotionFatigue2Procedure.execute(entity);
		}
		if (buttonID == 7) {

			BuyPotionHealth3Procedure.execute(entity);
		}
		if (buttonID == 8) {

			BuyPotionFatigue3Procedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(StorepotionButtonMessage.class, StorepotionButtonMessage::buffer, StorepotionButtonMessage::new, StorepotionButtonMessage::handler);
	}
}
