
package net.solocraft.network;

import net.solocraft.world.inventory.EquippedAbilitiesMenu;
import net.solocraft.procedures.SkillRemoveButton8Procedure;
import net.solocraft.procedures.SkillRemoveButton7Procedure;
import net.solocraft.procedures.SkillRemoveButton6Procedure;
import net.solocraft.procedures.SkillRemoveButton5Procedure;
import net.solocraft.procedures.SkillRemoveButton4Procedure;
import net.solocraft.procedures.SkillRemoveButton3Procedure;
import net.solocraft.procedures.SkillRemoveButton2Procedure;
import net.solocraft.procedures.SkillRemoveButton1Procedure;
import net.solocraft.procedures.EquipButton8Procedure;
import net.solocraft.procedures.EquipButton7Procedure;
import net.solocraft.procedures.EquipButton6Procedure;
import net.solocraft.procedures.EquipButton5Procedure;
import net.solocraft.procedures.EquipButton4Procedure;
import net.solocraft.procedures.EquipButton3Procedure;
import net.solocraft.procedures.EquipButton2Procedure;
import net.solocraft.procedures.EquipButton1Procedure;
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
public class EquippedAbilitiesButtonMessage {
	private final int buttonID, x, y, z;

	public EquippedAbilitiesButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public EquippedAbilitiesButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(EquippedAbilitiesButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(EquippedAbilitiesButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = EquippedAbilitiesMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			EquipButton1Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {

			EquipButton2Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 2) {

			EquipButton3Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 3) {

			EquipButton4Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 4) {

			EquipButton5Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 5) {

			EquipButton6Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 6) {

			EquipButton7Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 7) {

			EquipButton8Procedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 8) {

			SkillRemoveButton1Procedure.execute(entity);
		}
		if (buttonID == 9) {

			SkillRemoveButton2Procedure.execute(entity);
		}
		if (buttonID == 10) {

			SkillRemoveButton3Procedure.execute(entity);
		}
		if (buttonID == 11) {

			SkillRemoveButton4Procedure.execute(entity);
		}
		if (buttonID == 12) {

			SkillRemoveButton5Procedure.execute(entity);
		}
		if (buttonID == 13) {

			SkillRemoveButton6Procedure.execute(entity);
		}
		if (buttonID == 14) {

			SkillRemoveButton7Procedure.execute(entity);
		}
		if (buttonID == 15) {

			SkillRemoveButton8Procedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(EquippedAbilitiesButtonMessage.class, EquippedAbilitiesButtonMessage::buffer, EquippedAbilitiesButtonMessage::new, EquippedAbilitiesButtonMessage::handler);
	}
}
