
package net.solocraft.network;

import net.solocraft.world.inventory.EquippedAbilitiesMenu;
import net.solocraft.procedures.SkillRemoveButtonProcedure;
import net.solocraft.procedures.EquipButtonProcedure;
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
		if (entity == null)
			return;
		Level world = entity.level();
		HashMap guistate = EquippedAbilitiesMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID >= 0 && buttonID <= 7) {
			EquipButtonProcedure.execute(world, x, y, z, entity, buttonID + 1);
		}
		if (buttonID >= 8 && buttonID <= 15) {
			SkillRemoveButtonProcedure.execute(entity, buttonID - 7);
		}
		if (buttonID >= 16 && buttonID <= 23) {
			EquipButtonProcedure.execute(world, x, y, z, entity, buttonID - 7);
		}
		if (buttonID >= 24 && buttonID <= 31) {
			SkillRemoveButtonProcedure.execute(entity, buttonID - 15);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(EquippedAbilitiesButtonMessage.class, EquippedAbilitiesButtonMessage::buffer, EquippedAbilitiesButtonMessage::new, EquippedAbilitiesButtonMessage::handler);
	}
}
