package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free source regressions for the bounded Clear Dungeon command and
 * collision-safe Igris combat movement.
 */
public final class ShadowCommandSafetyRegression {
	private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "net", "solocraft");

	private ShadowCommandSafetyRegression() {
	}

	public static void main(String[] args) throws IOException {
		commandTickUsesRosterInsteadOfPlayerAreaScans();
		clearDungeonSearchRemainsSharedAndBounded();
		clearDungeonDisablesEveryOwnerFollowGoal();
		kaiselDefersClearTargetsToTheCoordinator();
		igrisCombatTeleportsRemainCollisionSafe();
		igrisDoesNotRestartAnActivePathEveryTick();
	}

	private static void commandTickUsesRosterInsteadOfPlayerAreaScans() throws IOException {
		String source = read("procedures", "ShadowCommandTickProcedure.java");
		expectTrue(source.contains("tickCommandedShadows(player)"),
				"The command tick must resolve the owner's authoritative summoned roster");
		expectFalse(source.contains("inflate(256"),
				"The command tick must not restore a 256-block entity scan per player");
		expectFalse(source.contains("getEntities("),
				"The command tick must not discover shadows through a level entity query");
	}

	private static void clearDungeonSearchRemainsSharedAndBounded() throws IOException {
		String source = read("util", "ShadowMonarchManager.java");
		expectTrue(source.contains("CLEAR_MAX_CANDIDATES = 128")
						&& source.contains("CLEAR_MAX_PATH_ATTEMPTS_PER_TICK = 8"),
				"Clear Dungeon must retain explicit candidate and pathfinder budgets");
		expectTrue(source.contains("tickClearDungeonCoordinator(owner, clearDungeonShadows)"),
				"Clear Dungeon assignment must remain shared by the owner's group");
		expectTrue(source.contains("encounter.trackedMobs()")
						&& source.contains("matchesDungeonContext"),
				"Clear Dungeon must preserve instance-aware target filtering");
		expectTrue(source.contains("CLEAR_FAILED_TARGET_COOLDOWN_TICKS")
						&& source.contains("CLEAR_STUCK_TICKS"),
				"Failed targets must retain cooldown and no-progress recovery");
		expectFalse(source.contains("inflate(96.0D), target -> isValidShadowTarget(target, shadow, owner)"),
				"Clear Dungeon must not restore the old per-shadow 96-block candidate scan");
	}

	private static void clearDungeonDisablesEveryOwnerFollowGoal() throws IOException {
		String predicate = read("procedures", "IsNotBerserkProcedure.java");
		expectTrue(predicate.contains("ShadowMonarchManager.shouldFollowOwner(entity)"),
				"The shared owner-follow predicate must reject Clear Dungeon");
		for (String entity : List.of(
				"BeruShadowEntity.java",
				"GoblinArcherShadowEntity.java",
				"GoblinMageShadowEntity.java",
				"IgrisShadowEntity.java",
				"KamishShadowEntity.java",
				"ShadowGreenOrcEntity.java",
				"ShadowHighOrcEntity.java",
				"ShadowIgrisEntity.java",
				"ShadowSold1Entity.java",
				"TuskShadowEntity.java")) {
			String source = read("entity", entity);
			expectTrue(source.contains("new FollowOwnerGoal"),
					entity + " must retain its normal recall goal");
			expectTrue(source.contains("canContinueToUse()")
							&& source.contains("IsNotBerserkProcedure.execute"),
					entity + " must stop an already-running recall goal when Clear Dungeon starts");
		}
	}

	private static void kaiselDefersClearTargetsToTheCoordinator() throws IOException {
		String source = read("entity", "ShadowKaiselinEntity.java");
		expectTrue(source.contains("isValidClearDungeonTarget")
						&& source.contains("shouldFollowOwner(this)"),
				"Kaisel must keep only safe coordinator targets and suppress owner recall during Clear");
	}

	private static void igrisCombatTeleportsRemainCollisionSafe() throws IOException {
		String helper = read("util", "IgrisCombatTeleportHelper.java");
		expectTrue(helper.contains("level.noCollision(igris, moved)")
						&& helper.contains("isLoadedAndInsideBorder")
						&& helper.contains("supportState.getCollisionShape"),
				"Igris combat teleports must validate collision, loaded bounds, and floor support");
		for (String procedure : List.of("IgrisHurtProcedure.java", "IgrisOnHitProcedure.java")) {
			String source = read("procedures", procedure);
			expectTrue(source.contains("IgrisCombatTeleportHelper"),
					procedure + " must use the shared safe teleport helper");
			expectFalse(source.contains(".teleportTo("),
					procedure + " must not restore an unchecked raw teleport");
		}
		String hurt = read("procedures", "IgrisHurtProcedure.java");
		int safeDodgeBranch = hurt.indexOf("if (IgrisCombatTeleportHelper.tryDodgeAttacker");
		String afterSafeDodge = safeDodgeBranch < 0 ? "" : hurt.substring(safeDodgeBranch);
		expectTrue(safeDodgeBranch >= 0 && afterSafeDodge.contains("event.setCanceled(true)"),
				"Igris dodge damage may only be canceled after a successful safe teleport");
	}

	private static void igrisDoesNotRestartAnActivePathEveryTick() throws IOException {
		String source = read("procedures", "IgrisShadowOnEntityTickUpdateProcedure.java");
		expectTrue(source.contains("mob.getNavigation().isDone()")
						&& source.contains("REPATH_INTERVAL_TICKS = 25"),
				"Igris may only recover a finished path on a bounded interval");
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
