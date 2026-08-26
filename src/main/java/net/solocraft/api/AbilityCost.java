package net.solocraft.api;

import net.solocraft.util.ManaRules;

/**
 * How heavy an ability is, which is what its mana cost is derived from.
 *
 * <p>Costs are a band rather than a number so an addon's abilities stay in step
 * with the mod's economy instead of drifting from it. Intelligence already
 * raises maximum mana, and cost is a fraction of that maximum, so an ability
 * must never add its own Intelligence term on top -- doing so would scale the
 * stat twice and make investment feel punishing.
 *
 * <p>Mirrors the internal band table rather than exposing it, so the internal
 * one stays free to change shape.
 */
public enum AbilityCost {
	/** Basic attack spell, mark, or stance toggle. */
	NOMINAL(ManaRules.Band.NOMINAL),
	/** Mobility, short control, single-target utility. */
	LOW(ManaRules.Band.LOW),
	/** Reliable burst, rescue, multi-target control. */
	MEDIUM(ManaRules.Band.MEDIUM),
	/** Major field, transformation, or protection domain. */
	HIGH(ManaRules.Band.HIGH),
	/** S-rank cinematic or long-duration ultimate. */
	APEX(ManaRules.Band.APEX);

	private final ManaRules.Band band;

	AbilityCost(ManaRules.Band band) {
		this.band = band;
	}

	ManaRules.Band band() {
		return band;
	}
}
