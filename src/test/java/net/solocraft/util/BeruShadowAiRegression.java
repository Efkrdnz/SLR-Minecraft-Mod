package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-contract regressions for Shadow Beru's hybrid ground/flight AI. */
public final class BeruShadowAiRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCES = Path.of(
			"src", "main", "resources");

	private BeruShadowAiRegression() {
	}

	public static void main(String[] args) throws IOException {
		groundCombatIsTheDefault();
		flightTargetsAndBossesUseDifferentPolicies();
		bossFlightIsBriefAndInfrequent();
		flightHasLayeredStuckRecovery();
		terrainAndWaterUseBoundedRecoveryFlight();
		airborneAnimationIsExplicitAndReliable();
	}

	private static void groundCombatIsTheDefault() throws IOException {
		String entity = readMain("entity", "BeruShadowEntity.java");
		expectTrue(entity.contains("return createGroundNavigation(world)")
				&& entity.contains("new GroundPathNavigation(this, world)"),
				"Shadow Beru must spawn with ground navigation");
		expectTrue(entity.contains("new LegacyMeleeAttackGoal(this, 1.35D, false)"),
				"Normal targets need a dedicated ground melee goal");
		expectTrue(entity.contains("tickGroundedOwnerFollow")
				&& entity.contains("EMERGENCY_RECALL_DISTANCE_SQR"),
				"Owner following must walk first and reserve flight for recovery");
	}

	private static void flightTargetsAndBossesUseDifferentPolicies()
			throws IOException {
		String policy = readMain("entity", "ai",
				"BeruShadowFlightPolicy.java");
		String tag = Files.readString(RESOURCES.resolve(Path.of(
				"data", "sololeveling", "tags", "entity_type",
				"beru_flight_targets.json")));
		expectTrue(policy.contains("target instanceof FlyingMob")
				&& policy.contains("target.isFallFlying()")
				&& policy.contains("target.isNoGravity() && !target.onGround()")
				&& policy.contains("beru_flight_targets"),
				"Sustained flight must recognize native, dynamic, and tagged flyers");
		expectTrue(tag.contains("minecraft:phantom")
				&& tag.contains("minecraft:ender_dragon")
				&& tag.contains("minecraft:wither")
				&& tag.contains("sololeveling:kaiselin")
				&& tag.contains("sololeveling:kamish"),
				"The built-in flight tag must cover primary vanilla and SLR flyers");
		expectTrue(policy.contains("Tags.EntityTypes.BOSSES")
				&& policy.contains("SOLO_BOSSES")
				&& policy.contains("DungeonMobLevelAdapter.MobRole.BOSS"),
				"Boss bursts must work for vanilla, SLR, and procedural bosses");
	}

	private static void bossFlightIsBriefAndInfrequent() throws IOException {
		String goal = readMain("entity", "ai",
				"BeruShadowAerialCombatGoal.java");
		int minimumDuration = intConstant(goal, "BOSS_FLIGHT_DURATION_MIN");
		int durationVariance = intConstant(goal,
				"BOSS_FLIGHT_DURATION_VARIANCE");
		int minimumCooldown = intConstant(goal, "BOSS_FLIGHT_COOLDOWN_MIN");
		expectTrue(minimumDuration > 0
				&& minimumDuration + durationVariance <= minimumCooldown,
				"Bosses must leave substantially more grounded time than flight time");
		expectTrue(goal.contains("BOSS_INITIAL_DELAY_MIN")
				&& goal.contains("bossFlightTicksRemaining")
				&& goal.contains("BeruShadowFlightPolicy.requiresSustainedFlight")
				&& goal.contains("BeruShadowFlightPolicy.isBossTarget"),
				"The aerial goal must separate permanent flyers from scheduled boss bursts");
	}

	private static void flightHasLayeredStuckRecovery() throws IOException {
		String goal = readMain("entity", "ai",
				"BeruShadowAerialCombatGoal.java");
		String controller = readMain("entity", "ai",
				"BeruFlightMoveControl.java");
		expectTrue(goal.contains("tickStuckRecovery")
				&& goal.contains("findEscapePoint")
				&& goal.contains("canFlyDirectlyTo")
				&& goal.contains("tryTeleportBehind(target, false)"),
				"Flight needs local escape steering and a last-resort safe reposition");
		expectTrue(controller.contains("isBlockedFor")
				&& controller.contains("resetBlockedState")
				&& controller.contains("velocity.length() / 0.30D")
				&& controller.contains("Vec3[] alternatives"),
				"The move controller must expose blockage and sample swept collision paths");
	}

	private static void terrainAndWaterUseBoundedRecoveryFlight()
			throws IOException {
		String entity = readMain("entity", "BeruShadowEntity.java");
		expectTrue(entity.contains("COMBAT_RECOVERY_FLIGHT_TICKS")
				&& entity.contains("updateCombatTraversalMovement")
				&& entity.contains("isInWaterOrBubble() || falling")
				&& entity.contains("beginTraversalRecoveryFlight")
				&& entity.contains("landedNearTarget"),
				"Water, falls, elevation, and stalled ground paths need bounded flight recovery followed by landing");
	}

	private static void airborneAnimationIsExplicitAndReliable()
			throws IOException {
		String entity = readMain("entity", "BeruShadowEntity.java");
		expectTrue(entity.contains("DATA_FLYING")
				&& entity.contains("updateFlightAnimationState")
				&& entity.contains("isFlyingAnimationActive()")
				&& entity.contains("|| !this.onGround();"),
				"Flight animation state must be synchronized independently of client grounding");
		expectTrue(entity.contains("triggerAnim(\"procedure\", animation)")
				&& entity.contains("triggerableAnim(\"start_flying\"")
				&& entity.contains("triggerableAnim(\"flyattack\"")
				&& entity.contains("thenLoop(\"flying\")"),
				"Takeoff, aerial attacks, and the continuous flight loop must all be registered");
		int predicate = entity.indexOf("private PlayState movementPredicate");
		int flying = entity.indexOf("isFlyingAnimationActive()", predicate);
		int idle = entity.indexOf("thenLoop(\"idle\")", predicate);
		expectTrue(predicate >= 0 && flying > predicate && idle > flying,
				"The movement predicate must choose flying before it can choose idle");
	}

	private static int intConstant(String source, String name) {
		Matcher matcher = Pattern.compile("private static final int " + name
				+ " = (\\d+);").matcher(source);
		if (!matcher.find())
			throw new AssertionError("Missing integer constant: " + name);
		return Integer.parseInt(matcher.group(1));
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
}
