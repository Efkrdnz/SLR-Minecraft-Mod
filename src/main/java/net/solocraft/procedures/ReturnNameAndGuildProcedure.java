package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnNameAndGuildProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return entity.getDisplayName().getString();
	}
}
