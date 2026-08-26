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
		dailyCompletionQueuesOnlyFixedNonXpRewards();
		dailyCompletionPreservesTheSharedRewardInbox();
		dailyResolutionWaitsForTheNextOverworldDay();
		dailyRewardsRemainPendingAcrossLogin();
		fullRecoveryOverflowIsUsedInsteadOfStacked();
		dailyPunishmentContinuesUntilJobChangeCompletion();
		storyIntroBossDoesNotGrantNormalBossRewards();
		becomingPlayerDoesNotGrantRewardsDirectly();
		guildBuildingsHaveNoRegistrationsTemplatesOrSpawner();
	}

	private static void dailyCompletionQueuesOnlyFixedNonXpRewards()
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
							"RewardManager.appendReward(player, " + reward + ");"),
					"Daily completion must queue " + reward + " for manual collection");
		}
		// The amount itself is owned by SkillPointEconomyRegression, which tests the
		// numbers directly. All this file cares about is that the daily still pays
		// skill points and reads them from the shared economy instead of a literal.
		expectTrue(source.contains("SkillPointRules.DAILY_QUEST"),
				"The normal Daily Quest must take its skill points from SkillPointRules");
		expectFalse(completion.contains("RewardCollectProcedure"),
				"Daily completion must not deliver rewards automatically");
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

	private static void dailyResolutionWaitsForTheNextOverworldDay()
			throws IOException {
		String source = readMain("util", "daily",
				"DailyQuestLifecycleManager.java");
		String failure = method(source, "private static void failQuest",
				"private static void completeQuest");
		String completion = method(source, "private static void completeQuest",
				"private static void activateSecretQuestIfEligible");
		expectTrue(failure.contains(
						"variables.lastDailyQuestDay = minecraftDay(player);"),
				"A failed quest must mark its resolution day");
		expectTrue(completion.contains(
						"variables.lastDailyQuestDay = minecraftDay(player);"),
				"A completed quest must mark its resolution day");
		expectTrue(source.contains(
						"player.server.overworld().getDayTime()"),
				"Daily resets must use one canonical Overworld clock");
		expectFalse(source.contains(
						"Math.floorDiv(player.level().getDayTime(), 24000L)"),
				"Changing dimensions must not change the Daily reset clock");
	}

	private static void dailyRewardsRemainPendingAcrossLogin()
			throws IOException {
		String helper = readMain("procedures", "DailyQuestHelper.java");
		String login = method(helper, "public static void onPlayerLoggedIn",
				"public static boolean isSecretQuest");
		expectFalse(login.contains("claimReward")
						|| login.contains("RewardCollectProcedure")
						|| login.contains("recoverQueuedSecretRewards"),
				"Login must leave pending Daily and Secret rewards for manual collection");
		expectFalse(helper.contains("RewardManager.claimReward"),
				"Daily Quest helpers must never auto-claim a System reward");
	}

	private static void fullRecoveryOverflowIsUsedInsteadOfStacked()
			throws IOException {
		String manager = readMain("util", "RewardManager.java");
		String append = method(manager, "public static void appendReward",
				"public static boolean hasPendingFullRecovery");
		expectTrue(append.contains(
						"isFullRecovery(cleanReward) && hasPendingFullRecovery(entity)")
						&& append.contains("applyFullRecovery(entity);"),
				"A second Full Recovery must be used instead of entering the reward inbox");
		expectTrue(manager.contains(
						"public static void setFullRecoveryReward")
						&& manager.contains(
								"public static boolean reconcileFullRecoveryRewards")
						&& manager.contains(
								"RewardCollectProcedure.execute(entity, FULL_RECOVERY_REWARD)"),
				"Legacy slots and existing duplicate saves must share the non-stackable Full Recovery rule");

		String setter = readMain("procedures",
				"SLRRewardSetFullRecoveryProcedure.java");
		expectTrue(setter.contains(
						"RewardManager.setFullRecoveryReward(entityiterator,")
						&& !setter.contains("capability.reward_"),
				"The Full Recovery reward command must not bypass RewardManager");
		expectTrue(readMain("procedures", "RewardScreenOpenProcedure.java")
						.contains("RewardManager.reconcileFullRecoveryRewards(serverPlayer)")
						&& readMain("procedures", "RewardGainAdvProcedure.java")
								.contains("RewardManager.appendReward(player, entry)"),
				"Reward-panel opening and queued boss bundles must repair duplicate Full Recoveries");
	}

	private static void dailyPunishmentContinuesUntilJobChangeCompletion()
			throws IOException {
		String lifecycle = readMain("util", "daily",
				"DailyQuestLifecycleManager.java");
		String punishmentManager = readMain("util", "daily",
				"DailyPunishmentManager.java");
		String punishmentTick = readMain("procedures",
				"SurvivalDimensionTickUpdateProcedure.java");
		String failure = method(lifecycle, "private static void failQuest",
				"private static boolean shouldPunishFailure");
		String condition = method(lifecycle,
				"private static boolean shouldPunishFailure",
				"private static boolean enterPunishmentZone");
		String punishment = method(lifecycle,
				"private static boolean enterPunishmentZone",
				"private static void completeQuest");
		expectTrue(failure.contains("shouldPunishFailure(player)")
						&& failure.contains("enterPunishmentZone(player)"),
				"An expired Daily Quest must route eligible players into punishment");
		expectTrue(condition.contains("SOLO_PUNISHMENT")
						&& condition.contains(
								"!JobChangeQuestManager.isFinished(player)"),
				"Punishment must remain enabled through Job Change and stop only at durable completion");
		expectTrue(punishment.contains(
						"DailyPunishmentManager.enter(player)"),
				"Daily failure must delegate to the safe punishment travel manager");
		expectTrue(punishmentManager.contains(
						"level.getChunk(targetX >> 4, targetZ >> 4)")
						&& punishmentManager.contains(
								"isSafeStandingPosition(level, candidate)")
						&& punishmentManager.contains(
								"createEmergencyPlatform(level, targetX, targetZ)")
						&& punishmentManager.contains(
								"level.getMinBuildHeight()")
						&& punishmentManager.contains(
								"public static boolean rescueFromVoid")
						&& punishmentTick.contains(
								"DailyPunishmentManager.tick(player)"),
				"Punishment entry must load terrain, validate a landing, create a fallback floor, and rescue void falls every tick");
		expectTrue(punishmentManager.contains("putDouble(RETURN_X")
						&& punishmentManager.contains("putString(RETURN_DIMENSION")
						&& punishmentManager.contains(
								"capability.punishment = remaining")
						&& punishmentManager.contains(
								"player.teleportTo(destination,"),
				"Punishment must retain a dimension-aware return point, timer, and server travel path");
	}

	private static void storyIntroBossDoesNotGrantNormalBossRewards()
			throws IOException {
		String source = readMain("procedures",
				"RewardGainAdvProcedure.java");
		int storyGuard = source.indexOf(
				"StoryModeIntroManager.isIntroActive(target)");
		int rewardQueue = source.indexOf("RewardManager.appendReward(player, entry)");
		expectTrue(storyGuard >= 0 && rewardQueue > storyGuard,
				"Story Intro encounters must exit before normal boss rewards are queued");
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
