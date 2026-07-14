
package net.solocraft.network;

import net.solocraft.procedures.Ability1OnKeyReleasedProcedure;
import net.solocraft.procedures.Ability1OnKeyPressedProcedure;
import net.solocraft.SololevelingMod;
import net.solocraft.util.BeastMonarchManager;
import net.solocraft.util.GoliathCombatManager;
import net.solocraft.util.LiuZhigangCombatManager;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Ability1Message {
	int type, pressedms;

	public Ability1Message(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public Ability1Message(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(Ability1Message message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(Ability1Message message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {

			Ability1OnKeyPressedProcedure.execute(world, x, y, z, entity);
		}
		if (type == 1) {
			if (BeastMonarchManager.isFangStance(entity))
				BeastMonarchManager.releasePredatorsIntercept(entity, pressedms);
			else if (GoliathCombatManager.isCombatStance(entity))
				GoliathCombatManager.releasePursuit(entity, pressedms);
			else if (LiuZhigangCombatManager.isCombatStance(entity))
				LiuZhigangCombatManager.releaseDragonFlash(entity, pressedms);
			else
				Ability1OnKeyReleasedProcedure.execute(world, x, y, z, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(Ability1Message.class, Ability1Message::buffer, Ability1Message::new, Ability1Message::handler);
	}
}
