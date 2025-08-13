package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange7Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon7Procedure.execute(entity)) {
			return ExchangeCordReturn7Procedure.execute(entity);
		}
		return "";
	}
}
