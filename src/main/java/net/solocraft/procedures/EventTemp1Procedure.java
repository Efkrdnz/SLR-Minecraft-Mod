package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.init.SololevelingModBlocks;
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
public class EventTemp1Procedure {
	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		execute(event, event.getLevel(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (DkcFloorRegistry.isDkc(world) && entity instanceof Player player
				&& !player.isCreative() && !player.isSpectator()) {
			if (event instanceof BlockEvent.EntityPlaceEvent placeEvent
					&& placeEvent.getPlacedBlock().is(SololevelingModBlocks.FROST_CAUSEWAY.get()))
				return;
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
