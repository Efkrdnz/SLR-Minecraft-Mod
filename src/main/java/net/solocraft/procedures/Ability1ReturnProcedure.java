package net.solocraft.procedures;

import net.solocraft.init.SololevelingModKeyMappings;

public class Ability1ReturnProcedure {
	public static String execute() {
		return SololevelingModKeyMappings.ABILITY_1.saveString().replace("key.keyboard.", "");
	}
}
