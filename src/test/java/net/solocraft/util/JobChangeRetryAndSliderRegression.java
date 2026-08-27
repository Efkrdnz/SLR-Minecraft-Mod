package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free regressions for restartable, non-farmable Job Change runs
 * and the bounded world-creation unlock-level slider.
 */
public final class JobChangeRetryAndSliderRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private JobChangeRetryAndSliderRegression() {
	}

	public static void main(String[] args) throws IOException {
		attemptAuthoritySurvivesDeathAndDisconnects();
		deathFailsAndCleansTheWholeAttempt();
		failedPlayerIsEjectedAfterRespawn();
		encounterEntitiesCarryAttemptIdentity();
		attemptEnemiesCannotProduceFarmableRewards();
		progressRequiresTheMatchingAttempt();
		duplicateKnightCallbacksCannotSkipVisibleFifty();
		opTriggersCanResetAndReassignQuests();
		jobChangeLevelUsesABoundedSlider();
	}

	private static void opTriggersCanResetAndReassignQuests()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String command = read("command", "SlrCommand.java");
		expectTrue(manager.contains("public static boolean forceTriggerQuest(ServerPlayer player)")
					&& manager.contains("capability.finished_quests = removeToken")
					&& manager.contains("capability.JOB = 0")
					&& manager.contains("capability.jobkey = true"),
				"Forced Job Change must reset completion status and restore the quest key");
		expectTrue(command.contains("Commands.literal(\"TriggerDaily\")")
					&& command.contains("DailyQuestLifecycleManager.startQuestNow(target)")
					&& command.contains("Commands.literal(\"TriggerJobchange\")")
					&& command.contains("JobChangeQuestManager.forceTriggerQuest(target)"),
				"The OP command branch must expose both daily and Job Change triggers");
	}

	private static void attemptAuthoritySurvivesDeathAndDisconnects()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String saved = read("util", "JobChangeAttemptSavedData.java");
		expectTrue(manager.contains(
						"JobChangeAttemptSavedData.get(player.server).start(")
						&& manager.contains("UUID.randomUUID()")
						&& manager.contains("Player.PERSISTED_NBT_TAG"),
				"Every run must receive a durable player attempt ID");
		expectTrue(saved.contains("Set<UUID> invalidate(")
						&& saved.contains("acknowledgeFailure(")
						&& saved.contains("retryAfterGameTime"),
				"Failed shared attempts must remain invalid for offline participants");
	}

	private static void deathFailsAndCleansTheWholeAttempt()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String cleanup = read("procedures",
				"JobChangeCleanupProcedure.java");
		expectTrue(manager.contains(
						"public static void onPlayerDeath(LivingDeathEvent event)")
						&& manager.contains(
								"failActiveAttempt(player)")
						&& manager.contains(
								"capability.jobtimer = STATE_IDLE")
						&& manager.contains(
								"capability.jobadvpoint = 0"),
				"Death in the Igris dimension must reset quest phase and progress");
		expectTrue(cleanup.contains(
						"public static void executeAttempt(")
						&& cleanup.contains(
								"JobChangeQuestManager.hasAttemptId("),
				"Failure cleanup must target only the matching run");
		expectTrue(manager.contains("RETRY_DELAY_TICKS = 20L * 10L")
						&& manager.contains(
								"DungeonDimensionPlayerLeavesDimensionProcedure"),
				"Shared participants must be removed safely and briefly rate-limited");
	}

	private static void failedPlayerIsEjectedAfterRespawn()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		int start = manager.indexOf(
				"private static void showPendingFailureNotice(");
		int end = manager.indexOf(
				"private static CompoundTag persistentPlayerData(", start);
		expectTrue(start >= 0 && end > start,
				"Failed-attempt respawn recovery must remain inspectable");
		String recovery = manager.substring(start, end);
		int exit = recovery.indexOf(
				".emergencyExit(player)");
		int consume = recovery.indexOf(
				"persisted.remove(FAILURE_NOTICE_TAG)");
		expectTrue(recovery.contains(
						"player.level().dimension().equals(IGRIS_DIMENSION)")
						&& exit >= 0 && consume > exit,
				"The failed player must leave the Igris dimension before the durable failure marker is consumed");
		expectTrue(occurrences(manager,
						"showPendingFailureNotice(player);") >= 2,
				"Both login and respawn must recover a pending failed-attempt exit");
	}

	private static void encounterEntitiesCarryAttemptIdentity()
			throws IOException {
		String placement = read("procedures",
				"DunPlaceIgrisProcedure.java");
		String igris = read("procedures",
				"IgrisEntityDiesProcedure.java");
		String corpse = read("procedures",
				"BloodRedComIgrisDeathTimeIsReachedProcedure.java");
		String portals = read("procedures",
				"SpawnerPortalOnEntityTickUpdateProcedure.java");
		expectTrue(placement.contains("executeForAttempt(")
						&& placement.contains(
								"tagAttemptEntity(spawned,"),
				"The generated arena boss and portal must inherit the run ID");
		expectTrue(igris.contains(
						"copyAttempt(defeatedBoss,")
						&& corpse.contains(
								"copyAttempt(entity, entityToSpawn)")
						&& portals.contains(
								"copyAttempt(portal, spawned)"),
				"Advancement portals, knights, and the Igris body must stay scoped to the run");
	}

	private static void attemptEnemiesCannotProduceFarmableRewards()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String playerXp = read("procedures", "XPGainProcedure.java");
		String shadowXp = read("util",
				"ShadowExperienceManager.java");
		expectTrue(manager.contains(
						"onAttemptMobDrops(LivingDropsEvent event)")
						&& manager.contains(
								"onAttemptMobExperience(")
						&& manager.contains(
								"event.setDroppedExperience(0)"),
				"Attempt mobs must drop neither loot nor vanilla XP");
		expectTrue(playerXp.contains(
						"JobChangeQuestManager.isAttemptEntity(entity)")
						&& occurrences(shadowXp,
								"JobChangeQuestManager.isAttemptEntity(")
								>= 2,
				"Attempt mobs must award neither Solo XP nor shadow XP");
	}

	private static void progressRequiresTheMatchingAttempt()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String knightCredit = read("procedures",
				"JobAdvPointGainProcedure.java");
		expectTrue(manager.contains(
						"JobChangeKnightBalance.QUEST_KNIGHT_TAG")
						&& occurrences(manager,
								"matchesActiveAttempt(") >= 3,
				"Only tagged Igris and knights from the player's run may advance it");
		expectTrue(knightCredit.contains(
						"creditedSourceForDeath("),
				"Environmental knight deaths must retain legitimate owner credit");
	}

	private static void duplicateKnightCallbacksCannotSkipVisibleFifty()
			throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String attempts = read("util", "JobChangeAttemptSavedData.java");
		String knightCredit = read("procedures",
				"JobAdvPointGainProcedure.java");
		int grantStart = manager.indexOf(
				"public static void grantAdvancementPoint(");
		int grantEnd = manager.indexOf(
				"public static void selectVessel(", grantStart);
		int selectionStart = manager.indexOf(
				"private static void enterSelection(");
		int selectionEnd = manager.indexOf(
				"private static void recover(", selectionStart);
		expectTrue(grantStart >= 0 && grantEnd > grantStart
						&& selectionStart >= 0 && selectionEnd > selectionStart,
				"Advancement completion paths must remain inspectable");
		String grant = manager.substring(grantStart, grantEnd);
		String selection = manager.substring(selectionStart, selectionEnd);

		int duplicateGuard = grant.indexOf(
				"ADVANCEMENT_POINT_CREDITED_TAG");
		int durableMark = grant.indexOf(
				"ADVANCEMENT_POINT_CREDITED_TAG", duplicateGuard + 1);
		int increment = grant.indexOf("capability.jobadvpoint = Math.min(");
		expectTrue(manager.contains(
						"ADVANCEMENT_POINT_CREDITED_TAG =")
						&& duplicateGuard >= 0 && durableMark > duplicateGuard
						&& increment > durableMark
						&& grant.indexOf("creditAdvancementKill(")
								< durableMark
						&& attempts.contains(
								"public boolean creditAdvancementKill(")
						&& attempts.contains("CreditedKills")
						&& occurrences(knightCredit,
								"grantAdvancementPoint(") >= 2,
				"The knight entity must reject the generated/event double callback before either path can increment progress twice");

		int allComplete = grant.indexOf("boolean allComplete =");
		int transition = grant.indexOf("enterSelection(player);");
		int cleanup = grant.indexOf(
				"JobChangeCleanupProcedure.completeAttempt(");
		int complete = grant.indexOf(".complete(attemptId);", cleanup);
		expectTrue(allComplete >= 0 && transition > allComplete
						&& cleanup > transition && complete > cleanup
						&& occurrences(grant, ".complete(attemptId);") == 1
						&& !selection.contains(
								"JobChangeAttemptSavedData.get("),
				"All players must leave advancement state before the shared attempt becomes inactive, preventing recovery from ejecting them at visible 49/50");
		expectTrue(selection.contains("SELECTION_OPEN_AFTER_TAG")
					&& selection.contains("SELECTION_OPEN_DELAY_TICKS")
					&& !selection.contains("sendSelectionState(player)"),
				"Arena cleanup must finish before the delayed selection screen can receive a held combat click");
	}

	private static void jobChangeLevelUsesABoundedSlider()
			throws IOException {
		String tab = read("client", "gui", "worldcreation",
				"SoloLevelingWorldCreationTab.java");
		expectTrue(tab.contains(
						"class JobChangeLevelSlider extends AbstractSliderButton")
						&& tab.contains("MINIMUM = 20")
						&& tab.contains("MAXIMUM = 100")
						&& tab.contains(
								"Job Change: Lv. "),
				"World creation must expose a labeled 20-100 Job Change slider");
		expectTrue(tab.contains(
						"clamp(rules, SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL")
						&& !tab.contains("List.of(40, 50, 60)"),
				"The old three-value Job Change cycle must be removed");
	}

	private static int occurrences(String source, String token) {
		int count = 0;
		for (int index = 0;
				(index = source.indexOf(token, index)) >= 0;
				index += token.length())
			count++;
		return count;
	}

	private static String read(String directory, String file)
			throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory)
				.resolve(file));
	}

	private static String read(String directory, String subdirectory,
			String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory)
				.resolve(subdirectory).resolve(file));
	}

	private static String read(String directory, String first,
			String second, String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory)
				.resolve(first).resolve(second).resolve(file));
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}
}
