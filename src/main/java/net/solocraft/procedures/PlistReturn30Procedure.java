package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistReturn30Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		double a = 0;
		String stringprev = "";
		String result1 = "";
		if (entity == (null)) {
			return "";
		}
		a = 30;
		int i = (int) a;
		result1 = PlistReturnProcedure.execute(entity, i);
		return result1;
	}
}
