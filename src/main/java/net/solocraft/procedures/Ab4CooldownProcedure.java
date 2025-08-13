package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class Ab4CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.WATER_BULLET_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(SololevelingModMobEffects.IMPACT_RUSH_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(SololevelingModMobEffects.BLOODLUST_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt3 && _livEnt3.hasEffect(SololevelingModMobEffects.OVER_HEAL_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt4 && _livEnt4.hasEffect(SololevelingModMobEffects.TRAP_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt5 && _livEnt5.hasEffect(SololevelingModMobEffects.TANK_INV_COOLDOWN.get())) {
			return true;
		}
		return false;
	}
}
