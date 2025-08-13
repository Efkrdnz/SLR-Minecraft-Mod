
package net.solocraft.potion;

import net.solocraft.procedures.FreezeOnEffectActiveTickProcedure;
import net.solocraft.procedures.FreezeEffectStartedappliedProcedure;
import net.solocraft.procedures.FreezeEffectExpiresProcedure;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
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
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		FreezeEffectStartedappliedProcedure.execute(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		FreezeOnEffectActiveTickProcedure.execute(entity);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		FreezeEffectExpiresProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ());
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
