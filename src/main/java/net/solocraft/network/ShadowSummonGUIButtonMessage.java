
package net.solocraft.network;

import net.solocraft.world.inventory.ShadowSummonGUIMenu;
import net.solocraft.procedures.SummonWolfProcedure;
import net.solocraft.procedures.SummonTuskProcedure;
import net.solocraft.procedures.SummonPolarBearProcedure;
import net.solocraft.procedures.SummonOrcProcedure;
import net.solocraft.procedures.SummonKnightProcedure;
import net.solocraft.procedures.SummonKamishProcedure;
import net.solocraft.procedures.SummonIgrisProcedure;
import net.solocraft.procedures.SummonHighOrcProcedure;
import net.solocraft.procedures.SummonGoblinMageProcedure;
import net.solocraft.procedures.SummonGoblinClubProcedure;
import net.solocraft.procedures.SummonGoblinArcherProcedure;
import net.solocraft.procedures.SummonBeruProcedure;
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
public class ShadowSummonGUIButtonMessage {
	private final int buttonID, x, y, z;

	public ShadowSummonGUIButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShadowSummonGUIButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShadowSummonGUIButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShadowSummonGUIButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = ShadowSummonGUIMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			SummonGoblinClubProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			SummonGoblinArcherProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			SummonGoblinMageProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			SummonWolfProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			SummonKnightProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			SummonPolarBearProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			SummonOrcProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			SummonIgrisProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			SummonBeruProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			SummonKamishProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 10) {

			SummonHighOrcProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 11) {

			SummonTuskProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowSummonGUIButtonMessage.class, ShadowSummonGUIButtonMessage::buffer, ShadowSummonGUIButtonMessage::new, ShadowSummonGUIButtonMessage::handler);
	}
}
