package net.solocraft.procedures;

import net.solocraft.world.inventory.EquippedAbilitiesMenu;
import net.solocraft.network.SololevelingModVariables;

import net.solocraft.network.compat.NetworkHooks;

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

public class OpenAbilitiesListProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player _player)
			_player.closeContainer();
		if (entity instanceof ServerPlayer _ent) {
			BlockPos _bpos = BlockPos.containing(x, y, z);
			int initialPage = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.map(capability -> capability.PslotSelecting >= 9 && capability.PslotSelecting <= 16 ? 2 : 1)
					.orElse(1);
			NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("EquippedAbilities");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					return new EquippedAbilitiesMenu(id, inventory,
							new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos).writeVarInt(initialPage));
				}
			}, data -> data.writeBlockPos(_bpos).writeVarInt(initialPage));
		}
	}
}
