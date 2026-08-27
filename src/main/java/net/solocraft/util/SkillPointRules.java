package net.solocraft.util;

/**
 * One place to tune the skill-point economy: how many points the System hands
 * out, and how much attribute a point actually buys.
 *
 * <p>Dependency-free on purpose so the balance can be exercised directly by the
 * regression rather than inferred from the procedures that consume it.
 */
public final class SkillPointRules {
	/** Completing the daily quest. */
	public static final int DAILY_QUEST = 3;

	/** The Secret daily doubles every objective, so it doubles the payout too. */
	public static final int SECRET_DAILY_QUEST = DAILY_QUEST * 2;

	/** Inclusive band for the random urgent-quest skill-point roll. */
	public static final int URGENT_QUEST_MIN = 2;
	public static final int URGENT_QUEST_MAX = 4;

	/** Bounds for a first-time boss kill, scaled by how dangerous the boss is. */
	public static final int BOSS_FIRST_KILL_MIN = 3;
	public static final int BOSS_FIRST_KILL_MAX = 10;

	/**
	 * Attributes bought per {@link #SKILL_POINTS_PER_BLOCK} skill points.
	 *
	 * <p>One for one. The scarcity of skill points is the balance lever now, not
	 * the exchange rate, and the compensating power sits in the evaluation's
	 * starting stats instead. The ratio is still expressed as a pair so a future
	 * retune only has to touch these two numbers -- everything downstream reads
	 * {@link #attributesFor} and {@link #costFor}.
	 */
	public static final int ATTRIBUTES_PER_BLOCK = 1;
	public static final int SKILL_POINTS_PER_BLOCK = 1;

	/**
	 * The smallest spend that still buys a whole attribute. At a one-for-one rate
	 * that is a single point; a coarser rate would raise it so an investment can
	 * never consume points for no gain.
	 */
	public static final int MINIMUM_SPEND = 1;

	private SkillPointRules() {
	}

	/**
	 * Absorbs double drift when deciding how many whole points a player holds.
	 *
	 * <p>Skill points are stored as a double because every player variable is, so
	 * a value that should be 3 can arrive as 2.9999999999999996 and a value that
	 * should be 0 can arrive as a stray fraction. Flooring alone turns the first
	 * into 2; ignoring the drift entirely turns the second into a point that can
	 * be displayed but never spent.
	 */
	public static final double DRIFT_TOLERANCE = 1.0e-6;

	/** Investment sizes above this are a typo rather than an intention. */
	public static final int MAX_INVESTMENT = 10_000;

	/**
	 * Whole skill points a player can actually spend.
	 *
	 * <p>Every surface that reads the pool -- the counter on the panel, the gate
	 * on the plus button, and the spend itself -- has to go through here. They
	 * used to disagree: the counter rounded, the gate tested {@code > 0}, and the
	 * spend floored, so a pool of 0.6 displayed as "1", lit the button, and then
	 * refused to buy anything. That is the whole of "I cannot spend my last
	 * skill point".
	 */
	public static int spendable(double skillPoints) {
		if (!Double.isFinite(skillPoints) || skillPoints <= 0.0D)
			return 0;
		return (int) Math.floor(skillPoints + DRIFT_TOLERANCE);
	}

	/**
	 * The investment size a stat button should use.
	 *
	 * <p>Clamped to a whole number of at least one. The field behind it is a
	 * double fed by a free text box, so it could hold 2.5 -- which spent two
	 * points, granted two attributes, and left half a point stranded in the pool
	 * forever. It could also hold 0, which bought nothing at all.
	 */
	public static int investmentSize(double raw) {
		if (!Double.isFinite(raw) || raw < MINIMUM_SPEND)
			return MINIMUM_SPEND;
		return (int) Math.min(MAX_INVESTMENT, Math.floor(raw + DRIFT_TOLERANCE));
	}

	/** Skill points a boss of the given danger tier awards on its first kill. */
	public static int bossFirstKill(int tier) {
		int span = BOSS_FIRST_KILL_MAX - BOSS_FIRST_KILL_MIN;
		int clamped = Math.max(0, Math.min(span, tier));
		return BOSS_FIRST_KILL_MIN + clamped;
	}

	/** Whole attributes bought by spending this many skill points. */
	public static int attributesFor(int skillPoints) {
		if (skillPoints <= 0)
			return 0;
		return skillPoints * ATTRIBUTES_PER_BLOCK / SKILL_POINTS_PER_BLOCK;
	}

	/** The cheapest spend that buys exactly this many attributes. */
	public static int costFor(int attributes) {
		if (attributes <= 0)
			return 0;
		return Math.ceilDiv(attributes * SKILL_POINTS_PER_BLOCK, ATTRIBUTES_PER_BLOCK);
	}

	/**
	 * Resolves one press of a stat's plus button.
	 *
	 * @param requested the player's configured investment size
   * @param available the player's unspent skill points
	 * @return how many points to charge and how many attributes to grant; both
	 *         zero when the player cannot afford a whole attribute
	 */
	public static Investment resolveInvestment(int requested, int available) {
		int budget = Math.min(Math.max(0, requested), Math.max(0, available));
		if (budget < MINIMUM_SPEND)
			budget = Math.min(MINIMUM_SPEND, Math.max(0, available));
		int attributes = attributesFor(budget);
		if (attributes <= 0)
			return new Investment(0, 0);
		// Charge only what the granted attributes are worth so the remainder of an
		// awkward budget stays in the pool instead of evaporating.
		return new Investment(Math.min(costFor(attributes), budget), attributes);
	}

	/** @param skillPointsSpent points to deduct @param attributesGained stat gain */
	public record Investment(int skillPointsSpent, int attributesGained) {
		public boolean isEmpty() {
			return skillPointsSpent <= 0 || attributesGained <= 0;
		}
	}
}
