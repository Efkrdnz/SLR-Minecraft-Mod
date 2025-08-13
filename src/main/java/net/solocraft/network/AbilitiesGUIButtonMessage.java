
package net.solocraft.network;

import net.solocraft.world.inventory.AbilitiesGUIMenu;
import net.solocraft.procedures.TjEnableProcedure;
import net.solocraft.procedures.SpeedPercentIncreaseProcedure;
import net.solocraft.procedures.SpeedPercentDecreaseProcedure;
import net.solocraft.procedures.ShadowGUIOpenProcedure;
import net.solocraft.procedures.OpenPanelOnKeyPressedProcedure;
import net.solocraft.procedures.OpenAbilitiesListProcedure;
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
public class AbilitiesGUIButtonMessage {
	private final int buttonID, x, y, z;

	public AbilitiesGUIButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public AbilitiesGUIButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(AbilitiesGUIButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(AbilitiesGUIButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = AbilitiesGUIMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			OpenPanelOnKeyPressedProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			SpeedPercentDecreaseProcedure.execute(entity);
		}
		if (buttonID == 2) {

			SpeedPercentIncreaseProcedure.execute(entity);
		}
		if (buttonID == 3) {

			TjEnableProcedure.execute(entity);
		}
		if (buttonID == 4) {

			ShadowGUIOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			OpenAbilitiesListProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(AbilitiesGUIButtonMessage.class, AbilitiesGUIButtonMessage::buffer, AbilitiesGUIButtonMessage::new, AbilitiesGUIButtonMessage::handler);
	}
}
