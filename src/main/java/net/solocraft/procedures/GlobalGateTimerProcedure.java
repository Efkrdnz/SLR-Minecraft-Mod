package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.util.GateSpawnerUtil;
import net.solocraft.util.DungeonBuilderMode;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

@EventBusSubscriber
public class GlobalGateTimerProcedure {
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		if (true) {
			execute(event, event.getLevel());
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
