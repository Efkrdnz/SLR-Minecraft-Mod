package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ExchangeCon7Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(ExchangeCordReturn7Procedure.execute(entity)).equals("")) {
			return true;
		}
		return false;
	}
}
