package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Regressions for per-level stat points and reused Fire Mage impact visuals. */
public final class LevelRewardFireVfxRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCE_ROOT = Path.of(
			"src", "main", "resources", "assets", "sololeveling");

	private LevelRewardFireVfxRegression() {
	}

	public static void main(String[] args) throws IOException {
		levelRewardRuleIsThreeAndHandlesMultiLevelGains();
		allPositiveLevelPathsUseTheSharedReward();
		levelCommandAwardsOnlyPositiveDifferences();
		fireExplosionsReuseLiuExecutionBurst();
		orbVariantRecolorsTheReusedBurstBlue();
	}

	private static void levelRewardRuleIsThreeAndHandlesMultiLevelGains() {
		expectTrue(LevelRewardRules.SKILL_POINTS_PER_LEVEL == 3,
				"Every gained level must award three spendable stat points");
		expectTrue(LevelRewardRules.skillPointsForLevels(4) == 12,
				"Multi-level XP gains must award points for every level");
		expectTrue(LevelRewardRules.skillPointsForLevels(-2) == 0,
				"Level decreases must never grant points");
	}

	private static void allPositiveLevelPathsUseTheSharedReward() throws IOException {
		String normal = readMain("procedures", "LevelUpProcedure.java");
		String item = readMain("procedures", "LevelItemRightclickedProcedure.java");
		expectTrue(normal.contains(
						"playerVars.SkillPoints += LevelRewardRules.skillPointsForLevels(levelsGained);"),
				"Normal XP leveling must award points for all levels gained in the tick");
		expectTrue(item.contains("LevelRewardRules.skillPointsForLevels(1)"),
				"The direct level-up item must use the shared per-level reward");
	}

	private static void levelCommandAwardsOnlyPositiveDifferences() throws IOException {
		String command = readMain("command", "SlrCommand.java");
		String setter = readMain("procedures", "SLRSetLevelProcedure.java");
		expectTrue(command.contains(
						"Commands.argument(\"amount\", IntegerArgumentType.integer(0, 500))"),
				"The System level command must accept whole levels");
		int positive = setter.indexOf("if (difference > 0.0D)");
		int award = setter.indexOf(
				"variables.SkillPoints += LevelRewardRules.skillPointsForLevels(");
		int decrease = setter.indexOf("else if (difference < 0.0D)");
		expectTrue(positive >= 0 && award > positive && decrease > award,
				"The level command must award three points per positive level difference");
		expectTrue(setter.indexOf("variables.SkillPoints", decrease) < 0,
				"Lowering a level must not add or remove already spent stat points");
	}

	private static void fireExplosionsReuseLiuExecutionBurst() throws IOException {
		String source = readMain("util", "FireMageSpellManager.java");
		String blast = section(source, "private static void blast(",
				"private static void spawnExecutionExplosionVfx");
		String visual = section(source, "private static void spawnExecutionExplosionVfx",
				"private static List<UUID> findCremationTargets");
		expectTrue(blast.contains("spawnExecutionExplosionVfx(level, caster, center, radius, stage);"),
				"Every Fire Mage damage blast must spawn the reused impact burst");
		expectTrue(visual.contains("LiuSwordVfxEntity.EXECUTION_EXPLOSION")
						&& visual.contains("LiuSwordVfxEntity.spawn("),
				"Fire Mage blasts must reuse Liu's maximum-charge execution explosion entity");
	}

	private static void orbVariantRecolorsTheReusedBurstBlue() throws IOException {
		String manager = readMain("util", "FireMageSpellManager.java");
		String visual = section(manager, "private static void spawnExecutionExplosionVfx",
				"private static List<UUID> findCremationTargets");
		String shader = Files.readString(RESOURCE_ROOT.resolve("shaders")
				.resolve("core").resolve("rendertype_liu_sword.fsh"));
		expectTrue(visual.contains("OrbOfAvariceManager.isHeldBy(caster)")
						&& visual.contains("OrbOfAvariceManager.BLUE_FIRE_PRIMARY")
						&& visual.contains("OrbOfAvariceManager.BLUE_FIRE_SECONDARY"),
				"Orb-amplified Fire Mage bursts must use the shared blue-flame palette");
		expectTrue(shader.contains("bool blueFire = base.b > base.r * 1.08;")
						&& shader.contains("blueFire ? vec3(0.68, 0.96, 1.0)"),
				"Liu's explosion shader must preserve a genuinely blue-hot Orb variant");
	}

	private static String section(String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: " + startToken);
		return source.substring(start, end);
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN_SOURCE;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}
}
