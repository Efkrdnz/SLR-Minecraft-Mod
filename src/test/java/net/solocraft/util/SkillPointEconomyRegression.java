package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pins the skill-point economy.
 *
 * <p>{@link SkillPointRules} is dependency-free, so the payouts and the
 * conversion rate are exercised directly rather than asserted as source text.
 * Only the wiring -- that the award sites actually read the rules -- is checked
 * by reading the sources.
 */
public final class SkillPointEconomyRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");
	private static final Pattern BOSS_TIER = Pattern.compile("bossSkillPoints\\((\\d+)\\)");

	private SkillPointEconomyRegression() {
	}

	public static void main(String[] args) throws IOException {
		questPayouts();
		bossFirstKillsStayInBand();
		conversionIsOneToOne();
		bulkInvestmentUsesAvailableRemainder();
		investmentNeverLosesOrInventsPoints();
		awakeningCompensatesForScarcePoints();
		awardSitesReadTheRules();
		lastPointStaysSpendable();
		investmentSizeIsAWholeNumber();
		everySurfaceReadsThePoolTheSameWay();
		System.out.println("Skill point economy regression checks passed.");
	}

	private static void questPayouts() {
		expect(SkillPointRules.DAILY_QUEST == 3,
				"The daily quest must award 3 skill points, found "
						+ SkillPointRules.DAILY_QUEST);
		expect(SkillPointRules.SECRET_DAILY_QUEST == 6,
				"The Secret daily doubles its objectives, so it must award 6, found "
						+ SkillPointRules.SECRET_DAILY_QUEST);
		expect(SkillPointRules.URGENT_QUEST_MIN == 2
						&& SkillPointRules.URGENT_QUEST_MAX == 4,
				"Urgent quests must roll 2-4 skill points");
	}

	private static void bossFirstKillsStayInBand() throws IOException {
		expect(SkillPointRules.bossFirstKill(0) == 3,
				"The weakest boss tier must award the 3-point floor");
		expect(SkillPointRules.bossFirstKill(7) == 10,
				"The strongest boss tier must award the 10-point ceiling");
		// Out-of-range tiers must clamp rather than escape the band.
		expect(SkillPointRules.bossFirstKill(-5) == SkillPointRules.BOSS_FIRST_KILL_MIN,
				"A negative tier must clamp to the floor");
		expect(SkillPointRules.bossFirstKill(999) == SkillPointRules.BOSS_FIRST_KILL_MAX,
				"An oversized tier must clamp to the ceiling");
		for (int tier = 0; tier < 7; tier++)
			expect(SkillPointRules.bossFirstKill(tier)
							< SkillPointRules.bossFirstKill(tier + 1),
					"A stronger boss must never award fewer points than a weaker one");

		// Every wired boss must land inside the band the user asked for.
		String rewards = read("procedures", "RewardGainAdvProcedure.java");
		Matcher matcher = BOSS_TIER.matcher(rewards);
		List<Integer> awards = new ArrayList<>();
		while (matcher.find())
			awards.add(SkillPointRules.bossFirstKill(Integer.parseInt(matcher.group(1))));
		expect(awards.size() >= 9,
				"Every first-kill boss reward must go through bossSkillPoints, found "
						+ awards.size());
		for (int award : awards)
			expect(award >= 3 && award <= 10,
					"A boss first-kill award escaped the 3-10 band: " + awards);
	}

	/**
	 * Skill points buy attributes one for one. Scarcity is the balance lever, and
	 * the compensating power lives in the awakening's starting stats.
	 */
	private static void conversionIsOneToOne() {
		for (int points = 0; points <= 25; points++) {
			expect(SkillPointRules.attributesFor(points) == points,
					points + " skill points must buy " + points + " attributes, got "
							+ SkillPointRules.attributesFor(points));
			expect(SkillPointRules.costFor(points) == points,
					points + " attributes must cost " + points + " points, got "
							+ SkillPointRules.costFor(points));
		}
		expect(SkillPointRules.MINIMUM_SPEND == 1,
				"At a one-for-one rate a single point must be spendable");
	}

	private static void bulkInvestmentUsesAvailableRemainder() {
		SkillPointRules.Investment five = SkillPointRules.resolveInvestment(5, 3);
		expect(five.skillPointsSpent() == 3 && five.attributesGained() == 3,
				"x5 must invest the remaining 3 points, got " + five);
		SkillPointRules.Investment ten = SkillPointRules.resolveInvestment(10, 7);
		expect(ten.skillPointsSpent() == 7 && ten.attributesGained() == 7,
				"x10 must invest the remaining 7 points, got " + ten);
	}

	/**
	 * The awakening grant offsets the scarcer skill points. If the conversion rate
	 * is ever retuned again this multiplier is the dial that keeps early
	 * progression whole, so it is pinned alongside it.
	 */
	private static void awakeningCompensatesForScarcePoints() throws IOException {
		expect(HunterEvaluationRewardService.STARTING_STAT_MULTIPLIER == 1.25D,
				"Evaluation starting stats must carry the 1.25 compensation multiplier");
		String service = read("util", "HunterEvaluationRewardService.java");
		expect(service.contains("Math.pow(rank, lore) * STARTING_STAT_MULTIPLIER"),
				"The rank curve must be scaled by the compensation multiplier");
		expect(service.contains("double flat = rank * lore * STARTING_STAT_MULTIPLIER;"),
				"The flat rank terms must be scaled too, not just the curve");
		expect(!service.contains("+= rank * lore"),
				"No starting-stat term may bypass the compensation multiplier");
		int scaledIntelligence = countOccurrences(service,
				"* STARTING_STAT_MULTIPLIER;");
		expect(scaledIntelligence >= 4,
				"Every Intelligence curve bonus must be scaled, found "
						+ scaledIntelligence + " scaled terms");
	}

	private static int countOccurrences(String haystack, String needle) {
		int count = 0;
		for (int at = haystack.indexOf(needle); at >= 0;
				at = haystack.indexOf(needle, at + needle.length()))
			count++;
		return count;
	}

	/**
	 * The invariant that matters in play: a press either does nothing, or charges
	 * exactly what it hands over. It must never take points and give no stat.
	 */
	private static void investmentNeverLosesOrInventsPoints() {
		for (int available = 0; available <= 40; available++) {
			for (int requested = 0; requested <= 40; requested++) {
				SkillPointRules.Investment result =
						SkillPointRules.resolveInvestment(requested, available);
				int spent = result.skillPointsSpent();
				int gained = result.attributesGained();
				expect(spent >= 0 && gained >= 0,
						"Investment must never go negative at " + requested + "/" + available);
				expect(spent <= available,
						"Investment spent " + spent + " of only " + available + " points");
				expect((spent == 0) == (gained == 0),
						"Investment must charge if and only if it grants; "
								+ requested + "/" + available + " gave " + result);
				if (gained > 0) {
					expect(spent >= SkillPointRules.costFor(gained),
							"Investment undercharged for " + gained + " attributes");
					expect(gained <= SkillPointRules.attributesFor(spent),
							"Investment granted more attributes than " + spent
									+ " points are worth");
				}
				if (available >= SkillPointRules.MINIMUM_SPEND)
					expect(gained > 0,
							"A player holding " + available
									+ " points must always be able to buy something");
			}
		}
	}

	private static void awardSitesReadTheRules() throws IOException {
		String daily = read("util", "daily", "DailyQuestLifecycleManager.java");
		expect(daily.contains("SkillPointRules.DAILY_QUEST")
						&& daily.contains("SkillPointRules.SECRET_DAILY_QUEST"),
				"Daily quest rewards must be derived from SkillPointRules");
		expect(!daily.contains("\"SP10\"") && !daily.contains("\"SP20\""),
				"The old hardcoded daily payouts must be gone");

		String urgent = read("util", "UrgentQuestManager.java");
		expect(urgent.contains("SkillPointRules.URGENT_QUEST_MIN"),
				"Urgent quest rewards must be derived from SkillPointRules");

		String helper = read("util", "StatInvestmentHelper.java");
		expect(helper.contains("SkillPointRules.resolveInvestment"),
				"Stat allocation must go through the shared conversion");
		for (String procedure : new String[] {
				"StrengthIncreaseProcedure", "SpeedIncreaseProcedure",
				"SenseIncreaseProcedure", "VitalityIncreaseProcedure",
				"IntelligenceIncreaseProcedure" }) {
			String source = read("procedures", procedure + ".java");
			expect(source.contains("StatInvestmentHelper.invest("),
					procedure + " must delegate to the shared stat investment helper");
		}
	}

	/**
	 * The bug this pins: the counter rounded the pool, the plus button tested
	 * {@code > 0}, and the spend floored. A pool of 0.6 therefore displayed as
	 * "1", lit the button, and bought nothing -- "I cannot spend my last skill
	 * point". All three now ask {@link SkillPointRules#spendable}.
	 */
	private static void lastPointStaysSpendable() {
		expect(SkillPointRules.spendable(0.6D) == 0,
				"A pool under one whole point must report nothing to spend, so the "
						+ "counter cannot advertise a point the spend will refuse");
		expect(SkillPointRules.spendable(1.0D) == 1,
				"A single whole point must be spendable");
		expect(SkillPointRules.spendable(2.9999999999999996D) == 3,
				"Double drift must not eat a point that arithmetic should have made whole");
		expect(SkillPointRules.spendable(-4.0D) == 0 && SkillPointRules.spendable(Double.NaN) == 0,
				"A corrupt pool must read as empty rather than as a negative spend");

		SkillPointRules.Investment last = SkillPointRules.resolveInvestment(1, 1);
		expect(last.skillPointsSpent() == 1 && last.attributesGained() == 1,
				"The final point must buy exactly one attribute for exactly one point");
	}

	/**
	 * {@code investvalue} is a double behind a free text box. A fractional one
	 * spent whole points while stranding a remainder that could never be spent,
	 * which is where the un-spendable last point came from.
	 */
	private static void investmentSizeIsAWholeNumber() {
		expect(SkillPointRules.investmentSize(2.5D) == 2,
				"A fractional investment size must floor rather than strand a remainder");
		expect(SkillPointRules.investmentSize(0.0D) == SkillPointRules.MINIMUM_SPEND,
				"An investment of zero must still buy one attribute, not nothing");
		expect(SkillPointRules.investmentSize(-3.0D) == SkillPointRules.MINIMUM_SPEND,
				"A negative investment size must not refund points");
		expect(SkillPointRules.investmentSize(1.0e9D) == SkillPointRules.MAX_INVESTMENT,
				"A huge investment size must clamp rather than overflow the spend");
		// Non-finite is corrupt rather than ambitious, so it takes the safe floor
		// rather than the ceiling -- a NaN must not spend ten thousand points.
		expect(SkillPointRules.investmentSize(Double.POSITIVE_INFINITY)
						== SkillPointRules.MINIMUM_SPEND
				&& SkillPointRules.investmentSize(Double.NaN) == SkillPointRules.MINIMUM_SPEND,
				"A corrupt investment size must fall back to the smallest spend");
	}

	/** Display, gate, and spend must not drift apart again. */
	private static void everySurfaceReadsThePoolTheSameWay() throws IOException {
		expect(read("procedures", "SkillPointsTextProcedure.java")
						.contains("SkillPointRules.spendable("),
				"The skill-point counter must show what the spend will accept");
		expect(read("procedures", "HasSkillPointsProcedure.java")
						.contains("SkillPointRules.spendable("),
				"The plus-button gate must agree with the spend");
		String helper = read("util", "StatInvestmentHelper.java");
		expect(helper.contains("SkillPointRules.spendable("),
				"The spend itself must read the pool through the shared rule");
		expect(helper.contains("SkillPointRules.investmentSize("),
				"The spend must clamp the text-box investment size");
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
