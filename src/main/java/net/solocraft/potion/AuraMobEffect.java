
package net.solocraft.potion;

import net.solocraft.procedures.AuraOnEffectActiveTickProcedure;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class AuraMobEffect extends MobEffect {
	public AuraMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.aura";
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		AuraOnEffectActiveTickProcedure.execute(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
