package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnExchange3Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (ExchangeCon3Procedure.execute(entity)) {
			return ExchangeCordReturn3Procedure.execute(entity);
		}
		return "";
	}
}
