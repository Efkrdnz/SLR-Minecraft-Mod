
package net.solocraft.network;

import net.solocraft.world.inventory.PanelRework2Menu;
import net.solocraft.procedures.VitalityIncreaseProcedure;
import net.solocraft.procedures.StrengthIncreaseProcedure;
import net.solocraft.procedures.SpeedIncreaseProcedure;
import net.solocraft.procedures.SenseIncreaseProcedure;
import net.solocraft.procedures.RewardScreenOpenProcedure;
import net.solocraft.procedures.OpenTrainingGUIProcedure;
import net.solocraft.procedures.OpenStoreGUIProcedure;
import net.solocraft.procedures.OpenMainQuestsGUIProcedure;
import net.solocraft.procedures.IntelligenceIncreaseProcedure;
import net.solocraft.procedures.CraftingGUIopenProcedure;
import net.solocraft.procedures.AbilitiesGUIopenProcedure;
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
public class PanelRework2ButtonMessage {
	private final int buttonID, x, y, z;

	public PanelRework2ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public PanelRework2ButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(PanelRework2ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(PanelRework2ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = PanelRework2Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			StrengthIncreaseProcedure.execute(entity);
		}
		if (buttonID == 1) {

			SpeedIncreaseProcedure.execute(entity);
		}
		if (buttonID == 2) {

			SenseIncreaseProcedure.execute(entity);
		}
		if (buttonID == 3) {

			VitalityIncreaseProcedure.execute(entity);
		}
		if (buttonID == 4) {

			IntelligenceIncreaseProcedure.execute(entity);
		}
		if (buttonID == 5) {

			OpenStoreGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			OpenMainQuestsGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			RewardScreenOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			CraftingGUIopenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenTrainingGUIProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 10) {

			AbilitiesGUIopenProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PanelRework2ButtonMessage.class, PanelRework2ButtonMessage::buffer, PanelRework2ButtonMessage::new, PanelRework2ButtonMessage::handler);
	}
}
