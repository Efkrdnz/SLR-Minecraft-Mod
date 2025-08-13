package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

public class SLRcompletedDungeonsProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double commaCount = 0;
		String gates_cleared = "";
		if (!(SololevelingModVariables.MapVariables.get(world).GatesCleared).isEmpty() || SololevelingModVariables.MapVariables.get(world).GatesCleared.contains(",")) {
			gates_cleared = SololevelingModVariables.MapVariables.get(world).GatesCleared;
			for (int i = (int) 0; i < (int) (gates_cleared).length(); i++) {
				if (gates_cleared.charAt(i) == ',') {
					commaCount = commaCount + 1;
				}
			}
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("\u00A7l\u00A79Amount of gates Cleared: "), false);
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal(("\u00A76" + commaCount)), false);
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("\u00A7l\u00A79Amount of gates Cleared:"), false);
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("\u00A75 0"), false);
		}
	}
}
