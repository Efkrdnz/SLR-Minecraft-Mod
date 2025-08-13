package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class ResetAfterReadjustingMonarchLimitProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_MONARCH_LIMIT)) < SololevelingModVariables.MapVariables.get(world).shmlimit) {
			for (Entity entityiterator : new ArrayList<>(world.players())) {
				if ((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
					{
						double _setval = 0;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.JOB = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
					SololevelingModVariables.MapVariables.get(world).shmlimit = 0;
					SololevelingModVariables.MapVariables.get(world).syncData(world);
				}
			}
		}
	}
}
