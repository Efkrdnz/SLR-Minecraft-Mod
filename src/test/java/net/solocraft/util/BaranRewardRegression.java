package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Dependency-free checks for Baran's rewards and Shadow Monarch-only abilities. */
public final class BaranRewardRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path DATA = Path.of(
			"src", "main", "resources", "data");

	private BaranRewardRegression() {
	}

	public static void main(String[] args) throws IOException {
		baranQueuesTheAdditionalDaggerAndConditionalRunestone();
		baranRewardsAreOneTimeAndSurviveDeath();
		shadowExchangeNoLongerUnlocksFromStoredShadows();
		runestoneRemainsCreativeButHasNoOtherSurvivalSource();
		runestoneUseRequiresShadowMonarch();
		shadowRunestoneSkillsRequireShadowMonarch();
		completedLegacyRunsReceiveTheMissingRewardOnce();
	}

	private static void baranQueuesTheAdditionalDaggerAndConditionalRunestone()
			throws IOException {
		String boss = readMain("procedures", "DKCBossKillRewardProcedure.java");
		String baranBranch = section(boss,
				"if (entity instanceof BaranEntity) {",
				"if (exactKaiselin)");
		expectTrue(baranBranch.contains(
						"BaranVictoryRewards.grantIfNeeded(player);"),
				"Baran's exact death branch must resolve his personal rewards");

		String rewards = readMain("util", "BaranVictoryRewards.java");
		expectTrue(rewards.contains(
						"\"ITEM:sololeveling:demon_kings_dagger\""),
				"Baran must queue the second Demon King's Dagger");
		expectTrue(rewards.contains(
						"\"ITEM:sololeveling:runestone_shadow_exchange\""),
				"Baran must own the Shadow Exchange runestone reward");
		expectFalse(rewards.contains("if (shadowMonarch)"),
				"The runestone reward must not be restricted to one vessel");
		expectTrue(rewards.contains(
						"RewardManager.appendReward(player, SHADOW_EXCHANGE_RUNESTONE_REWARD);"),
				"Every Baran victor must receive the ability runestone");
	}

	private static void baranRewardsAreOneTimeAndSurviveDeath()
			throws IOException {
		String rewards = readMain("util", "BaranVictoryRewards.java");
		expectTrue(rewards.contains("REWARDS_RESOLVED_TAG")
						&& rewards.contains("data.getBoolean(REWARDS_RESOLVED_TAG)")
						&& rewards.contains("data.putBoolean(REWARDS_RESOLVED_TAG, true)"),
				"Baran rewards need a durable one-shot receipt");
		expectTrue(rewards.contains("Player.PERSISTED_NBT_TAG"),
				"The receipt must survive player death and floor retries");
	}

	private static void shadowExchangeNoLongerUnlocksFromStoredShadows()
			throws IOException {
		String progression = readMain("util", "VesselProgressionManager.java");
		expectFalse(progression.contains("vars.ShadowExchange = true"),
				"Progress reconciliation must not auto-unlock Shadow Exchange");
		String rules = readMain("util", "VesselProgressionRules.java");
		expectFalse(rules.contains("SHADOW_EXCHANGE_STORAGE")
						|| rules.contains("shadowStorageUsage >= 30"),
				"Stored shadows must not remain an alternate unlock path");
	}

	private static void runestoneRemainsCreativeButHasNoOtherSurvivalSource()
			throws IOException {
		String tabs = readMain("init", "SololevelingModTabs.java");
		expectTrue(tabs.contains(
						"tabData.accept(SololevelingModItems.RUNESTONE_SHADOW_EXCHANGE"),
				"The Shadow Exchange runestone must remain available in its creative tab");
		try (Stream<Path> files = Files.walk(DATA)) {
			for (Path file : files.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".json")
							|| path.toString().endsWith(".mcfunction")).toList()) {
				expectFalse(Files.readString(file).contains(
								"sololeveling:runestone_shadow_exchange"),
						"A data-pack acquisition path still references the runestone: "
								+ DATA.relativize(file));
			}
		}
	}

	private static void runestoneUseRequiresShadowMonarch()
			throws IOException {
		String exchangeUse = readMain("procedures",
				"RunestoneShadowExchangeRightclickedProcedure.java");
		expectTrue(exchangeUse.contains(
				"if (!VesselProgressionManager.isShadowMonarch(entity))"),
				"Shadow Exchange must require the Shadow Monarch vessel");
		String manifestationUse = readMain("procedures",
				"RunestoneShadowBodyManifestationProcedure.java");
		expectTrue(manifestationUse.contains(
						"if (!VesselProgressionManager.isShadowMonarch(entity))"),
				"Shadow Manifestation must require the Shadow Monarch vessel");
	}

	private static void shadowRunestoneSkillsRequireShadowMonarch()
			throws IOException {
		String jobs = readMain("util", "JobSkillManager.java");
		String grants = section(jobs,
				"private static List<String> skillsForEntityJob",
				"private static CompoundTag runestoneData");
		expectTrue(grants.contains("if (VesselProgressionManager.isShadowMonarch(entity) && vars.ShadowExchange")
						&& grants.contains("if (VesselProgressionManager.isShadowMonarch(entity) && vars.ShadowBody"),
				"Consumed Shadow runestones must only grant skills to the Shadow Monarch");

		String exchange = readMain("procedures",
				"DoesHaveExchangeProcedure.java");
		String manifestation = readMain("procedures",
				"DoesHaveShadowManifestationProcedure.java");
		String abilityThree = readMain("procedures",
				"Ability3OnKeyPressedProcedure.java");
		String abilityFour = readMain("procedures",
				"Ability4OnKeyPressedProcedure.java");
		String exchangeManager = readMain("util",
				"ShadowExchangeManager.java");
		expectTrue(exchange.contains(
						"VesselProgressionManager.isShadowMonarch(entity)")
						&& manifestation.contains(
								"VesselProgressionManager.isShadowMonarch(entity)")
						&& abilityThree.contains(
								"if (VesselProgressionManager.isShadowMonarch(entity))")
						&& abilityFour.contains(
								"if (VesselProgressionManager.isShadowMonarch(entity))")
						&& exchangeManager.contains(
								"private static boolean canUseExchange")
						&& exchangeManager.contains(
								"VesselProgressionManager.isShadowMonarch(player)"),
				"Shadow helpers must require the active Shadow Monarch vessel");
		String progression = readMain("util", "VesselProgressionManager.java");
		expectFalse(progression.contains("JobSkillManager.hasLearnedSkill"),
				"Shadow vessel ownership must not come from a previously learned skill");

		String temporaryArmor = readMain("util",
				"TemporaryArmorSessionManager.java");
		String vessels = readMain("util", "VesselManager.java");
		expectTrue(temporaryArmor.contains(
						"boolean hasRequiredJob = variables.JOB == requiredJob;")
						&& temporaryArmor.contains(
								"public static void endForVesselChange")
						&& vessels.contains(
								"TemporaryArmorSessionManager.endForVesselChange(player);"),
				"Vessel changes must cancel and restore an active or delayed shadow manifestation");
	}

	private static void completedLegacyRunsReceiveTheMissingRewardOnce()
			throws IOException {
		String progression = readMain("util", "VesselProgressionManager.java");
		String dkc = section(progression,
				"private static void syncDemonKingsCastle",
				"private static void syncVesselIdentity");
		expectTrue(dkc.contains("vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR")
						&& dkc.contains("BaranVictoryRewards.grantIfNeeded(player);"),
				"Completed pre-update Castle runs must reconcile Baran's missing reward");
	}

	private static String section(String source, String startToken,
			String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: "
					+ startToken);
		return source.substring(start, end);
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
