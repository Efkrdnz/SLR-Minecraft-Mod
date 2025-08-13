package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.entity.RedGateEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class PortalNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return false;
		double rand = 0;
		if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_GATE_SPAWNING)) {
			if ((world instanceof Level _lvl ? _lvl.dimension() : Level.OVERWORLD) == Level.OVERWORLD) {
				if (SololevelingModVariables.MapVariables.get(world).gatetimer >= (world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY))) {
					rand = Mth.nextInt(RandomSource.create(), 1, 12);
					if (rand == 7) {
						if (entity instanceof RedGateEntity) {
							if (!SololevelingModVariables.MapVariables.get(world).RedGate) {
								return true;
							} else {
								return false;
							}
						} else {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
