
package net.solocraft.network;

import net.solocraft.world.inventory.ShadowGUIMenu;
import net.solocraft.procedures.DismissWolfProcedure;
import net.solocraft.procedures.DismissSoldierProcedure;
import net.solocraft.procedures.DismissGoblinProcedure;
import net.solocraft.procedures.DismissGoblinMageProcedure;
import net.solocraft.procedures.DismissGoblinArcherProcedure;
import net.solocraft.procedures.BerserkOwnerButtonProcedure;
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
public class ShadowGUIButtonMessage {
	private final int buttonID, x, y, z;

	public ShadowGUIButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShadowGUIButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShadowGUIButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShadowGUIButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = ShadowGUIMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			BerserkOwnerButtonProcedure.execute(entity);
		}
		if (buttonID == 1) {

			DismissSoldierProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			DismissGoblinProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			DismissGoblinMageProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			DismissWolfProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			DismissGoblinArcherProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowGUIButtonMessage.class, ShadowGUIButtonMessage::buffer, ShadowGUIButtonMessage::new, ShadowGUIButtonMessage::handler);
	}
}
