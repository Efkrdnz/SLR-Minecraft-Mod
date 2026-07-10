package net.solocraft.procedures;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class SungJinWooRightClickedOnEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity sourceentity) {
		if (sourceentity instanceof Player player && !player.level().isClientSide()) {
			player.displayClientMessage(Component.literal("§7Lore guilds are disabled for now."), false);
		}
	}
}
