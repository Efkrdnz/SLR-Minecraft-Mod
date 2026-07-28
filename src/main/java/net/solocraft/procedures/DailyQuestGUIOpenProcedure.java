package net.solocraft.procedures;

import net.solocraft.world.inventory.DailyQuestsMenu;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import io.netty.buffer.Unpooled;

public class DailyQuestGUIOpenProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !SystemPlayerAccess.hasSystem(entity))
			return;
		if (entity instanceof ServerPlayer serverPlayer) {
			BlockPos menuPosition = BlockPos.containing(x, y, z);
			NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("DailyQuests");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new DailyQuestsMenu(id, inventory,
							new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(menuPosition));
				}
			}, menuPosition);
		}
	}
}
