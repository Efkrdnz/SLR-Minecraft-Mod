
package net.solocraft.network;

import net.solocraft.procedures.DKCPathTeleportProcedure;
import net.solocraft.SololevelingMod;
import net.solocraft.world.inventory.PathMenu;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PathButtonMessage {
	private final int buttonID, x, y, z;

	public PathButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public PathButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(PathButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(PathButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		if (!(entity instanceof ServerPlayer player) || !(player.containerMenu instanceof PathMenu))
			return;
		Level world = player.level();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if ((!vars.dkc_started && vars.dkc_cleared <= 0)
				|| net.solocraft.dkc.DkcFloorRegistry.isDkc(player.level()))
			return;
		// buttonID 0-19 correspond to floors 1-20; DkcEnterFloor procedures were empty stubs
		if (buttonID >= 0 && buttonID < 20) {
			player.closeContainer();
			DKCPathTeleportProcedure.execute(player, buttonID + 1);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PathButtonMessage.class, PathButtonMessage::buffer, PathButtonMessage::new, PathButtonMessage::handler);
	}
}
