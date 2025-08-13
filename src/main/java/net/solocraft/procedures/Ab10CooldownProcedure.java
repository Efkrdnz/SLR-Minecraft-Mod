package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class Ab10CooldownProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.BACKSTAB_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(SololevelingModMobEffects.ELDER_BEAST_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(SololevelingModMobEffects.SO_LCOOLDOWN.get()) || entity instanceof LivingEntity _livEnt3 && _livEnt3.hasEffect(SololevelingModMobEffects.TAUNT_COOLDOWN.get())) {
			return true;
		}
		return false;
	}
}
