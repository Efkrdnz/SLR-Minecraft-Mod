package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source-contract regressions for opt-in ability terrain
 * destruction, its world-creation control, and the shared safety envelope.
 */
public final class AbilityDestructionRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net",
			"solocraft");
	private static final Path RESOURCES = Path.of("src", "main", "resources");

	private AbilityDestructionRegression() {
	}

	public static void main(String[] args) throws IOException {
		gameruleIsOptInAndLivesInTheSoloLevelingTab();
		sharedEngineKeepsWorkBoundedAndLoaded();
		protectedBlocksAndNeoForgeHooksPrecedeDropFreeMutation();
		abilityBreakEventsCannotFarmDailyObjectives();
		representativeAbilitiesUseSharedScaledProfiles();
		antaresOwnsTheLargestBoundedDestructionSuite();
		legacyExplosionsCannotBypassTheGamerule();
	}

	private static void gameruleIsOptInAndLivesInTheSoloLevelingTab()
			throws IOException {
		String rules = readMain("init", "SololevelingModGameRules.java");
		String tab = readMain("client", "gui", "worldcreation",
				"SoloLevelingWorldCreationTab.java");
		String mixin = readMain("mixins", "CreateWorldScreenMixin.java");
		String legacy = readMain("procedures",
				"WorldGriefingFixTemporaryProcedure.java");
		String language = Files.readString(RESOURCES.resolve(Path.of("assets",
				"sololeveling", "lang", "en_us.json")));

		assertContains(rules,
				"SOLO_ABILITY_DESTRUCTION = GameRules.register(\"soloAbilityDestruction\"",
				"SOLO_ABILITY_DESTRUCTION_MODE = GameRules.register(\"soloAbilityDestructionMode\"",
				"GameRules.IntegerValue.create(-1)",
				"enum AbilityDestructionMode",
				"FALSE(0, \"false\")",
				"PARTIAL(1, \"partial\")",
				"TRUE(2, \"true\")",
				"SOLO_WORLD_GRIEFING = SOLO_ABILITY_DESTRUCTION",
				"setAbilityDestructionMode");
		expectEquals(1, occurrences(rules,
				"GameRules.register(\"soloAbilityDestruction\""),
				"Ability destruction must have one authoritative gamerule");
		expectFalse(rules.contains("GameRules.register(\"soloWorldGriefing\""),
				"The retired forced-on gamerule must not be registered separately");
		expectFalse(legacy.contains("@SubscribeEvent")
					|| legacy.contains(".set(true")
					|| legacy.contains("BooleanValue.create(true)"),
				"The legacy compatibility shim must never force destruction on");

		assertContains(tab,
				"private final CycleButton<SololevelingModGameRules.AbilityDestructionMode> abilityDestruction",
				"SololevelingModGameRules.abilityDestructionMode(rules)",
				"Component.literal(\"Ability Destruction\")",
				"SololevelingModGameRules.setAbilityDestructionMode(",
				"this.abilityDestruction.setValue(",
				"rows.addChild(this.abilityDestruction, 2",
				"Red Gates and the Demon King's Castle are always protected.");
		assertOrdered(tab,
				"rows.addChild(this.storyMode, 2",
				"rows.addChild(this.abilityDestruction, 2",
				"Settings are stored with this world.");
		assertContains(mixin,
				"sololeveling$appendWorldCreationTab",
				"new SoloLevelingWorldCreationTab(",
				"Arrays.copyOf(existingTabs, existingTabs.length + 1)");
		assertContains(language,
				"\"gamerule.soloAbilityDestruction\"",
				"\"gamerule.soloAbilityDestruction.description\"",
				"\"gamerule.soloAbilityDestructionMode\"",
				"\"gamerule.soloAbilityDestructionMode.description\"");
	}

	private static void sharedEngineKeepsWorkBoundedAndLoaded()
			throws IOException {
		String manager = readMain("util", "AbilityDestructionManager.java");
		assertContains(manager,
				"MAX_MUTATIONS_PER_TICK = 96",
				"MAX_INSPECTIONS_PER_TICK = 512",
				"MAX_MUTATIONS_PER_JOB_PER_TICK = 48",
				"MAX_QUEUED_POSITIONS = 24576",
				"MAX_QUEUED_JOBS = 128",
				"MAX_QUEUED_POSITIONS_PER_OWNER = 6144",
				"MAX_QUEUED_JOBS_PER_OWNER = 24",
				"MAX_REQUESTS_PER_OWNER_PER_TICK = 4",
				"MAX_REQUESTS_PER_TICK = 24",
				"level.getGameTime() + 200L",
				"private static final Set<QueuedBlock> QUEUED_BLOCKS",
				"QUEUED_BLOCKS.contains(queued)",
				"queuedPositions >= MAX_QUEUED_POSITIONS",
				"false",
				"player.getId() != job.ownerEntityId",
				"!enabled(player)",
				"level.hasChunkAt(pos)",
				"player.serverLevel().hasChunkAt(BlockPos.containing(point))",
				"level.getMinBuildHeight()",
				"level.getMaxBuildHeight()",
				"level.getWorldBorder().isWithinBounds(pos)",
				"Double.isFinite(attribute)",
				"Math.log1p(Math.max(0.0D, attribute))",
				"Math.log1p(attributeCeiling(profile))",
				"profile.maximumBudget",
				"Mth.clamp(visualRadius, 0.5D, 24.0D)",
				"scaled.budget + reserve");
		assertContains(manager,
				"return reserveRequest(player)",
				"queuedJobsFor(player.getUUID())",
				"queuedPositionsFor(player.getUUID())");
		assertContains(manager,
				"SololevelingModGameRules.abilityDestructionMode(",
				"!DkcFloorRegistry.isDkc(level)",
				"!SnowRedGateArenaManager.isRedGateDimension(level.dimension())",
				"AbilityDestructionMode.PARTIAL",
				"private static boolean isDungeonContext(ServerPlayer player)",
				"DungeonInstanceSavedData.get(level).listInstances()");
		expectFalse(manager.contains("getChunk(")
					|| manager.contains("getChunkAt(")
					|| manager.contains("setChunkForced("),
				"Ability destruction must never load or force a chunk");
		expectFalse(manager.contains(".explode(")
					|| manager.contains("ExplosionInteraction"),
				"The shared engine must not use unbounded vanilla explosions");
		expectFalse(manager.contains("scaled.budget * 3"),
				"Large jobs must not waste the queue on three times their usable budget");

		String fissure = section(manager,
				"public static void fissure(",
				"/** Queues a fractured annulus");
		assertContains(fissure,
				"double depth = Mth.clamp(0.85D + scaled.radius * 0.25D",
				"origin.y - depth",
				"origin.y + 0.15D",
				"schedule(player, profile, scaled, candidates)");
		expectFalse(fissure.contains("line(player"),
				"Ground fissures must be shallow surface scars, not buried cylinders");

		String ring = section(manager,
				"public static void ring(",
				"@SubscribeEvent");
		assertContains(ring,
				"Math.max(scaled.radius, visualRadius)",
				"Math.sin(Math.atan2(dz, dx) * 4.0D)",
				"spokeError <= 0.12D");
		expectFalse(ring.contains("visualRadius * 0.58D"),
				"Shockwave terrain must reach the supplied visual radius");
	}

	private static void protectedBlocksAndNeoForgeHooksPrecedeDropFreeMutation()
			throws IOException {
		String manager = readMain("util", "AbilityDestructionManager.java");
		String destroy = section(manager,
				"private static boolean destroyOne(",
				"private static boolean cheapCandidate(");
		String candidates = section(manager,
				"private static boolean cheapCandidate(",
				"private static boolean canSchedule(");
		String immune = Files.readString(RESOURCES.resolve(Path.of("data",
				"sololeveling", "tags", "block",
				"ability_destruction_immune.json")));

		assertOrdered(destroy,
				"!enabled(player)",
				"!cheapCandidate(level, pos, maximumHardness)",
				"!level.mayInteract(player, pos)",
				"!player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY)",
				"level.getBlockState(pos).canEntityDestroy(level, pos, player)",
				"new BlockEvent.BreakEvent",
				"NeoForge.EVENT_BUS.post(event)",
				"EventHooks.onEntityDestroyBlock",
				"state.onDestroyedByPlayer(level, pos, player, false",
				"state.getBlock().destroy(level, pos, state)");
		assertContains(destroy,
				"POSTING_BREAK_EVENT.set(true)",
				"finally",
				"POSTING_BREAK_EVENT.set(false)",
				"!level.getBlockState(pos).equals(state)",
				"!cheapCandidate(level, pos, maximumHardness)");
		expectFalse(destroy.contains("destroyBlock(")
					|| destroy.contains("dropResources(")
					|| destroy.contains("popResource("),
				"Ability terrain changes must suppress drops and avoid bypass helpers");

		assertContains(candidates,
				"state.hasBlockEntity()",
				"level.getBlockEntity(pos) != null",
				"!state.getFluidState().isEmpty()",
				"state.is(IMMUNE_BLOCKS)",
				"state.is(BlockTags.WITHER_IMMUNE)",
				"state.getDestroySpeed(level, pos)",
				"hardness >= 0.0F && hardness <= maximumHardness");
		for (String id : new String[] {"minecraft:bedrock",
				"minecraft:command_block", "minecraft:structure_block",
				"minecraft:end_portal", "minecraft:spawner",
				"sololeveling:dungeon_wall",
				"sololeveling:unbreakable_deepslate",
				"sololeveling:custom_portal",
				"sololeveling:hunter_rank_evaluator"})
			expectTrue(immune.contains("\"" + id + "\""),
					"Missing protected destruction tag entry: " + id);
	}

	private static void abilityBreakEventsCannotFarmDailyObjectives()
			throws IOException {
		String manager = readMain("util", "AbilityDestructionManager.java");
		String daily = readMain("util", "daily",
				"DailyQuestObjectiveManager.java");
		String handler = section(daily,
				"public static void onBlockBroken(BlockEvent.BreakEvent event)",
				"public static void onLivingDeath(LivingDeathEvent event)");
		assertContains(manager,
				"public static boolean isPostingAbilityBreakEvent()",
				"return POSTING_BREAK_EVENT.get()");
		assertContains(handler,
				"AbilityDestructionManager.isPostingAbilityBreakEvent()",
				"return;");
		assertOrdered(handler,
				"AbilityDestructionManager.isPostingAbilityBreakEvent()",
				"!isSurvival(player)",
				"recordMinedBlock(player, event.getPos())");
	}

	private static void representativeAbilitiesUseSharedScaledProfiles()
			throws IOException {
		assertProfile("util", "GoliathCombatManager.java",
				"GOLIATH_SMASH", "GOLIATH_COLLAPSE",
				"GOLIATH_PURSUIT_PATH", "GOLIATH_PURSUIT_IMPACT");
		String goliath = readMain("util", "GoliathCombatManager.java");
		String enhancedStrike = section(goliath,
				"public static void enhancedStrike(Player player)",
				"public static void castCapture(Entity entity)");
		expectFalse(enhancedStrike.contains("AbilityDestructionManager."),
				"Every Goliath enhanced-strike left click must remain terrain-safe");
		assertProfile("util", "BeastMonarchManager.java",
				"BEAST_CLAW_RIFT", "BEAST_RUBBLE_JAW");
		assertProfile("util", "FireMageSpellManager.java",
				"FIRE_ORB", "FIRE_DOMINION", "FIRE_HEAVENFALL");
		assertProfile("util", "StormMageSpellManager.java",
				"STORM_THUNDERCLAP", "STORM_SKYBREAKER");
		assertProfile("util", "FrostMonarchManager.java", "FROST_SPEAR");
		assertProfile("util", "ArcaneMageSpellManager.java",
				"ARCANE_IMPACT", "ARCANE_CONVERGENCE");
		assertProfile("util", "BarrierMageSpellManager.java",
				"BARRIER_COLLAPSE", "BARRIER_CATASTROPHE");
		assertProfile("util", "TankerSkillManager.java", "TANKER_SLAM");
		assertProfile("procedures", "UpforceSlashProcedure.java",
				"FIGHTER_SLAM");
		assertProfile("entity", "SwordBeamProjectileEntity.java",
				"RANKER_IMPACT");
		assertProfile("util", "WhiteFlameMonarchManager.java",
				"WHITE_FLAME_BREATH", "WHITE_FLAME_HELLSTORM");
		String whiteFlame = readMain("util", "WhiteFlameMonarchManager.java");
		assertContains(whiteFlame,
				"level.clip(new ClipContext(eye, intendedEnd",
				"terrainHit.getType() == HitResult.Type.BLOCK",
				"AbilityDestructionManager.Profile.WHITE_FLAME_BREATH, eye",
				"terrainEnd,");
		assertProfile("entity", "RadiruBloodSpearEntity.java",
				"WHITE_FLAME_SPEAR");
		assertProfile("procedures", "FireReleaseBeamProcedure.java",
				"FIRE_BEAM_CHARGED", "FIRE_BEAM_OVERCHARGED");
		assertProfile("util", "GrandMarshalAbilityManager.java",
				"GRAND_MARSHAL_GRAVITY", "GRAND_MARSHAL_DREAD",
				"GRAND_MARSHAL_SKY_REND", "LIU_SWORD_CUT");

		for (String file : new String[] {"GoliathCombatManager.java",
				"BeastMonarchManager.java", "FireMageSpellManager.java",
				"StormMageSpellManager.java", "FrostMonarchManager.java",
				"ArcaneMageSpellManager.java", "BarrierMageSpellManager.java",
				"TankerSkillManager.java"}) {
			String source = readMain("util", file);
			expectTrue(source.contains("TemporaryStatBonusManager.effective")
						|| source.contains("effectiveStrength(player)")
						|| source.contains("effectiveIntelligence(player)")
						|| source.contains("MageCombatHelper.intelligence("),
					file + " destruction must be driven by an ability attribute");
		}
	}

	private static void antaresOwnsTheLargestBoundedDestructionSuite()
			throws IOException {
		String profiles = readMain("util", "AbilityDestructionManager.java");
		String antares = readMain("util", "AntaresCombatManager.java");
		assertContains(antares,
				"AbilityDestructionManager.Profile.ANTARES_CLAW",
				"AbilityDestructionManager.Profile.ANTARES_CLAW_FINISH",
				"AbilityDestructionManager.Profile.ANTARES_BREATH",
				"AbilityDestructionManager.Profile.ANTARES_DESCENT",
				"AbilityDestructionManager.Profile.ANTARES_ROAR",
				"AbilityDestructionManager.Profile.ANTARES_EXTINCTION",
				"AbilityDestructionManager.Profile.ANTARES_EXTINCTION_FINISH",
				"AbilityDestructionManager.ring(player",
				"state.origin.add(state.direction.scale(state.range))",
				"state.focus = intendedEnd",
				"BlockHitResult terrainHit = terrainHit(player, state.origin, state.focus)",
				"boolean contactedTerrain = elapsed >= 5L",
				"finishDescent(player, state, contactedTerrain)",
				"if (contactedTerrain)",
				"strength + intelligence * 0.75D",
				"state.manifested || state.pulse == 2",
				"case 0 -> 48.0D",
				"case 1 -> 88.0D",
				"default -> 128.0D",
				"case 0 -> 64.0D",
				"case 1 -> 112.0D",
				"default -> 160.0D");
		assertContains(profiles,
				"ANTARES_DESCENT(500, 1800",
				"ANTARES_ROAR(320, 1000",
				"ANTARES_EXTINCTION(260, 700",
				"ANTARES_EXTINCTION_FINISH(500, 1400",
				"case ANTARES_BREATH -> 64.0D",
				"case ANTARES_EXTINCTION -> 160.0D",
				"case ANTARES_CLAW -> 12.0D");
		expectTrue(occurrences(antares,
				"AbilityDestructionManager.Profile.ANTARES_") >= 7,
				"Every destructive Antares attack must remain wired");
		String manifestation = section(antares,
				"public static void toggleManifestation(Entity entity)",
				"public static void onPlayerTick(PlayerTickEvent.Post event)");
		expectFalse(manifestation.contains("AbilityDestructionManager."),
				"Manifestation itself must empower impacts, not grief terrain passively");
	}

	private static void legacyExplosionsCannotBypassTheGamerule()
			throws IOException {
		String fireBeam = readMain("procedures", "FireReleaseBeamProcedure.java");
		String bearTrap = readMain("procedures",
				"BearTrapPlayerCollidesWithThisEntityProcedure.java");
		String goliath = readMain("util", "GoliathCombatManager.java");
		assertContains(fireBeam,
				"AbilityDestructionManager.line(player",
				"Level.ExplosionInteraction.NONE");
		assertContains(bearTrap, "Level.ExplosionInteraction.NONE");
		for (String source : new String[] {fireBeam, bearTrap}) {
			expectFalse(source.contains("ExplosionInteraction.TNT")
						|| source.contains("ExplosionInteraction.MOB")
						|| source.contains("ExplosionInteraction.BLOCK")
						|| source.contains("true, Level.ExplosionInteraction.NONE"),
					"Legacy ability explosions must never bypass the shared gamerule");
		}
		expectFalse(goliath.contains("destroyBlock("),
				"Goliath must use the shared protected destruction engine");
	}

	private static void assertProfile(String directory, String file,
			String... profiles) throws IOException {
		String source = readMain(directory, file);
		for (String profile : profiles)
			expectTrue(source.contains(
					"AbilityDestructionManager.Profile." + profile),
					file + " is missing destruction profile " + profile);
		expectTrue(source.contains("AbilityDestructionManager.impact(")
					|| source.contains("AbilityDestructionManager.line(")
					|| source.contains("AbilityDestructionManager.fissure(")
					|| source.contains("AbilityDestructionManager.ring("),
				file + " must route terrain work through the shared manager");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static String section(String source, String startToken,
			String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken,
				Math.max(0, start + startToken.length()));
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: "
					+ startToken + " -> " + endToken);
		return source.substring(start, end);
	}

	private static void assertOrdered(String source, String... tokens) {
		int previous = -1;
		for (String token : tokens) {
			int current = source.indexOf(token, previous + 1);
			expectTrue(current > previous,
					"Missing or out-of-order source token: " + token);
			previous = current;
		}
	}

	private static void assertContains(String source, String... tokens) {
		for (String token : tokens)
			expectTrue(source.contains(token),
					"Missing source contract: " + token);
	}

	private static int occurrences(String source, String token) {
		int count = 0;
		for (int index = 0;
				(index = source.indexOf(token, index)) >= 0;
				index += token.length())
			count++;
		return count;
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		expectTrue(expected == actual,
				message + " (expected " + expected + ", got " + actual + ")");
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
