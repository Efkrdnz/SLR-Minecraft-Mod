package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source regressions for reset-safe delayed dungeon entry.
 */
public final class PlayerEntryGenerationRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private PlayerEntryGenerationRegression() {
	}

	public static void main(String[] args) throws IOException {
		guardIsPersistentAndResetSafe();
		proceduralGateFiltersAndRollsBackInvalidEntrants();
		datapackGateGeneratesOnlyForCurrentEntrants();
		instanceKeyChecksEveryDelayedStage();
		fixedGatesCheckEveryDelayedStage();
		dkcLoginRecoveryCannotOutliveReset();
	}

	private static void guardIsPersistentAndResetSafe() throws IOException {
		String guard = read("util", "PlayerEntryGenerationGuard.java");
		String policy = read("util", "PlayerResetKeyPolicy.java");
		expectTrue(guard.contains("public static long begin(ServerPlayer player)")
						&& guard.contains("invalidate(player);")
						&& guard.contains("return capture(player);"),
				"Each new delayed entry must supersede the player's older callbacks");
		expectTrue(guard.contains(
						"server.getPlayerList().getPlayer(player.getUUID()) == player"),
				"Tokens must reject stale ServerPlayer objects after reconnect/clone");
		expectTrue(policy.contains(
						"PlayerEntryGenerationGuard.GENERATION_TAG.equals(key)"),
				"Reset clearing must preserve the incremented generation");
	}

	private static void proceduralGateFiltersAndRollsBackInvalidEntrants()
			throws IOException {
		String gate = read("dungeon", "ProceduralDungeonGateHandler.java");
		String runs = read("dungeon", "ProceduralGateRunSavedData.java");
		expectTrue(gate.contains("PlayerEntryGenerationGuard.begin(entrant)")
						&& gate.contains("currentPreparedEntrants("),
				"Normal procedural gates must capture and filter player generations");
		expectTrue(gate.contains(
						"ProceduralDungeonCompletionHandler.removeUnscopedEntrant(")
						&& runs.contains(
								"public void removeEntrant(String dungeonTag, UUID playerId)"),
				"Invalidated entry must not remain in the durable unscoped roster");
		int filter = gate.indexOf("List<ServerPlayer> currentEntrants =");
		int generate = gate.indexOf("ProceduralDungeonGenerator.generate(");
		expectTrue(filter >= 0 && generate > filter,
				"No procedural dungeon may generate after every entrant reset");
	}

	private static void datapackGateGeneratesOnlyForCurrentEntrants()
			throws IOException {
		String gate = read("dungeon", "DatapackDungeonGateHandler.java");
		int filter = gate.indexOf("List<ServerPlayer> currentEntrants =");
		int generate = gate.indexOf("DungeonRuntimeGenerator.generate(");
		expectTrue(gate.contains("PlayerEntryGenerationGuard.begin(entrant)")
						&& filter >= 0 && generate > filter,
				"Datapack generation must follow generation-token filtering");
		expectTrue(gate.contains(
						"target, seed, currentEntrants, null")
						&& gate.contains(
								"for (ServerPlayer entrant : currentEntrants)"),
				"Only still-prepared party members may own or enter the instance");
	}

	private static void instanceKeyChecksEveryDelayedStage()
			throws IOException {
		for (String file : new String[] {
				"InstanceDungeonKeyRightclickedOnBlockProcedure.java",
				"InstanceDungeonKeyLoggerOnBlockRightClickedProcedure.java" }) {
			String source = read("procedures", file);
			expectTrue(source.contains("PlayerEntryGenerationGuard.begin(player)"),
					file + " must capture one entry generation");
			expectTrue(occurrences(source,
							"PlayerEntryGenerationGuard.isCurrent") >= 4,
					file + " must guard every nested teleport/generation stage");
		}
	}

	private static void fixedGatesCheckEveryDelayedStage()
			throws IOException {
		for (String file : new String[] {
				"PortalAncientGolemRightClickedOnEntityProcedure.java",
				"PortalBeruPlayerCollidesWithThisEntityProcedure.java",
				"PortalCemeteryRightClickedOnEntityProcedure.java",
				"PortalKargalgansThroneRoomRightClickedOnEntityProcedure.java",
				"PortalLabRightClickedOnEntityProcedure.java",
				"PortalLushRightClickedOnEntityProcedure.java" }) {
			String source = read("procedures", file);
			expectTrue(source.contains(
							"PlayerEntryGenerationGuard.begin(entryPlayer)"),
					file + " must start a mutually-exclusive entry generation");
			expectTrue(occurrences(source,
							"PlayerEntryGenerationGuard.isCurrent") >= 3,
					file + " must guard all 10/5/10 delayed stages");
			expectTrue(occurrences(source,
							"entryPlayer.level().dimension() != dungeonDimension")
							>= 2,
					file + " must reject callbacks after a manual/reset exit");
		}

		String sewers = read("procedures",
				"PortalSewersRightClickedOnEntityProcedure.java");
		expectTrue(sewers.contains(
						"PlayerEntryGenerationGuard.begin(player)"),
				"Sewers entry must capture the shared generation");
		expectTrue(occurrences(sewers, "isCurrentEntry(") >= 4,
				"Sewers must validate each 10/5/10 delayed stage");
		expectTrue(sewers.contains(
						"gateId.equals(player.getPersistentData().getString(ENTRY_GATE_KEY))"),
				"Sewers callbacks must remain bound to the exact pending gate");
		expectTrue(sewers.contains("boolean newerEntryOwnsGate")
						&& sewers.contains(
								"generationWasClaimed && !newerEntryOwnsGate"),
				"An older callback must not roll back a replacement entry's gate claim");
	}

	private static void dkcLoginRecoveryCannotOutliveReset()
			throws IOException {
		String source = read("dkc", "DkcFloorRecoveryHandler.java");
		expectTrue(source.contains(
						"PlayerEntryGenerationGuard.capture(player)")
				&& source.contains(
						"PlayerEntryGenerationGuard.isCurrent(player"),
				"Queued DKC login recovery must not teleport a freshly reset player");
	}

	private static int occurrences(String source, String token) {
		int count = 0;
		for (int index = 0; (index = source.indexOf(token, index)) >= 0;
				index += token.length())
			count++;
		return count;
	}

	private static String read(String directory, String file)
			throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory).resolve(file));
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}
}
