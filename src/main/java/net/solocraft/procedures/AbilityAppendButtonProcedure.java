package net.solocraft.procedures;

import net.solocraft.world.inventory.EquippedAbilitiesMenu;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SkillListHelper;

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

public class AbilityAppendButtonProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, int index, boolean closeFirst) {
		if (entity == null)
			return;
		if (!(SkillListHelper.rawSkillAt(entity, index)).equals("empty")) {
			if (closeFirst && entity instanceof Player _player)
				_player.closeContainer();
			String ability_to_append = SkillListHelper.rawSkillAt(entity, index);
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				SkillSlotHelper.setSlot(capability, (int) capability.PslotSelecting, ability_to_append);
				capability.syncPlayerVariables(entity);
			});
			if (entity instanceof ServerPlayer _ent) {
				BlockPos _bpos = BlockPos.containing(x, y, z);
				NetworkHooks.openScreen((ServerPlayer) _ent, new MenuProvider() {
					@Override
					public Component getDisplayName() {
						return Component.literal("EquippedAbilities");
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new EquippedAbilitiesMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(_bpos));
					}
				}, _bpos);
			}
		}
	}
}
