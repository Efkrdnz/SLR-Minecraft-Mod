package net.solocraft.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class LevelBarProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "" + Math.round(entity instanceof Player _plr ? _plr.experienceLevel : 0);
	}
}
