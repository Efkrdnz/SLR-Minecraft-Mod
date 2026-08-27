package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free source regressions for shared shadow commands, safe aerial
 * movement, bounded Clear Dungeon behavior, and village utility generation.
 */
public final class ShadowCommandSafetyRegression {
	private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "net", "solocraft");
	private static final Path MAIN_RESOURCES = Path.of("src", "main", "resources");

	private ShadowCommandSafetyRegression() {
	}

	public static void main(String[] args) throws IOException {
		commandTickUsesRosterInsteadOfPlayerAreaScans();
		clearDungeonSearchRemainsSharedAndBounded();
		everySummonedShadowUsesAuthoritativeCommandTargeting();
		groundShadowsUseSharedCommandAwareRecall();
		ownerCombatAlwaysOutranksCommandMode();
		kaiselDefersClearTargetsToTheCoordinator();
		shadowBeruUsesSafeHybridCombat();
		villageGenerationOwnsBothUtilityStructures();
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
		expectTrue(source.contains("prepareShadowTraversal")
						&& source.contains("setCanFloat(true)")
						&& source.contains("getAttribute(Attributes.STEP_HEIGHT).setBaseValue")
						&& source.contains("tickClearTraversal"),
				"Shared shadows need water, step-height, and travel-objective recovery");
		expectFalse(source.contains("inflate(96.0D), target -> isValidShadowTarget(target, shadow, owner)"),
				"Clear Dungeon must not restore the old per-shadow 96-block candidate scan");
	}

	private static void everySummonedShadowUsesAuthoritativeCommandTargeting()
			throws IOException {
		for (String entity : List.of(
				"BeruShadowEntity.java",
				"GoblinClubShadowEntity.java",
				"GoblinArcherShadowEntity.java",
				"GoblinMageShadowEntity.java",
				"IgrisShadowEntity.java",
				"KamishShadowEntity.java",
				"OrcShadowEntity.java",
				"ShadowGreenOrcEntity.java",
				"ShadowHighOrcEntity.java",
				"ShadowPolarBearEntity.java",
				"ShadowSold1Entity.java",
				"SteelFangWolfShadowEntity.java",
				"TuskShadowEntity.java")) {
			String source = read("entity", entity);
			expectTrue(source.contains(
							"targetSelector.addGoal(0, new ShadowCommandTargetGoal(this))"),
					entity + " must let the shared command goal own target selection");
			expectFalse(source.contains("new OwnerHurtTargetGoal")
							|| source.contains("new OwnerHurtByTargetGoal"),
					entity + " must not retain a generated target goal that can override commands");
		}
	}

	private static void groundShadowsUseSharedCommandAwareRecall()
			throws IOException {
		for (String entity : List.of(
				"GoblinClubShadowEntity.java",
				"GoblinArcherShadowEntity.java",
				"GoblinMageShadowEntity.java",
				"IgrisShadowEntity.java",
				"KamishShadowEntity.java",
				"OrcShadowEntity.java",
				"ShadowGreenOrcEntity.java",
				"ShadowHighOrcEntity.java",
				"ShadowPolarBearEntity.java",
				"ShadowSold1Entity.java",
				"SteelFangWolfShadowEntity.java",
				"TuskShadowEntity.java")) {
			String source = read("entity", entity);
			expectTrue(source.contains("new ShadowFollowOwnerGoal(this)"),
					entity + " must use the shared close-range owner recall goal");
			expectFalse(source.contains("new FollowParentGoal"),
					entity + " must never try to follow a non-existent animal parent");
		}
		String follow = read("entity", "ai", "ShadowFollowOwnerGoal.java");
		expectTrue(follow.contains("1.4D, 8.0F, 3.0F")
						&& count(follow, "ShadowMonarchManager.shouldFollowOwner") >= 2
						&& count(follow, "!hasLiveTarget()") >= 2,
				"Recall must start promptly and stop immediately when the command mode disallows following");
	}

	private static void ownerCombatAlwaysOutranksCommandMode()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		expectTrue(manager.contains("findOwnerCombatPriorityTarget")
						&& manager.contains("getLastHurtMobTimestamp()")
						&& manager.contains("getLastHurtByMobTimestamp()"),
				"Owner attacks and incoming attacks must be ordered by their latest combat timestamp");
		expectTrue(manager.contains("if (applyOwnerCombatPriority(shadow, owner))")
						&& manager.contains("if (applyOwnerCombatPriority(mob, owner))"),
				"Owner combat intent must be checked before every command-mode assignment");
		String goal = read("entity", "ai", "ShadowCommandTargetGoal.java");
		expectTrue(goal.contains("void tick()")
						&& goal.contains("tickShadowTargeting(shadow)")
						&& goal.contains("Flag.TARGET"),
				"Every shadow must refresh owner combat intent each AI tick while blocking lower-priority selectors");
	}

	private static void kaiselDefersClearTargetsToTheCoordinator() throws IOException {
		String source = read("entity", "ShadowKaiselinEntity.java");
		expectTrue(source.contains("isValidClearDungeonTarget")
						&& source.contains("shouldFollowOwner(this)")
						&& source.contains("findOwnerCombatPriorityTarget(this, owner)"),
				"Kaisel must keep only safe coordinator targets and suppress owner recall during Clear");
	}

	private static void shadowBeruUsesSafeHybridCombat() throws IOException {
		String entity = read("entity", "BeruShadowEntity.java");
		expectTrue(entity.contains("new BeruFlightMoveControl")
						&& entity.contains("new FlyingPathNavigation")
						&& entity.contains("new GroundPathNavigation")
						&& entity.contains("new LegacyMeleeAttackGoal")
						&& entity.contains("new BeruShadowAerialCombatGoal(this)")
						&& entity.contains("builder.add(Attributes.FLYING_SPEED"),
				"Shadow Beru must support both grounded melee and selective three-dimensional combat");
		expectFalse(entity.contains("new FlyingMoveControl"),
				"Shadow Beru must not restore vanilla's slow-gliding flight controller");
		expectFalse(entity.contains("new OpenDoorGoal"),
				"Flying Shadow Beru must not construct vanilla's ground-navigation-only door goal");
		expectTrue(entity.contains("updateNonCombatMovement")
						&& entity.contains("tickGroundedOwnerFollow")
						&& entity.contains("tickEmergencyRecallFlight")
						&& entity.contains("this.setFlightMode(false)")
						&& entity.contains("ShadowMonarchManager.shouldFollowOwner(this)")
						&& entity.contains("getMoveControl().setWantedPosition"),
				"Shadow Beru must follow on foot, restore gravity, and bound emergency recall flight");
		expectFalse(entity.contains("\"attacking\", 4, this::attackingPredicate"),
				"Beru's generic ground attack controller must not override authored aerial animations");

		String controller = read("entity", "ai", "BeruFlightMoveControl.java");
		expectTrue(controller.contains("collisionSafeVelocity")
						&& controller.contains("brake()")
						&& controller.contains("setDirectVelocity")
						&& controller.contains("Attributes.FLYING_SPEED"),
				"Shadow Beru flight must accelerate, brake, and steer around collision");

		String goal = read("entity", "ai", "BeruShadowAerialCombatGoal.java");
		expectTrue(goal.contains("FlightPhase.ASCEND")
						&& goal.contains("FlightPhase.DIVE")
						&& goal.contains("performDiveSlam")
						&& goal.contains("tickAerialDash"),
				"Shadow Beru must retain aerial pursuit, dash strikes, and the boss-style dive slam");
		expectTrue(goal.contains("ShadowMonarchManager.canShadowDamage")
						&& goal.contains("damageSources().mobAttack(beru)"),
				"Every scripted Shadow Beru hit must preserve shadow ownership and damage credit");
		expectTrue(goal.contains("level().noCollision(beru, movedBounds)")
						&& goal.contains("getWorldBorder()")
						&& goal.contains("hasChunkAt(blockPos)"),
				"Shadow Beru's boss-style teleport must be collision-, border-, and chunk-safe");
	}

	private static void villageGenerationOwnsBothUtilityStructures()
			throws IOException {
		String injector = read("worldgen",
				"VillageUtilityStructureInjector.java");
		expectTrue(injector.contains("path.startsWith(\"village/\")")
						&& injector.contains("path.endsWith(\"/town_centers\")"),
				"Utility pieces must be limited to vanilla village start pools");
		expectTrue(count(injector, "addPiece(context, pieces,") == 2
						&& injector.contains("\"istanceenterance\"")
						&& injector.contains("\"evaluation\""),
				"Each village must receive exactly one instance entrance and one evaluator");
		expectTrue(injector.contains("findCollisionPiece")
						&& injector.contains("WORLD_SURFACE_WG")
						&& injector.contains("OCEAN_FLOOR_WG"),
				"Village utility placement must check existing pieces, terrain slope, and water depth");
		String villageMixin = read("mixins",
				"VillageJigsawPlacementMixin.java");
		expectTrue(villageMixin.contains("@Mixin(JigsawStructure.class)")
						&& villageMixin.contains("findGenerationPoint(")
						&& villageMixin.contains("this.startPool"),
				"Village utilities must be appended outside the placement hook replaced by Loquat and rewritten by Waystones");
		expectFalse(villageMixin.contains("@Mixin(JigsawPlacement.class)"),
				"Village utility generation must not compete with mods that rewrite JigsawPlacement");
		expectFalse(villageMixin.contains("net.blay09")
						|| villageMixin.contains("snownee.loquat"),
				"Village compatibility must avoid direct dependencies on other mods' implementation classes");

		String mixins = Files.readString(
				MAIN_RESOURCES.resolve("mixins.sololeveling.json"));
		expectTrue(mixins.contains("VillageJigsawPlacementMixin"),
				"The village piece injector must be active in the production mixin config");
		Path structureSets = MAIN_RESOURCES.resolve(Path.of("data",
				"sololeveling", "worldgen", "structure_set"));
		expectFalse(Files.exists(structureSets.resolve(
						"instance_dungeon_enterance.json"))
						|| Files.exists(structureSets.resolve("evaluator.json")),
				"The two utility buildings must not retain independent random-spread generation");
		Path structures = MAIN_RESOURCES.resolve(Path.of("data",
				"sololeveling", "worldgen", "structure"));
		expectTrue(Files.exists(structures.resolve(
						"instance_dungeon_enterance.json"))
						&& Files.exists(structures.resolve("evaluator.json")),
				"The named structure definitions must remain available for explicit /place structure use");
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

	private static String read(String firstDirectory, String secondDirectory,
			String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(firstDirectory)
				.resolve(secondDirectory).resolve(file));
	}

	private static int count(String source, String needle) {
		int matches = 0;
		for (int index = source.indexOf(needle); index >= 0;
				index = source.indexOf(needle, index + needle.length()))
			matches++;
		return matches;
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
