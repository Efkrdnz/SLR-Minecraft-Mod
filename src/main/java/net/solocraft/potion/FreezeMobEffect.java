
package net.solocraft.potion;

import net.solocraft.procedures.FreezeOnEffectActiveTickProcedure;
import net.solocraft.procedures.FreezeEffectStartedappliedProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class FreezeMobEffect extends MobEffect {
	public FreezeMobEffect() {
		super(MobEffectCategory.HARMFUL, -8848641);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.freeze";
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		FreezeEffectStartedappliedProcedure.execute(entity);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		FreezeOnEffectActiveTickProcedure.execute(entity);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
