package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class ReturnJobCD3Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return new java.text.DecimalFormat("##")
				.format((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.JOB_COOLDOWN_3.get()) ? _livEnt.getEffect(SololevelingModMobEffects.JOB_COOLDOWN_3.get()).getDuration() : 0) / 20);
	}
}
