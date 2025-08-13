package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnNameProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "\u00A7f\u00A7l" + "Name: " + entity.getDisplayName().getString();
	}
}
