package net.solocraft.util;

import net.solocraft.entity.ai.TuskShadowCombatPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Behaviour and source-contract regressions for Shadow Tusk's caster AI. */
public final class TuskShadowAiRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCES = Path.of(
			"src", "main", "resources");

	private TuskShadowAiRegression() {
	}

	public static void main(String[] args) throws IOException {
		rangedBandRulesAreStable();
		damageActuallyScalesWithShadowAttack();
		dedicatedCombatMovementIsRegistered();
		movementHasLayeredRecovery();
		spellsUseOneSafeScheduler();
		presentationMatchesImpactTimings();
		orbAmplificationCannotBeAppliedTwice();
	}

	private static void rangedBandRulesAreStable() {
		expectTrue(TuskShadowCombatPolicy.shouldRetreat(6.99D),
				"Targets inside seven blocks must make Tusk retreat");
		expectFalse(TuskShadowCombatPolicy.shouldRetreat(7.0D),
				"The retreat boundary must not jitter at exactly seven blocks");
		expectTrue(TuskShadowCombatPolicy.shouldApproach(21.0D, true),
				"Targets beyond the preferred range must be approached");
		expectTrue(TuskShadowCombatPolicy.shouldApproach(14.0D, false),
				"Blocked line of sight must force a new firing lane");
		expectTrue(TuskShadowCombatPolicy.isUsefulCastingPosition(14.0D,
				true), "Tusk needs a stable mid-range casting band");
		expectFalse(TuskShadowCombatPolicy.isUsefulCastingPosition(14.0D,
				false), "Tusk must never cast through a wall");
	}

	private static void damageActuallyScalesWithShadowAttack() {
		expectEquals(7.0F, TuskShadowCombatPolicy.soulFlameDamage(6.0D),
				"Soul Flame must preserve its early-game damage floor");
		expectEquals(21.0F, TuskShadowCombatPolicy.soulFlameDamage(30.0D),
				"Soul Flame must scale from leveled shadow attack");
		expectEquals(12.0F, TuskShadowCombatPolicy.curseFieldDamage(30.0D),
				"Curse Field must scale without becoming the single-target nuke");
		expectEquals(30.0F, TuskShadowCombatPolicy.groundSmashDamage(30.0D),
				"Ground Smash must carry a full attack-damage coefficient");
	}

	private static void dedicatedCombatMovementIsRegistered()
			throws IOException {
		String entity = readMain("entity", "TuskShadowEntity.java");
		expectTrue(entity.contains("new TuskShadowCombatGoal(this)")
				&& entity.contains("Attributes.FOLLOW_RANGE, 48")
				&& entity.indexOf("new TuskShadowCombatGoal(this)")
						< entity.indexOf("new ShadowFollowOwnerGoal(this)"),
				"Tusk needs a higher-priority combat mover and a useful target range");
	}

	private static void movementHasLayeredRecovery() throws IOException {
		String goal = readMain("entity", "ai", "TuskShadowCombatGoal.java");
		String manager = readMain("util", "ShadowMonarchManager.java");
		expectTrue(goal.contains("EnumSet.of(Flag.MOVE, Flag.LOOK)")
				&& goal.contains("tryCastingLane")
				&& goal.contains("DefaultRandomPos.getPosAway")
				&& goal.contains("tryRecoverStuckShadowNearOwner")
				&& goal.contains("TuskShadowCombatManager.isCasting"),
				"Movement must seek lanes, retreat, preserve casts, and recover stalls");
		expectTrue(manager.contains("tryRecoverStuckShadowNearOwner")
				&& manager.contains("tryRecallShadowNearOwner")
				&& manager.contains("isSafeShadowRecoveryPosition"),
				"Last-resort recall must reuse the collision-safe owner recovery gate");
		expectSame(TuskShadowCombatPolicy.RecoveryStage.REPATH,
				TuskShadowCombatPolicy.recoveryStage(20),
				"Twenty stalled ticks must trigger repathing");
		expectSame(TuskShadowCombatPolicy.RecoveryStage.ESCAPE,
				TuskShadowCombatPolicy.recoveryStage(40),
				"Forty stalled ticks must trigger terrain escape");
		expectSame(TuskShadowCombatPolicy.RecoveryStage.RECALL,
				TuskShadowCombatPolicy.recoveryStage(70),
				"Seventy stalled ticks must permit safe owner recovery");
	}

	private static void spellsUseOneSafeScheduler() throws IOException {
		String scheduler = readMain("util", "TuskShadowCombatManager.java");
		String procedure = readMain("procedures",
				"TuskShadowOnEntityTickUpdateProcedure.java");
		expectTrue(scheduler.contains("NEXT_GLOBAL_CAST_AT")
				&& scheduler.contains("CAST_RELEASE_AT")
				&& scheduler.contains("CAST_END_AT")
				&& scheduler.contains("getAttributeValue(Attributes.ATTACK_DAMAGE)")
				&& scheduler.contains("getSensing().hasLineOfSight")
				&& scheduler.contains("ShadowMonarchManager.canShadowDamage")
				&& scheduler.contains("CURSE_FIELD_MAX_TARGETS")
				&& scheduler.contains("GROUND_SMASH_MAX_TARGETS"),
				"Spells need one bounded, line-of-sight and friendly-fire-safe timeline");
		expectTrue(procedure.contains("TuskShadowCombatManager.tick(tusk)")
				&& !procedure.contains("DATA_fireball")
				&& !procedure.contains("DATA_smash")
				&& !procedure.contains("tryCombatSpells")
				&& procedure.contains("Math.floorMod(staggeredTick, 4)"),
				"The tick bridge must not retain legacy spell timers or particle spam");
	}

	private static void presentationMatchesImpactTimings() throws IOException {
		String animation = Files.readString(RESOURCES.resolve(Path.of(
				"assets", "sololeveling", "animations",
				"kardalgan_stand.animation.json")));
		int smash = animation.indexOf("\"groundsmash\"");
		int cast = animation.indexOf("\"cast\"", smash);
		String smashClip = smash >= 0 && cast > smash
				? animation.substring(smash, cast) : "";
		expectTrue(smashClip.contains("\"animation_length\": 1.25")
				&& smashClip.contains("\"0.85\"")
				&& !smashClip.contains("[3, 3, 3]"),
				"Ground Smash must be short, impact-readable, and free of 3x body scaling");
	}

	private static void orbAmplificationCannotBeAppliedTwice()
			throws IOException {
		String equipment = readMain("util",
				"ShadowEquipmentCombatHandler.java");
		String scheduler = readMain("util", "TuskShadowCombatManager.java");
		expectTrue(equipment.contains("TUSK_ORB_DAMAGE_MULTIPLIER = 2.0F")
				&& equipment.contains("event.setAmount(event.getAmount()")
				&& !scheduler.contains("ORB_OF_AVARICE")
				&& !scheduler.contains("TUSK_ORB_DAMAGE_MULTIPLIER"),
				"The existing damage event must remain the Orb's only multiplier");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expectEquals(float expected, float actual,
			String message) {
		if (Math.abs(expected - actual) > 0.0001F)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectSame(Object expected, Object actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
