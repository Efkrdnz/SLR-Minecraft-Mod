package net.solocraft.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class HealthTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return Math.round(entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + "" + "/" + "" + Math.round(entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1);
	}
}
