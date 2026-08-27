package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free wiring regressions for the Mana Quiver runestone and the
 * equipped Arise boss-extraction path.
 */
public final class RangerAriseUnlockRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path ASSETS = Path.of(
			"src", "main", "resources", "assets", "sololeveling");

	private RangerAriseUnlockRegression() {
	}

	public static void main(String[] args) throws IOException {
		manaQuiverIsAUniversalRunestoneUnlock();
		rangersAlwaysReceiveManaQuiver();
		ariseIsEquippableAndUsesTheSkillDispatcher();
		ariseExtractsIgrisBeruAndKaisel();
		bossSoulsGuaranteeTheThirdEligibleAttempt();
		overwhelmingTargetsRespectCreativeBypass();
		legacyRightClickExtractionStaysDisabled();
	}

	private static void manaQuiverIsAUniversalRunestoneUnlock()
			throws IOException {
		String items = readMain("init", "SololevelingModItems.java");
		String tabs = readMain("init", "SololevelingModTabs.java");
		String procedure = readMain("procedures",
				"RunestoneManaQuiverRCProcedure.java");
		String ranger = readMain("util", "RangerCombatManager.java");
		String language = Files.readString(
				ASSETS.resolve(Path.of("lang", "en_us.json")));

		expectTrue(items.contains("RUNESTONE_MANA_QUIVER")
						&& items.contains("\"runestone_mana_quiver\"")
						&& tabs.contains("RUNESTONE_MANA_QUIVER.get()"),
				"Mana Quiver runestone must be registered and exposed in its tab");
		expectTrue(procedure.contains("learnManaQuiverFromRunestone"),
				"Mana Quiver must use its universal, token-safe unlock path");
		String learner = method(ranger,
				"public static void learnManaQuiverFromRunestone",
				"public static boolean activateSkill");
		expectFalse(learner.contains("isRanger(player)")
						|| learner.contains("ranger_only"),
				"Mana Quiver must not reject non-Ranger players");
		expectTrue(learner.contains("grantSkill(player, MANA_QUIVER)")
						&& learner.contains("stack.shrink(1)"),
				"Mana Quiver must unlock and consume its runestone normally");
		expectTrue(language.contains(
						"\"item.sololeveling.runestone_mana_quiver\"")
						&& language.contains(
								"\"tooltip.sololeveling.runestone_mana_quiver.unlock\": \"Right-click to learn Mana Quiver.\"")
						&& Files.isRegularFile(ASSETS.resolve(Path.of(
								"models", "item", "runestone_mana_quiver.json"))),
				"The runestone must have a model, name, and unlock tooltip");
	}

	private static void rangersAlwaysReceiveManaQuiver()
			throws IOException {
		String ranger = readMain("util", "RangerCombatManager.java");
		String reconcile = method(ranger,
				"public static void reconcileRanger",
				"public static boolean grantSkill");
		String awakening = readMain("util", "StatAwakeningManager.java");
		String starters = awakening.substring(
				awakening.indexOf("private static final String[][] STARTER_SKILLS"));

		expectTrue(reconcile.contains("ensureSkill(vars, MANA_QUIVER)"),
				"Ranger reconciliation must grant Mana Quiver at every Hunter rank");
		expectTrue(ranger.contains("slr_ranger_core_reconciled_v6"),
				"Existing Rangers must be migrated to the guaranteed Mana Quiver unlock");
		expectTrue(starters.contains(
						"{ RangerCombatManager.MANA_QUIVER, RangerCombatManager.BACK_STEP }"),
				"Class awakening must immediately grant Mana Quiver and Back Step");
	}

	private static void ariseIsEquippableAndUsesTheSkillDispatcher()
			throws IOException {
		String list = readMain("util", "SkillListHelper.java");
		String progression = readMain("util",
				"VesselProgressionManager.java");
		String jobs = readMain("util", "JobSkillManager.java");
		String overlay = readMain("client", "screens",
				"DisplayOverlay.java");

		expectTrue(list.contains("JobSkillManager.ARISE"),
				"Arise must appear in the unlocked skill equip list");
		expectTrue(progression.contains(
						"add(skills, JobSkillManager.ARISE, JobSkillManager.SHADOW_SUMMON"),
				"Shadow Monarch progression must grant Arise");
		expectTrue(jobs.contains(
						"case ARISE -> AriseSkillProcedure.execute")
						&& jobs.contains("case ARISE -> \"arise\""),
				"An equipped Arise must cast and report its real cooldown");
		expectTrue(overlay.contains("case JobSkillManager.ARISE"),
				"Equipped Arise must retain its skill-slot icon");
	}

	private static void ariseExtractsIgrisBeruAndKaisel()
			throws IOException {
		String arise = readMain("procedures", "AriseSkillProcedure.java");
		String igrisDeath = readMain("procedures",
				"BloodRedComIgrisDeathTimeIsReachedProcedure.java");
		String oldIgris = readMain("entity", "IgrisEntity.java");
		String kaisel = readMain("entity", "KaiselinEntity.java");

		expectTrue(arise.contains(
						"getEntitiesOfClass(IgrisDeadBodyEntity.class")
						&& arise.contains(
								"getEntitiesOfClass(BeruDeadBodyEntity.class"),
				"Arise must scan the visible Igris and Beru corpses");
		expectTrue(arise.contains(
						"case \"igris\" -> SololevelingModEntities.IGRIS_SHADOW")
						&& arise.contains(
								"case \"beru\" -> SololevelingModEntities.BERU_SHADOW")
						&& arise.contains(
								"case \"kaisel\" -> SololevelingModEntities.SHADOW_KAISELIN"),
				"All requested bosses must resolve to their real shadow entities");
		expectTrue(arise.contains("case \"igris\" ->")
						&& arise.contains("case \"beru\" ->")
						&& arise.contains("case \"kaisel\" ->")
						&& arise.contains("\"dkc_spawned_by\""),
				"Boss extraction must track ownership and protect owner-bound souls");
		expectFalse(igrisDeath.contains("dungeon_dimension_igris"),
				"Blood Red Igris must leave an extractable corpse outside one dimension");
		expectTrue(oldIgris.contains(
						"BloodRedComIgrisDeathTimeIsReachedProcedure.execute"),
				"The legacy Igris boss must also leave an extractable corpse");
		expectTrue(kaisel.contains("putString(\"soultype\", \"kaisel\")"),
				"Kaiselin must continue leaving its Kaisel extraction soul");
	}

	private static void bossSoulsGuaranteeTheThirdEligibleAttempt()
			throws IOException {
		for (String type : new String[] { "igris", "beru", "tusk", "kaisel" })
			expectTrue(AriseExtractionRules.isBossSoul(type),
					type + " must use boss-soul persistence");
		expectFalse(AriseExtractionRules.isBossSoul("highorc"),
				"Ordinary elite souls must retain their normal lifetime");
		expectTrue(AriseExtractionRules.MAX_BOSS_EXTRACTION_FAILURES == 3
						&& AriseExtractionRules.isGuaranteedAttempt(2)
						&& !AriseExtractionRules.isGuaranteedAttempt(1),
				"The third eligible extraction attempt must be guaranteed");

		String soulTick = readMain("procedures",
				"ShadowSoulOnEntityTickUpdateProcedure.java");
		String beruTick = readMain("procedures",
				"BeruDeadBodyOnEntityTickUpdateProcedure.java");
		String igrisBody = readMain("entity", "IgrisDeadBodyEntity.java");
		String arise = readMain("procedures", "AriseSkillProcedure.java");
		expectTrue(soulTick.contains("AriseExtractionRules.isBossSoul")
						&& soulTick.indexOf("AriseExtractionRules.isBossSoul")
								< soulTick.indexOf(
										"world.getLevelData().getGameTime() % 20"),
				"Boss ShadowSoul entities must bypass the short ordinary-soul timer");
		expectFalse(beruTick.contains("DATA_life"),
				"Beru's corpse must not retain its old 25-second despawn timer");
		expectTrue(igrisBody.contains("setPersistenceRequired()")
						&& igrisBody.contains(
								"public boolean removeWhenFarAway")
						&& igrisBody.contains(
								"builder.define(DATA_arise, 0)"),
				"Igris's corpse must persist and begin with zero failed attempts");
		expectTrue(arise.contains(
						"AriseExtractionRules.nextFailureCount")
						&& arise.contains(
								"AriseExtractionRules.isGuaranteedAttempt(previousFailures)")
						&& arise.contains("hasExtractionRights(player, target)"),
				"Arise must guarantee the third eligible attempt and enforce kill ownership");
	}

	private static void overwhelmingTargetsRespectCreativeBypass()
			throws IOException {
		expectFalse(AriseExtractionRules.isOverwhelming(30.0D, 70.0D,
						false),
				"A target forty levels stronger must remain attemptable");
		expectFalse(AriseExtractionRules.isOverwhelming(30.0D, 110.0D,
						true),
				"Creative mode must bypass the overwhelming-target gate");
		expectTrue(AriseExtractionRules.isOverwhelming(30.0D, 110.0D,
						false),
				"Only targets vastly stronger than the player may be too strong to extract");
		expectFalse(AriseExtractionRules.isOverwhelming(50.0D, 70.0D,
						false),
				"The easier extraction boundary must remain attemptable");
		expectTrue(AriseExtractionRules.successChance(30.0D, 70.0D,
						false) >= 0.49D,
				"A difficult but earned target must have a practical early attempt chance");
		expectTrue(AriseExtractionRules.successChance(1.0D, 100.0D,
						true) == 1.0D,
				"Creative extraction must not fail its post-gate random roll");

		String arise = readMain("procedures", "AriseSkillProcedure.java");
		String ordinarySpawner = readMain("procedures",
				"AriseDetectEntityProcedure.java");
		String igrisSpawner = readMain("procedures",
				"BloodRedComIgrisDeathTimeIsReachedProcedure.java");
		expectTrue(arise.contains(
						"player.getAbilities().instabuild")
						&& arise.contains(
								"AriseExtractionRules.isOverwhelming")
						&& arise.contains(
								"AriseExtractionRules.TARGET_LEVEL_TAG"),
				"Equipped Arise must enforce the easier target-strength rule with a creative bypass");
		expectTrue(ordinarySpawner.contains(
						"DungeonLevelHelper.levelOf(defeated)")
						&& ordinarySpawner.contains(
								"AriseExtractionRules.TARGET_LEVEL_TAG")
						&& ordinarySpawner.contains(
								"AriseExtractionRules.EXTRACTION_OWNER_TAG")
						&& ordinarySpawner.contains(
								"ShadowKillCreditHelper.creditedPlayerForDeath")
						&& igrisSpawner.contains(
								"DungeonLevelHelper.levelOf(entity)")
						&& igrisSpawner.contains(
								"AriseExtractionRules.EXTRACTION_OWNER_TAG"),
				"Spawned souls and boss corpses must retain target level and sole-kill ownership");
	}

	private static void legacyRightClickExtractionStaysDisabled()
			throws IOException {
		expectFalse(readMain("entity", "ShadowSoulEntity.java")
						.contains("ARISEProcedure.execute"),
				"Ordinary souls must be extracted through equipped Arise");
		expectFalse(readMain("entity", "IgrisDeadBodyEntity.java")
						.contains("IgrisDeadBodyRightClickedOnEntityProcedure.execute"),
				"Igris extraction must be driven by equipped Arise");
		expectFalse(readMain("entity", "BeruDeadBodyEntity.java")
						.contains("BeruDeadBodyRightClickedOnEntityProcedure.execute"),
				"Beru extraction must be driven by equipped Arise");
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
		Path path = MAIN;
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
