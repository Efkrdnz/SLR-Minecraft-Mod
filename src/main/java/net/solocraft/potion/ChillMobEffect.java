package net.solocraft.potion;

import net.solocraft.util.FrostChillRules;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;

/**
 * The visible face of {@link FrostChillRules}.
 *
 * <p>The chill meter itself lives on the server. This effect mirrors only the
 * <em>stage</em>, carried in the amplifier, which buys three things for free:
 * vanilla syncs it to the client so the screen frost can scale without a custom
 * packet, the player gets an inventory icon telling them how cold they are, and
 * the penalties show up as ordinary attribute modifiers in the tooltip instead
 * of being hidden inside boss code.
 *
 * <p>The modifiers are deliberately flat across stages -- the escalation comes
 * from the manager stacking vanilla slowness and mining fatigue on top, so a
 * player reads their situation from effects they already understand.
 */
public class ChillMobEffect extends MobEffect {
	public ChillMobEffect() {
		super(MobEffectCategory.HARMFUL, 0xFF9FD8FF);
		addAttributeModifier(Attributes.MOVEMENT_SPEED,
				ResourceLocation.parse("sololeveling:chill_movement"), -0.06D,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.ATTACK_SPEED,
				ResourceLocation.parse("sololeveling:chill_attack_speed"), -0.08D,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		// The manager owns every per-tick consequence; this effect is state, not
		// behaviour. Ticking here would double-apply the penalties.
		return false;
	}
}
