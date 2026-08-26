package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

import java.util.ArrayList;

@EventBusSubscriber
public class LoreAccurateRankProcedure {
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
		if (world.getLevelData().getGameTime() % 100 == 0) {
			if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_LEVELING_LORE_ACCURATE_RANKS) == true) {
				for (Entity entityiterator : new ArrayList<>(world.players())) {
					{
						double _setval = 2;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.LoreAccurateRankStart = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
			} else {
				for (Entity entityiterator : new ArrayList<>(world.players())) {
					{
						double _setval = 2;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.LoreAccurateRankStart = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
			}
		}
	}
}
