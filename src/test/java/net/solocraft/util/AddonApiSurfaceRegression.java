package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps the addon-facing API a facade rather than a second implementation.
 *
 * <p>The API in {@code net.solocraft.api} exists so addons can scale abilities
 * off hunter stats, pay for them out of the same mana pool, and describe classes
 * and vessels the mod has never heard of. Its whole value is that it routes to
 * the systems the game already runs on. The moment one of these computes or
 * copies something itself, addons and the base mod are running different rules,
 * and the difference shows up as an ability that ignores buffs, charges the
 * wrong price, or a vessel list that disagrees with the selection screen.
 *
 * <p>Source-text based to match the other regressions here, none of which load
 * Minecraft classes.
 */
public final class AddonApiSurfaceRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");

	/** Enum constants declared inside ManaRules.Band. */
	private static final Pattern BAND_CONSTANT = Pattern.compile("([A-Z][A-Z_]*)\\s*\\(");

	/** A CooldownManager call handed a raw, un-namespaced ability key. */
	private static final Pattern UNNAMESPACED_COOLDOWN = Pattern.compile(
			"CooldownManager\\.\\w+\\(\\s*entity\\s*,\\s*ability\\b");

	/** The jobId argument of a built-in VesselDefinition declaration. */
	private static final Pattern VESSEL_JOB_ID = Pattern.compile(
			"new VesselDefinition\\([A-Z_]+\\s*,\\s*[A-Za-z_\"][A-Za-z0-9_\"]*\\s*,\\s*(\\d+)");

	/** Predicates that mean "this vessel's heightened form is active". */
	private static final String[] FORM_PREDICATES = {
			"public static boolean isSpiritualized(Entity",
			"public static boolean isManifested(Entity",
			"public static boolean isCombatStance(Entity",
			"public static boolean isFangStance(Entity" };

	private AddonApiSurfaceRegression() {
	}

	public static void main(String[] args) throws IOException {
		manaFacadeDelegates();
		statsFacadeReadsEffectiveValues();
		exposedCostBandsCoverTheInternalOnes();
		cooldownKeysAreNamespaced();
		vesselRegistryStaysAView();
		vesselMirrorCannotImpersonateABuiltIn();
		vesselStateAsksEveryVessel();
		contributedFormsPersistAndSync();
		formsAreSetThroughTheSyncedPath();
		formIdsCannotBreakTheStoredFormat();
		builtInStancesOutrankContributedClaims();
		meleeClaimsAreRevalidatedServerSide();
		contributedVesselsOnlyRunWhereBuiltInsFailed();
		contributedVesselsShareTheGating();
		stylesUseTheExistingStorage();
		abilityCostIsSettledAfterTheEffect();
		spiritualizationGearClosesEveryEscapeRoute();
		presentationsAreReplacedNotMerged();
		clientDrawnDetailTravelsOnTheWire();
		styleRulesStayLoadableWithoutMinecraft();
		contributedThemingCannotReachBuiltIns();
		togglesCanAlwaysBeTurnedOff();
		skillListMatchesTheModsOwnFormat();
		SessionSurvivesAContributedClass();
		syncNeverStripsBehaviour();
		jobChangeGeneratesGroundBeforeDropping();
		rulersAuthorityIsCommandOnly();
		ourExplosionsSpareDroppedItems();
		dungeonBossIsAlwaysPlaced();

		System.out.println("Addon API surface regression checks passed.");
	}

	/**
	 * Mana must be read and spent through ManaRules, which owns Creative
	 * exemption, the floor at zero, and client sync. An addon subtracting the
	 * field itself would desync the HUD and charge Creative players.
	 */
	private static void manaFacadeDelegates() throws IOException {
		String source = read("api", "HunterMana.java");
		String[] required = {
				"ManaRules.currentMana", "ManaRules.maximumMana", "ManaRules.isFree",
				"ManaRules.cost", "ManaRules.canAfford", "ManaRules.spend" };
		for (String call : required)
			expect(source.contains(call),
					"HunterMana must delegate to " + call + " rather than reimplementing it");
		expect(!source.contains("PLAYER_VARIABLES_CAPABILITY"),
				"HunterMana must not touch the player attachment directly; ManaRules owns that");
	}

	/**
	 * The effective readers include equipment, effects, and temporary buffs. The
	 * raw attachment fields do not, so an ability scaled off them would quietly
	 * stop responding to every buff in the game.
	 */
	private static void statsFacadeReadsEffectiveValues() throws IOException {
		String source = read("api", "HunterStats.java");
		String[] stats = { "Strength", "Agility", "Perception", "Vitality", "Intelligence" };
		for (String stat : stats) {
			expect(source.contains("TemporaryStatBonusManager.effective" + stat),
					"HunterStats." + stat.toLowerCase() + " must read the effective value");
			expect(!source.contains("variables." + stat) && !source.contains("capability." + stat),
					"HunterStats must not read the raw " + stat
							+ " field; that ignores buffs, equipment, and effects");
		}
	}

	/**
	 * A band the internal table has but the exposed enum lacks is a cost tier no
	 * addon can reach, which pushes authors into hardcoding numbers instead.
	 */
	private static void exposedCostBandsCoverTheInternalOnes() throws IOException {
		List<String> internal = internalBands();
		String exposed = read("api", "AbilityCost.java");
		expect(!internal.isEmpty(), "Could not read any bands from ManaRules.Band");
		for (String band : internal)
			expect(exposed.contains("ManaRules.Band." + band),
					"AbilityCost does not expose the " + band
							+ " cost band, so addons cannot use it");
	}

	private static List<String> internalBands() throws IOException {
		String source = read("util", "ManaRules.java");
		int start = source.indexOf("public enum Band {");
		expect(start >= 0, "ManaRules must still declare the Band enum");
		int end = source.indexOf(";", start);
		expect(end > start, "Could not find the end of the Band constant list");

		List<String> bands = new ArrayList<>();
		Matcher matcher = BAND_CONSTANT.matcher(
				source.substring(start + "public enum Band {".length(), end));
		while (matcher.find())
			bands.add(matcher.group(1));
		return bands;
	}

	/**
	 * Addon cooldowns share one key space with the mod's own. An addon that
	 * picked a bare name like "dash" could cancel a built-in ability's cooldown,
	 * or have its own cancelled, depending only on who wrote first.
	 */
	private static void cooldownKeysAreNamespaced() throws IOException {
		String source = read("api", "AbilityCooldowns.java");
		expect(source.contains("key(owner, ability)"),
				"AbilityCooldowns must namespace keys by the calling mod");
		Matcher raw = UNNAMESPACED_COOLDOWN.matcher(source);
		expect(!raw.find(),
				"AbilityCooldowns passes a raw ability key to CooldownManager, which lets an "
						+ "addon collide with the mod's own cooldowns");
	}

	/**
	 * The registry derives its built-ins from the list VesselManager owns. Copying
	 * them instead would mean a vessel added to one and not the other, and a
	 * selection screen that disagrees with the mod about what exists.
	 */
	private static void vesselRegistryStaysAView() throws IOException {
		String source = read("api", "vessel", "VesselRegistry.java");
		expect(source.contains("VesselManager.definitions()"),
				"VesselRegistry must derive its built-ins from VesselManager.definitions()");

		// Any built-in identity appearing as a literal here means it was copied.
		for (String identity : builtInIdentities())
			expect(!source.contains("\"" + identity + "\""),
					"VesselRegistry hardcodes the built-in vessel \"" + identity
							+ "\"; it must derive built-ins rather than copy them");
	}

	private static void vesselMirrorCannotImpersonateABuiltIn() throws IOException {
		String registry = read("api", "vessel", "VesselRegistry.java");
		Matcher declared = Pattern.compile("CUSTOM_VESSEL_LEGACY_MIRROR = ([0-9]+)")
				.matcher(registry);
		expect(declared.find(), "VesselRegistry must declare CUSTOM_VESSEL_LEGACY_MIRROR");
		int mirror = Integer.parseInt(declared.group(1));
		expect(mirror != 0, "The vessel mirror must not be 0, which reads as holding no vessel");

		Matcher jobIds = VESSEL_JOB_ID.matcher(read("util", "VesselManager.java"));
		boolean sawAny = false;
		while (jobIds.find()) {
			sawAny = true;
			expect(Integer.parseInt(jobIds.group(1)) != mirror,
					"The vessel mirror " + mirror + " collides with a built-in vessel job id");
		}
		expect(sawAny, "Could not read any built-in vessel job ids from VesselManager");
	}

	/**
	 * Every vessel that can enter a heightened form must be reachable through the
	 * one question addons are told to ask. A vessel added with its own predicate
	 * and not wired in here would silently never count as spiritualized.
	 */
	private static void vesselStateAsksEveryVessel() throws IOException {
		String state = read("api", "vessel", "VesselState.java");
		for (String manager : managersDeclaringAForm())
			expect(state.contains(manager + "."),
					"VesselState does not ask " + manager
							+ ", so its heightened form would never count as spiritualized");
	}

	private static List<String> managersDeclaringAForm() throws IOException {
		List<String> managers = new ArrayList<>();
		try (Stream<Path> paths = Files.list(MAIN.resolve("util"))) {
			for (Path path : paths.toList()) {
				String name = path.getFileName().toString();
				if (!name.endsWith(".java"))
					continue;
				String source = Files.readString(path).replace("\r\n", "\n");
				for (String predicate : FORM_PREDICATES) {
					if (source.contains(predicate)) {
						managers.add(name.substring(0, name.length() - ".java".length()));
						break;
					}
				}
			}
		}
		expect(!managers.isEmpty(), "Found no managers declaring a heightened-form predicate");
		return managers;
	}

	private static List<String> builtInIdentities() throws IOException {
		String source = read("util", "VesselManager.java");
		Matcher matcher = Pattern.compile(
				"new VesselDefinition\\([A-Z_]+\\s*,\\s*\"([a-z0-9_]+)\"").matcher(source);
		List<String> identities = new ArrayList<>();
		while (matcher.find())
			identities.add(matcher.group(1));
		return identities;
	}

	/**
	 * Equipping is the easy half; the guards are why the helper exists.
	 *
	 * <p>Each of these is a way the temporary gear escapes or the player's real
	 * gear is lost. Dropping any one of them turns a spiritualization into an
	 * item duplicator or an inventory wipe, and neither failure is visible until
	 * a player reports it.
	 */
	private static void spiritualizationGearClosesEveryEscapeRoute() throws IOException {
		String source = read("api", "vessel", "SpiritualizationGear.java");
		String[] guards = {
				"LivingDeathEvent", "PlayerLoggedOutEvent", "PlayerLoggedInEvent",
				"ItemTossEvent", "LivingDropsEvent", "EntityJoinLevelEvent" };
		for (String guard : guards)
			// The full parameter, not the bare name: a substring match would accept
			// a handler renamed to something that never fires.
			expect(source.contains(guard + " event)"),
					"SpiritualizationGear must still subscribe to " + guard
							+ "; without it the form's gear escapes or eats real equipment");

		expect(source.contains("restoreAll"),
				"Death, logout and login must end every form, not one named form");
		// A form that re-saves on a second activate overwrites the player's real
		// armour with the form's own, and the original is gone for good.
		expect(source.contains("form.getBoolean(HAS_ARMOR)")
						&& source.contains("form.getBoolean(HAS_HANDS)"),
				"Equipping twice must be a no-op; re-saving would overwrite the player's "
						+ "real gear with the form's own and lose the original");
	}

	/**
	 * Presentations are replaced wholesale on reload and on sync. Merging would
	 * leave a class or vessel themed by a data pack that is no longer loaded.
	 */
	private static void presentationsAreReplacedNotMerged() throws IOException {
		for (String[] file : new String[][] {
				{ "api", "hunter", "HunterClassRegistry.java" },
				{ "api", "vessel", "VesselRegistry.java" } }) {
			String source = read(file);
			expect(source.contains("PRESENTATION.clear()"),
					file[file.length - 1] + " must clear presentations before applying new ones");
		}
		String sync = read("network", "AbilityDefinitionSyncMessage.java");
		expect(sync.contains("HunterClassRegistry.replacePresentations")
						&& sync.contains("VesselRegistry.replacePresentations"),
				"A sync must replace both presentation sets, or a client keeps theming "
						+ "something the server no longer has");
	}

	/**
	 * Anything the client draws has to cross the wire. Data packs load on the
	 * server only, so a detail left out of this message works in singleplayer --
	 * where the integrated server hides it -- and is blank on a real server.
	 */
	private static void clientDrawnDetailTravelsOnTheWire() throws IOException {
		String sync = read("network", "AbilityDefinitionSyncMessage.java");
		expect(sync.contains("ability.icon()"),
				"The ability icon must be written, or HUD slots draw the empty template "
						+ "on a dedicated server");
		expect(sync.contains("presentation.backdrop()") && sync.contains("presentation.color()"),
				"A Monarch's colour and backdrop must be written; the selection screen "
						+ "is client-side and cannot read the data pack");
	}

	/**
	 * {@link ClassStyleRules} is exercised by the regressions without Minecraft
	 * on the classpath. A direct reference to the contributed registry would drag
	 * ResourceLocation in and fail the whole suite with a NoClassDefFoundError
	 * rather than a readable message -- which is exactly what happened once.
	 */
	private static void styleRulesStayLoadableWithoutMinecraft() throws IOException {
		String source = read("util", "ClassStyleRules.java");
		expect(!source.contains("import net.minecraft."),
				"ClassStyleRules must not import Minecraft; the style economy is tested "
						+ "without a game");
		expect(!source.contains("HunterStyleRegistry"),
				"ClassStyleRules must reach contributed styles through the installed "
						+ "source, not by calling the registry");
		expect(source.contains("installContributedSource"),
				"The contributed style hook must still exist");
	}

	/** An addon must not be able to restyle anything the mod ships. */
	private static void contributedThemingCannotReachBuiltIns() throws IOException {
		expect(read("util", "ClassStyleRules.java").contains("if (!builtIn.isEmpty())"),
				"A class with shipped styles must keep only those; an addon cannot add "
						+ "a sixth Mage style");
		expect(read("api", "vessel", "VesselRegistry.java")
						.contains("vessel.kind() == Vessel.Kind.MONARCH"),
				"Only Monarchs resolve a contributed presentation; Rulers present alike");
		expect(read("api", "vessel", "VesselPresentationLoader.java")
						.contains("kind() != Vessel.Kind.MONARCH"),
				"A presentation naming a Ruler must be reported, not silently ignored");
		expect(read("client", "screens", "DisplayOverlay.java")
						.contains("default -> contributedSkillTexture("),
				"Built-in icons must be matched before a contributed one is consulted");
	}

	/**
	 * A toggle must be switchable off regardless of its own cooldown.
	 *
	 * <p>The cooldown gate used to sit above the toggle-off branch while the
	 * cooldown was armed at the end of the same method, so any toggle declaring
	 * {@code cooldown_ticks} locked the player inside its own form -- still
	 * paying upkeep -- until that cooldown expired.
	 *
	 * <p>Pinned by source order because the failure is invisible where it would
	 * normally be tested: {@code CooldownManager} bypasses cooldowns in Creative,
	 * so this only ever reproduced in Survival.
	 */
	private static void togglesCanAlwaysBeTurnedOff() throws IOException {
		String source = read("api", "skill", "HunterAbilityRegistry.java");

		int castAt = source.indexOf("public static boolean cast(");
		expect(castAt >= 0, "cast() must still exist");
		String body = source.substring(castAt);

		int toggleOff = body.indexOf("VesselState.isFormActive(player, ability.formId())");
		int gate = body.indexOf("AbilityCooldowns.isOnCooldown(player, owner, key)");
		expect(toggleOff >= 0, "cast() must still have a toggle-off branch");
		expect(gate >= 0, "cast() must still gate real casts on the cooldown");
		expect(toggleOff < gate,
				"The toggle-off branch must come BEFORE the cooldown gate. Turning a "
						+ "form off spends nothing and produces no effect, so gating it "
						+ "on the cooldown that arming it set traps the player in the form");

		// And the arming has to stay after both, or turning off would re-arm it.
		int arm = body.indexOf("AbilityCooldowns.set(player, owner, key");
		expect(arm > gate,
				"The cooldown must be armed after the gate, on a real cast only");
	}

	/**
	 * The skill list has a leading "." sentinel, and HunterSkills has to write it.
	 *
	 * <p>Without it everything works until {@code canonicalizeSkillList} next
	 * rewrites the field -- it re-adds the sentinel, the first entry then reads as
	 * ".Grave Spiritualization", and the hunter is told they never learned a skill
	 * they are holding the runestone for. The runestone then teaches it again,
	 * appending a duplicate.
	 *
	 * <p>Pinned against the canonicaliser itself rather than a literal, so the two
	 * cannot drift apart.
	 */
	private static void skillListMatchesTheModsOwnFormat() throws IOException {
		String rules = read("procedures", "ClassProgressionRules.java");
		expect(rules.contains("output.isEmpty() ? \".\" : \".\" + String.join(\",\", output) + \",\""),
				"canonicalizeSkillList must still produce the \".\"-prefixed format; "
						+ "if it changed, HunterSkills must change with it");

		String skills = read("api", "skill", "HunterSkills.java");
		expect(skills.contains("new StringBuilder(\".\")"),
				"HunterSkills.write must emit the leading \".\" sentinel, or the next "
						+ "canonicalisation pass makes every contributed skill unreadable");
		expect(skills.contains("while (name.startsWith(\".\"))"),
				"HunterSkills must strip the sentinel when reading, or the first skill "
						+ "in the list is never matched");

		String defaults = read("network", "SololevelingModVariables.java");
		expect(defaults.contains("public String Plist = \".\";"),
				"The empty skill list is a bare \".\", which is what makes the "
						+ "sentinel load-bearing rather than cosmetic");
	}

	/**
	 * A live evaluation session must survive a save/reload with a contributed
	 * class intact.
	 *
	 * <p>The session is written to NBT and read back constantly during the
	 * ceremony. Bounding its class id with the legacy built-in bound turned every
	 * contributed draw into zero, which stalled the phase machine mid-reroll --
	 * the screen sat on a full progress bar and never reached STYLE. Masking the
	 * remaining-class bag with the built-in mask stripped the contributed bits
	 * out of the shuffle on the same round trip, so a contributed class only ever
	 * reappeared on a refill.
	 */
	private static void SessionSurvivesAContributedClass() throws IOException {
		String source = read("util", "HunterEvaluationManager.java");

		expect(source.contains("session.classId = boundedSessionClass("),
				"A session's class id must use the session bound, which allows "
						+ "contributed ids; the legacy bound zeroes them");
		expect(!source.contains("session.classId = boundedClass("),
				"The legacy built-in bound must not be applied to a session's id");
		expect(source.contains("boundedSessionClass(int value)")
						&& source.contains("HunterEvaluationRules.MAX_CLASS_ID"),
				"boundedSessionClass must bound against MAX_CLASS_ID");

		int bag = source.indexOf("session.remainingClassMask = tag.getInt(");
		expect(bag >= 0, "The session must still restore its remaining-class bag");
		String bagLine = source.substring(bag, Math.min(source.length(), bag + 260));
		expect(bagLine.contains("drawableClassMask()"),
				"The restored bag must be masked with the drawable mask; the built-in "
						+ "mask drops contributed classes out of the shuffle");
		expect(!bagLine.contains("ALL_CLASSES_MASK"),
				"The restored bag must not be masked with the built-in-only mask");
	}

	/**
	 * The definition sync must not remove an ability's behaviour.
	 *
	 * <p>In single-player the client and the integrated server share this
	 * registry, so the handler runs against the server too. Clearing and
	 * re-registering with a null executor therefore stripped the executor the
	 * datapack had just supplied -- and the ability still had its name, colour,
	 * icon and cooldown, so it looked like it cast while doing nothing at all.
	 *
	 * <p>The give-away symptom is a toggle whose form activates and whose melee
	 * claim takes the attack button while producing no effect, which is exactly
	 * how it was reported.
	 */
	private static void syncNeverStripsBehaviour() throws IOException {
		String sync = read("network", "AbilityDefinitionSyncMessage.java");
		expect(sync.contains("HunterAbilityRegistry.replaceDataDefinitions("),
				"The sync must replace definitions through the executor-preserving "
						+ "path");
		expect(!sync.contains("register(ability, (String) null)"),
				"The sync must not re-register synced abilities with a null executor; "
						+ "in single-player that erases the server's own behaviour");
		expect(!sync.contains("clearDataDefinitions()"),
				"The sync must not clear definitions itself; that is what dropped the "
						+ "executors");

		String registry = read("api", "skill", "HunterAbilityRegistry.java");
		expect(registry.contains("public static synchronized void replaceDataDefinitions("),
				"replaceDataDefinitions must exist");
		int at = registry.indexOf("replaceDataDefinitions(");
		String body = registry.substring(at, Math.min(registry.length(), at + 1400));
		expect(body.contains("executorClassName") && body.contains("existing.executor"),
				"replaceDataDefinitions must carry both a deferred class name and an "
						+ "already-resolved executor across the replace");
	}

	/**
	 * The Job Change arena must exist before the player can fall into it.
	 *
	 * <p>The arena sits at a random coordinate up to thirty million blocks out in
	 * a dimension where nothing is loaded. Teleporting first only requests those
	 * chunks; restoring gravity on a fixed timer then assumed generation had
	 * finished. On a slower server it had not, and the player fell through empty
	 * space into the void -- while a friend beside them, whose own coordinates had
	 * generated in time, was fine.
	 */
	private static void jobChangeGeneratesGroundBeforeDropping() throws IOException {
		String source = read("procedures", "JobChangeQuestEntryProcedure.java");

		expect(source.contains("ensureArenaChunksLoaded("),
				"The arena chunks must be generated before the player is sent there");
		expect(source.contains("ChunkStatus.FULL"),
				"Chunk generation must be forced to completion, not merely requested");

		int ensure = source.indexOf("ensureArenaChunksLoaded(player.serverLevel()");
		int teleport = source.indexOf("player.connection.teleport(entryX");
		expect(ensure >= 0 && teleport >= 0 && ensure < teleport,
				"The ground must be generated BEFORE the teleport, not after");

		expect(source.contains("restoreGravityWhenGrounded("),
				"Gravity must be handed back when there is ground, not on a timer");
		expect(source.contains("hasGroundBeneath("),
				"Something must actually check for ground beneath the player");
	}

	/**
	 * Selecting a Ruler through the Job Change quest must not hand out Ruler's
	 * Authority. The runestone Igris drops is how it is earned; granting it on
	 * selection made that drop pointless. A vessel handed over by command is the
	 * exception, because the recipient may never have fought Igris at all.
	 */
	private static void rulersAuthorityIsCommandOnly() throws IOException {
		String source = read("util", "VesselManager.java");

		expect(source.contains("boolean enforceLimit, boolean grantAuthority"),
				"Assignment must carry whether the Authority comes with it");
		expect(source.contains("RULER.equals(definition.type()) && grantAuthority"),
				"A Ruler vessel must only grant Authority when the caller asked for it");
		expect(source.contains("assignPlayer(player, definition, enforceLimit, false)"),
				"The default assignment path must not grant Authority");

		int cmd = source.indexOf("public static int assign(CommandContext");
		expect(cmd >= 0, "The command entry point must still exist");
		String command = source.substring(cmd, Math.min(source.length(), cmd + 1500));
		expect(command.contains("assignPlayer(player, definition, true, true)"),
				"The command is the one path that grants Authority");
	}

	/**
	 * This mod's own explosions must not delete dropped loot.
	 *
	 * <p>Most of them already pass {@code ExplosionInteraction.NONE} so they leave
	 * the terrain alone, but a vanilla explosion still damages every entity in
	 * range and an item entity has too little health to survive one. A boss using
	 * an area attack over a fight's worth of drops erased them.
	 *
	 * <p>An unattributed explosion cannot be recognised as ours, so the guard only
	 * works while the mod's own explosions name a source.
	 */
	private static void ourExplosionsSpareDroppedItems() throws IOException {
		String guard = read("util", "ModExplosionItemGuard.java");
		expect(guard.contains("ExplosionEvent.Detonate"),
				"The guard must run while the explosion is choosing its victims");
		expect(guard.contains("ItemEntity") && guard.contains("ExperienceOrb"),
				"Both dropped items and experience must be spared");
		expect(guard.contains("getNamespace()"),
				"The guard must be scoped to this mod's own explosions; vanilla TNT "
						+ "destroying items is a rule players rely on");

		for (String procedure : new String[] {
				"LightBallWhileProjectileFlyingTickProcedure",
				"FireFlyHitsSomeoneProcedure" }) {
			String source = read("procedures", procedure + ".java");
			expect(!source.contains("explode(null"),
					procedure + " must attribute its explosion, or the item guard "
							+ "cannot tell the explosion is ours");
		}
	}

	/**
	 * A gate must generate with its boss, standing in open space.
	 *
	 * <p>This has been wrong twice. Spawning on the raw room centre without
	 * checking meant a pillar there left the gate with no boss at all, silently.
	 * Borrowing the ordinary mob's spawn point instead was worse: that check
	 * clears a one-block column, which is honest for a goblin and meaningless
	 * for a boss two and a half blocks wide, so the boss appeared inside a wall
	 * and suffocated.
	 *
	 * <p>Both failures are invisible to whoever generated the dungeon -- one is
	 * an empty room, the other a boss that dies before the player reaches it.
	 */
	private static void dungeonBossIsAlwaysPlaced() throws IOException {
		Path generator = Path.of("src", "main", "java", "net", "solocraft",
				"dungeon", "ProceduralDungeonGenerator.java");
		String source = Files.readString(generator).replace("\r\n", "\n");

		int at = source.indexOf("BossChoice bossChoice = pickBoss(");
		expect(at >= 0, "The boss spawn block must still exist");
		String block = source.substring(at);

		expect(block.contains("bossRoom.centerX()") && block.contains("bossRoom.centerZ()"),
				"The boss belongs at the room centre, the farthest point from every "
						+ "wall; a random point can be flush against one");
		expect(!block.contains("spawnPoint(level, bossRoom"),
				"The boss must not use the ordinary mob spawn point: it clears a "
						+ "one-block column, which a boss does not fit inside");
		expect(block.contains("clearFor(level, bossPos, bossChoice.type()"),
				"The pocket must be cleared to the boss's own size");
		expect(block.contains("LOGGER.error"),
				"A boss that still cannot be placed must be reported; a silent "
						+ "failure is a gate the player cannot complete");

		int carve = source.indexOf("private static void clearFor(");
		expect(carve >= 0, "clearFor must exist");
		String body = source.substring(carve, Math.min(source.length(), carve + 1200));
		expect(body.contains("type.getWidth()") && body.contains("type.getHeight()"),
				"The pocket must be sized from the entity, not from a fixed box");
		expect(body.contains("interiorHeight"),
				"The carve must be bounded by the room's interior height, or it "
						+ "reaches through the ceiling and opens the dungeon");
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	/**
	 * Contributed forms live in a synced field so the client can render an aura
	 * for one without the addon writing a packet. A field missing from any
	 * persistence path would make a transformation vanish on reconnect, or never
	 * reach the client at all.
	 */
	private static void contributedFormsPersistAndSync() throws IOException {
		String source = read("network", "SololevelingModVariables.java");
		String[] required = {
				"public String activeForms = \"\";",
				"clone.activeForms = original.activeForms;",
				"nbt.putString(\"activeForms\", activeForms);",
				"activeForms = nbt.getString(\"activeForms\");",
				"variables.activeForms = message.data.activeForms;" };
		for (String line : required)
			expect(source.contains(line),
					"PlayerVariables is missing an activeForms persistence path: " + line);
	}

	/**
	 * Turning a form on is a server decision. Letting the client set it would
	 * make a transformation something a modified client could grant itself.
	 */
	private static void formsAreSetThroughTheSyncedPath() throws IOException {
		String source = read("api", "vessel", "VesselState.java");
		int start = source.indexOf("public static boolean setFormActive(");
		expect(start >= 0, "VesselState must expose setFormActive(Entity, String, boolean)");
		String body = source.substring(start);
		expect(body.contains("capability.activeForms"),
				"setFormActive must write the synced field");
		expect(body.contains("syncPlayerVariables"),
				"setFormActive must sync, or the client never renders the form");
	}

	/**
	 * Ids are stored comma separated. One containing a comma would split into two
	 * forms that no lookup could ever match, and the failure would look like the
	 * form simply never activating.
	 */
	private static void formIdsCannotBreakTheStoredFormat() throws IOException {
		String source = read("api", "vessel", "VesselState.java");
		expect(source.contains("replace(\",\", \"\")"),
				"VesselState must strip commas from form ids before storing them");
	}

	/**
	 * Built-in stances must be tested before contributed claims. If a claim could
	 * be reached first, an addon could take the attack button away from Goliath
	 * or the Beast Monarch simply by existing.
	 */
	private static void builtInStancesOutrankContributedClaims() throws IOException {
		String source = read("mixins", "VesselCombatAttackMixin.java");
		int goliath = source.indexOf("GoliathCombatManager.isCombatStance(player)");
		int beast = source.indexOf("BeastMonarchManager.isFangStance(player)");
		int contributed = source.indexOf("VesselMelee.fire(player)");
		expect(goliath >= 0 && beast >= 0,
				"The attack mixin must still test the built-in stances");
		expect(contributed >= 0,
				"The attack mixin must consult VesselMelee so contributed vessels can claim melee");
		expect(goliath < contributed && beast < contributed,
				"A contributed melee claim is tested before a built-in stance; built-ins must win");
	}

	/**
	 * The client reports a button press. It does not get to report that a
	 * transformation was in effect, or any client could grant itself one.
	 */
	private static void meleeClaimsAreRevalidatedServerSide() throws IOException {
		String source = read("network", "VesselMeleeMessage.java");
		expect(source.contains("VesselState.isFormActive"),
				"VesselMeleeMessage must re-check the form is active before firing the event");
		int check = source.indexOf("VesselState.isFormActive");
		int post = source.indexOf("EVENT_BUS.post");
		expect(post >= 0, "VesselMeleeMessage must post VesselMeleeAttackEvent");
		expect(check < post,
				"VesselMeleeMessage posts the event before re-checking the form is active");
	}

	/**
	 * Contributed vessel selection must sit inside the branch where the built-in
	 * lookup already failed. Anywhere else and it could intercept, or subtly
	 * reorder, the selection of a shipped vessel -- which grants a permanent
	 * character choice and is not something to get wrong.
	 */
	private static void contributedVesselsOnlyRunWhereBuiltInsFailed() throws IOException {
		String source = read("util", "JobChangeQuestManager.java");
		int lookup = source.indexOf("VesselDefinition definition = VesselManager.definition(type, identity);");
		expect(lookup >= 0, "selectVessel must still resolve built-in vessels first");
		int nullBranch = source.indexOf("if (definition == null) {", lookup);
		expect(nullBranch > lookup, "selectVessel must still handle the unresolved case");

		int call = source.indexOf("selectContributedVessel(player, identity)");
		expect(call > nullBranch,
				"selectContributedVessel is reached before the built-in lookup fails");
		// Exactly one call site: a second could bypass the built-in path entirely.
		expect(source.indexOf("selectContributedVessel(player, identity)", call + 1) < 0,
				"selectContributedVessel has more than one call site in the selection flow");
	}

	/**
	 * The same gates a built-in answers to, applied to contributed vessels: the
	 * shared claim store, the same limit game rule, and the registry assignment
	 * that writes the same three fields VesselManager writes.
	 */
	private static void contributedVesselsShareTheGating() throws IOException {
		String source = read("util", "JobChangeQuestManager.java");
		int start = source.indexOf("private static boolean selectContributedVessel(");
		expect(start >= 0, "JobChangeQuestManager must declare selectContributedVessel");
		String body = source.substring(start);

		expect(body.contains("VesselClaimSavedData.get(player.serverLevel())"),
				"Contributed vessels must be claimed against the same saved data as built-ins");
		expect(body.contains("VesselManager.vesselLimit(player)"),
				"Contributed vessels must respect the same server vessel limit game rule");
		expect(body.contains("VesselRegistry.assign(player, vessel)"),
				"Contributed vessels must be assigned through VesselRegistry");
		expect(body.contains("selectionOpenReady(player)"),
				"Contributed vessels must respect the same selection-ready gate");

		// Built-in claim keys are type + ":" + identity. An unprefixed contributed
		// key could be impersonated by a mod namespaced "monarch" or "ruler".
		expect(body.contains("\"vessel:\" + vessel.id()"),
				"Contributed claim keys must be prefixed so they cannot collide with built-in keys");
	}

	/**
	 * A hunter has one style. Two registries keeping their own answer is how two
	 * mods end up disagreeing about the same hunter, so contributed styles live in
	 * the field the mod already reads.
	 */
	private static void stylesUseTheExistingStorage() throws IOException {
		String source = read("api", "hunter", "HunterStyleRegistry.java");
		expect(source.contains("capability.classStyle"),
				"HunterStyleRegistry must store styles in the existing classStyle field");
		expect(source.contains("syncPlayerVariables"),
				"Assigning a style must sync, or the client keeps the old one until relog");
		expect(source.contains("HunterClassRegistry.of(entity).id().equals(style.owningClass())"),
				"A style must not be assignable to a hunter who does not hold its class");
	}

	/**
	 * Cost has to be worked out after the effect, or an ability that reaches four
	 * targets costs the same as one that reached none.
	 */
	private static void abilityCostIsSettledAfterTheEffect() throws IOException {
		String source = read("api", "skill", "HunterAbilityRegistry.java");
		int check = source.indexOf("HunterMana.canAfford");
		int run = source.indexOf("executor.execute(");
		int charge = source.indexOf("HunterMana.spend(");
		expect(check >= 0 && run >= 0 && charge >= 0,
				"cast must check affordability, run the executor, then charge");
		expect(check < run,
				"Affordability must be checked before the effect, so a cast cannot start on empty");
		expect(run < charge,
				"Mana is charged before the effect runs, so cost cannot scale with what it reached");
	}
}
