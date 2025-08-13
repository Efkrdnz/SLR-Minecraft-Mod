package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.CommandEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class FatigueGameruleSetProcedure {
	@SubscribeEvent
	public static void onCommand(CommandEvent event) {
		Entity entity = event.getParseResults().getContext().getSource().getEntity();
		if (entity != null) {
			execute(event, entity.level(), event.getParseResults().getReader().getString());
		}
	}

	public static void execute(LevelAccessor world, String command) {
		execute(null, world, command);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, String command) {
		if (command == null)
			return;
		if (command.contains("/gamerule soloFatigue")) {
			if (!world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_FATIGUE)) {
				for (Entity entityiterator : new ArrayList<>(world.players())) {
					{
						double _setval = 0;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Fatigue = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
			}
		}
	}
}
