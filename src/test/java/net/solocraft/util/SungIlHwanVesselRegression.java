package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free source-contract regressions for Sung Il-Hwan's vessel
 * progression, equip routing, combat lifecycle, and bounded presentation.
 */
public final class SungIlHwanVesselRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path RESOURCES = Path.of(
			"src", "main", "resources");
	private static final Path ASSETS = RESOURCES.resolve(Path.of(
			"assets", "sololeveling"));
	private static final Path ADVANCEMENTS = RESOURCES.resolve(Path.of(
			"data", "sololeveling", "advancement"));

	private SungIlHwanVesselRegression() {
	}

	public static void main(String[] args) throws IOException {
		vesselIsSelectableAndOwnsOneCanonicalJobId();
		unlocksAndEquipListUseTheAgreedOrder();
		centralInputAndSwitchAwayCleanupOwnEverySkill();
		rulerIconsAreReusedForEveryAbility();
		advancementChainMatchesServerUnlocks();
		spiritualizationUsesTheSharedFullBodyAuraLifecycle();
		visualFactsArePrivateWhereNeededAndStrictlyBounded();
		coreCombatContractIsServerAuthoritativeAndRecoverable();
		fearPowerHandlesTargetsWithoutCombatAttributes();
		assassinStanceOwnsEmptySwingLineCuts();
		spatialExecutionTargetsAndTraversesItsWholeExpandingSphere();
	}

	private static void vesselIsSelectableAndOwnsOneCanonicalJobId()
			throws IOException {
		String vessels = readMain("util", "VesselManager.java");
		String selection = readMain("util",
				"JobChangeQuestManager.java");
		String screen = readMain("client", "gui", "system",
				"VesselSelectionScreen.java");
		String commands = readMain("command", "SlrCommand.java");
		String legacyTitle = readMain("procedures",
				"TitleTextProcedure.java");

		expectTrue(count(vessels,
						"new VesselDefinition(RULER, \"sung_il_hwan\", 7")
						== 1,
				"Sung Il-Hwan must have exactly one canonical vessel definition");
		String workInProgress = section(vessels,
				"private static final Set<String> WORK_IN_PROGRESS",
				"private static final List<VesselDefinition> DEFINITIONS");
		expectTrue(workInProgress.contains("\"sung_il_hwan\""),
				"Sung Il-Hwan must appear as WIP outside developer preview mode");
		expectTrue(selection.contains(
						"!VesselManager.isSelectableFor(player, definition)")
						&& screen.contains(
								"VesselManager.isDeveloperPreview(definition)")
						&& vessels.contains(
								"DeveloperModeManager.isEnabled(player)"),
				"Server selection and client presentation must share the developer-only WIP bypass");
		expectTrue(vessels.contains(
						"\"sung_il_whan\".equalsIgnoreCase(identity) ? \"sung_il_hwan\"")
						&& commands.contains(
								"VesselManager.assign(arguments, \"ruler\", \"sung_il_hwan\")"),
				"The legacy misspelling and administrator command must resolve the canonical identity");
		expectTrue(legacyTitle.contains(
						"\"sung_il_hwan\".equals(vars.vesselIdentity)")
						&& legacyTitle.contains("return \"Silent Authority\""),
				"Legacy panels and the MP overlay must not label Sung Il-Hwan as none");

		Pattern definitionPattern = Pattern.compile(
				"new VesselDefinition\\([^\\r\\n]*?,\\s*(\\d+),");
		Matcher matcher = definitionPattern.matcher(vessels);
		Set<Integer> jobIds = new HashSet<>();
		int definitions = 0;
		while (matcher.find()) {
			definitions++;
			expectTrue(jobIds.add(Integer.parseInt(matcher.group(1))),
					"Every vessel must own a unique legacy JOB id");
		}
		expectTrue(definitions >= 9 && jobIds.contains(7),
				"Canonical vessel definitions must retain Sung Il-Hwan as JOB 7");
	}

	private static void unlocksAndEquipListUseTheAgreedOrder()
			throws IOException {
		String progression = readMain("util",
				"VesselProgressionManager.java");
		String jobSkills = readMain("util", "JobSkillManager.java");
		String equipList = readMain("util", "SkillListHelper.java");
		String daggerTags = Files.readString(RESOURCES.resolve(Path.of(
				"data", "minecraft", "tags", "item", "dagger.json")))
				+ Files.readString(RESOURCES.resolve(Path.of(
						"data", "minecraft", "tags", "item", "dagger_pack.json")));
		String sungUnlocks = section(progression, "case 7 -> {",
				"case 9 -> {");

		assertOrdered(sungUnlocks,
				"skills.add(JobSkillManager.SUNG_SPIRITUALIZATION)",
				"if (level >= 55)",
				"skills.add(JobSkillManager.SUNG_PREDATORS_PRESENCE)",
				"if (level >= 70)",
				"skills.add(JobSkillManager.SUNG_ASSASSIN_STANCE)",
				"if (level >= 90)",
				"skills.add(JobSkillManager.SUNG_SPATIAL_EXECUTION)");
		assertOrdered(equipList,
				"JobSkillManager.SUNG_PREDATORS_PRESENCE",
				"JobSkillManager.SUNG_ASSASSIN_STANCE",
				"JobSkillManager.SUNG_SPATIAL_EXECUTION",
				"JobSkillManager.SUNG_SPIRITUALIZATION");
		expectTrue(jobSkills.contains("private static final List<String> SUNG_SKILLS")
						&& jobSkills.contains("case 7 -> SUNG_SKILLS")
						&& count(jobSkills, "SUNG_SPIRITUALIZATION") >= 5
						&& count(jobSkills, "SUNG_PREDATORS_PRESENCE") >= 5
						&& count(jobSkills, "SUNG_ASSASSIN_STANCE") >= 5
						&& count(jobSkills, "SUNG_SPATIAL_EXECUTION") >= 5,
				"All four abilities must be recognized as JOB 7-owned skills");
		expectTrue(equipList.contains(
						"if (!JobSkillManager.isJobSkill(skill))"),
				"The central ordered allowlist must include every Sung skill or the equip UI will hide it");
		expectTrue(daggerTags.contains("\"sololeveling:gravity_dagger\""),
				"Gravity Dagger must satisfy Assassin Stance's shared dagger-tag requirement");
	}

	private static void centralInputAndSwitchAwayCleanupOwnEverySkill()
			throws IOException {
		String jobSkills = readMain("util", "JobSkillManager.java");
		String press = readMain("procedures",
				"UseSkillOnKeyPressedProcedure.java");
		String release = readMain("procedures",
				"UseSkillOnKeyReleasedProcedure.java");
		String vessels = readMain("util", "VesselManager.java");
		String progressReset = readMain("util",
				"PlayerProgressResetManager.java");

		expectTrue(press.contains(
						"JobSkillManager.cast(world, x, y, z, entity, _selectedPower)")
						&& release.contains(
								"JobSkillManager.release(entity, power, pressedMs)"),
				"Equipped skill press and release must pass through the central dispatcher");
		expectTrue(jobSkills.contains(
						"SungIlHwanCombatManager.press(entity, skill)")
						&& jobSkills.contains(
								"SungIlHwanCombatManager.release(entity, skill, pressedMs)")
						&& jobSkills.contains(
								"SUNG_SPATIAL_EXECUTION, SUNG_SPIRITUALIZATION")
						&& jobSkills.contains("case 7 -> SUNG_SKILLS"),
				"The dispatcher must route every Sung skill and reject it for other jobs");
		expectTrue(jobSkills.contains("clearStaleEquippedSkills(")
						&& jobSkills.contains(
								"SkillSlotHelper.setSlot(vars, slot, \"\")")
						&& jobSkills.contains("vars.PselectedPower = \"\"")
						&& jobSkills.contains(
								"if (ALL_JOB_SKILLS.contains(cleaned))"),
				"Changing or resetting a vessel must clear stale Sung hotbar and selected skills");
		expectTrue(count(vessels,
						"VesselProgressionManager.reconcileEntitlements(player)")
						>= 2,
				"Both reset and assignment must immediately rebuild skill entitlements");
		expectTrue(progressReset.contains(
						"SungIlHwanCombatManager.resetPlayerState(player)"),
				"A full player-progress reset must clear Sung's persistent stage, risk, and runtime state");
	}

	private static void rulerIconsAreReusedForEveryAbility()
			throws IOException {
		String overlay = readMain("client", "screens",
				"DisplayOverlay.java");
		assertContains(overlay,
				"case JobSkillManager.SUNG_PREDATORS_PRESENCE -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_goliath_1.png\")",
				"case JobSkillManager.SUNG_ASSASSIN_STANCE -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_goliath_2.png\")",
				"case JobSkillManager.SUNG_SPATIAL_EXECUTION -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_goliath_3.png\")",
				"case JobSkillManager.SUNG_SPIRITUALIZATION -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_spiritualize_goliath.png\")");
		for (String icon : new String[] {
				"icon_goliath_1.png",
				"icon_goliath_2.png",
				"icon_goliath_3.png",
				"icon_spiritualize_goliath.png" }) {
			expectTrue(Files.isRegularFile(ASSETS.resolve(
							Path.of("textures", "screens", icon))),
					"Reused ruler-vessel icon is missing: " + icon);
		}
	}

	private static void advancementChainMatchesServerUnlocks()
			throws IOException {
		String progression = readMain("util",
				"VesselProgressionManager.java");
		String language = Files.readString(ASSETS.resolve(
				Path.of("lang", "en_us.json")));
		String presence = advancement("vessels", "rulers",
				"predators_presence.json");
		String stance = advancement("vessels", "rulers",
				"assassin_stance.json");
		String execution = advancement("vessels", "rulers",
				"spatial_execution.json");

		expectTrue(presence.contains(
						"\"parent\": \"sololeveling:vessels/rulers/silent_manifestation\"")
						&& stance.contains(
								"\"parent\": \"sololeveling:vessels/rulers/predators_presence\"")
						&& execution.contains(
								"\"parent\": \"sololeveling:vessels/rulers/assassin_stance\""),
				"Sung's advancement tree must mirror the skill unlock order");
		for (String source : new String[] {
				presence, stance, execution })
			expectTrue(source.contains(
							"\"trigger\": \"minecraft:impossible\""),
					"Skill advancements must remain server-authored");
		assertContains(progression,
				"\"vessels/rulers/predators_presence\"",
				"\"vessels/rulers/assassin_stance\"",
				"\"vessels/rulers/spatial_execution\"");
		for (String key : new String[] {
				"advancements.vessels.rulers.predators_presence.title",
				"advancements.vessels.rulers.predators_presence.descr",
				"advancements.vessels.rulers.assassin_stance.title",
				"advancements.vessels.rulers.assassin_stance.descr",
				"advancements.vessels.rulers.spatial_execution.title",
				"advancements.vessels.rulers.spatial_execution.descr" })
			expectTrue(language.contains("\"" + key + "\""),
					"Missing Sung advancement language key: " + key);
	}

	private static void spiritualizationUsesTheSharedFullBodyAuraLifecycle()
			throws IOException {
		String registry = readMain("client", "aura",
				"PlayerAuraRegistry.java");
		String auraSystem = readMain("util", "PlayerAuraSystem.java");
		String clientAura = readMain("client", "aura",
				"ClientPlayerAuraManager.java");
		String combat = readMain("util",
				"SungIlHwanCombatManager.java");
		String preset = section(registry,
				"SUNG_IL_HWAN_SPIRITUALIZATION",
				"SHADOW_MONARCH_MANIFESTATION");

		expectTrue(preset.contains("\"sung_il_hwan_spiritualization\"")
						&& preset.contains("GOLD_GLOW")
						&& preset.contains(
								"new PlayerAuraDefinition.FluidProfile(")
						&& preset.contains(
								"PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA"),
				"Sung Spiritualization must be a registered gold full-body fluid aura, not detached ad-hoc VFX");
		assertContains(auraSystem,
				"public static void setContinuous(ServerPlayer player",
				"public static void clearContinuous(ServerPlayer player)",
				"PacketDistributor.TRACKING_ENTITY_AND_SELF",
				"PlayerEvent.StartTracking",
				"PlayerEvent.StopTracking",
				"PlayerEvent.PlayerLoggedInEvent",
				"PlayerEvent.PlayerChangedDimensionEvent");
		assertContains(clientAura,
				"EntityLeaveLevelEvent",
				"LevelEvent.Unload",
				"clearEntity(event.getEntity().getId())",
				"public static void clear()");
		expectTrue(combat.contains("SPIRITUALIZATION_AURA")
						&& combat.contains("\"sung_il_hwan_spiritualization\"")
						&& combat.contains(
								"PlayerAuraSystem.setContinuous(player, SPIRITUALIZATION_AURA")
						&& combat.contains(
								"PlayerAuraSystem.clearContinuous(player)")
						&& combat.contains(
								"SPIRITUALIZATION_AURA.equals(")
						&& count(combat, "setSpiritualizationAura(player,") >= 3
						&& count(combat, "clearSpiritualizationAura(player)") >= 3,
				"Stage activation, resync, teardown, and reset must share the persistent aura API without clearing another form's aura");
	}

	private static void coreCombatContractIsServerAuthoritativeAndRecoverable()
			throws IOException {
		String combat = readMain("util",
				"SungIlHwanCombatManager.java");

		assertContains(combat,
				"SKILL_PREDATORS_PRESENCE",
				"SKILL_ASSASSIN_STANCE",
				"SKILL_SPATIAL_EXECUTION",
				"SKILL_SPIRITUALIZATION",
				"public static void press(Entity entity, String skillName)",
				"public static void release(Entity entity, String skillName, int pressedMs)",
				"public static void tick(ServerPlayer player)",
				"Player.PERSISTED_NBT_TAG");
		expectTrue(combat.contains("isShiftKeyDown()")
						&& combat.contains("STAGE_TWO")
						&& combat.contains("EXHAUST"),
				"Stage II must be a sneak-recast of active Stage I and lead to exhaustion");
		expectTrue(combat.contains("LivingDeathEvent")
						&& combat.contains("RISK")
						&& combat.contains("Mth.clamp")
						&& combat.contains("recoverRisk("),
				"Stage II death must apply bounded, durable, recoverable risk");
		expectTrue(combat.contains("isSungIlHwanVessel(")
						&& combat.contains("teardownAfterIdentityLoss(player)")
						&& combat.contains("STATE_PRESENCE, false")
						&& combat.contains("STATE_STANCE, false")
						&& combat.contains("PlayerLoggedOutEvent"),
				"Logout or switching away must clean every transient combat mode without erasing durable risk");
		expectTrue(combat.contains("PlayerRespawnEvent")
						&& combat.contains("syncPersistentPresentation(player, state)")
						&& combat.contains("sendRiskFeedback"),
				"Login and respawn must resynchronize the durable stage, exhaustion, and fracture presentation");
		expectTrue(combat.contains("FEAR")
						&& combat.contains("hasLineOfSight")
						&& combat.contains("opponent.hasLineOfSight(caster)")
						&& combat.contains("opponent.getLookAngle()"),
				"Fear must be server-authored and PvP buildup must pause without mutual visual awareness");
		expectTrue(combat.contains("TagKey.create")
						&& combat.contains("dagger")
						&& combat.contains("LivingIncomingDamageEvent"),
				"Assassin Stance must enforce dagger combat through server combat events");
		expectTrue(combat.contains(
						"state(victim).getInt(STATE_EXHAUSTION_REMAINING) > 0")
						&& combat.contains(
								"event.setAmount(event.getAmount()")
						&& combat.contains(
								"CooldownManager.setFullDuration(player, \"mana_refresh\""),
				"Stage II exhaustion must reduce defenses and suppress ordinary mana regeneration");
		expectTrue(combat.contains(
						"CooldownManager.set(player, SEVER_COOLDOWN")
						&& combat.contains(
								"CooldownManager.set(player, SKILL_SPATIAL_EXECUTION")
						&& combat.contains("STAGE_ONE")
						&& combat.contains("STAGE_TWO"),
				"Stage I and II must provide progressively stronger cooldown benefits only to Sung's attacks");
		expectTrue(combat.contains("fixed, non-cancellable Stage II")
						&& combat.contains("dying during Stage II or exhaustion")
						&& combat.contains("Ruler's Fracture"),
				"Spiritualization's tooltip must disclose its fixed exhaustion and death-risk commitment");
		expectTrue(combat.contains("sendExecutionCharge")
						&& combat.contains("sendExecutionRelease")
						&& combat.contains("sendExecutionFracture"),
				"Spatial Execution must drive its charge, release, and delayed fracture presentation");
	}

	private static void fearPowerHandlesTargetsWithoutCombatAttributes()
			throws IOException {
		String combat = readMain("util",
				"SungIlHwanCombatManager.java");
		String namedHunterCombat = readMain("util",
				"NamedHunterCombatManager.java");
		String safeLookup = section(combat,
				"private static double attributeValueOrZero",
				"private static double targetPower");
		String targetPower = section(combat,
				"private static double targetPower",
				"private static boolean isBossLike");
		String namedHunterPower = section(namedHunterCombat,
				"private static double combatPower",
				"private static LivingEntity resolveAttacker");

		assertContains(safeLookup,
				"AttributeInstance instance = entity.getAttribute(attribute)",
				"instance == null ? 0.0D : instance.getValue()");
		assertContains(targetPower,
				"attributeValueOrZero(target, Attributes.ARMOR)",
				"attributeValueOrZero(target, Attributes.ATTACK_DAMAGE)");
		expectFalse(targetPower.contains("target.getAttributeValue("),
				"Fear power must not assume that every LivingEntity exposes armor or attack-damage attributes");
		assertContains(namedHunterPower,
				"AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE)",
				"attack == null ? 0.0D : attack.getValue()");
		expectFalse(namedHunterPower.contains(
						"entity.getAttributeValue(Attributes.ATTACK_DAMAGE)"),
				"Related combat-power estimators must tolerate attackers without generic.attack_damage too");
	}

	private static void assassinStanceOwnsEmptySwingLineCuts()
			throws IOException {
		String combat = readMain("util",
				"SungIlHwanCombatManager.java");
		String packet = readMain("network",
				"SungIlHwanAttackMessage.java");
		String mixin = readMain("mixins",
				"SungIlHwanAttackMixin.java");
		String mixinConfig = Files.readString(RESOURCES.resolve(
				"mixins.sololeveling.json"));
		String betterCombat = readMain("client", "compat", "bettercombat",
				"SungIlHwanBetterCombatCompat.java");
		String betterCombatBootstrap = readMain("client",
				"LiuBetterCombatBootstrap.java");
		String lineCut = section(combat,
				"public static boolean performAssassinLineCut",
				"public static int fearTier");
		String geometry = section(combat,
				"private static List<LivingEntity> targetsAlongLine",
				"private static LivingEntity findLookTarget");
		String serverAttack = section(combat,
				"public static void onAttackEntity",
				"public static void onDaggerDamage");
		String livingAttack = section(combat,
				"public static void onAssassinLivingAttack",
				"public static void onDaggerDamage");
		String attackCadence = section(combat,
				"private static int stanceAttackIntervalTicks",
				"private static boolean hasDagger");
		String clientAttack = section(mixin,
				"private void sololeveling$requestAssassinLineCut",
				"\n\t}");
		String packetPayload = section(packet,
				"private final byte mode",
				"public SungIlHwanAttackMessage()");

		assertContains(packet,
				"private static final byte ATTACK_REQUEST",
				"private static final byte STANCE_SYNC",
				"ServerPlayer sender = context.getSender()",
				"SungIlHwanCombatManager.performAssassinLineCut(sender)",
				"public static boolean isClientStanceActive()",
				"public static void syncStance(ServerPlayer player, boolean active)");
		expectTrue(!packetPayload.contains("target")
						&& !packetPayload.contains("damage")
						&& !packetPayload.contains("Vec3")
						&& !packet.contains("buffer.readDouble"),
				"The client attack request may carry no trusted target, geometry, or damage facts");
		assertOrdered(clientAttack,
				"SungIlHwanCombatManager.shouldReplaceBasicAttack(player)",
				"sendToServer(",
				"new SungIlHwanAttackMessage()");
		expectTrue(mixin.contains(
						"@Mixin(value = Minecraft.class, priority = 900)")
						&& mixinConfig.contains("\"SungIlHwanAttackMixin\"")
						&& !clientAttack.contains("setReturnValue")
						&& !clientAttack.contains("callback.cancel"),
				"The vanilla observer must preserve the normal swing and run after Better Combat gets first chance to claim startAttack");
		assertContains(betterCombat,
				"BetterCombatClientEvents.ATTACK_HIT.register",
				"SungIlHwanCombatManager.shouldReplaceBasicAttack(player)",
				"new SungIlHwanAttackMessage()");
		expectTrue(betterCombatBootstrap.contains(
						"ModList.get().isLoaded(\"bettercombat\")")
						&& betterCombatBootstrap.contains(
								"SungIlHwanBetterCombatCompat")
						&& betterCombatBootstrap.contains(
								"Class.forName("),
				"Better Combat must stay optional while registering its actual attack-frame/whiff bridge when present");
		assertContains(combat,
				"public static boolean shouldReplaceBasicAttack(Entity entity)",
				"isSungIlHwanVessel(player)",
				"isDagger(player.getMainHandItem())",
				"state(player).getBoolean(STATE_STANCE)");
		expectTrue(lineCut.contains("player != null && !player.isAlive()")
						|| lineCut.contains(
								"player == null || !player.isAlive()"),
				"The authoritative empty-swing handler must reject dead or missing players");
		assertContains(lineCut,
				"!shouldReplaceBasicAttack(player)",
				"!player.level().hasChunkAt(player.blockPosition())",
				"LAST_STANCE_ATTACK.get(player.getUUID())",
				"stanceAttackIntervalTicks(player)",
				"LAST_STANCE_ATTACK.put(player.getUUID(), now)",
				"float cooledStrength = Math.max(0.75F",
				"double range = overloaded ?",
				"double width = overloaded ?",
				"targetsAlongLine(player, origin, direction",
				"SungIlHwanVfxEventMessage.sendSpatialSlash(player, null",
				"player.resetAttackStrengthTicker()");
		assertOrdered(lineCut,
				"for (LivingEntity target : targetsAlongLine",
				"SungIlHwanVfxEventMessage.sendSpatialSlash(player, null");
		assertContains(geometry,
				"Vec3 endpoint = origin.add(unitDirection.scale(range))",
				"getEntitiesOfClass(",
				"!player.hasLineOfSight(target)",
				"segmentIntersectsExpandedAabb(origin, endpoint",
				"AABB expanded = bounds.inflate(width)",
				"expanded.clip(origin, endpoint)",
				"if (targets.size() > maximum)",
				"targets.subList(0, maximum)");
		expectFalse(geometry.contains("nearbyTargets(player"),
				"Forward line candidates must be filtered by the real cut geometry before any target cap");
		assertContains(serverAttack,
				"AttackEntityEvent event",
				"shouldReplaceBasicAttack(player)",
				"replaceOrAlreadyReplacedAssassinAttack(player)",
				"event.setCanceled(true)");
		assertContains(livingAttack,
				"LivingIncomingDamageEvent event",
				"INTERNAL_DAMAGE.contains(player.getUUID())",
				"shouldReplaceBasicAttack(player)",
				"replaceOrAlreadyReplacedAssassinAttack(player)",
				"event.setCanceled(true)");
		expectTrue(combat.contains(
						"@SubscribeEvent(priority = EventPriority.HIGHEST)"),
				"The vanilla attack suppression must run at highest server event priority");
		assertContains(attackCadence,
				"Attributes.ATTACK_SPEED",
				"20.0D / attackSpeed",
				"Math.ceil(vanillaCooldownTicks * 0.8D)",
				"2, 10");
		expectTrue(!lineCut.substring(lineCut.indexOf(
								"SungIlHwanVfxEventMessage.sendSpatialSlash(player, null"))
								.contains("if (confirmedHit)"),
				"Assassin Stance must author its large forward cut even when the swing intersects nothing");
	}

	private static void spatialExecutionTargetsAndTraversesItsWholeExpandingSphere()
			throws IOException {
		String combat = readMain("util",
				"SungIlHwanCombatManager.java");
		String safeTeleport = readMain("util",
				"IgrisCombatTeleportHelper.java");
		String begin = section(combat,
				"private static void beginSpatialExecution",
				"private static void tickExecutionCharge");
		String charge = section(combat,
				"private static void tickExecutionCharge",
				"private static void releaseSpatialExecution");
		String release = section(combat,
				"private static void releaseSpatialExecution",
				"private static void tickExecutionTraversal");
		String traversal = section(combat,
				"private static void tickExecutionTraversal",
				"private static void completeExecutionTraversal");
		String completion = section(combat,
				"private static void completeExecutionTraversal",
				"private static void abortExecutionTraversal");
		String fractures = section(combat,
				"private static void tickFractures",
				"private static List<LivingEntity> executionTargets");
		String acquisition = section(combat,
				"private static List<LivingEntity> executionTargets",
				"private static double executionRadius");
		String radius = section(combat,
				"private static double executionRadius",
				"private static Vec3 executionCenter");
		String center = section(combat,
				"private static Vec3 executionCenter",
				"private static boolean returnToExecutionOrigin");
		String returnPath = section(combat,
				"private static boolean returnToExecutionOrigin",
				"private static List<LivingEntity> nearbyTargets");

		expectTrue(begin.contains("Vec3 focus = executionCenter(player)")
						&& begin.contains(
								"new ChargeState(player.level().getGameTime(), seed)")
						&& !begin.contains("getLookAngle()"),
				"Execution charging must begin on the player rather than an aimed focus target");
		assertContains(charge,
				"long heldTicks = Math.max(0L, now - charge.startedAt)",
				"double radius = executionRadius(player, heldTicks)",
				"sendExecutionTarget(player, null, focus, radius",
				"for (LivingEntity target : executionTargets(player, focus, radius))",
				"sendExecutionTarget(player, target, focus, radius");
		expectFalse(charge.contains("heldTicks > MAX_CHARGE_TICKS"),
				"Reaching maximum charge must hold the fully expanded sphere instead of silently cancelling it");
		expectTrue(radius.contains(
						"heldTicks / (double) MAX_CHARGE_TICKS")
						&& radius.contains(
								"Mth.clamp(")
						&& radius.contains(
								"Mth.lerp(progress, EXECUTION_START_RADIUS, maximum)")
						&& center.contains("return player.position().add("),
				"The server's elapsed hold time must expand a sphere that follows the player");
		assertContains(acquisition,
				"new AABB(focus, focus).inflate(radius)",
				"distanceToAabbSqr(focus",
				"MAX_EXECUTION_TARGETS",
				"targets.subList(0, MAX_EXECUTION_TARGETS)");
		expectTrue(combat.contains(
						"private static final int MAX_EXECUTION_TARGETS = 96;")
						&& combat.contains(
								"MAX_PENDING_FRACTURES = MAX_EXECUTION_TARGETS"),
				"Execution must include the whole practical sphere under one generous pathological-density ceiling");
		expectTrue(!acquisition.contains("hasLineOfSight")
						&& !charge.contains("bestExecutionTarget")
						&& !release.contains("findLookTarget")
						&& release.contains(
								"List<LivingEntity> targets = executionTargets(player, focus, radius)"),
				"Every valid enemy in the bounded sphere must be acquired regardless of aim direction or walls");

		assertContains(release,
				"player.level().getGameTime() - charge.startedAt",
				"ExecutionTraversal traversal = new ExecutionTraversal(",
				"player.level().dimension(), originalPosition, safeReturnPosition",
				"player.getYRot(), player.getXRot(), focus, radius",
				"EXECUTION_TRAVERSALS.put(player.getUUID(), traversal)");
		expectTrue(!release.contains("hurtInternally(")
						&& !traversal.contains("hurtInternally("),
				"Traversal is setup and presentation only; damage must wait for the common fracture timestamp");
		assertContains(traversal,
				"!player.level().dimension().equals(traversal.dimension)",
				"IgrisCombatTeleportHelper.tryMoveBehindTarget(player, target)",
				"traversal.nextTarget++",
				"traversal.nextStepAt = now + 1L",
				"completeExecutionTraversal(player, traversal, now)");
		assertContains(safeTeleport,
				"public static boolean tryMoveBehindTarget",
				"!isLoadedAndInsideBorder(level, moved)",
				"!level.noCollision(igris, moved)",
				"level.containsAnyLiquid(moved)",
				"isHazardous(");
		assertContains(completion,
				"if (!returnToExecutionOrigin(player, traversal))",
				"long sharedExecuteAt = now + EXECUTION_FRACTURE_DELAY",
				"new FractureState(target.targetId, sharedExecuteAt",
				"EXECUTION_FRACTURE_DELAY, traversal.seed");
		expectTrue(combat.contains(
						"private static final int EXECUTION_FRACTURE_DELAY = 20;")
						&& fractures.contains(
								"for (FractureState fracture : new ArrayList<>(pending))")
						&& fractures.contains("fracture.executeAt > now")
						&& fractures.contains("hurtInternally(player, target, fracture.damage)")
						&& !fractures.contains("processed >=")
						&& !fractures.contains("processed++"),
				"Every marked target must take its delayed damage in one shared 20-tick resolution pass");
		assertContains(returnPath,
				"traversal.originalPosition",
				"traversal.safeReturnPosition",
				"traversal.originalYaw",
				"traversal.originalPitch",
				"isSafeExecutionReturn(level, player, destination)",
				"player.teleportTo(level, destination.x, destination.y, destination.z",
				"IgrisCombatTeleportHelper.isSafeDestination",
				"findSafeExecutionReturnPosition",
				"destinationLevel.getSharedSpawnPos()");
		assertContains(safeTeleport,
				"public static boolean isSafeDestination",
				"supportState.getCollisionShape",
				"isHazardous(feetState)",
				"SUPPORT_CHECK_DEPTH");
		expectTrue(!combat.contains("resolvePendingFractures")
						&& !combat.contains("damageTraversalTargets")
						&& count(combat,
								"PENDING_FRACTURES.remove(player.getUUID())") >= 4,
				"Death, logout, identity loss, dimension changes, and resets must cancel queued damage instead of resolving before the shared timestamp");
		expectTrue(count(combat, "abortExecutionTraversal(player,") >= 5
						&& combat.contains(
								"EXECUTION_TRAVERSALS.remove(player.getUUID())")
						&& combat.contains(
								"private final Vec3 originalPosition")
						&& combat.contains("private final float originalYaw")
						&& combat.contains("private final float originalPitch"),
				"Death, logout, identity loss, dimension changes, and resets must abort safely and restore the recorded position and view when possible");
	}

	private static void visualFactsArePrivateWhereNeededAndStrictlyBounded()
			throws IOException {
		String packet = readMain("network",
				"SungIlHwanVfxEventMessage.java");
		String state = readMain("client", "renderer",
				"SungIlHwanVfxClientState.java");
		String renderer = readMain("client", "renderer",
				"SungIlHwanVfxRenderer.java");
		String renderTypes = readMain("client", "renderer", "shader",
				"SungIlHwanVfxRenderTypes.java");
		String overlay = readMain("client", "screens",
				"SungIlHwanVfxOverlay.java");
		String fragmentShader = Files.readString(ASSETS.resolve(Path.of(
				"shaders", "core", "rendertype_sung_il_hwan_vfx.fsh")));

		assertContains(packet,
				"sendStage(ServerPlayer caster",
				"sendStageEnd(ServerPlayer caster",
				"sendFearPulse(ServerPlayer caster",
				"sendFearMark(ServerPlayer caster",
				"sendSpatialSlash(ServerPlayer caster",
				"sendExecutionCharge(ServerPlayer caster",
				"sendExecutionTarget(ServerPlayer caster",
				"sendExecutionRelease(ServerPlayer caster",
				"sendExecutionFracture(ServerPlayer caster",
				"sendExecutionCancel(ServerPlayer caster",
				"sendExhaustion(ServerPlayer caster",
				"sendRiskFeedback(ServerPlayer caster");
		String charge = section(packet,
				"public static void sendExecutionCharge",
				"public static void sendExecutionTarget");
		String targetMark = section(packet,
				"public static void sendExecutionTarget",
				"public static void sendExecutionRelease");
		String release = section(packet,
				"public static void sendExecutionRelease",
				"public static void sendExecutionFracture");
		expectTrue(charge.contains(
						"create(EXECUTION_PUBLIC_CHARGE, caster, null, origin, origin")
						&& charge.contains(
								"FLAG_ESSENTIAL | FLAG_PRIVATE_CASTER")
						&& charge.contains("sendTo(caster"),
				"Opponents may see the charge tell, but only the caster may receive aim facts");
		expectTrue(targetMark.contains("sendTo(caster")
						&& targetMark.contains(
								"FLAG_ESSENTIAL | FLAG_PRIVATE_CASTER")
						&& targetMark.contains(
								"scopedTarget == null ? 1 : 2")
						&& !targetMark.contains("sendNear(")
						&& !targetMark.contains(
								"PacketDistributor.TRACKING_ENTITY"),
				"Each acquired Execution mark must remain owner-only and must never disclose target identities to observers");
		expectTrue(release.contains(
						"create(EXECUTION_RELEASE, caster, null")
						&& !release.contains(
								"create(EXECUTION_RELEASE, caster, scopedTarget"),
				"The public Judgment Cut release may disclose its sphere but never a chosen entity id");
		expectTrue(packet.contains(
						"NetworkDirection.PLAY_TO_CLIENT")
						&& packet.contains(
								"DistExecutor.unsafeRunWhenOn(Dist.CLIENT")
						&& packet.contains(
								"player.getId() != message.casterEntityId")
						&& state.contains(
								"message.casterEntityId != minecraft.player.getId()")
						&& packet.contains(
								"Mth.clamp(range, 1.0D, 128.0D)"),
				"The packet and client cache must be dedicated-server safe, clientbound, private-validated, and range-bounded");
		expectTrue(state.contains("MAX_EVENTS = 160")
						&& state.contains("while (EVENTS.size() > MAX_EVENTS)")
						&& state.contains("message.variant < 2")
						&& state.contains("message.variant >= 2")
						&& renderer.contains("FrameBudget")
						&& renderer.contains("maxVertices")
						&& renderer.contains("maxVisible")
						&& renderer.contains(
								"event.getFrustum().isVisible(bounds)"),
				"Client reconstruction must preserve the sphere plus individual marks under hard event, vertex, visibility, and frustum budgets");
		expectTrue(overlay.contains(
						"Owner-only Spatial Execution targeting HUD")
						&& overlay.contains(
								"without implying that the sphere center is an individual target")
						&& overlay.contains("renderExhaustion")
						&& overlay.contains("renderRisk"),
				"Targeting HUD must consume the caster-centered sphere rather than treating an individual mark as the field center");
		assertContains(renderer,
				"private static final int GOLD_AMBER",
				"private static final int GOLD_PALE",
				"private static final int GOLD_HOT",
				"private static final int GOLD_WHITE",
				"private static void renderSpatialSlash",
				"private static void renderPrivateTarget",
				"if (event.variant >= 2)",
				"private static void renderExecutionRelease",
				"frame.quality.executionCuts",
				"randomPointInSphere(",
				"private static void renderFracture");
		String stageRenderer = section(renderer,
				"private static void renderStage",
				"private static void renderStageEnd");
		String stageEndRenderer = section(renderer,
				"private static void renderStageEnd",
				"private static void renderFearPulse");
		String presenceRenderer = section(renderer,
				"private static void renderFearPulse",
				"private static void renderFearMark");
		String slashRenderer = section(renderer,
				"private static void renderSpatialSlash",
				"private static void renderPublicCharge");
		String sharpGeometry = section(renderer,
				"private static void drawSharpBlade",
				"private static void drawCrossedLine");
		expectTrue(stageRenderer.contains("renderSpiritualGroundEnergy")
						&& renderer.contains(
								"private static void renderSpiritualGroundEnergy")
						&& renderer.contains(
								"private static void drawEnergyStrand")
						&& !stageRenderer.contains("drawHorizontalRing")
						&& !stageEndRenderer.contains("drawHorizontalRing"),
				"Spiritualization must use broken three-dimensional roots/flame tongues without a flat platform or ending seal");
		assertContains(presenceRenderer,
				"renderPresenceBodySurge",
				"renderPresenceShockShell");
		expectTrue(renderer.contains(
						"private static void renderPresenceBodySurge")
						&& renderer.contains(
								"private static void renderPresenceShockShell")
						&& renderer.contains(
								"private static void drawEnergyShell")
						&& !presenceRenderer.contains("drawHorizontalRing"),
				"Predator's Presence must erupt from the whole body into a true three-dimensional shock shell, not another expanding floor circle");
		expectTrue(count(slashRenderer, "drawSharpBlade(") >= 4
						&& slashRenderer.contains("MATERIAL_SLASH")
						&& !slashRenderer.contains("drawCrossedLine("),
				"Assassin cuts must use pointed layered razor geometry rather than constant-width crossed cards");
		assertContains(sharpGeometry,
				"drawTaperedSlashRibbon",
				"Math.sin(Math.PI * first)",
				"Math.sin(Math.PI * second)",
				"width * 0.34F");
		expectTrue(fragmentShader.contains(
						"Spatial cuts use tapered geometry plus a procedural razor profile")
						&& fragmentShader.contains("float filament")
						&& fragmentShader.contains("float needle")
						&& fragmentShader.contains("float tipFade"),
				"The dedicated slash material must retain a sharp gold/white filament even with the custom shader active");
		String fractureRenderer = section(renderer,
				"private static void renderFracture",
				"private static void renderExhaustion");
		expectTrue(fractureRenderer.contains(
						"Vec3 sphereCenter = Vec3.ZERO")
						&& !fractureRenderer.contains(
								"new Vec3(0.0D, 1.0D, 0.0D)"),
				"The delayed fracture field must reuse the exact server-authored sphere center without a second vertical offset");
		expectTrue(count(renderer, "randomPointInSphere(") >= 3
						&& renderer.contains(
								"this.executionCuts = Math.max("),
				"Execution release and the simultaneous fracture must fill the bounded sphere with a gold Judgment-Cut-style cut field");
		expectTrue(renderTypes.contains("RegisterShadersEvent")
						&& renderTypes.contains(
								"SungIlHwanVfxClientState.onResourceReload()")
						&& renderTypes.contains(
								"RenderType.entityTranslucentEmissive"),
				"Custom shaders must reload safely and retain a vanilla fallback");
		for (String file : new String[] {
				"rendertype_sung_il_hwan_vfx.vsh",
				"rendertype_sung_il_hwan_vfx.fsh",
				"rendertype_sung_il_hwan_vfx_surface.json",
				"rendertype_sung_il_hwan_vfx_emissive.json" })
			expectTrue(Files.isRegularFile(ASSETS.resolve(
							Path.of("shaders", "core", file))),
					"Missing Sung Il-Hwan shader resource: " + file);
	}

	private static String advancement(String... parts)
			throws IOException {
		Path path = ADVANCEMENTS;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
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
					"Expected ordered source token: " + token);
			previous = current;
		}
	}

	private static void assertContains(String source, String... tokens) {
		for (String token : tokens)
			expectTrue(source.contains(token),
					"Missing source contract: " + token);
	}

	private static int count(String source, String token) {
		int result = 0;
		for (int position = 0;
				(position = source.indexOf(token, position)) >= 0;
				position += token.length())
			result++;
		return result;
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
