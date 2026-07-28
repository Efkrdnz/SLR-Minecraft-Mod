package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free balance and wiring regressions for contribution-based shadow
 * experience.
 */
public final class ShadowExperienceRulesRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCES = Path.of(
			"src", "main", "resources");

	private ShadowExperienceRulesRegression() {
	}

	public static void main(String[] args) throws IOException {
		targetStrengthControlsThePool();
		damageContributionControlsTheShare();
		earlyCatchUpFadesOut();
		runtimeTracksDamageInsteadOfFinalBlows();
		bossCoverageIncludesCurrentEncounters();
	}

	private static void targetStrengthControlsThePool() {
		int normal = ShadowExperienceRules.targetXpPool(
				20.0D, 3.0D, 2.0D, 0.0D, 0.0D,
				-1, false, false, false);
		int boss = ShadowExperienceRules.targetXpPool(
				150.0D, 14.0D, 18.0D, 4.0D, 20.0D,
				-1, true, true, false);
		int configuredBoss = ShadowExperienceRules.targetXpPool(
				20.0D, 3.0D, 2.0D, 0.0D, 0.0D,
				2_000, false, false, false);
		int passive = ShadowExperienceRules.targetXpPool(
				20.0D, 3.0D, 2.0D, 0.0D, 0.0D,
				-1, false, false, true);
		expectTrue(boss > normal * 8,
				"Boss stats and classification must decisively beat a normal mob");
		expectTrue(configuredBoss >= 200,
				"Datapack base XP must act as a ten-percent shadow-XP floor");
		expectTrue(passive < normal,
				"Passive-animal farming must pay less than combat targets");
	}

	private static void damageContributionControlsTheShare() {
		int mostlyShadow = ShadowExperienceRules.contributionXp(
				100, 90.0D, 100.0D, 35);
		int smallFinisher = ShadowExperienceRules.contributionXp(
				100, 1.0D, 100.0D, 35);
		int noDamage = ShadowExperienceRules.contributionXp(
				100, 0.0D, 100.0D, 35);
		expectEquals(90, mostlyShadow,
				"A shadow keeps its 90% share when the player lands the final blow");
		expectEquals(1, smallFinisher,
				"A one-damage final blow must not receive the full target pool");
		expectEquals(0, noDamage,
				"A shadow which dealt no damage receives no XP");
	}

	private static void earlyCatchUpFadesOut() {
		expectEquals(250, ShadowExperienceRules.contributionXp(
				100, 100.0D, 100.0D, 5),
				"Levels 1-10 need the fast catch-up rate");
		expectEquals(175, ShadowExperienceRules.contributionXp(
				100, 100.0D, 100.0D, 15),
				"Levels 11-20 need the reduced catch-up rate");
		expectEquals(125, ShadowExperienceRules.contributionXp(
				100, 100.0D, 100.0D, 25),
				"Levels 21-30 need the final catch-up rate");
		expectEquals(100, ShadowExperienceRules.contributionXp(
				100, 100.0D, 100.0D, 35),
				"Catch-up must end after level 30");
	}

	private static void runtimeTracksDamageInsteadOfFinalBlows()
			throws IOException {
		String runtime = readMain("util", "ShadowExperienceManager.java");
		String events = readMain("procedures",
				"ShadowMonarchXpProcedure.java");
		String roster = readMain("util", "ShadowMonarchManager.java");

		expectTrue(runtime.contains("LivingDamageEvent")
						&& runtime.contains("recordShadowContribution")
						&& runtime.contains("TOTAL_DAMAGE")
						&& runtime.contains("victim.getMaxHealth()"),
				"Runtime XP must use actual damage and the target's full fight");
		expectTrue(runtime.contains("event.getSource().getDirectEntity()")
						&& runtime.contains("Projectile"),
				"Direct and projectile shadow damage must both resolve");
		expectTrue(events.contains("onEntityDamaged(LivingDamageEvent")
						&& events.contains("awardContributions(event.getEntity())"),
				"Damage must be recorded before contributions are paid on death");
		expectFalse(events.contains("grantKillXp")
						|| runtime.contains("getBbWidth")
						|| runtime.contains("getBbHeight"),
				"Final-blow and bounding-box XP logic must stay removed");
		expectTrue(roster.contains("ShadowExperienceRules.contributionXp")
						&& roster.contains("grantCombatXp("),
				"Roster leveling must consume the balanced contribution award");
	}

	private static void bossCoverageIncludesCurrentEncounters()
			throws IOException {
		String bosses = Files.readString(RESOURCES.resolve(Path.of(
				"data", "minecraft", "tags", "entity_types",
				"soloboss.json")));
		for (String id : new String[] {
				"blood_red_com_igris", "ancient_samurai",
				"cerberus", "vulcan", "baran" }) {
			expectTrue(bosses.contains("\"sololeveling:" + id + "\""),
					id + " must be classified as a boss for shadow XP");
		}
	}

	private static String readMain(String folder, String file)
			throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(folder).resolve(file));
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + " (expected " + expected
					+ ", got " + actual + ")");
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
