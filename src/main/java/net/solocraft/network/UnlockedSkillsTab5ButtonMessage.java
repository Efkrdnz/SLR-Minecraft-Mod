
package net.solocraft.network;

import net.solocraft.world.inventory.UnlockedSkillsTab5Menu;
import net.solocraft.procedures.OpenAbilitylist6Procedure;
import net.solocraft.procedures.OpenAbilitylist4Procedure;
import net.solocraft.procedures.AbilityAppendButton40Procedure;
import net.solocraft.procedures.AbilityAppendButton39Procedure;
import net.solocraft.procedures.AbilityAppendButton38Procedure;
import net.solocraft.procedures.AbilityAppendButton37Procedure;
import net.solocraft.procedures.AbilityAppendButton36Procedure;
import net.solocraft.procedures.AbilityAppendButton35Procedure;
import net.solocraft.procedures.AbilityAppendButton34Procedure;
import net.solocraft.procedures.AbilityAppendButton33Procedure;
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
public class UnlockedSkillsTab5ButtonMessage {
	private final int buttonID, x, y, z;

	public UnlockedSkillsTab5ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public UnlockedSkillsTab5ButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(UnlockedSkillsTab5ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(UnlockedSkillsTab5ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = UnlockedSkillsTab5Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			AbilityAppendButton33Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			AbilityAppendButton34Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			AbilityAppendButton35Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			AbilityAppendButton36Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			AbilityAppendButton37Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			AbilityAppendButton38Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			AbilityAppendButton39Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			AbilityAppendButton40Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			OpenAbilitylist4Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenAbilitylist6Procedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UnlockedSkillsTab5ButtonMessage.class, UnlockedSkillsTab5ButtonMessage::buffer, UnlockedSkillsTab5ButtonMessage::new, UnlockedSkillsTab5ButtonMessage::handler);
	}
}
