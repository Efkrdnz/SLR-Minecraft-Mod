
package net.solocraft.potion;

import net.solocraft.procedures.SwordEnhanceOnEffectActiveTickProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class SwordEnhanceMobEffect extends MobEffect {
	public SwordEnhanceMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -13261);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.sword_enhance";
	}

	@Override
	public boolean isInstantenous() {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		SwordEnhanceOnEffectActiveTickProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
