
package net.solocraft.network;

import net.solocraft.procedures.AbilityAppendButtonProcedure;
import net.solocraft.world.inventory.UnlockedSkillsTab6Menu;
import net.solocraft.procedures.OpenAbilitylist7Procedure;
import net.solocraft.procedures.OpenAbilitylist5Procedure;
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
public class UnlockedSkillsTab6ButtonMessage {
	private final int buttonID, x, y, z;

	public UnlockedSkillsTab6ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public UnlockedSkillsTab6ButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(UnlockedSkillsTab6ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(UnlockedSkillsTab6ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = UnlockedSkillsTab6Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 41, true);
		}
		if (buttonID == 1) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 42, true);
		}
		if (buttonID == 2) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 43, true);
		}
		if (buttonID == 3) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 44, true);
		}
		if (buttonID == 4) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 45, true);
		}
		if (buttonID == 5) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 46, true);
		}
		if (buttonID == 6) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 47, true);
		}
		if (buttonID == 7) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 48, true);
		}
		if (buttonID == 8) {

			OpenAbilitylist5Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenAbilitylist7Procedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UnlockedSkillsTab6ButtonMessage.class, UnlockedSkillsTab6ButtonMessage::buffer, UnlockedSkillsTab6ButtonMessage::new, UnlockedSkillsTab6ButtonMessage::handler);
	}
}
