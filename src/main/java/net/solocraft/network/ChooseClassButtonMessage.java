
package net.solocraft.network;

import net.solocraft.world.inventory.ChooseClassMenu;
import net.solocraft.procedures.ClassChooseButton6Procedure;
import net.solocraft.procedures.ClassChooseButton5Procedure;
import net.solocraft.procedures.ClassChooseButton4Procedure;
import net.solocraft.procedures.ClassChooseButton3Procedure;
import net.solocraft.procedures.ClassChooseButton2Procedure;
import net.solocraft.procedures.ClassChooseButton1Procedure;
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
public class ChooseClassButtonMessage {
	private final int buttonID, x, y, z;

	public ChooseClassButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ChooseClassButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ChooseClassButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ChooseClassButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = ChooseClassMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			ClassChooseButton1Procedure.execute(entity);
		}
		if (buttonID == 1) {

			ClassChooseButton2Procedure.execute(entity);
		}
		if (buttonID == 2) {

			ClassChooseButton3Procedure.execute(entity);
		}
		if (buttonID == 3) {

			ClassChooseButton4Procedure.execute(entity);
		}
		if (buttonID == 4) {

			ClassChooseButton5Procedure.execute(entity);
		}
		if (buttonID == 5) {

			ClassChooseButton6Procedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ChooseClassButtonMessage.class, ChooseClassButtonMessage::buffer, ChooseClassButtonMessage::new, ChooseClassButtonMessage::handler);
	}
}
