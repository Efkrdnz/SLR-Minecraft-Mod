package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange4Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon4Procedure.execute(entity)) {
			return ExchangeCordReturn4Procedure.execute(entity);
		}
		return "";
	}
}
