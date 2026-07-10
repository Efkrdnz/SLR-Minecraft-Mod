
package net.solocraft.network;

import net.solocraft.procedures.AbilityAppendButtonProcedure;
import net.solocraft.world.inventory.UnlockedSkillsTab1Menu;
import net.solocraft.procedures.OpenAbilitiesListProcedure;
import net.solocraft.procedures.OpenAbilitylist2Procedure;
import net.solocraft.util.ShadowMonarchManager;
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
public class UnlockedSkillsTab1ButtonMessage {
	private final int buttonID, x, y, z;
	private final int skillIndex;

	public UnlockedSkillsTab1ButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.skillIndex = buffer.readInt();
	}

	public UnlockedSkillsTab1ButtonMessage(int buttonID, int x, int y, int z) {
		this(buttonID, x, y, z, 0);
	}

	public UnlockedSkillsTab1ButtonMessage(int buttonID, int x, int y, int z, int skillIndex) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.skillIndex = skillIndex;
	}

	public static void buffer(UnlockedSkillsTab1ButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeInt(message.skillIndex);
	}

	public static void handler(UnlockedSkillsTab1ButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z, message.skillIndex);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		handleButtonAction(entity, buttonID, x, y, z, 0);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, int skillIndex) {
		if (entity == null)
			return;
		Level world = entity.level();
		HashMap guistate = UnlockedSkillsTab1Menu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 101) {
			OpenAbilitiesListProcedure.execute(world, x, y, z, entity);
			return;
		}
		if (buttonID == 100 && skillIndex > 0) {
			ShadowMonarchManager.removeFormationSkill(entity, skillIndex);
			return;
		}
		if (skillIndex > 0) {
			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, skillIndex, false);
			return;
		}
		if (buttonID == 0) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 1, false);
		}
		if (buttonID == 1) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 2, false);
		}
		if (buttonID == 2) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 3, true);
		}
		if (buttonID == 3) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 4, true);
		}
		if (buttonID == 4) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 5, true);
		}
		if (buttonID == 5) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 6, true);
		}
		if (buttonID == 6) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 7, true);
		}
		if (buttonID == 7) {

			AbilityAppendButtonProcedure.execute(world, x, y, z, entity, 8, true);
		}
		if (buttonID == 8) {

			OpenAbilitylist2Procedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(UnlockedSkillsTab1ButtonMessage.class, UnlockedSkillsTab1ButtonMessage::buffer, UnlockedSkillsTab1ButtonMessage::new, UnlockedSkillsTab1ButtonMessage::handler);
	}
}
