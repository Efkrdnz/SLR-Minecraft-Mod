package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistReturn7Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String stringprev = "";
		String result1 = "";
		double a = 0;
		if (entity == (null)) {
			return "";
		}
		a = 7;
		int i = (int) a;
		result1 = PlistReturnProcedure.execute(entity, i);
		return result1;
	}
}
