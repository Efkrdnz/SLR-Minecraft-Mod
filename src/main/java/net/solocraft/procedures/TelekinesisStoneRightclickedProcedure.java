package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.RulersAuthorityManager;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

public class TelekinesisStoneRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (!(entity instanceof Player player) || player.level().isClientSide())
			return;

		if (RulersAuthorityManager.hasAuthority(player)) {
			player.displayClientMessage(Component.literal("You already have \"Ruler's Authority\""), true);
			return;
		}

		SololevelingModVariables.PlayerVariables variables = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
		if (variables == null)
			return;

		String abilities = variables.abilities == null ? "" : variables.abilities.replace('"', ' ').trim();
		variables.abilities = abilities.isEmpty() ? "telekinesis" : abilities + " telekinesis";
		variables.syncPlayerVariables(player);

		if (!player.isCreative())
			itemstack.shrink(1);
		if (world instanceof Level level)
			level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENCHANTMENT_TABLE_USE,
					SoundSource.NEUTRAL, 1, 1);
		player.displayClientMessage(Component.literal("Learned \"Ruler's Authority\""), true);
	}
}
