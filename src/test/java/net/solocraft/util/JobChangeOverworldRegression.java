package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source regressions for the server-authoritative Overworld
 * boundary around normal Job Change quest initiation and re-entry.
 */
public final class JobChangeOverworldRegression {
	private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "net", "solocraft");

	private JobChangeOverworldRegression() {
	}

	public static void main(String[] args) throws IOException {
		managerGuardsUnlockAndRunStart();
		automaticUnlockPreservesAdminReconciliation();
		physicalKeyIsCheckedBeforeConsumption();
		normalEntryReturnsAResultAndMutatesOnlyAfterTheGuard();
		legacyPortalSurvivesDeniedEntry();
		igrisProgressAndAdminSelectionRemainDimensionIndependent();
	}

	private static void managerGuardsUnlockAndRunStart() throws IOException {
		String source = read("util", "JobChangeQuestManager.java");
		expectTrue(source.contains("Level.OVERWORLD.equals(entity.level().dimension())"),
				"The authoritative dimension predicate must name Minecraft's Overworld key");
		expectTrue(source.contains("public static boolean unlock(Entity entity)")
						&& source.contains("!isOverworld(player) || isUnlocked(player) || isFinished(player)"),
				"Quest unlock must fail authoritatively outside the Overworld");
		expectTrue(source.indexOf("isUnlocked(player)") < source.indexOf("capability.unlocked_quests = append"),
				"Repeated Job Key use must not report success or consume another key for an already-unlocked quest");
		expectTrue(source.contains("public static boolean startDungeonRun(ServerPlayer player)")
						&& source.contains("player == null || !isOverworld(player)"),
				"Fresh dungeon-run state must not start outside the Overworld");
	}

	private static void automaticUnlockPreservesAdminReconciliation() throws IOException {
		String source = method(read("util", "JobChangeQuestManager.java"),
				"public static void unlockIfEligible", "public static boolean hasAdvancementPlayerNear");
		int adminReconcile = source.indexOf("if (vars.JOB > 0");
		int dimensionGuard = source.indexOf("if (!isOverworld(player))");
		expectTrue(adminReconcile >= 0 && dimensionGuard > adminReconcile,
				"Existing/admin-assigned vessels must reconcile before the normal unlock dimension guard");
		expectTrue(source.contains("finish(player)"),
				"Admin vessel reconciliation must retain quest completion");
	}

	private static void physicalKeyIsCheckedBeforeConsumption() throws IOException {
		String source = read("procedures", "JobKeyRightclickedProcedure.java");
		int dimensionGuard = source.indexOf("if (!JobChangeQuestManager.isOverworld(player))");
		int unlock = source.indexOf("if (!JobChangeQuestManager.unlock(player))");
		int consume = source.indexOf("clearOrCountMatchingItems");
		expectTrue(dimensionGuard >= 0 && unlock > dimensionGuard && consume > unlock,
				"The physical Job Key must pass both authoritative guards before one item is consumed");
		expectTrue(source.contains("instanceof ServerPlayer"),
				"Physical Job Key mutation must be server-only");
	}

	private static void normalEntryReturnsAResultAndMutatesOnlyAfterTheGuard() throws IOException {
		String source = read("procedures", "JobChangeQuestEntryProcedure.java");
		expectTrue(source.contains("public static boolean execute(LevelAccessor world, Entity entity)"),
				"Normal entry must report whether teleport entry was accepted");
		int selectionRecovery = source.indexOf("isSelectionPending(player)");
		int dimensionGuard = source.indexOf("if (!JobChangeQuestManager.isOverworld(player))");
		int start = source.indexOf("JobChangeQuestManager.startDungeonRun(player)");
		int dungeonFlag = source.indexOf("putBoolean(\"slr_job_change_dungeon\", true)");
		expectTrue(selectionRecovery >= 0 && dimensionGuard > selectionRecovery,
				"Selection recovery inside the Igris trial must remain available before normal-entry denial");
		expectTrue(start > dimensionGuard && dungeonFlag > start,
				"Quest state and dungeon receipts must only mutate after the Overworld guard");
		expectTrue(source.contains("return true;"),
				"Accepted normal entry must return success");
	}

	private static void legacyPortalSurvivesDeniedEntry() throws IOException {
		String source = read("procedures", "PortalJobChangeRightClickedOnEntityProcedure.java");
		expectTrue(source.contains("if (JobChangeQuestEntryProcedure.execute(world, player)")
						&& source.indexOf("portal.discard()") > source.indexOf("JobChangeQuestEntryProcedure.execute"),
				"The legacy portal may discard only after accepted entry");
	}

	private static void igrisProgressAndAdminSelectionRemainDimensionIndependent() throws IOException {
		String manager = read("util", "JobChangeQuestManager.java");
		String advancement = method(manager, "public static List<ServerPlayer> beginAdvancementPhase",
				"public static void grantAdvancementPoint");
		String recovery = method(manager, "private static void recover", "private static List<ServerPlayer> questParticipants");
		String adminSelection = method(manager, "public static void openSelectionFromCommand",
				"public static void finish");
		expectFalse(advancement.contains("isOverworld("),
				"Igris victory must still advance the quest inside its dungeon dimension");
		expectFalse(recovery.contains("isOverworld("),
				"Login/respawn selection recovery must remain dimension-independent");
		expectFalse(adminSelection.contains("isOverworld("),
				"Admin-opened vessel selection must remain dimension-independent");
	}

	private static String method(String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: " + startToken);
		return source.substring(start, end);
	}

	private static String read(String directory, String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory).resolve(file));
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
