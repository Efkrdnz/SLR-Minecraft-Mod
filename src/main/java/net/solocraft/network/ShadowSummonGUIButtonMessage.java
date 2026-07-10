
package net.solocraft.network;

import net.solocraft.world.inventory.ShadowDismissMenu;
import net.solocraft.world.inventory.ShadowSummonGUIMenu;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.SololevelingMod;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import io.netty.buffer.Unpooled;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShadowSummonGUIButtonMessage {
	private final int buttonID, x, y, z;
	private final String payload;

	public ShadowSummonGUIButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.payload = buffer.readUtf(64);
	}

	public ShadowSummonGUIButtonMessage(int buttonID, int x, int y, int z) {
		this(buttonID, x, y, z, "");
	}

	public ShadowSummonGUIButtonMessage(int buttonID, int x, int y, int z, String payload) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.payload = payload == null ? "" : payload;
	}

	public static void buffer(ShadowSummonGUIButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeUtf(message.payload, 64);
	}

	public static void handler(ShadowSummonGUIButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			handleButtonAction(entity, buttonID, x, y, z, message.payload);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		handleButtonAction(entity, buttonID, x, y, z, "");
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, String payload) {
		if (entity == null)
			return;
		Level world = entity.level();
		HashMap guistate = ShadowSummonGUIMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 100) {
			String name = ShadowMonarchManager.saveFormationFromSummoned(entity, payload);
			if (!name.isEmpty() && !entity.level().isClientSide())
				entity.displayClientMessage(Component.literal("Saved formation: " + name), true);
			return;
		}
		if (buttonID == 101) {
			openDismiss(entity, x, y, z);
			return;
		}
		String type = switch (buttonID) {
			case 0 -> "goblin_club";
			case 1 -> "goblin_archer";
			case 2 -> "goblin_mage";
			case 3 -> "wolf";
			case 4 -> "knight";
			case 5 -> "polar_bear";
			case 6 -> "orc";
			case 7 -> "igris";
			case 8 -> "beru";
			case 9 -> "kamish";
			case 10 -> "high_orc";
			case 11 -> "tusk";
			case 12 -> "kaisel";
			default -> "";
		};
		if (!type.isEmpty()) {
			if ("all".equalsIgnoreCase(payload))
				ShadowMonarchManager.summonAllOfType(world, x, y, z, entity, type);
			else
				ShadowMonarchManager.summonType(world, x, y, z, entity, type);
		}
	}

	private static void openDismiss(Player entity, int x, int y, int z) {
		if (!(entity instanceof ServerPlayer serverPlayer))
			return;
		BlockPos pos = new BlockPos(x, y, z);
		NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("Shadow Dismiss");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				return new ShadowDismissMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
			}
		}, pos);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowSummonGUIButtonMessage.class, ShadowSummonGUIButtonMessage::buffer, ShadowSummonGUIButtonMessage::new, ShadowSummonGUIButtonMessage::handler);
	}
}
