package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class Ab8CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.DUAL_WIELDING_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(SololevelingModMobEffects.LIGHTBALL_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(SololevelingModMobEffects.SWORD_DANCE_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt3 && _livEnt3.hasEffect(SololevelingModMobEffects.WILLPOWER_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt4 && _livEnt4.hasEffect(SololevelingModMobEffects.HASTE_BUFF_COOLDOWN.get())) {
			return true;
		}
		return false;
	}
}
