
package net.solocraft.network;

import net.solocraft.world.inventory.UnlockedSkillsTab3Menu;
import net.solocraft.procedures.OpenAbilitylist4Procedure;
import net.solocraft.procedures.OpenAbilitylist2Procedure;
import net.solocraft.procedures.AbilityAppendButton24Procedure;
import net.solocraft.procedures.AbilityAppendButton23Procedure;
import net.solocraft.procedures.AbilityAppendButton22Procedure;
import net.solocraft.procedures.AbilityAppendButton21Procedure;
import net.solocraft.procedures.AbilityAppendButton20Procedure;
import net.solocraft.procedures.AbilityAppendButton19Procedure;
import net.solocraft.procedures.AbilityAppendButton18Procedure;
import net.solocraft.procedures.AbilityAppendButton17Procedure;
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
public class UnlockedSkillsTab3ButtonMessage {
	private final int buttonID, x, y, z;

	public UnlockedSkillsTab3ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public UnlockedSkillsTab3ButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(UnlockedSkillsTab3ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(UnlockedSkillsTab3ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = UnlockedSkillsTab3Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			AbilityAppendButton17Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			AbilityAppendButton18Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			AbilityAppendButton19Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			AbilityAppendButton20Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			AbilityAppendButton21Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			AbilityAppendButton22Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			AbilityAppendButton23Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			AbilityAppendButton24Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			OpenAbilitylist2Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenAbilitylist4Procedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UnlockedSkillsTab3ButtonMessage.class, UnlockedSkillsTab3ButtonMessage::buffer, UnlockedSkillsTab3ButtonMessage::new, UnlockedSkillsTab3ButtonMessage::handler);
	}
}
