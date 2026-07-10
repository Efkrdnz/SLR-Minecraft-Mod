package net.solocraft.procedures;

import net.solocraft.world.inventory.ShadowExchangeSaveMenu;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;

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
import net.minecraft.ChatFormatting;

import io.netty.buffer.Unpooled;

public class OpenShadowExchange1Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dungeoning) {
			if (entity instanceof ServerPlayer _ent) {
				BlockPos _bpos = BlockPos.containing(x, y, z);
				NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
					@Override
					public Component getDisplayName() {
						return Component.literal("ShadowExchangeSave");
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new ShadowExchangeSaveMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
					}
				}, _bpos);
			}
		} else {
			if (entity instanceof ServerPlayer player)
				SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D3D, 80,
						Component.literal("EXCHANGE LOCKED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
						Component.literal("You cannot set an anchor in this dimension.").withStyle(ChatFormatting.RED));
		}
	}
}
