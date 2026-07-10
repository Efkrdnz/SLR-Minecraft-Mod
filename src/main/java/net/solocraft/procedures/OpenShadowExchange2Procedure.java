package net.solocraft.procedures;

import net.solocraft.world.inventory.ShadowExchangeSETMenu;
import net.solocraft.util.ShadowExchangeManager;
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

public class OpenShadowExchange2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (ShadowExchangeManager.hasAnchor(entity, 1)) {
			if (entity instanceof ServerPlayer _ent) {
				BlockPos _bpos = BlockPos.containing(x, y, z);
				NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
					@Override
					public Component getDisplayName() {
						return Component.literal("ShadowExchangeSET");
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new ShadowExchangeSETMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
					}
				}, _bpos);
			}
		} else {
			if (entity instanceof ServerPlayer player)
				SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D3D, 80,
						Component.literal("NO ANCHORS").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
						Component.literal("Set a Shadow Exchange anchor first.").withStyle(ChatFormatting.RED));
		}
	}
}
