
package net.solocraft.network;

import net.solocraft.procedures.AbilityAppendButtonProcedure;
import net.solocraft.world.inventory.UnlockedSkillsTab4Menu;
import net.solocraft.procedures.OpenAbilitylist5Procedure;
import net.solocraft.procedures.OpenAbilitylist3Procedure;
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
public class UnlockedSkillsTab4ButtonMessage {
	private final int buttonID, x, y, z;

	public UnlockedSkillsTab4ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public UnlockedSkillsTab4ButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(UnlockedSkillsTab4ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(UnlockedSkillsTab4ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = UnlockedSkillsTab4Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 25, true);
		}
		if (buttonID == 1) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 26, true);
		}
		if (buttonID == 2) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 27, true);
		}
		if (buttonID == 3) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 28, true);
		}
		if (buttonID == 4) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 29, true);
		}
		if (buttonID == 5) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 30, true);
		}
		if (buttonID == 6) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 31, true);
		}
		if (buttonID == 7) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 32, true);
		}
		if (buttonID == 8) {

			OpenAbilitylist3Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 9) {

			OpenAbilitylist5Procedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UnlockedSkillsTab4ButtonMessage.class, UnlockedSkillsTab4ButtonMessage::buffer, UnlockedSkillsTab4ButtonMessage::new, UnlockedSkillsTab4ButtonMessage::handler);
	}
}
