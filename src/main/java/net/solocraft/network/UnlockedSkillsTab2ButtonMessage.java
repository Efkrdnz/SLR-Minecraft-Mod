
package net.solocraft.network;

import net.solocraft.world.inventory.UnlockedSkillsTab2Menu;
import net.solocraft.procedures.OpenAbilitylistProcedure;
import net.solocraft.procedures.OpenAbilitylist3Procedure;
import net.solocraft.procedures.AbilityAppendButton9Procedure;
import net.solocraft.procedures.AbilityAppendButton16Procedure;
import net.solocraft.procedures.AbilityAppendButton15Procedure;
import net.solocraft.procedures.AbilityAppendButton14Procedure;
import net.solocraft.procedures.AbilityAppendButton13Procedure;
import net.solocraft.procedures.AbilityAppendButton12Procedure;
import net.solocraft.procedures.AbilityAppendButton11Procedure;
import net.solocraft.procedures.AbilityAppendButton10Procedure;
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
public class UnlockedSkillsTab2ButtonMessage {
	private final int buttonID, x, y, z;

	public UnlockedSkillsTab2ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public UnlockedSkillsTab2ButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(UnlockedSkillsTab2ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(UnlockedSkillsTab2ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = UnlockedSkillsTab2Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			AbilityAppendButton9Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			AbilityAppendButton10Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			AbilityAppendButton11Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			AbilityAppendButton12Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			AbilityAppendButton13Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			AbilityAppendButton14Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			AbilityAppendButton15Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			AbilityAppendButton16Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			OpenAbilitylistProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenAbilitylist3Procedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UnlockedSkillsTab2ButtonMessage.class, UnlockedSkillsTab2ButtonMessage::buffer, UnlockedSkillsTab2ButtonMessage::new, UnlockedSkillsTab2ButtonMessage::handler);
	}
}
