package net.solocraft.api;

import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;

/**
 * Read-only access to a hunter's System record.
 *
 * <p>{@link HunterStats} covers the five combat stats an ability scales off.
 * This covers everything else an addon might branch on: how far the hunter has
 * come, what they have cleared, and what the System currently wants from them.
 *
 * <p>Reading only. Progression is the mod's to award -- an addon that could
 * grant ranks or clear a dungeon would be writing the mod's story for it, and
 * two mods disagreeing about whether a boss is dead is not a state worth having.
 *
 * <p>Safe on both sides: everything here rides the player attachment, which is
 * synchronised, so the client sees the same answers for drawing an overlay.
 */
public final class HunterProgress {
	private HunterProgress() {
	}

	/** Unspent skill points. */
	public static int skillPoints(Entity entity) {
		return (int) Math.floor(read(entity, variables -> variables.SkillPoints));
	}

	/** System level. */
	public static int level(Entity entity) {
		return (int) Math.floor(read(entity, variables -> variables.Level));
	}

	/** Hunter rank as the System shows it, for example {@code E} or {@code S}. */
	public static String rank(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables == null || variables.ranking == null ? "" : variables.ranking;
	}

	/**
	 * Rank as a number, for comparisons.
	 *
	 * <p>Compare against another hunter's rather than against a literal: the
	 * numbering is the mod's and is not part of this contract.
	 */
	public static int rankNumber(Entity entity) {
		return (int) Math.floor(read(entity, variables -> variables.rankingnum));
	}

	/** Rank of the hunter's licence, which can lag behind their true rank. */
	public static int hunterRank(Entity entity) {
		return (int) Math.floor(read(entity, variables -> variables.HunterRank));
	}

	/** True once this hunter has killed any boss the System counted. */
	public static boolean hasKilledBoss(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables != null && variables.BossKilled;
	}

	// ── Demon King's Castle ──────────────────────────────────────────────────

	public static boolean isCastleUnlocked(Entity entity) {
		return read(entity, variables -> variables.dkc_unlocked) > 0.0D;
	}

	public static boolean isCastleStarted(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables != null && variables.dkc_started;
	}

	/** How many times the castle has been cleared. */
	public static int castleClears(Entity entity) {
		return (int) Math.floor(read(entity, variables -> variables.dkc_cleared));
	}

	// ── Daily quest ──────────────────────────────────────────────────────────

	/** True while a daily quest is running. */
	public static boolean hasActiveDailyQuest(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables != null && variables.ActiveDaily;
	}

    /**
     * True when the running daily is the Secret variant.
     *
     * <p>The Secret daily doubles every objective and its payout, so an addon
     * scaling a reward off the daily should ask rather than assume.
     */
	public static boolean isSecretDailyQuest(Entity entity) {
		return read(entity, variables -> variables.dailysecrettrans) > 1.0D;
	}

	/** Objectives completed in the running daily. */
	public static int dailyQuestTasksDone(Entity entity) {
		return (int) Math.floor(read(entity, variables -> variables.dailytasks));
	}

	/** True while the hunter is inside daily training. */
	public static boolean isInDailyTraining(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables != null && variables.isdailytraining;
	}

	private static double read(Entity entity,
			java.util.function.ToDoubleFunction<SololevelingModVariables.PlayerVariables> getter) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables == null ? 0.0D : getter.applyAsDouble(variables);
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
