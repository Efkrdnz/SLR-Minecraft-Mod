package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistReturn51Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String stringprev = "";
		String result1 = "";
		if (entity == (null)) {
			return "";
		}
		int i = 51;
		result1 = PlistReturnProcedure.execute(entity, i);
		return result1;
	}
}
