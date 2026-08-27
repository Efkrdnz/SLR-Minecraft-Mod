
package net.solocraft.potion;

import net.solocraft.procedures.HasteBuffOnEffectActiveTickProcedure;
import net.solocraft.procedures.HasteBuffEffectStartedappliedProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class HasteBuffMobEffect extends MobEffect {
	public HasteBuffMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -16726529);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.haste_buff";
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		HasteBuffEffectStartedappliedProcedure.execute(entity);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		HasteBuffOnEffectActiveTickProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ());
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
