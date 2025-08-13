package net.solocraft.procedures;

import net.solocraft.init.SololevelingModKeyMappings;

public class Ability2ReturnProcedure {
	public static String execute() {
		return SololevelingModKeyMappings.ABILITY_2.saveString().replace("key.keyboard.", "");
	}
}
