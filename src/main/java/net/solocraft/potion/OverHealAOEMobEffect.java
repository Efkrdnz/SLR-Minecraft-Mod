
package net.solocraft.potion;

import net.solocraft.procedures.OverhealProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class OverHealAOEMobEffect extends MobEffect {
	public OverHealAOEMobEffect() {
		super(MobEffectCategory.NEUTRAL, -15561185);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.over_heal_aoe";
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		OverhealProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
