package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class ExchangeCordReturn2Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String stringprev = "";
		stringprev = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ExchangeCords;
		if (stringprev.contains(",")) {
			stringprev = stringprev.replace(stringprev.substring((int) stringprev.indexOf("."), (int) stringprev.indexOf(",") + ",".length()), ".");
			if (stringprev.contains(",")) {
				return stringprev.substring((int) stringprev.indexOf(".") + ".".length(), (int) stringprev.indexOf(","));
			}
		}
		return "";
	}
}
