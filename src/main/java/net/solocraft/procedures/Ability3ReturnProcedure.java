package net.solocraft.procedures;

import net.solocraft.init.SololevelingModKeyMappings;

public class Ability3ReturnProcedure {
	public static String execute() {
		return SololevelingModKeyMappings.ABILITY_3.saveString().replace("key.keyboard.", "");
	}
}
