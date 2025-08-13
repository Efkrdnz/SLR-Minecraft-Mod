package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange2Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon2Procedure.execute(entity)) {
			return ExchangeCordReturn2Procedure.execute(entity);
		}
		return "";
	}
}
