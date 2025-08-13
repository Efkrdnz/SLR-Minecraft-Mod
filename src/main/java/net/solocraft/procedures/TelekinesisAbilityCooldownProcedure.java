package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class TelekinesisAbilityCooldownProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.TELEKINESIS_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.TELEKINESIS_COOLDOWN.get()).getDuration() : 0) > 0) {
			return "" + Math.round((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.TELEKINESIS_COOLDOWN.get()) ? _livEnt.getEffect(SololevelingModMobEffects.TELEKINESIS_COOLDOWN.get()).getDuration() : 0) / 20);
		}
		return "";
	}
}
