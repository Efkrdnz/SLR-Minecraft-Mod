package net.solocraft.procedures;

import net.solocraft.init.SololevelingModKeyMappings;

public class Ability4ReturnProcedure {
	public static String execute() {
		return SololevelingModKeyMappings.ABILITY_4.saveString().replace("key.keyboard.", "");
	}
}
