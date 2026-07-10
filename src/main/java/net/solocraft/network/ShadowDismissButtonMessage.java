package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.world.inventory.ShadowDismissMenu;
import net.solocraft.world.inventory.ShadowSummonGUIMenu;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

import io.netty.buffer.Unpooled;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShadowDismissButtonMessage {
	private final int buttonID, x, y, z;

	public ShadowDismissButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public ShadowDismissButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(ShadowDismissButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(ShadowDismissButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			handleButtonAction(entity, message.buttonID, message.x, message.y, message.z);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		if (entity == null)
			return;
		Level world = entity.level();
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 100) {
			openSummon(entity, x, y, z);
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
			case 7 -> "high_orc";
			default -> "";
		};
		if (!type.isEmpty())
			ShadowMonarchManager.dismissShadowType(entity, type);
	}

	private static void openSummon(Player entity, int x, int y, int z) {
		if (!(entity instanceof ServerPlayer serverPlayer))
			return;
		BlockPos pos = new BlockPos(x, y, z);
		NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("Shadow Summon");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				return new ShadowSummonGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
			}
		}, pos);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(ShadowDismissButtonMessage.class, ShadowDismissButtonMessage::buffer, ShadowDismissButtonMessage::new, ShadowDismissButtonMessage::handler);
	}
}
