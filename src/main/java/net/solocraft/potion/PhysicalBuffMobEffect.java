
package net.solocraft.potion;

import net.solocraft.procedures.PhysicalBuffOnEffectActiveTickProcedure;
import net.solocraft.procedures.PhysicalBuffEffectStartedappliedProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class PhysicalBuffMobEffect extends MobEffect {
	public PhysicalBuffMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -37888);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.physical_buff";
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		PhysicalBuffEffectStartedappliedProcedure.execute(entity);
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		PhysicalBuffOnEffectActiveTickProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ());
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
