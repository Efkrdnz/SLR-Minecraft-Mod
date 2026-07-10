package net.solocraft.procedures;

import net.solocraft.world.inventory.RewardPanelMenu;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.RewardManager;
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

public class RewardScreenOpenProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
			if (RewardManager.hasRewards(entity)) {
				if (entity instanceof ServerPlayer _ent) {
					BlockPos _bpos = BlockPos.containing(x, y, z);
					NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
						@Override
						public Component getDisplayName() {
							return Component.literal("RewardPanel");
						}

						@Override
						public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
							return new RewardPanelMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
						}
					}, _bpos);
				}
			} else {
				if (entity instanceof Player _player)
					_player.closeContainer();
				if (entity instanceof ServerPlayer player) {
					SystemNotifications.showTitleUnder(player, 0xFFFFB83D, 70,
							Component.literal("NO REWARDS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
							Component.literal("There are no rewards to collect.").withStyle(ChatFormatting.GRAY));
				}
			}
		}
	}
}
