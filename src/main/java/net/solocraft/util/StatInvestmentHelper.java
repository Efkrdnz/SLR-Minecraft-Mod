package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

/**
 * Applies a stat plus-button press.
 *
 * <p>The five stat procedures were byte-identical apart from the field they
 * wrote, which meant the conversion rate lived in five places. It lives in
 * {@link SkillPointRules} now and they all route through here.
 */
public final class StatInvestmentHelper {
	public enum Stat {
		STRENGTH,
		AGILITY,
		PERCEPTION,
		VITALITY,
		INTELLIGENCE
	}

	private StatInvestmentHelper() {
	}

	/** Perception has always been capped; the other four are open-ended. */
	public static final int PERCEPTION_CAP = 100;

	public static void invest(Entity entity, Stat stat) {
		if (entity == null || stat == null)
			return;
		SololevelingModVariables.PlayerVariables current = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		invest(entity, stat, SkillPointRules.investmentSize(current.investvalue));
	}

	/**
	 * Applies an explicit investment size supplied by a server-bound UI action.
	 * The shared rules clamp it to the points the player actually has.
	 */
	public static void invest(Entity entity, Stat stat, int requested) {
		if (entity == null || stat == null)
			return;
		SololevelingModVariables.PlayerVariables current = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		SkillPointRules.Investment investment = SkillPointRules.resolveInvestment(
				requested,
				SkillPointRules.spendable(current.SkillPoints));
		if (investment.isEmpty())
			return;

		int gain = investment.attributesGained();
		if (stat == Stat.PERCEPTION) {
			int headroom = PERCEPTION_CAP - (int) Math.floor(current.perception);
			gain = Math.min(gain, Math.max(0, headroom));
			if (gain <= 0)
				return;
		}
		// Charge for what is actually granted, so a capped Perception press cannot
		// bill the player for attributes it refused to hand over.
		int cost = Math.min(SkillPointRules.costFor(gain),
				investment.skillPointsSpent());
		int granted = gain;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					// Snap to a whole number: a pool that drifts fractional is exactly what
					// made the last point undisplayable-but-unspendable.
					capability.SkillPoints = Math.max(0.0D,
							SkillPointRules.spendable(capability.SkillPoints) - cost);
					switch (stat) {
						case STRENGTH -> capability.Strength += granted;
						case AGILITY -> capability.Speed += granted;
						case PERCEPTION -> capability.perception += granted;
						case VITALITY -> capability.Vitality += granted;
						case INTELLIGENCE -> capability.Intelligence += granted;
					}
					capability.syncPlayerVariables(entity);
				});
	}
}
