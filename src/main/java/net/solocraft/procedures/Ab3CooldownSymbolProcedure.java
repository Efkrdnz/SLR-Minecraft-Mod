package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class Ab3CooldownSymbolProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.FIREBALL_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt1 && _livEnt1.hasEffect(SololevelingModMobEffects.HEAVY_IMPACT_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(SololevelingModMobEffects.STEALTH_COOLDOWN.get()) || entity instanceof LivingEntity _livEnt3 && _livEnt3.hasEffect(SololevelingModMobEffects.HEALING_BEAM_COOLDOWN.get())
				|| entity instanceof LivingEntity _livEnt4 && _livEnt4.hasEffect(SololevelingModMobEffects.LEAP_COOLDOWN.get())) {
			return true;
		}
		return false;
	}
}
