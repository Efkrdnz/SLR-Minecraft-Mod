package net.solocraft.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards the end of the progression: the cap, and the arc that closes it.
 *
 * <p>Levelling used to run to {@code Integer.MAX_VALUE}. The cap gives the climb
 * a summit and the Cartenon return turns that summit into an ending. The most
 * important thing to protect is the connection between the two -- a cap with no
 * reachable finale leaves {@code systemReleased} permanently unset, which is
 * exactly the half-built state this arc was written to fix.
 */
public final class LevelCapEndgameRegression {
	private static final Path SRC = Path.of("src", "main", "java", "net", "solocraft");

	private LevelCapEndgameRegression() {
	}

	public static void main(String[] args) throws Exception {
		capResolutionHandlesGarbage();
		theCurveMatchesTheClosedForm();
		levelNeverExceedsTheCap();
		theCapIsReachableAndTheFinaleFollows();
		releaseStopsProgressionButNotTheShop();
		theArcCanAlwaysBeRetried();
		posedStatuesActuallyFight();
		theArcSearchesTheWholeTemple();
		arcStateIsClearedByAProgressReset();
		System.out.println("LevelCapEndgameRegression passed");
	}

	/**
	 * The single most breakable thing in the arc.
	 *
	 * <p>CartenonTempleGenerator places every temple statue with
	 * {@code setNoAi(true)} so it stands as scenery. Waking the temple without
	 * clearing that sets a target on a frozen mob, and the finale becomes a room
	 * of statues standing still while the player hits them.
	 */
	private static void posedStatuesActuallyFight() throws Exception {
		String generator = Files.readString(SRC.resolve("util/CartenonTempleGenerator.java"));
		if (!generator.contains("setNoAi(true)"))
			throw new AssertionError("the generator no longer poses statues, so this "
					+ "guard has stopped measuring anything");
		String finale = Files.readString(SRC.resolve("util/CartenonFinaleManager.java"));
		if (!finale.contains("setNoAi(false)"))
			throw new AssertionError("woken statues keep the generator's disabled AI "
					+ "and will stand frozen through the entire finale");
		// The intro marker hard-disables the Statue of God's only attack goal.
		if (!finale.contains("STORY_STATUE_TAG"))
			throw new AssertionError("the finale never clears the story marker, which "
					+ "disables the Statue of God's melee goal outright");
		// It has no target selector of its own; the intro drove it by script.
		if (!finale.contains("retargetGod"))
			throw new AssertionError("nothing re-targets the Statue of God, so it "
					+ "stops fighting the moment its target clears");
	}

	/** The temple is 154 blocks deep; a player-radius search misses most of it. */
	private static void theArcSearchesTheWholeTemple() throws Exception {
		String finale = Files.readString(SRC.resolve("util/CartenonFinaleManager.java"));
		if (finale.contains("getBoundingBox().inflate("))
			throw new AssertionError("the arc searches a radius around the player "
					+ "again; guardians at the far end of the hall and the Statue of "
					+ "God on its dais will never be found");
		if (!finale.contains("CartenonTempleManager.instanceBounds("))
			throw new AssertionError("the arc does not search the instance footprint");
	}

	/**
	 * PlayerResetKeyPolicy preserves the "slr_cartenon_" prefix to protect
	 * awakening state. Arc state must not hide behind it, or a progress reset
	 * leaves the player stuck mid-run or staring at an armed release prompt.
	 */
	private static void arcStateIsClearedByAProgressReset() throws Exception {
		String policy = Files.readString(SRC.resolve("util/PlayerResetKeyPolicy.java"));
		if (!policy.contains("\"slr_cartenon_\""))
			throw new AssertionError("the preserved prefix changed; re-check where "
					+ "the arc's player tags live");
		String finale = Files.readString(SRC.resolve("util/CartenonFinaleManager.java"));
		String temple = Files.readString(SRC.resolve("util/CartenonTempleManager.java"));
		for (String tag : new String[] {"STAGE_TAG = ", "LINE_TAG = ", "INSTANCE_TAG = ",
				"STARTED_AT_TAG = "}) {
			int at = finale.indexOf(tag);
			if (at < 0)
				throw new AssertionError("missing arc tag " + tag);
			if (finale.startsWith("\"slr_cartenon_", at + tag.length()))
				throw new AssertionError(tag + "is under the preserved prefix, so a "
						+ "progress reset would strand the player mid-arc");
		}
		int release = temple.indexOf("RELEASE_PENDING_TAG = ");
		if (release >= 0 && temple.startsWith("\"slr_cartenon_",
				release + "RELEASE_PENDING_TAG = ".length()))
			throw new AssertionError("the release prompt tag survives a progress "
					+ "reset and would stay armed forever");
	}

	private static void capResolutionHandlesGarbage() {
		if (!LevelCapRules.isUnlimited(LevelCapRules.resolveCap(0)))
			throw new AssertionError("zero must mean unlimited");
		// Negative caps most likely mean a command typo. Pinning the player to
		// level 1 would be far worse than leaving progression open.
		for (int bad : new int[] {-1, -150, Integer.MIN_VALUE}) {
			if (!LevelCapRules.isUnlimited(LevelCapRules.resolveCap(bad)))
				throw new AssertionError("a negative cap of " + bad + " capped the player");
		}
		if (LevelCapRules.resolveCap(Integer.MAX_VALUE) != LevelCapRules.MAXIMUM_LEVEL_CAP)
			throw new AssertionError("an absurd cap was not clamped");
		if (LevelCapRules.isCapped(10, LevelCapRules.UNLIMITED))
			throw new AssertionError("an unlimited world capped a player");
		if (LevelCapRules.resolveCap(LevelCapRules.DEFAULT_LEVEL_CAP)
				!= LevelCapRules.DEFAULT_LEVEL_CAP)
			throw new AssertionError("the shipped default does not survive resolution");
	}

	/** The closed form must match the loop the level-up actually walks. */
	private static void theCurveMatchesTheClosedForm() {
		double walked = 0.0D;
		for (int level = 0; level < 200; level++) {
			if (Math.abs(LevelCapRules.cumulativeXpFor(level) - walked) > 1.0E-6D)
				throw new AssertionError("8N^2 disagrees with the real curve at level "
						+ level + ": " + LevelCapRules.cumulativeXpFor(level)
						+ " vs " + walked);
			walked += LevelCapRules.requiredXpFor(level);
		}
		double toCap = LevelCapRules.xpRemainingTo(120, LevelCapRules.DEFAULT_LEVEL_CAP);
		if (toCap <= 0.0D)
			throw new AssertionError("the last stretch costs nothing");
		if (LevelCapRules.xpRemainingTo(150, LevelCapRules.DEFAULT_LEVEL_CAP) != 0.0D)
			throw new AssertionError("a capped player still has XP left to earn");
		if (LevelCapRules.xpRemainingTo(10, LevelCapRules.UNLIMITED) != 0.0D)
			throw new AssertionError("an unlimited world reports a finite goal");
	}

	private static void levelNeverExceedsTheCap() {
		for (int cap : new int[] {10, 100, 150, 300}) {
			for (int level = 0; level <= 400; level++) {
				int clamped = LevelCapRules.clampLevel(level, cap);
				if (clamped > cap)
					throw new AssertionError("level " + level + " survived cap " + cap);
				if (level <= cap && clamped != level)
					throw new AssertionError("a sub-cap level was altered");
				if (!LevelCapRules.isCapped(cap, cap))
					throw new AssertionError("reaching the cap does not read as capped");
			}
		}
	}

	/**
	 * The connection that matters. A cap with no finale, or a finale nothing can
	 * open, leaves the whole release path dead.
	 */
	private static void theCapIsReachableAndTheFinaleFollows() throws Exception {
		String levelUp = Files.readString(SRC.resolve("procedures/LevelUpProcedure.java"));
		if (!levelUp.contains("LevelCapRules.isCapped"))
			throw new AssertionError("the level-up loop is unbounded again");

		String authority = Files.readString(SRC.resolve("util/SystemAuthorityManager.java"));
		if (!authority.contains("isFinaleAvailable"))
			throw new AssertionError("nothing reports that the finale is available");

		String temple = Files.readString(SRC.resolve("util/CartenonTempleManager.java"));
		if (!temple.contains("offerFinaleSummons"))
			throw new AssertionError("reaching the cap never offers the return");
		if (!temple.contains("CartenonFinaleManager.begin("))
			throw new AssertionError("entering the finale instance never starts the arc");

		String finale = Files.readString(SRC.resolve("util/CartenonFinaleManager.java"));
		if (!finale.contains("SystemAuthorityManager.release("))
			throw new AssertionError("the arc never releases the System, so "
					+ "systemReleased can still never be set");
		if (!finale.contains("TrueMonarchRules.rewardForJob"))
			throw new AssertionError("the release grants no reward");
		// Both statue phases have to exist, or the arc collapses to a cutscene.
		for (String guardian : new String[] {"StatueswordEntity", "StatueaxeEntity",
				"StatuehammerEntity", "StatueOfGodEntity"}) {
			if (!finale.contains(guardian))
				throw new AssertionError("the arc never uses " + guardian);
		}
	}

	private static void releaseStopsProgressionButNotTheShop() throws Exception {
		record Gate(String file, String label) {
		}
		Gate[] mustStop = {
				new Gate("procedures/XPGainProcedure.java", "XP"),
				new Gate("util/daily/DailyQuestLifecycleManager.java", "daily quests"),
				new Gate("util/UrgentQuestManager.java", "urgent quests"),
				new Gate("procedures/RewardGainAdvProcedure.java", "boss rewards"),
		};
		for (Gate gate : mustStop) {
			String source = Files.readString(SRC.resolve(gate.file()));
			if (!source.contains("systemReleased") && !source.contains("isReleased("))
				throw new AssertionError(gate.label() + " keep flowing after the "
						+ "System is released");
		}
		// Spending what you already banked has to keep working, or the ending
		// reads as a punishment rather than a trade.
		String investment = Files.readString(SRC.resolve("util/StatInvestmentHelper.java"));
		if (investment.contains("systemReleased") || investment.contains("isReleased("))
			throw new AssertionError("releasing the System also blocked spending "
					+ "banked skill points");
	}

	/** Declining or walking out must leave the ending reachable later. */
	private static void theArcCanAlwaysBeRetried() throws Exception {
		String finale = Files.readString(SRC.resolve("util/CartenonFinaleManager.java"));
		if (!finale.contains("cancelFinaleOffer"))
			throw new AssertionError("declining or abandoning the arc never reopens "
					+ "the offer, making the ending permanently missable");
		String saved = Files.readString(SRC.resolve("util/CartenonProgressSavedData.java"));
		for (String field : new String[] {"FinaleOffered", "FinaleResolved", "FinaleInstance"}) {
			if (!saved.contains(field))
				throw new AssertionError(field + " is not persisted, so the finale "
						+ "state is lost on restart");
		}
	}
}
