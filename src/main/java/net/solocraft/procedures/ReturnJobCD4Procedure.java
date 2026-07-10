package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;

import net.minecraft.world.entity.Entity;

public class ReturnJobCD4Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return new java.text.DecimalFormat("##")
				.format(CooldownManager.getRemainingSeconds(entity, "job_4"));
	}
}
