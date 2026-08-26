
package net.solocraft.network;

import net.solocraft.procedures.DKCPathTeleportProcedure;
import net.solocraft.SololevelingMod;
import net.solocraft.world.inventory.PathMenu;
import net.solocraft.dkc.DkcFloorRegistry;

import net.solocraft.network.compat.NetworkEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class PathButtonMessage {
	public enum Action {
		ENTER_FLOOR,
		EXIT_CASTLE
	}

	private final Action action;
	private final int floor;

	public PathButtonMessage(FriendlyByteBuf buffer) {
		this.action = buffer.readEnum(Action.class);
		this.floor = buffer.readVarInt();
	}

	/**
	 * Compatibility constructor for the generated Floor 1-20 buttons.
	 * Prefer {@link #enterFloor(int)} in the tower screen.
	 */
	public PathButtonMessage(int buttonID, int x, int y, int z) {
		this(Action.ENTER_FLOOR, buttonID + 1);
	}

	private PathButtonMessage(Action action, int floor) {
		this.action = action;
		this.floor = floor;
	}

	public static PathButtonMessage enterFloor(int floor) {
		return new PathButtonMessage(Action.ENTER_FLOOR, floor);
	}

	public static PathButtonMessage enterFloor(int floor, int x, int y, int z) {
		return enterFloor(floor);
	}

	public static PathButtonMessage exitCastle() {
		return new PathButtonMessage(Action.EXIT_CASTLE, 0);
	}

	public static PathButtonMessage exitCastle(int x, int y, int z) {
		return exitCastle();
	}

	public static void buffer(PathButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeEnum(message.action);
		buffer.writeVarInt(message.floor);
	}

	public static void handler(PathButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleButtonAction(context.getSender(), message));
		context.setPacketHandled(true);
	}

	/** Compatibility entry point retained until all generated screen code is replaced. */
	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		handleButtonAction(entity, enterFloor(buttonID + 1));
	}

	private static void handleButtonAction(Player entity, PathButtonMessage message) {
		if (!(entity instanceof ServerPlayer player) || !(player.containerMenu instanceof PathMenu))
			return;
		if (message.action == Action.EXIT_CASTLE) {
			if (!DkcFloorRegistry.isDkc(player.level()))
				return;
			player.closeContainer();
			DKCPathTeleportProcedure.returnToSavedOverworld(player);
			return;
		}
		if (message.action != Action.ENTER_FLOOR
				|| message.floor < DkcFloorRegistry.FIRST_FLOOR
				|| message.floor > DkcFloorRegistry.LAST_FLOOR)
			return;
		// Never trust the menu snapshot or the requested floor. Preflight every
		// live rule before closing the menu, then execute rechecks once more.
		if (!DKCPathTeleportProcedure.canTravelToFloor(player, message.floor, true))
			return;
		player.closeContainer();
		DKCPathTeleportProcedure.execute(player, message.floor);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PathButtonMessage.class, PathButtonMessage::buffer, PathButtonMessage::new, PathButtonMessage::handler);
	}
}
