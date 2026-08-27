package net.solocraft.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Pure balance, developer-gate, registration, and placeholder-art contracts. */
public final class SilladBossRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net",
			"solocraft");
	private static final Path RESOURCES = Path.of("src", "main", "resources");
	private static final Path ASSETS = RESOURCES.resolve(Path.of("assets",
			"sololeveling"));

	private SilladBossRegression() {
	}

	public static void main(String[] args) throws IOException {
		phaseBoundariesAreStable();
		multiplayerScalingIsBounded();
		frostbiteAndDamageSafeguardsStayBounded();
		frozenDomainAndExecutionAreBounded();
		icePrisonPressureIsBoundedAndBreakable();
		chaRemainsStrongWithoutTradingWithAMonarch();
		entityExposesTheCanonicalActionVocabulary();
		spawnRequiresAnActorAuthorizedDeveloperTicket();
		commandIsDeveloperOnlyAndNoSpawnEggExists();
		placeholderUsesTheVanillaPlayerBipedAndOneColorTexture();
	}

	private static void phaseBoundariesAreStable() {
		expectEquals(SilladBossRules.PHASE_ONE,
				SilladBossRules.phaseForHealth(900.0F, 900.0F),
				"Full health should begin in phase one");
		expectEquals(SilladBossRules.PHASE_ONE,
				SilladBossRules.phaseForHealth(630.01F, 900.0F),
				"Phase two must not begin before its visible threshold");
		expectEquals(SilladBossRules.PHASE_TWO,
				SilladBossRules.phaseForHealth(630.0F, 900.0F),
				"Exactly seventy percent health should begin phase two");
		expectEquals(SilladBossRules.PHASE_TWO,
				SilladBossRules.phaseForHealth(360.01F, 900.0F),
				"Phase two should last until the forty-percent boundary");
		expectEquals(SilladBossRules.PHASE_THREE,
				SilladBossRules.phaseForHealth(360.0F, 900.0F),
				"Exactly forty percent health should begin phase three");
		expectEquals(SilladBossRules.PHASE_THREE,
				SilladBossRules.phaseForHealth(-50.0F, 900.0F),
				"Negative health must clamp to the final phase");
		expectEquals(SilladBossRules.PHASE_ONE,
				SilladBossRules.phaseForHealth(Float.NaN, 900.0F),
				"Invalid health input must fail safely");
	}

	private static void multiplayerScalingIsBounded() {
		expectEquals(1, SilladBossRules.clampEngagedPlayers(-20),
				"An encounter always has at least one effective player");
		expectEquals(4, SilladBossRules.clampEngagedPlayers(50),
				"Scaling must stop at the four-player design cap");
		expectNear(1.0F, SilladBossRules.incomingDamageMultiplier(1),
				0.0001F, "Solo damage should be unchanged");
		expectNear(1.0F / 1.45F,
				SilladBossRules.incomingDamageMultiplier(2), 0.0001F,
				"A second player should add controlled effective health");
		expectNear(1.0F / 2.35F,
				SilladBossRules.incomingDamageMultiplier(99), 0.0001F,
				"Incoming mitigation must cap at four players");
		expectNear(1.15F, SilladBossRules.outgoingDamageMultiplier(4),
				0.0001F, "Four-player outgoing damage should cap at fifteen percent");
		expectNear(1.15F, SilladBossRules.outgoingDamageMultiplier(99),
				0.0001F, "Out-of-range party counts may not amplify damage further");
	}

	private static void frostbiteAndDamageSafeguardsStayBounded() {
		expectEquals(5, SilladBossRules.MAX_FROSTBITE,
				"Frostbite should retain five readable stacks");
		expectEquals(120, SilladBossRules.FROSTBITE_LIFETIME,
				"Frostbite should expire after six clean seconds");
		expectEquals(12, SilladBossRules.PLAYER_ROOT_TICKS,
				"Player hard control must remain under one second");
		expectEquals(120, SilladBossRules.FREEZE_IMMUNITY_TICKS,
				"Freeze immunity should prevent immediate control chains");
		expectEquals(20, SilladBossRules.BRITTLE_DELAY_TICKS,
				"Shatter must wait a full second after Freeze starts");
		expectEquals(40, SilladBossRules.BRITTLE_DURATION_TICKS,
				"The readable Brittle punish window must stay bounded");
		expectEquals(0, SilladBossRules.clampFrostbite(-4),
				"Frostbite cannot become negative");
		expectEquals(5, SilladBossRules.clampFrostbite(90),
				"Frostbite cannot exceed the HUD capacity");
		expectEquals(4, SilladBossRules.frostbiteCap(true),
				"Freeze-immune targets may not immediately refreeze");
		expectEquals(5, SilladBossRules.frostbiteCap(false),
				"Unprotected targets may reach the normal Freeze threshold");
		expectEquals(4, SilladBossRules.addFrostbite(4, 20, true),
				"Large gains must respect temporary Freeze immunity");
		expectEquals(5, SilladBossRules.addFrostbite(5,
				Integer.MAX_VALUE, false),
				"Untrusted stack gains must not overflow below the cap");
		expectNear(125.0F, SilladBossRules.clampIncomingHit(9000.0F),
				0.0001F, "One hit may not skip the complete encounter");
		expectNear(0.0F, SilladBossRules.clampIncomingHit(Float.NaN),
				0.0001F, "Invalid damage must fail closed");
	}

	private static void icePrisonPressureIsBoundedAndBreakable()
			throws IOException {
		expectEquals(480, SilladBossRules.PRISON_DURATION_TICKS,
				"Ice Prison needs a finite twenty-four-second safety duration");
		expectNear(16.0F, SilladBossRules.PRISON_HIT_CAP, 0.0001F,
				"One oversized attack may not erase the prison mechanic");
		expectNear(96.0F, SilladBossRules.prisonIntegrity(1, 600.0F),
				0.0001F, "A normal-rank solo prison should remain breakable");
		expectNear(132.0F, SilladBossRules.prisonIntegrity(4, 600.0F),
				0.0001F, "Multiplayer prison durability should be bounded");
		expectNear(216.0F, SilladBossRules.prisonIntegrity(1, 600.0F,
				ShadowMonarchManager.RANK_MARSHAL), 0.0001F,
				"Marshal shadows need a Monarch-calibrated prison");
		expectNear(240.0F, SilladBossRules.prisonIntegrity(1, 600.0F,
				ShadowMonarchManager.RANK_GRAND_MARSHAL), 0.0001F,
				"Grand Marshal prisons must scale above Marshal prisons");
		expectNear(276.0F, SilladBossRules.prisonIntegrity(4, 600.0F,
				ShadowMonarchManager.RANK_GRAND_MARSHAL), 0.0001F,
				"Group-scaled Grand Marshal integrity must stay bounded");
		expectNear(12.96F, SilladBossRules.prisonRegeneration(216.0F),
				0.0001F, "Prison regeneration should demand sustained damage");
		expectNear(4.0F, SilladBossRules.prisonerAttackDamage(3.0D),
				0.0001F, "Weak summons must make visible break-out progress");
		expectNear(6.5F, SilladBossRules.prisonerAttackDamage(20.0D),
				0.0001F, "Strong shadows may not trivialize the shell instantly");
		expectNear(3.5F, SilladBossRules.prisonDot(100.0F, 1),
				0.0001F, "DOT should pressure rather than delete a summon");
		expectNear(12.0F,
				(float) SilladBossRules.prisonManaDrain(1000.0D, 1),
				0.0001F, "One prison should have a readable mana floor");
		expectNear(48.0F,
				(float) SilladBossRules.prisonManaDrain(100000.0D, 20),
				0.0001F, "Multi-prison mana drain must have a hard cap");

		String manager = readMain("util", "SilladIcePrisonManager.java");
		String combat = readMain("util", "SilladBossCombatManager.java");
		String entity = readMain("entity", "SilladBossEntity.java");
		String barrier = readMain("entity", "BarrierVfxEntity.java");
		String renderer = readMain("client", "renderer",
				"SilladBossRenderer.java");
		String shadows = readMain("util", "ShadowMonarchManager.java");
		assertContains(manager,
				"BarrierVfxEntity.SILLAD_ICE_PRISON",
				"ServerTickEvent.Post",
				"LivingIncomingDamageEvent",
				"PRISON_DURATION_TICKS",
				"prisonRegeneration",
				"prisonManaDrain",
				"prisonerAttackDamage",
				"capability.MP",
				"guardManualDismiss",
				"releaseAllForBoss",
				"session.anchor.y",
				"setCustomNameVisible(true)");
		expectFalse(manager.contains("MobEffects.JUMP"),
				"The prison must pin movement instead of amplifying jumps");
		assertContains(combat,
				"mob.getTarget() == sillad",
				"prisonCandidates",
				"captureWave",
				"activeCountForBoss",
				"combatFootwork",
				"GLACIAL_EXECUTION",
				"SilladDamageTypes.trueFrost",
				"tickFrozenDomainPressure",
				"now > frost.executionReadyUntil");
		assertContains(entity,
				"SololevelingModItems.ICE_SPEAR",
				"Attributes.MOVEMENT_SPEED, 0.40D",
				"attackingSillad");
		assertContains(renderer, "ItemInHandLayer");
		assertContains(barrier,
				"SILLAD_ICE_PRISON = 11",
				"SilladBossRules.PRISON_HIT_CAP",
				"shouldBeSaved()");
		assertContains(shadows,
				"SilladIcePrisonManager.guardManualDismiss",
				"SilladIcePrisonManager.isImprisoned",
				"appliedShadowRank(Entity entity)");
	}

	private static void frozenDomainAndExecutionAreBounded()
			throws IOException {
		expectEquals(16, SilladBossRules.frozenDomainRadius(1),
				"Phase one must establish a visible frozen arena");
		expectEquals(23, SilladBossRules.frozenDomainRadius(2),
				"Phase two must expand the frozen arena");
		expectEquals(30, SilladBossRules.frozenDomainRadius(3),
				"Phase three must control the full battlefield");
		expectEquals(48, SilladBossRules.FROZEN_DOMAIN_COLUMN_BUDGET,
				"Terrain conversion must retain a hard per-tick cap");
		expectEquals(50, SilladBossRules.NON_PLAYER_ROOT_TICKS,
				"NPC Freeze must last long enough to communicate the execution");
		expectEquals(200, SilladBossRules.EXECUTION_WINDOW_TICKS,
				"Long casts must not erase the earned execution opportunity");
		expectNear(18.0F, SilladBossRules.trueFrostExecutionDamage(
				100.0F, true, 1, 1), 0.0001F,
				"Solo phase-one player execution damage must be survivable");
		expectNear(25.3F, SilladBossRules.trueFrostExecutionDamage(
				100.0F, true, 3, 4), 0.0001F,
				"Final-phase group execution damage must retain its player cap");
		expectNear(50.0F, SilladBossRules.trueFrostExecutionDamage(
				260.0F, false, 1, 1), 0.0001F,
				"A frozen S-rank hunter must feel the Monarch's true damage");
		expectNear(86.0F, SilladBossRules.trueFrostExecutionDamage(
				100000.0F, false, 3, 4), 0.0001F,
				"Non-player execution damage must have a hard ceiling");
		expectNear(0.0F, SilladBossRules.trueFrostExecutionDamage(
				Float.NaN, false, 3, 4), 0.0001F,
				"Invalid health must fail closed");

		String domain = readMain("util", "SilladFrozenDomainManager.java");
		assertContains(domain,
				"GLOBAL_INSPECTION_BUDGET = 384",
				"FROZEN_DOMAIN_COLUMN_BUDGET",
				"level.hasChunkAt(pos)",
				"level.getBlockEntity(pos)",
				"state.getDestroySpeed(level, pos) < 0.0F",
				"previous.is(DOMAIN_IMMUNE)",
				"CommonHooks.canEntityDestroy",
				"BlockSnapshot.create",
				"EventHooks.onBlockPlace",
				"sillad_domain_immune",
				"Blocks.PACKED_ICE",
				"Blocks.BLUE_ICE");
		Path immuneTag = RESOURCES.resolve(Path.of("data", "sololeveling",
				"tags", "block", "sillad_domain_immune.json"));
		Path damageType = RESOURCES.resolve(Path.of("data", "sololeveling",
				"damage_type", "sillad_true_frost.json"));
		Path bypassTag = RESOURCES.resolve(Path.of("data", "minecraft",
				"tags", "damage_type", "bypasses_armor.json"));
		assertContains(Files.readString(immuneTag), "minecraft:spawner",
				"minecraft:end_portal", "minecraft:command_block");
		assertContains(Files.readString(damageType),
				"sillad_true_frost", "\"scaling\": \"never\"",
				"\"effects\": \"freezing\"");
		assertContains(Files.readString(bypassTag),
				"sololeveling:sillad_true_frost", "\"replace\": false");
		String language = Files.readString(ASSETS.resolve(Path.of("lang",
				"en_us.json")));
		assertContains(language,
				"entity.sololeveling.sillad_boss.action.glacial_execution",
				"message.sololeveling.sillad.glacial_execution",
				"death.attack.sillad_true_frost.player");
	}

	private static void chaRemainsStrongWithoutTradingWithAMonarch()
			throws IOException {
		String cha = readMain("entity", "ChaHaeInEntity.java");
		String tick = readMain("procedures",
				"ChaHaeInOnEntityTickUpdateProcedure.java");
		String defense = readMain("util", "NamedHunterCombatManager.java");
		String legacy = readMain("procedures", "SwingRandomChaProcedure.java");
		assertContains(cha,
				"new LegacyMeleeAttackGoal(this, 1.35D",
				"Attributes.MAX_HEALTH, 260",
				"Attributes.ARMOR, 16",
				"Attributes.ATTACK_DAMAGE, 22",
				"Attributes.MOVEMENT_SPEED, 0.39");
		assertContains(tick,
				"SWORD_DANCE_CYCLE = 160",
				"OVERHEAD_CYCLE = 120",
				"DASH_CHARGE_TICKS = 28",
				"getHolderOrThrow(DamageTypes.MOB_ATTACK), cha), 16.0F",
				"direction.x * 0.90D");
		assertContains(defense,
				"CURRENT_PROFILE_VERSION = 2",
				"new Profile(260.0D, 16.0D, 22.0D, 0.39D",
				"SilladDamageTypes.isTrueFrost",
				"reacted ? 30L : 8L");
		assertContains(legacy,
				"!(sourceentity instanceof SilladBossEntity)");
	}

	private static void entityExposesTheCanonicalActionVocabulary()
			throws IOException {
		Set<String> expected = Set.of("IDLE", "PHASE_TRANSITION",
				"FROST_CLEAVE", "ICE_SPEAR", "FLASH_FREEZE", "FROZEN_PATH",
				"FROST_COUNTER", "STILLNESS_DECREE", "SPIRE_CAGE",
				"WHITEOUT_PROCESSION", "WINTER_REMEMBERS", "CROWN_OF_WINTER",
				"ABSOLUTE_ZERO", "GLACIAL_EXECUTION", "FROST_STEP");
		Set<String> actual = Arrays.stream(SilladBossRules.Action.values())
				.map(Enum::name).collect(Collectors.toUnmodifiableSet());
		expectEquals(expected, actual,
				"Rules, AI, and future animations need one canonical action vocabulary");

		String entity = readMain("entity", "SilladBossEntity.java");
		for (String action : expected)
			expectTrue(entity.contains(action),
					"Sillad entity is missing semantic action: " + action);
		String combat = readMain("util", "SilladBossCombatManager.java");
		String serverImplementation = entity + "\n" + combat;
		assertContains(serverImplementation,
				"SilladBossRules.phaseForHealth",
				"SilladBossRules.incomingDamageMultiplier",
				"SilladBossRules.outgoingDamageMultiplier",
				"SilladBossRules.clampFrostbite",
				"SilladBossRules.clampIncomingHit",
				"SilladBossRules.FROSTBITE_LIFETIME",
				"SilladBossRules.PLAYER_ROOT_TICKS",
				"SilladBossRules.NON_PLAYER_ROOT_TICKS",
				"SilladBossRules.FREEZE_IMMUNITY_TICKS",
				"SilladBossRules.BRITTLE_DELAY_TICKS",
				"SilladBossRules.BRITTLE_DURATION_TICKS");
		assertContains(entity,
				"SilladBossCombatManager.tick",
				"getNavigation().stop()");
	}

	private static void spawnRequiresAnActorAuthorizedDeveloperTicket()
			throws IOException {
		String spawn = readMain("util", "SilladBossSpawnManager.java");
		assertContains(spawn,
				"spawnForDeveloper(ServerPlayer",
				"DeveloperModeManager.isEnabled",
				"AUTHORIZED_BY_TAG",
				"PENDING_AUTHORIZATIONS",
				"EntityJoinLevelEvent",
				"EventPriority.HIGHEST",
				"event.setCanceled(true)",
				"event.loadedFromDisk()",
				"putUUID",
				"hasUUID");
		expectTrue(spawn.contains("PENDING_AUTHORIZATIONS.remove")
				|| spawn.contains("remove(entity.getUUID())"),
				"A fresh authorization ticket must be consumed exactly once");
	}

	private static void commandIsDeveloperOnlyAndNoSpawnEggExists()
			throws IOException {
		String command = readMain("command", "SlrCommand.java");
		assertContains(command,
				"Commands.literal(\"boss\")",
				"Commands.literal(\"spawn\")",
				"Commands.literal(\"sillad\")",
				"requires(SlrCommand::isDeveloperSource)",
				"spawnSillad",
				"SilladBossSpawnManager.spawnForDeveloper",
				"DeveloperModeManager.isEnabled");

		String items = readMain("init", "SololevelingModItems.java");
		expectFalse(items.contains("SILLAD_BOSS_SPAWN_EGG")
				|| items.contains("sillad_boss_spawn_egg"),
				"The developer boss may not have a public spawn egg");
		try (Stream<Path> files = Files.walk(MAIN)) {
			String allJava = files.filter(path -> path.toString().endsWith(".java"))
					.map(SilladBossRegression::readUnchecked)
					.collect(Collectors.joining("\n"))
					.replaceAll("\\s+", "");
			expectFalse(allJava.contains(
					"SpawnPlacements.register(SololevelingModEntities.SILLAD_BOSS"),
					"Sillad must not receive a natural spawn placement");
		}
	}

	private static void placeholderUsesTheVanillaPlayerBipedAndOneColorTexture()
			throws IOException {
		String renderer = readMain("client", "renderer",
				"SilladBossRenderer.java");
		String registry = readMain("init", "SololevelingModEntities.java");
		String renderers = readMain("init",
				"SololevelingModEntityRenderers.java");
		assertContains(renderer,
				"HumanoidModel<SilladBossEntity>",
				"ModelLayers.PLAYER",
				"textures/entities/sillad_boss_placeholder.png");
		assertContains(registry,
				"SILLAD_BOSS = register(\"sillad_boss\"",
				"SilladBossEntity.createAttributes()");
		String normalizedRegistry = registry.replaceAll("\\s+", "");
		expectTrue(normalizedRegistry.contains(".sized(0.6F,1.8F)")
				|| normalizedRegistry.contains(".sized(0.6f,1.8f)"),
				"The placeholder should use normal player-like dimensions");
		expectTrue(renderers.contains(
				"SILLAD_BOSS.get(), SilladBossRenderer::new"),
				"The vanilla biped renderer must be registered client-side");

		Path texture = ASSETS.resolve(Path.of("textures", "entities",
				"sillad_boss_placeholder.png"));
		expectTrue(Files.isRegularFile(texture) && Files.size(texture) > 0L,
				"The placeholder texture must be packaged");
		BufferedImage image = ImageIO.read(texture.toFile());
		expectTrue(image != null && image.getWidth() == 64
				&& image.getHeight() == 64,
				"The placeholder texture must be exactly 64x64");
		int color = image.getRGB(0, 0);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++)
				expectEquals(color, image.getRGB(x, y),
						"Every placeholder pixel must use one testing color");
		}
		int alpha = color >>> 24 & 0xFF;
		int red = color >>> 16 & 0xFF;
		int green = color >>> 8 & 0xFF;
		int blue = color & 0xFF;
		expectTrue(alpha == 255 && blue > red && blue >= green,
				"The single opaque testing color should read as frost blue");

		String language = Files.readString(ASSETS.resolve(Path.of("lang",
				"en_us.json")));
		expectTrue(language.contains("\"entity.sololeveling.sillad_boss\""),
				"Sillad needs a visible entity name");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static String readUnchecked(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static void assertContains(String source, String... tokens) {
		for (String token : tokens)
			expectTrue(source.contains(token),
					"Missing source contract: " + token);
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectEquals(Object expected, Object actual,
			String message) {
		if (!expected.equals(actual))
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectNear(float expected, float actual,
			float tolerance, String message) {
		if (Math.abs(expected - actual) > tolerance)
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
