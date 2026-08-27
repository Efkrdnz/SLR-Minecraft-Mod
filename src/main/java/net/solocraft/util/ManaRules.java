package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Single source of truth for maximum mana and native ability cost bands.
 *
 * <p>Before this class every manager recomputed {@code 1000 + 100 * INT} as a
 * literal, and legacy procedures hardcoded flat costs that had drifted apart.
 * The bands below are calibrated so that at {@code INT = 0} each flat floor
 * reproduces a cost the game already charged, which keeps existing abilities
 * feeling identical while letting them scale afterwards.</p>
 *
 * <p>Intelligence already raises {@link #maximumMana}, so no ability may add a
 * second direct {@code + stat * k} term to its cost. Structural growth belongs
 * in stage load and effect load instead.</p>
 */
public final class ManaRules {
	/** Mana granted before any Intelligence contribution. */
	public static final double BASE_MAXIMUM_MANA = 1000.0D;
	/** Maximum mana added per point of effective Intelligence. */
	public static final double MANA_PER_INTELLIGENCE = 100.0D;

	/** Per-stage cost growth shared by every staged ability, stages 1..5. */
	private static final double[] STAGE_LOAD = {0.0D, 1.00D, 1.10D, 1.20D, 1.30D, 1.40D};

	private ManaRules() {
	}

	/**
	 * Cost bands expressed as a fraction of maximum mana plus a flat floor. The
	 * floor dominates for low-Intelligence hunters so physical classes keep
	 * predictable costs.
	 */
	public enum Band {
		/** Basic attack spell, mark, or stance toggle. */
		NOMINAL(0.0035D, 20),
		/** Mobility, short control, single-target utility. */
		LOW(0.020D, 80),
		/** Reliable burst, rescue, multi-target control. */
		MEDIUM(0.040D, 200),
		/** Major field, transformation, or protection domain. */
		HIGH(0.090D, 600),
		/** S-rank cinematic or long-duration ultimate. */
		APEX(0.180D, 1500);

		private final double fraction;
		private final int flatFloor;

		Band(double fraction, int flatFloor) {
			this.fraction = fraction;
			this.flatFloor = flatFloor;
		}

		public double fraction() {
			return fraction;
		}

		public int flatFloor() {
			return flatFloor;
		}
	}

	/**
	 * The Intelligence-derived pool. <b>Every mana cost derives from this.</b>
	 *
	 * <p>Split out from {@link #maximumMana} when the Black Heart introduced a
	 * flat reservoir grant. Costs in this mod are fractions of a pool, so pricing
	 * anything against a reservoir that a capstone reward inflates by a hundred
	 * thousand would multiply that cost by roughly twenty-five and quietly make
	 * the ability unusable for exactly the player who earned the reward. If a
	 * call site is deciding what something <em>costs</em>, it belongs here.
	 */
	public static double costBasis(Entity entity) {
		if (entity == null)
			return BASE_MAXIMUM_MANA;
		return maximumManaFor(TemporaryStatBonusManager.effectiveIntelligence(entity));
	}

	/** Flat, non-Intelligence reservoir grants. Never a cost basis. */
	public static double flatManaBonus(Entity entity) {
		if (entity == null)
			return 0.0D;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> TrueMonarchRules.flatManaBonus(variables.trueMonarchHeart))
				.orElse(0.0D);
	}

	/**
	 * Effective maximum mana: the Intelligence pool plus any flat grants. This is
	 * the reservoir the bar shows and the ceiling restoration fills toward.
	 */
	public static double maximumMana(Entity entity) {
		return costBasis(entity) + flatManaBonus(entity);
	}

	/**
	 * Maximum mana derived from an already-resolved Intelligence value. Callers
	 * that have snapshotted the stat should use this so one cast cannot read two
	 * different Intelligence values.
	 */
	public static double maximumManaFor(double intelligence) {
		return BASE_MAXIMUM_MANA
				+ MANA_PER_INTELLIGENCE * Math.max(0.0D, intelligence);
	}

	/** Stage multiplier for stages 1..5; out-of-range stages clamp. */
	public static double stageLoad(int stage) {
		return STAGE_LOAD[Math.max(1, Math.min(5, stage))];
	}

	/**
	 * Bounded effect load for abilities that accept more than one target. The
	 * first target is free; each additional accepted target adds 15 percent, and
	 * the count is clamped to the ability's own cap before it reaches here.
	 */
	public static double effectLoad(int acceptedTargets) {
		return 1.0D + 0.15D * Math.max(0, acceptedTargets - 1);
	}

	/** Base cost for a band, before stage and effect load. */
	public static int cost(Entity entity, Band band) {
		return cost(entity, band, 1, 1, 1.0D);
	}

	/**
	 * Full cost contract:
	 * {@code max(floor, MMP * band) * stageLoad * effectLoad * execution}.
	 * Creative players always pay nothing, matching the existing managers.
	 */
	public static int cost(Entity entity, Band band, int stage,
			int acceptedTargets, double executionModifier) {
		if (isFree(entity))
			return 0;
		return costFor(
				entity == null ? 0.0D
						: TemporaryStatBonusManager.effectiveIntelligence(entity),
				band, stage, acceptedTargets, executionModifier);
	}

	/**
	 * Dependency-free cost core. Intelligence enters the formula exactly once,
	 * through maximum mana; no caller may add a second direct stat term.
	 */
	public static int costFor(double intelligence, Band band, int stage,
			int acceptedTargets, double executionModifier) {
		if (band == null)
			return 0;
		double base = Math.max(band.flatFloor(),
				maximumManaFor(intelligence) * band.fraction());
		double total = base * stageLoad(stage) * effectLoad(acceptedTargets)
				* Math.max(0.0D, executionModifier);
		return (int) Math.max(0.0D, Math.ceil(total));
	}

	/** Convenience overload for the common single-target, stage-one case. */
	public static int costFor(double intelligence, Band band) {
		return costFor(intelligence, band, 1, 1, 1.0D);
	}

	/** Creative mode remains free and cooldown-free across all ability paths. */
	public static boolean isFree(Entity entity) {
		return entity instanceof Player player && player.isCreative();
	}

	/** Current spendable mana. */
	public static double currentMana(Entity entity) {
		if (entity == null)
			return 0.0D;
		return entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).MP;
	}

	/** True when the caster can pay, or is creative. */
	public static boolean canAfford(Entity entity, int cost) {
		return cost <= 0 || isFree(entity) || currentMana(entity) >= cost;
	}

	/**
	 * Deducts mana only when the caster can actually pay. Returning false leaves
	 * mana untouched so a failed preflight never charges a partial cost.
	 */
	public static boolean spend(Entity entity, int cost) {
		if (entity == null)
			return false;
		if (cost <= 0 || isFree(entity))
			return true;
		if (currentMana(entity) < cost)
			return false;
		entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.MP = Math.max(0.0D, capability.MP - cost);
					capability.syncPlayerVariables(entity);
				});
		return true;
	}
}
