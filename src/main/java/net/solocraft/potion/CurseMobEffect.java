package net.solocraft.potion;

import net.solocraft.SololevelingMod;
import net.solocraft.util.CurseType;
import net.solocraft.util.CurseEffectHooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * One shared implementation for every Curse Mage curse.
 *
 * <p>Curses were previously plain persistent-data with a manual sweep over every
 * living entity each tick. Moving them onto the effect system hands duration,
 * expiry, saving, particles and the inventory icon to vanilla, and lets it tick
 * only the entities that actually carry a curse.
 *
 * <p>Ownership is the one thing an effect cannot carry: a {@link net.minecraft.world.effect.MobEffectInstance}
 * has no notion of who applied it, and Culling, Mana Rot and every cooldown need
 * that. So the owner stays in the target's persistent data and this class is the
 * source of truth for presence and timing.
 */
public class CurseMobEffect extends MobEffect {
	private final CurseType curse;

	public CurseMobEffect(CurseType curse) {
		super(MobEffectCategory.HARMFUL, curse.accentColor());
		this.curse = curse;
		// Passive curses express themselves as attribute modifiers so their
		// strength is visible in the effect tooltip rather than hidden in a second
		// vanilla effect stapled alongside.
		switch (curse) {
			case ENFEEBLEMENT -> addAttributeModifier(Attributes.ATTACK_DAMAGE,
					modifierId("enfeeblement"), -0.30D,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			case LEADEN -> addAttributeModifier(Attributes.MOVEMENT_SPEED,
					modifierId("leaden"), -0.30D,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			default -> {
			}
		}
	}

	public CurseType curse() {
		return curse;
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.curse_" + curse.key();
	}

	/** Only the draining curses do per-tick work; the rest are states. */
	@Override
	public boolean isInstantenous() {
		return ticks();
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return ticks();
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		CurseEffectHooks.tick(entity, curse, amplifier);
		return true;
	}

	private boolean ticks() {
		return curse == CurseType.WITHERING || curse == CurseType.MANA_ROT;
	}

	private static ResourceLocation modifierId(String name) {
		return ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
				"curse_" + name);
	}
}
