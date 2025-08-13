package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnCooldownAmountProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (!(CooldownRemainingOnTickProcedure.execute(entity)).equals("")) {
			return "CD: " + CooldownRemainingOnTickProcedure.execute(entity);
		}
		return "Ready!";
	}
}
