package net.solocraft.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

public class HungerBarProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "" + Math.round(entity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0);
	}
}
