package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistReturn23Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		double a = 0;
		String stringprev = "";
		String result1 = "";
		if (entity == (null)) {
			return "";
		}
		a = 23;
		int i = (int) a;
		result1 = PlistReturnProcedure.execute(entity, i);
		return result1;
	}
}
