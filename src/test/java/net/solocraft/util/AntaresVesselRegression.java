package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free regressions for Antares progression, balance contracts,
 * server authority, bounded VFX, portable shaders, and canonical skill art.
 */
public final class AntaresVesselRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net",
			"solocraft");
	private static final Path RESOURCES = Path.of("src", "main", "resources");
	private static final Path ASSETS = RESOURCES.resolve(Path.of("assets",
			"sololeveling"));
	private static final Path ADVANCEMENTS = RESOURCES.resolve(Path.of("data",
			"sololeveling", "advancement"));

	private AntaresVesselRegression() {
	}

	public static void main(String[] args) throws IOException {
		pureRuinAndControlRulesStayBounded();
		vesselSelectionAndUnlockOrderAreCanonical();
		vesselSelectionGuiProvidesCanonicalAntaresPath();
		centralSkillRoutingOwnsCooldownsAndCanonicalIcons();
		combatRemainsServerAuthoredSafeAndRecoverable();
		longRangeBeamsPierceTerrainWithoutUnboundedTargeting();
		manifestationUsesSharedAuraAndBalancedMitigation();
		visualEventsAreBoundedAndShaderPackSafe();
		advancementChainMatchesUnlockOrder();
	}

	private static void pureRuinAndControlRulesStayBounded() {
		expectEquals(3, AntaresCombatRules.MAX_RUIN,
				"Antares must retain the readable three-charge Ruin budget");
		expectEquals(0, AntaresCombatRules.clampRuin(-20),
				"Ruin cannot become negative");
		expectEquals(3, AntaresCombatRules.clampRuin(50),
				"Ruin cannot exceed its HUD capacity");
		expectEquals(3, AntaresCombatRules.gainRuin(2, 50),
				"Large gains must remain clamped");
		expectEquals(1, AntaresCombatRules.gainRuin(1, -4),
				"Negative gains may not spend Ruin");
		expectTrue(AntaresCombatRules.canSpendFullRuin(3)
					&& !AntaresCombatRules.canSpendFullRuin(2),
				"Finishers require exactly a full Ruin meter");
		expectNear(68.0F, AntaresCombatRules.playerDamage(100.0F), 0.001F,
				"PvP damage must retain its explicit reduction");
		expectNear(0.28D, AntaresCombatRules.bossControlScale(true), 0.0001D,
				"Boss displacement must be reduced without being silently immune");
		expectNear(1.0D, AntaresCombatRules.bossControlScale(false), 0.0001D,
				"Ordinary targets must receive normal control strength");
	}

	private static void vesselSelectionAndUnlockOrderAreCanonical()
			throws IOException {
		String vessels = readMain("util", "VesselManager.java");
		String commands = readMain("command", "SlrCommand.java");
		String title = readMain("procedures", "TitleTextProcedure.java");
		String progression = readMain("util", "VesselProgressionManager.java");
		String equip = readMain("util", "SkillListHelper.java");

		expectTrue(count(vessels,
					"new VesselDefinition(MONARCH, ANTARES_IDENTITY, 10") == 1
					&& count(vessels, "ANTARES_DEFINITION);") == 1,
				"Antares must own one canonical JOB 10 definition");
		expectTrue(commands.contains(
					"VesselManager.assign(arguments, \"monarch\", \"antares\")")
					&& title.contains("\"antares\".equals(vars.vesselIdentity)")
					&& title.contains("Monarch of Destruction"),
				"Commands and legacy UI must resolve the Antares identity");

		Pattern definitionPattern = Pattern.compile(
				"new VesselDefinition\\([^\\r\\n]*?,\\s*(\\d+),");
		Matcher matcher = definitionPattern.matcher(vessels);
		Set<Integer> jobIds = new HashSet<>();
		while (matcher.find())
			expectTrue(jobIds.add(Integer.parseInt(matcher.group(1))),
					"Vessel JOB ids must remain unique");
		expectTrue(jobIds.contains(10), "Antares JOB 10 was not discovered");

		String unlocks = section(progression, "case 10 -> {",
				"default -> {");
		assertOrdered(unlocks,
				"skills.add(JobSkillManager.ANTARES_DESTRUCTION_CLAW)",
				"AntaresCombatRules.BREATH_LEVEL",
				"skills.add(JobSkillManager.ANTARES_BREATH)",
				"AntaresCombatRules.DESCENT_LEVEL",
				"skills.add(JobSkillManager.ANTARES_DESCENT)",
				"AntaresCombatRules.ROAR_LEVEL",
				"skills.add(JobSkillManager.ANTARES_ROAR)",
				"AntaresCombatRules.EXTINCTION_LEVEL",
				"skills.add(JobSkillManager.ANTARES_EXTINCTION)",
				"AntaresCombatRules.MANIFESTATION_LEVEL",
				"skills.add(JobSkillManager.ANTARES_MANIFESTATION)");
		assertOrdered(equip,
				"JobSkillManager.ANTARES_DESTRUCTION_CLAW",
				"JobSkillManager.ANTARES_BREATH",
				"JobSkillManager.ANTARES_DESCENT",
				"JobSkillManager.ANTARES_ROAR",
				"JobSkillManager.ANTARES_EXTINCTION",
				"JobSkillManager.ANTARES_MANIFESTATION");
	}

	private static void vesselSelectionGuiProvidesCanonicalAntaresPath()
			throws IOException {
		String vessels = readMain("util", "VesselManager.java");
		String selection = readMain("client", "gui", "system",
				"VesselSelectionScreen.java");
		String quest = readMain("util", "JobChangeQuestManager.java");
		String packet = readMain("network", "VesselSelectionMessage.java");
		String wip = section(vessels,
				"private static final Set<String> WORK_IN_PROGRESS",
				"private static final VesselDefinition ANTARES_DEFINITION");

		assertContains(vessels,
				"public static final String ANTARES_IDENTITY = \"antares\"",
				"public static VesselDefinition antaresDefinition()",
				"public static boolean isAntares(VesselDefinition definition)",
				"public static AssignmentResult assignAntaresVessel(ServerPlayer player",
				"return assignPlayer(player, ANTARES_DEFINITION, enforceLimit)");
		expectTrue(!wip.contains("ANTARES_IDENTITY")
					&& !vessels.contains("|| isAntares(definition)"),
				"Antares must be publicly selectable rather than a developer-only WIP vessel");
		assertContains(selection,
				"private void chooseAntares(int index)",
				"VesselDefinition antares = VesselManager.antaresDefinition()",
				// The screen now lists contributed vessels after the built-ins, so the
				// lookup spans that combined list. Built-ins keep their original order,
				// so Antares sits at the same index and the guard means the same thing.
				"index != listedVessels.indexOf(antares)",
				"submitChoice(index, antares)",
				"new VesselSelectionMessage(definition.type(), definition.identity())",
				"case \"rakan\" -> THEME_BEAST",
				"case \"antares\" -> THEME_DESTRUCTION",
				"themeWeights[7]",
				"renderDestructionFallback(graphics, time",
				"private static void drawPixelLine");
		assertContains(quest,
				"VesselManager.isSelectableFor(player, definition)",
				"VesselManager.isAntares(definition)",
				"VesselManager.assignAntaresVessel(player, true)",
				"AssignmentResult.LOCKED");
		assertContains(packet,
				"ServerPlayer player = context.getSender()",
				"JobChangeQuestManager.selectVessel(player, message.type, message.identity)");
	}

	private static void centralSkillRoutingOwnsCooldownsAndCanonicalIcons()
			throws IOException {
		String skills = readMain("util", "JobSkillManager.java");
		String overlay = readMain("client", "screens", "DisplayOverlay.java");
		assertContains(skills,
				"case 10 -> ANTARES_SKILLS",
				"if (job == 7",
				"AntaresCombatManager.castDestructionClaw(entity)",
				"AntaresCombatManager.castBreathOfDestruction(entity)",
				"AntaresCombatManager.castMonarchsDescent(entity)",
				"AntaresCombatManager.castSovereignRoar(entity)",
				"AntaresCombatManager.castExtinction(entity)",
				"AntaresCombatManager.toggleManifestation(entity)",
				"AntaresCombatManager.CLAW_COOLDOWN",
				"AntaresCombatManager.BREATH_COOLDOWN",
				"AntaresCombatManager.DESCENT_COOLDOWN",
				"AntaresCombatManager.ROAR_COOLDOWN",
				"AntaresCombatManager.EXTINCTION_COOLDOWN",
				"AntaresCombatManager.MANIFESTATION_COOLDOWN");
		assertContains(overlay,
				"case JobSkillManager.ANTARES_DESTRUCTION_CLAW -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_antares_claw.png\")",
				"case JobSkillManager.ANTARES_BREATH -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_antares_breathofdestruction.png\")",
				"case JobSkillManager.ANTARES_DESCENT -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_antares_monarchdescend.png\")",
				"case JobSkillManager.ANTARES_ROAR -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_antares_monarchsroar.png\")",
				"case JobSkillManager.ANTARES_EXTINCTION -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_antares_extinction.png\")",
				"case JobSkillManager.ANTARES_MANIFESTATION -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_antares_spiritualize.png\")");
		for (String icon : new String[] {"icon_antares_claw.png",
				"icon_antares_breathofdestruction.png",
				"icon_antares_monarchdescend.png",
				"icon_antares_monarchsroar.png",
				"icon_antares_extinction.png",
				"icon_antares_spiritualize.png"})
			expectTrue(Files.size(ASSETS.resolve(Path.of("textures", "screens",
					icon))) > 0, "Canonical Antares icon is missing: " + icon);
	}

	private static void combatRemainsServerAuthoredSafeAndRecoverable()
			throws IOException {
		String combat = readMain("util", "AntaresCombatManager.java");
		String packet = readMain("network", "AntaresVfxEventMessage.java");
		assertContains(combat,
				"instanceof ServerPlayer player",
				"private static final Map<UUID, RuinState> RUIN",
				"private static final Map<UUID, BreathState> BREATHS",
				"private static final Map<UUID, DescentState> DESCENTS",
				"private static final Map<UUID, ExtinctionState> EXTINCTIONS",
				"AntaresCombatRules.canSpendFullRuin",
				"setRuin(player, 0, true)",
				"RUIN_DECAY_DELAY = 240",
				"RUIN_DECAY_INTERVAL = 100",
				"CooldownManager.setFullDuration(player, EXTINCTION_COOLDOWN",
				"ClipContext.Block.COLLIDER",
				"player.canHarmPlayer(other)",
				"player.isAlliedTo(target)",
				"ShadowMonarchManager.isOwnedShadow(target, player)",
				"AntaresCombatRules.bossControlScale(isBoss(target))",
				"event.setCanceled(true)",
				"LivingDeathEvent",
				"PlayerLoggedOutEvent",
				"PlayerChangedDimensionEvent",
				"ServerStoppingEvent");
		expectFalse(combat.contains("destroyBlock(")
					|| combat.contains("setBlockAndUpdate(")
					|| combat.contains("removeBlock("),
				"Antares visuals and impacts may not grief world blocks");
		expectFalse(packet.contains("PLAY_TO_SERVER"),
				"Antares VFX must contain no client-trusted gameplay request");
	}

	private static void longRangeBeamsPierceTerrainWithoutUnboundedTargeting()
			throws IOException {
		String combat = readMain("util", "AntaresCombatManager.java");
		String packet = readMain("network", "AntaresVfxEventMessage.java");
		assertContains(combat,
				"BREATH_RANGE = 144.0D",
				"MANIFESTED_BREATH_RANGE = 176.0D",
				"EXTINCTION_RANGE = 288.0D",
				"MANIFESTED_EXTINCTION_RANGE = 352.0D",
				"private static Vec3[] piercingBeam",
				"private static BlockHitResult terrainHit",
				"BREATH_TARGET_CAP = 32",
				"EXTINCTION_TARGET_CAP = 48",
				"BREATH_TARGET_CAP))",
				"EXTINCTION_TARGET_CAP))");
		String geometry = section(combat, "private static Vec3[] piercingBeam",
				"private static BlockHitResult terrainHit");
		expectFalse(geometry.contains(".clip("),
				"Beam geometry must not stop at the first terrain collision");
		assertContains(packet, "SEND_RANGE = 384.0D");
	}

	private static void manifestationUsesSharedAuraAndBalancedMitigation()
			throws IOException {
		String combat = readMain("util", "AntaresCombatManager.java");
		String registry = readMain("client", "aura", "PlayerAuraRegistry.java");
		String playerRenderer = readMain("client", "renderer",
				"AntaresManifestationRenderer.java");
		String preset = section(registry, "ANTARES_MANIFESTATION",
				"RULER_BLUE");
		assertContains(combat,
				"PlayerAuraSystem.setContinuous(player, MANIFESTATION_AURA",
				"PlayerAuraSystem.clearContinuous(player)",
				"PlayerAuraSystem.burst(player, MANIFESTATION_AURA",
				"event.getSource().is(DamageTypeTags.IS_FIRE)",
				"event.getNewDamage() * 0.55F",
				"event.getSource().is(DamageTypeTags.IS_EXPLOSION)",
				"event.getNewDamage() * 0.78F",
				"drainMana(player, 16)",
				"Attributes.MOVEMENT_SPEED",
				"Attributes.KNOCKBACK_RESISTANCE");
		expectFalse(combat.contains("MobEffects.FIRE_RESISTANCE"),
				"Manifestation promises resistance, not complete fire immunity");
		expectTrue(preset.contains("\"antares_manifestation\"")
					&& preset.contains("FluidStyle.LIQUID_FLAME"),
				"Manifestation must register a full-body destruction aura");
		assertContains(playerRenderer,
				"RenderPlayerEvent.Post",
				"ClientPlayerAuraManager.activeFor(player.getId())",
				"ANTARES_MANIFESTATION.id()",
				"AntaresVfxRenderer.drawWingPair",
				"ParticleStatus.MINIMAL");
	}

	private static void visualEventsAreBoundedAndShaderPackSafe()
			throws IOException {
		String packet = readMain("network", "AntaresVfxEventMessage.java");
		String state = readMain("client", "renderer",
				"AntaresVfxClientState.java");
		String renderer = readMain("client", "renderer",
				"AntaresVfxRenderer.java");
		String renderTypes = readMain("client", "renderer", "shader",
				"AntaresVfxRenderTypes.java");
		String hud = readMain("client", "screens", "AntaresRuinOverlay.java");
		String selectionShader = Files.readString(ASSETS.resolve(Path.of(
				"shaders", "core", "vessel_selection_background.fsh")));
		assertContains(packet,
				"EVENT_TYPE_COUNT = 15",
				"MAX_DURATION_TICKS",
				"MAX_FUTURE_START_TICKS",
				"FLAG_PRIVATE_CASTER",
				"PacketDistributor.NEAR",
				"PacketDistributor.PLAYER");
		assertContains(state,
				"MAX_EVENTS = 128",
				"while (EVENTS.size() > MAX_EVENTS)",
				"message.privateToCaster()",
				"RUIN_SYNC",
				"HudState");
		assertContains(renderer,
				"DeferredWorldShaderRenderer.requestDepthAtStage",
				"DeferredWorldShaderRenderer.worldPoseStack",
				"IrisCompat.isRenderingShadowPass()",
				"ParticleStatus.MINIMAL",
				"FrameBudget",
				"maxVertices",
				"drawDragonHead",
				"drawWingPair",
				"renderExtinctionAftermath");
		expectFalse(renderer.contains("RenderSystem.")
					|| renderer.contains("GL11.")
					|| renderer.contains("glEnable("),
				"World VFX may not mutate global OpenGL state");
		assertContains(renderTypes,
				"RegisterShadersEvent",
				"WorldShaderVertexFormat.NEW_ENTITY",
				"RenderType.entityTranslucentEmissive",
				"AntaresVfxClientState.onResourceReload()");
		assertContains(hud,
				"GuiGraphics",
				"AntaresCombatManager.isAntaresVessel",
				"extinctionChargeProgress",
				"drawDiamond");
		assertContains(selectionShader,
				"vec3 destructionTheme",
				"w1.w > 0.001",
				"destructionTheme(uv, t)",
				"vec3(1.0, 0.08, 0.035) * w1.w");
		expectTrue(selectionShader.startsWith("#version 150"),
				"The Antares vessel background must remain macOS-compatible GLSL 150");

		for (String file : new String[] {"rendertype_antares_vfx.vsh",
				"rendertype_antares_vfx.fsh",
				"rendertype_antares_vfx_surface.json",
				"rendertype_antares_vfx_emissive.json"})
			expectTrue(Files.isRegularFile(ASSETS.resolve(Path.of("shaders",
					"core", file))), "Missing Antares shader resource: " + file);
		for (String file : new String[] {"rendertype_antares_vfx.vsh",
				"rendertype_antares_vfx.fsh"}) {
			String shader = Files.readString(ASSETS.resolve(Path.of("shaders",
					"core", file))).toLowerCase();
			expectTrue(shader.startsWith("#version 150"),
					"macOS-compatible GLSL 150 is required: " + file);
			for (String forbidden : new String[] {"layout(", "image2d",
					"atomic", "buffer {", "#version 4"})
				expectFalse(shader.contains(forbidden),
						"Unsupported shader feature in " + file + ": " + forbidden);
		}
	}

	private static void advancementChainMatchesUnlockOrder()
			throws IOException {
		String root = advancement("vessels", "monarch_of_destruction.json");
		String claw = advancement("vessels", "destruction",
				"destruction_claw.json");
		String breath = advancement("vessels", "destruction",
				"breath_of_destruction.json");
		String descent = advancement("vessels", "destruction",
				"monarchs_descent.json");
		String roar = advancement("vessels", "destruction",
				"sovereign_roar.json");
		String extinction = advancement("vessels", "destruction",
				"extinction.json");
		String manifestation = advancement("vessels", "destruction",
				"monarch_manifestation.json");
		expectTrue(root.contains("\"parent\": \"sololeveling:system/vessel\"")
					&& claw.contains("sololeveling:vessels/monarch_of_destruction")
					&& breath.contains("sololeveling:vessels/destruction/destruction_claw")
					&& descent.contains("sololeveling:vessels/destruction/breath_of_destruction")
					&& roar.contains("sololeveling:vessels/destruction/monarchs_descent")
					&& extinction.contains("sololeveling:vessels/destruction/sovereign_roar")
					&& manifestation.contains("sololeveling:vessels/destruction/extinction"),
				"Antares advancement parents must mirror gameplay chronology");
		for (String source : new String[] {root, claw, breath, descent, roar,
				extinction, manifestation})
			expectTrue(source.contains("\"trigger\": \"minecraft:impossible\""),
					"Antares advancement awards must remain server-authored");
		String language = Files.readString(ASSETS.resolve(Path.of("lang",
				"en_us.json")));
		for (String key : new String[] {
				"advancements.vessels.monarch_of_destruction.title",
				"advancements.vessels.monarch_of_destruction.descr",
				"advancements.vessels.destruction.destruction_claw.title",
				"advancements.vessels.destruction.breath_of_destruction.title",
				"advancements.vessels.destruction.monarchs_descent.title",
				"advancements.vessels.destruction.sovereign_roar.title",
				"advancements.vessels.destruction.extinction.title",
				"advancements.vessels.destruction.monarch_manifestation.title",
				"gui.sololeveling.antares.ruin"})
			expectTrue(language.contains("\"" + key + "\""),
					"Missing Antares language key: " + key);
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static String advancement(String... parts) throws IOException {
		Path path = ADVANCEMENTS;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static String section(String source, String startToken,
			String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, Math.max(0, start + startToken.length()));
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
			expectTrue(source.contains(token), "Missing source contract: " + token);
	}

	private static int count(String source, String token) {
		int matches = 0;
		for (int at = 0; (at = source.indexOf(token, at)) >= 0;
				at += token.length())
			matches++;
		return matches;
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		expectTrue(expected == actual,
				message + " (expected " + expected + ", got " + actual + ")");
	}

	private static void expectNear(double expected, double actual,
			double tolerance, String message) {
		expectTrue(Math.abs(expected - actual) <= tolerance,
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
