package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source regressions for shadow roster continuity across owner
 * death, shadow death, dismissal and later re-summoning.
 */
public final class ShadowLevelPersistenceRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private ShadowLevelPersistenceRegression() {
	}

	public static void main(String[] args) throws IOException {
		ownerDeathCopiesTheAuthoritativeRoster();
		failedRevivalDoesNotDeleteShadowOwnership();
		shadowDeathReleasesRatherThanDeletesItsRosterSlot();
		reSummoningReusesTheExistingProgressRecord();
		despawnAndLogoutKeepRosterProgress();
	}

	private static void ownerDeathCopiesTheAuthoritativeRoster()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String cloneMethod = method(manager,
				"public static void preserveProgressAfterPlayerClone",
				"public static void handleTrackedShadowDeath");
		expectTrue(cloneMethod.contains(
				"replacementData.put(ROOT, originalData.getCompound(ROOT).copy())"),
				"Death cloning must deep-copy the roster containing shadow level and XP");
		expectTrue(cloneMethod.contains("PLAYER_COMMAND")
						&& cloneMethod.contains("PLAYER_RESET_GENERATION"),
				"Death cloning must preserve command mode and reset generation with the roster");
		expectFalse(cloneMethod.contains("ensureRoster(")
						|| cloneMethod.contains("createShadow("),
				"Death cloning must never reconstruct level-one roster entries");

		String events = read("procedures",
				"ShadowInventoryDeathDropProcedure.java");
		expectTrue(events.contains(
				"@SubscribeEvent(priority = EventPriority.HIGHEST)")
						&& events.contains(
								"preserveProgressAfterPlayerClone("),
				"The roster copy must run at the start of PlayerEvent.Clone");
	}

	private static void failedRevivalDoesNotDeleteShadowOwnership()
			throws IOException {
		String revival = read("procedures",
				"ShadowDeathReviveProcedure.java");
		expectTrue(revival.contains(
				"if (variables == null || variables.MP < manaCost)")
						&& revival.indexOf("variables.MP < manaCost")
								< revival.indexOf("event.setCanceled(true)"),
				"Insufficient mana must permit a true shadow death");
		for (String ownershipCounter : new String[] {
				"ordshadowmax",
				"GobShadowMax",
				"ShadowGoblinArcherMax",
				"ShadowGoblinMageMax",
				"WolfShadowMax",
				"orcmax",
				"highorcmax",
				"polarbearmax",
				"berumax",
				"shadowdragonmax",
				"tuskmax",
				"shadowstorageusage" }) {
			expectFalse(revival.contains(ownershipCounter),
					"A true death must not reduce ownership/storage: "
							+ ownershipCounter);
		}
		expectFalse(revival.contains("OrdShadow - 1")
						|| revival.contains("GobShadow - 1")
						|| revival.contains("spawned - 1")
						|| revival.contains("Spawned - 1"),
				"The revival handler must leave active-count release to the exact roster handler");
	}

	private static void shadowDeathReleasesRatherThanDeletesItsRosterSlot()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String deathMethod = method(manager,
				"public static void handleTrackedShadowDeath",
				"public static void saveBossHealthBeforeDespawn");
		expectTrue(deathMethod.contains(
				"shadowEntity.getUUID().equals(shadow.getUUID(\"summoned\"))"),
				"A dying entity may only release its own exact summoned slot");
		expectTrue(deathMethod.contains("shadow.remove(\"summoned\")")
						&& deathMethod.contains(
								"owner.getPersistentData().put(ROOT, root(owner))")
						&& deathMethod.contains(
								"updateLegacySpawnCounter(owner, type, -1)"),
				"Shadow death must persist the existing roster slot as available");
		expectFalse(deathMethod.contains("shadows.remove(")
						|| deathMethod.contains("shadow.putInt(\"level\", 1)")
						|| deathMethod.contains("shadow.putInt(\"xp\", 0)"),
				"Shadow death must not delete or reset level/XP");

		String events = read("procedures",
				"ShadowInventoryDeathDropProcedure.java");
		int drop = events.indexOf("dropStoredShadowInventory(event.getEntity())");
		int release = events.indexOf("handleTrackedShadowDeath(event.getEntity())");
		expectTrue(drop >= 0 && release > drop,
				"Death handling must save/drop carried items before releasing the slot");
		expectTrue(events.contains(
				"@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)"),
				"Shadow death bookkeeping must run after and ignore successful revival cancellation");
	}

	private static void reSummoningReusesTheExistingProgressRecord()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String available = method(manager,
				"private static CompoundTag firstAvailableShadow",
				"private static List<CompoundTag> ownedRosterWithinLimit");
		expectTrue(available.contains("shadow.remove(\"summoned\")")
						&& available.contains("best = shadow")
						&& available.contains("return best"),
				"A dead or missing summon must make its existing roster entry reusable");
		String summon = method(manager,
				"private static boolean summonShadow",
				"private static boolean recallShadowFromOtherDimension");
		expectTrue(summon.contains("tagSummonedEntity(owner, shadow, spawned)"),
				"Re-summoning must tag the new entity from the retained roster record");
		expectFalse(summon.contains("createShadow("),
				"Re-summoning a roster entry must not create a duplicate level-one shadow");
	}

	private static void despawnAndLogoutKeepRosterProgress()
			throws IOException {
		String dismissal = read("procedures", "Ability3ResetProcedure.java");
		expectTrue(dismissal.contains("saveBossHealthBeforeDespawn")
						&& dismissal.contains("dropStoredShadowInventory")
						&& dismissal.contains("entityiterator.discard()"),
				"Dismissal/logout must despawn entities while retaining roster records");
		expectFalse(dismissal.contains("sololeveling_shadow_monarch")
						|| dismissal.contains("putInt(\"level\", 1)")
						|| dismissal.contains("putInt(\"xp\", 0)"),
				"Dismissal/logout must not reset the authoritative roster");
	}

	private static String method(String source, String startMarker,
			String endMarker) {
		int start = source.indexOf(startMarker);
		int end = start < 0 ? -1 : source.indexOf(endMarker,
				start + startMarker.length());
		expectTrue(start >= 0 && end > start,
				"Could not isolate method: " + startMarker);
		return source.substring(start, end);
	}

	private static String read(String directory, String file)
			throws IOException {
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
