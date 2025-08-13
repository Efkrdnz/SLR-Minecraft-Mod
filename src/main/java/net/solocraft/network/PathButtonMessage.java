
package net.solocraft.network;

import net.solocraft.world.inventory.PathMenu;
import net.solocraft.procedures.DkcEnterFloor9Procedure;
import net.solocraft.procedures.DkcEnterFloor8Procedure;
import net.solocraft.procedures.DkcEnterFloor7Procedure;
import net.solocraft.procedures.DkcEnterFloor6Procedure;
import net.solocraft.procedures.DkcEnterFloor5Procedure;
import net.solocraft.procedures.DkcEnterFloor4Procedure;
import net.solocraft.procedures.DkcEnterFloor3Procedure;
import net.solocraft.procedures.DkcEnterFloor2Procedure;
import net.solocraft.procedures.DkcEnterFloor20Procedure;
import net.solocraft.procedures.DkcEnterFloor1Procedure;
import net.solocraft.procedures.DkcEnterFloor19Procedure;
import net.solocraft.procedures.DkcEnterFloor18Procedure;
import net.solocraft.procedures.DkcEnterFloor17Procedure;
import net.solocraft.procedures.DkcEnterFloor16Procedure;
import net.solocraft.procedures.DkcEnterFloor15Procedure;
import net.solocraft.procedures.DkcEnterFloor14Procedure;
import net.solocraft.procedures.DkcEnterFloor13Procedure;
import net.solocraft.procedures.DkcEnterFloor12Procedure;
import net.solocraft.procedures.DkcEnterFloor11Procedure;
import net.solocraft.procedures.DkcEnterFloor10Procedure;
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
public class PathButtonMessage {
	private final int buttonID, x, y, z;

	public PathButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public PathButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(PathButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(PathButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = PathMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			DkcEnterFloor1Procedure.execute();
		}
		if (buttonID == 1) {

			DkcEnterFloor2Procedure.execute();
		}
		if (buttonID == 2) {

			DkcEnterFloor3Procedure.execute();
		}
		if (buttonID == 3) {

			DkcEnterFloor4Procedure.execute();
		}
		if (buttonID == 4) {

			DkcEnterFloor5Procedure.execute();
		}
		if (buttonID == 5) {

			DkcEnterFloor6Procedure.execute();
		}
		if (buttonID == 6) {

			DkcEnterFloor7Procedure.execute();
		}
		if (buttonID == 7) {

			DkcEnterFloor8Procedure.execute();
		}
		if (buttonID == 8) {

			DkcEnterFloor9Procedure.execute();
		}
		if (buttonID == 9) {

			DkcEnterFloor10Procedure.execute();
		}
		if (buttonID == 10) {

			DkcEnterFloor11Procedure.execute();
		}
		if (buttonID == 11) {

			DkcEnterFloor12Procedure.execute();
		}
		if (buttonID == 12) {

			DkcEnterFloor13Procedure.execute();
		}
		if (buttonID == 13) {

			DkcEnterFloor14Procedure.execute();
		}
		if (buttonID == 14) {

			DkcEnterFloor15Procedure.execute();
		}
		if (buttonID == 15) {

			DkcEnterFloor16Procedure.execute();
		}
		if (buttonID == 16) {

			DkcEnterFloor17Procedure.execute();
		}
		if (buttonID == 17) {

			DkcEnterFloor18Procedure.execute();
		}
		if (buttonID == 18) {

			DkcEnterFloor19Procedure.execute();
		}
		if (buttonID == 19) {

			DkcEnterFloor20Procedure.execute();
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PathButtonMessage.class, PathButtonMessage::buffer, PathButtonMessage::new, PathButtonMessage::handler);
	}
}
