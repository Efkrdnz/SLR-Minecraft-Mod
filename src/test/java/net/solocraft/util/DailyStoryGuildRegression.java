package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Dependency-free regressions for fixed Daily Quest rewards, Story Mode reward
 * isolation, and complete removal of the two overworld guild buildings.
 */
public final class DailyStoryGuildRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCES = Path.of(
			"src", "main", "resources", "data", "sololeveling");

	private DailyStoryGuildRegression() {
	}

	public static void main(String[] args) throws IOException {
		dailyCompletionUsesOnlyFixedNonXpRewards();
		dailyCompletionPreservesTheSharedRewardInbox();
		storyIntroBossDoesNotGrantNormalBossRewards();
		becomingPlayerDoesNotGrantRewardsDirectly();
		guildBuildingsHaveNoRegistrationsTemplatesOrSpawner();
	}

	private static void dailyCompletionUsesOnlyFixedNonXpRewards()
			throws IOException {
		String source = readMain("util", "daily",
				"DailyQuestLifecycleManager.java");
		String completion = method(source, "private static void completeQuest",
				"private static void activateSecretQuestIfEligible");
		for (String reward : List.of(
				"DAILY_FULL_RECOVERY_REWARD",
				"DAILY_SKILL_POINTS_REWARD",
				"DAILY_ITEM_REWARD")) {
			expectTrue(completion.contains(
							"RewardCollectProcedure.execute(player, " + reward + ");"),
					"Daily completion must grant " + reward + " directly");
		}
		expectTrue(source.contains(
						"private static final String DAILY_SKILL_POINTS_REWARD = \"SP10\";"),
				"The normal Daily Quest must grant ten skill points");
		expectFalse(completion.contains("\"XP"),
				"Daily completion must never manufacture an XP reward");
	}

	private static void dailyCompletionPreservesTheSharedRewardInbox()
			throws IOException {
		String source = readMain("util", "daily",
				"DailyQuestLifecycleManager.java");
		String completion = method(source, "private static void completeQuest",
				"private static void activateSecretQuestIfEligible");
		expectFalse(completion.contains("variables.reward_1")
						|| completion.contains("variables.reward_2")
						|| completion.contains("variables.reward_3"),
				"Daily completion must neither consume nor overwrite shared pending rewards");
		expectFalse(completion.contains(
						"RewardCollectProcedure.execute(player, reward"),
				"Daily completion must not execute a reward copied from the shared inbox");
	}

	private static void storyIntroBossDoesNotGrantNormalBossRewards()
			throws IOException {
		String source = readMain("procedures",
				"RewardGainAdvProcedure.java");
		int storyGuard = source.indexOf(
				"if (StoryModeIntroManager.isIntroActive(entity))");
		int rewardRead = source.indexOf("reward_1");
		expectTrue(storyGuard >= 0 && rewardRead > storyGuard,
				"Story Intro encounters must exit before normal boss rewards are read or granted");
	}

	private static void becomingPlayerDoesNotGrantRewardsDirectly()
			throws IOException {
		String source = readMain("util", "CartenonTempleManager.java");
		String acceptance = method(source,
				"public static void resolveAwakeningChoice",
				"@SubscribeEvent(priority = EventPriority.LOWEST)");
		expectFalse(acceptance.contains("RewardCollectProcedure")
						|| acceptance.contains("RewardManager")
						|| acceptance.contains("reward_1")
						|| acceptance.contains("reward_2")
						|| acceptance.contains("reward_3"),
				"Accepting the System must not grant or mutate pending rewards");
	}

	private static void guildBuildingsHaveNoRegistrationsTemplatesOrSpawner()
			throws IOException {
		for (String relative : List.of(
				"worldgen/structure/guild_build.json",
				"worldgen/structure/white_tiger_guild.json",
				"worldgen/structure_set/guild_build.json",
				"worldgen/structure_set/white_tiger_guild.json",
				"worldgen/template_pool/guild_build.json",
				"worldgen/template_pool/white_tiger_guild.json",
				"structures/ahjinguild.nbt",
				"structures/whitetigetdd.nbt",
				"structures/whitetiger.nbt")) {
			expectFalse(Files.exists(RESOURCES.resolve(relative)),
					"Removed guild building asset was restored: " + relative);
		}
		expectFalse(Files.exists(MAIN_SOURCE.resolve("procedures")
						.resolve("DDungeonSpawnerRightclickedProcedure.java")),
				"The removed White Tiger building still has an explicit spawner");

		Path worldgen = RESOURCES.resolve("worldgen");
		try (Stream<Path> files = Files.walk(worldgen)) {
			for (Path file : files.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".json")).toList()) {
				String source = Files.readString(file).toLowerCase();
				expectFalse(source.contains("sololeveling:guild_build")
								|| source.contains("sololeveling:white_tiger_guild")
								|| source.contains("sololeveling:ahjinguild"),
						"Guild building remains registered by "
								+ worldgen.relativize(file));
			}
		}
	}

	private static String method(String source, String startToken,
			String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: "
					+ startToken);
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

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
