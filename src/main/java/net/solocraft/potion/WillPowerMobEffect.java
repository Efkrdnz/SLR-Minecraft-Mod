
package net.solocraft.potion;

import net.solocraft.procedures.WillPowerOnEffectActiveTickProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class WillPowerMobEffect extends MobEffect {
	public WillPowerMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -2720512);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.will_power";
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		WillPowerOnEffectActiveTickProcedure.execute(entity.level(), entity.getX(), entity.getZ(), entity);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}
}
