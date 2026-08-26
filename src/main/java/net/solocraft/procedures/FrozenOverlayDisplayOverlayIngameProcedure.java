package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.FrostChillRules;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class FrozenOverlayDisplayOverlayIngameProcedure {
	/** Fully opaque frost is reserved for the hard freeze. */
	private static final float FREEZE_OPACITY = 1.0F;

	public static boolean execute(Entity entity) {
		return opacity(entity) > 0.0F;
	}

	/**
	 * How strongly the frost pane should be drawn, 0 to 1.
	 *
	 * <p>The overlay used to be all-or-nothing on the hard freeze, which meant
	 * the ambient cold of Sillad's domain was invisible until it had already
	 * won. Scaling with the chill stage makes the screen itself the readout: the
	 * player watches the edges creep in and can decide to go find some warmth.
	 */
	public static float opacity(Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return 0.0F;
		if (living.hasEffect(SololevelingModMobEffects.FREEZE))
			return FREEZE_OPACITY;
		MobEffectInstance chill = living.getEffect(SololevelingModMobEffects.CHILL);
		if (chill == null)
			return 0.0F;
		return switch (FrostChillRules.stageForAmplifier(chill.getAmplifier())) {
			case CHILLED -> 0.18F;
			case NUMB -> 0.38F;
			case FROSTBOUND -> 0.62F;
			case GLACIAL -> 0.82F;
			default -> 0.0F;
		};
	}
}
