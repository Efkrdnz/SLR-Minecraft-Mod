
package net.solocraft.network;

import net.solocraft.world.inventory.QuestsMenu;
import net.solocraft.procedures.OpenPathGuiProcedure;
import net.solocraft.procedures.JobChangeQuestEntryProcedure;
import net.solocraft.procedures.DemonKingsCastleKeyUseProcedure;
import net.solocraft.procedures.DKCPathTeleportProcedure;
import net.solocraft.procedures.DailyQuestGUIOpenProcedure;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;
import net.solocraft.util.DkcQuestManager;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class QuestsButtonMessage {
	private static final ResourceKey<Level> DKC_DIMENSION = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling", "dungeon_dimension_dkc"));
	private final int buttonID, x, y, z;

	public QuestsButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public QuestsButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(QuestsButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(QuestsButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
		HashMap guistate = QuestsMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			DailyQuestGUIOpenProcedure.execute(world, x, y, z, entity);
		}
		if (buttonID == 1) {
			if (!(entity instanceof ServerPlayer player) || !DkcQuestManager.isVisible(player))
				return;
			if (player.level().dimension().equals(DKC_DIMENSION)) {
				DKCPathTeleportProcedure.returnToSavedOverworld(player);
				player.closeContainer();
				return;
			}
			SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
			if (!vars.dkc_started && vars.dkc_cleared <= 0) {
				DemonKingsCastleKeyUseProcedure.execute(world, player, ItemStack.EMPTY);
				player.closeContainer();
				return;
			}
			OpenPathGuiProcedure.execute(world, x, y, z, player);
		}
		if (buttonID == 2) {
			if (entity instanceof ServerPlayer player) {
				JobChangeQuestEntryProcedure.execute(world, player);
				player.closeContainer();
			}
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(QuestsButtonMessage.class, QuestsButtonMessage::buffer, QuestsButtonMessage::new, QuestsButtonMessage::handler);
	}
}
