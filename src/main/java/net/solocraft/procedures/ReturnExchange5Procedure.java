package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange5Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon5Procedure.execute(entity)) {
			return ExchangeCordReturn5Procedure.execute(entity);
		}
		return "";
	}
}
