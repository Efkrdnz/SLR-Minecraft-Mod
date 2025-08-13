package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange1Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon1Procedure.execute(entity)) {
			return ExchangeCordReturn1Procedure.execute(entity);
		}
		return "";
	}
}
