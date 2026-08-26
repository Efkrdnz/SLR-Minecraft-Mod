package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source regressions for manual Grand Marshal appointment,
 * cap-bypassing admin levels, summon-screen validation, and borrowed abilities.
 */
public final class GrandMarshalFeatureRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private GrandMarshalFeatureRegression() {
	}

	public static void main(String[] args) throws IOException {
		grandMarshalIsNeverAutomaticallyGranted();
		appointmentIsManualExclusiveAndLevelGated();
		marshalAndAboveAlwaysCarryTheDomainBoost();
		adminLevelCommandsPreserveUncappedLevels();
		summonScreenPromotionIsServerValidated();
		borrowedAbilityRequiresTheLivingCommanderAndSharesCooldown();
		borrowedAbilityIsADeveloperOnlyWipPreview();
	}

	private static void marshalAndAboveAlwaysCarryTheDomainBoost()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String upkeep = method(manager,
				"private static void maintainMarshalDomainBoost",
				"private static void maintainMarshalDomainEffect");
		expectTrue(upkeep.contains("rank < RANK_MARSHAL"),
				"The permanent rank domain must begin at Marshal and include Grand Marshal");
		expectTrue(upkeep.contains("MobEffects.DAMAGE_BOOST")
						&& upkeep.contains("MobEffects.MOVEMENT_SPEED")
						&& upkeep.contains("MobEffects.DAMAGE_RESISTANCE")
						&& upkeep.contains(
								"SololevelingModMobEffects.DOMAIN_BOOST"),
				"Marshal upkeep must mirror every gameplay and visual part of Monarch's Domain");

		String stats = method(manager,
				"private static void applyLevelStats",
				"private static void applyLevelStatsPreservingHealth");
		expectTrue(stats.contains("maintainMarshalDomainBoost(living, rank)"),
				"Newly summoned and newly promoted Marshals must receive the domain immediately");
		String synchronization = method(manager,
				"private static void synchronizeShadowLevel",
				"private static List<CompoundTag> summonedOwnedShadows");
		expectTrue(synchronization.contains(
								"maintainMarshalDomainBoost(living, rank)"),
				"Normal authoritative roster synchronization must continuously restore the rank domain");
		String refresh = method(manager,
				"private static void maintainMarshalDomainEffect",
				"private static void clearIntrinsicMarshalDomainBoost");
		expectTrue(refresh.contains(
								"MARSHAL_DOMAIN_REFRESH_THRESHOLD_TICKS")
						&& refresh.contains("TEMPORARY_DOMAIN_UNTIL"),
				"Permanent upkeep must refresh expiring buffs without shortening a temporary cast");

		String demotion = method(manager,
				"private static void clearIntrinsicMarshalDomainBoost",
				"private static void removeIntrinsicMarshalDomainEffect");
		expectTrue(demotion.contains("TEMPORARY_DOMAIN_UNTIL")
						&& demotion.contains(
								"removeIntrinsicMarshalDomainEffect"),
				"Demotion must remove only intrinsic effects and preserve a currently cast Domain");
		String domain = read("procedures", "DomainChargeProcedure.java");
		expectTrue(domain.contains(
						"ShadowMonarchManager.markTemporaryDomainBoost(entityiterator, 3000)"),
				"The active Domain cast must mark its own lifetime independently of legacy domainef");
	}

	private static void grandMarshalIsNeverAutomaticallyGranted()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		expectTrue(manager.contains("RANK_SCHEMA_VERSION = 3"),
				"The explicit-promotion save migration must be versioned");
		String migration = method(manager,
				"private static void migrateShadowRanks",
				"private static void repairGrandMarshalClaim");
		expectTrue(migration.contains("root.remove(GRAND_MARSHAL_ID)")
						&& migration.contains("automaticRankForLevel("),
				"Legacy automatic Grand Marshals must migrate back to their earned rank");
		expectFalse(migration.contains(
				"shadow.putInt(RANK, RANK_GRAND_MARSHAL)"),
				"Migration must not choose a Grand Marshal automatically");

		String promotion = method(manager,
				"private static boolean promoteShadow",
				"public static int shadowLevelCap");
		expectTrue(promotion.contains("oldRank < RANK_MARSHAL"),
				"Ordinary XP may still advance boss shadows up to Marshal");
		expectFalse(promotion.contains("oldRank == RANK_MARSHAL")
						|| promotion.contains(
								"newRank = RANK_GRAND_MARSHAL"),
				"Ordinary XP must never auto-promote Marshal to Grand Marshal");
	}

	private static void appointmentIsManualExclusiveAndLevelGated()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String assignment = method(manager,
				"public static GrandMarshalAssignmentResult assignGrandMarshal",
				"public static GrandMarshalCommander activeGrandMarshal");
		expectTrue(assignment.contains(
				"VesselProgressionManager.isShadowMonarch(serverPlayer)")
						&& assignment.contains(
								"rankOf(target) != RANK_MARSHAL")
						&& assignment.contains(
								"grandMarshalRequiredLevel(type)"),
				"Appointment must require a Shadow Monarch, Marshal rank, and its boss level threshold");
		expectTrue(assignment.contains(
				"previous.putInt(RANK, RANK_MARSHAL)")
						&& assignment.contains(
								"ownerRoot.putString(GRAND_MARSHAL_ID")
						&& assignment.contains(
								"target.putInt(RANK, RANK_GRAND_MARSHAL)"),
				"Assigning a new commander must replace, not duplicate, the one Grand Marshal seat");
		expectTrue(assignment.contains("JobSkillManager.syncJobSkills"),
				"Appointment must immediately expose the borrowed skill");
	}

	private static void adminLevelCommandsPreserveUncappedLevels()
			throws IOException {
		String command = read("command", "SlrCommand.java");
		expectTrue(command.contains("Commands.literal(\"shadows\")")
						&& command.contains("Commands.literal(\"level\")")
						&& command.contains("Commands.literal(\"add\")")
						&& command.contains("Commands.literal(\"set\")")
						&& command.contains(
								"ShadowMonarchManager.MAX_ADMIN_SHADOW_LEVEL"),
				"/slr <player> shadows level must expose bounded add and set operations");

		String manager = read("util", "ShadowMonarchManager.java");
		String levelCommand = method(manager,
				"public static ShadowLevelCommandResult modifyShadowLevels",
				"public static boolean dismissShadowType");
		expectTrue(levelCommand.contains(
				"shadow.putInt(ADMIN_LEVEL_FLOOR, newLevel)")
						&& levelCommand.contains(
								"recalculateRankAfterAdminLevel")
						&& levelCommand.contains(
								"refreshSummonedShadowRank"),
				"Commanded levels must survive normalization and update rank/live entities");
		String normalization = method(manager,
				"private static int effectiveShadowLevelCap",
				"private static void synchronizeShadowLevel");
		expectTrue(normalization.contains("ADMIN_LEVEL_FLOOR")
						&& normalization.contains(
								"effectiveShadowLevelCap(player, shadow)"),
				"Normal roster synchronization must respect the admin override floor");
	}

	private static void summonScreenPromotionIsServerValidated()
			throws IOException {
		String menu = read("world", "inventory",
				"ShadowSummonGUIMenu.java");
		expectTrue(menu.contains("FLAG_GRAND_MARSHAL_ELIGIBLE")
						&& menu.contains("FLAG_GRAND_MARSHAL_ACTIVE"),
				"Grand Marshal state must be synchronized through menu data");
		String screen = read("client", "gui",
				"ShadowSummonGUIScreen.java");
		expectTrue(screen.contains("new ShadowSummonGUIButtonMessage(300 + entry.id")
						&& screen.contains("GrandMarshalButton"),
				"The boss cards must expose the manual promotion control");

		String packet = read("network",
				"ShadowSummonGUIButtonMessage.java");
		String handler = method(packet,
				"private static void assignGrandMarshal",
				"private static void openDismiss");
		expectTrue(handler.contains(
				"entity.containerMenu instanceof ShadowSummonGUIMenu")
						&& handler.contains(
								"ShadowMonarchManager.assignGrandMarshal"),
				"Promotion packets must validate the active summon menu before mutating rank");
	}

	private static void borrowedAbilityRequiresTheLivingCommanderAndSharesCooldown()
			throws IOException {
		String manager = read("util", "ShadowMonarchManager.java");
		String active = method(manager,
				"public static GrandMarshalCommander activeGrandMarshal",
				"private static long saturatingDisplayXpAdd");
		expectTrue(active.contains("shadow.hasUUID(\"summoned\")")
						&& active.contains("living.isAlive()")
						&& active.contains(
								"summoned.level() != player.level()")
						&& active.contains(
								"isCurrentSummonedInstance"),
				"Borrowing must require the exact living, summoned commander in the player's dimension");

		String ability = read("util",
				"GrandMarshalAbilityManager.java");
		expectTrue(ability.contains(
				"public static final String COOLDOWN_KEY = \"grand_marshal_authority\"")
						&& ability.contains(
								"CooldownManager.isOnCooldown(player, COOLDOWN_KEY)")
						&& ability.contains(
								"CooldownManager.set(player, COOLDOWN_KEY"),
				"Every signature variant must use one shared cooldown");
		for (String boss : new String[] {
				"igris", "beru", "tusk", "kamish", "kaisel" }) {
			expectTrue(ability.contains("case \"" + boss + "\""),
					"Missing curated Grand Marshal ability for " + boss);
		}

		String jobs = read("util", "JobSkillManager.java");
		expectTrue(jobs.contains("GRAND_MARSHAL_AUTHORITY")
						&& jobs.contains(
								"GrandMarshalAbilityManager.cast(entity)")
						&& jobs.contains(
								"ShadowMonarchManager.hasAssignedGrandMarshal(entity)"),
				"The appointed commander must grant one equipable authority skill");
	}

	private static void borrowedAbilityIsADeveloperOnlyWipPreview()
			throws IOException {
		String ability = read("util", "GrandMarshalAbilityManager.java");
		expectTrue(ability.contains(
						"!DeveloperModeManager.isEnabled(player)")
						&& ability.contains("WIP (Work in progress)"),
				"Grand Marshal Authority must refuse normal-player casts as WIP");

		String jobs = read("util", "JobSkillManager.java");
		expectTrue(jobs.contains(
						"GRAND_MARSHAL_AUTHORITY.equals(skill)")
						&& jobs.contains(
								"!DeveloperModeManager.isEnabled(entity)")
						&& jobs.contains("WIP (Work in progress)"),
				"The equipment GUI tooltip must disguise the borrowed authority as WIP");
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

	private static String read(String... relative) throws IOException {
		Path path = MAIN_SOURCE;
		for (String part : relative)
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
