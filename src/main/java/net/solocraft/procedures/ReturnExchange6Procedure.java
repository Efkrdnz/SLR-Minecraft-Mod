package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange6Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon6Procedure.execute(entity)) {
			return ExchangeCordReturn6Procedure.execute(entity);
		}
		return "";
	}
}
