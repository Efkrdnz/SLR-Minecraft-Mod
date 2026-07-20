package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.util.GateSpawnerUtil;
import net.solocraft.util.DungeonBuilderMode;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class GlobalGateTimerProcedure {
	@SubscribeEvent
	public static void onWorldTick(TickEvent.LevelTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.level);
		}
	}

	public static void execute(LevelAccessor world) {
		execute(null, world);
	}

	private static void execute(@Nullable Event event, LevelAccessor world) {
		if (DungeonBuilderMode.isActive(world))
			return;
		if (!world.isClientSide()) {
			if ((world instanceof Level _lvl ? _lvl.dimension() : Level.OVERWORLD) == Level.OVERWORLD) {
				if (world.getLevelData().getGameTime() % 20 == 0) {
					if (SololevelingModVariables.MapVariables.get(world).gatetimer < (world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY))) {
						SololevelingModVariables.MapVariables.get(world).gatetimer = SololevelingModVariables.MapVariables.get(world).gatetimer + 1;
						SololevelingModVariables.MapVariables.get(world).syncData(world);
					} else if (SololevelingModVariables.MapVariables.get(world).gatetimer >= (world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY))) {
						GateSpawnerUtil.spawnNearRandomOverworldPlayer(world);
					}
				}
			}
		}
	}
}
