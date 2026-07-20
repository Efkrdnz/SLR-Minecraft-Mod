package net.solocraft.network;

import net.solocraft.procedures.CustomHudToggleProcedure;
import net.solocraft.SololevelingMod;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;

/**
 * Server-side toggles for the System Settings screen:
 * <ul>
 *   <li>id 0 — Combat Mode</li>
 *   <li>id 1 — Custom HUD (delegates to {@link CustomHudToggleProcedure})</li>
 * </ul>
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SystemSettingsButtonMessage {
	private final int buttonID, x, y, z;

	public SystemSettingsButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public SystemSettingsButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(SystemSettingsButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(SystemSettingsButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleButtonAction(context.getSender(), message.buttonID, message.x, message.y, message.z));
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		if (entity == null)
			return;
		Level world = entity.level();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.combatmode = !capability.combatmode;
				capability.syncPlayerVariables(entity);
			});
		}
		if (buttonID == 1) {
			CustomHudToggleProcedure.execute(entity);
		}
		if (buttonID == 2 && SystemPlayerAccess.hasSystem(entity)) {
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.pvpUrgentQuests = !capability.pvpUrgentQuests;
				capability.syncPlayerVariables(entity);
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(SystemSettingsButtonMessage.class, SystemSettingsButtonMessage::buffer, SystemSettingsButtonMessage::new, SystemSettingsButtonMessage::handler);
	}
}
