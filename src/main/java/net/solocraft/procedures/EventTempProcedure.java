package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.level.BlockEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class EventTempProcedure {
	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		execute(event, event.getLevel(), event.getPlayer());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (DkcFloorRegistry.isDkc(world) && entity instanceof Player player
				&& !player.isCreative() && !player.isSpectator()) {
			if (event != null && event.isCancelable())
				event.setCanceled(true);
			return;
		}
		if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.DISABLE_BLOCK_BREAKING)) {
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dungeoning) {
				if (event != null && event.isCancelable()) {
					event.setCanceled(true);
				}
			}
		}
	}
}
