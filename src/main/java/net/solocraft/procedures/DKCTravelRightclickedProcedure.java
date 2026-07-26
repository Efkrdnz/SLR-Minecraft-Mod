package net.solocraft.procedures;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

/** Legacy travel-item entry point, routed through the secured DKC key flow. */
public class DKCTravelRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity instanceof ServerPlayer player)
			DemonKingsCastleKeyUseProcedure.execute(world, player, ItemStack.EMPTY);
	}
}
