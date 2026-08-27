
package net.solocraft.potion;

import net.solocraft.procedures.SwordOfLightOnEffectActiveTickProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class SwordDanceMobEffect extends MobEffect {
	public SwordDanceMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.sword_dance";
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		SwordOfLightOnEffectActiveTickProcedure.execute(entity.level(), entity.getX(), entity.getZ(), entity);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
