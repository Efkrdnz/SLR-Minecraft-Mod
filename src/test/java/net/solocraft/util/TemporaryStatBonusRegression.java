package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free regressions for derived temporary stats and held-weapon
 * passives.
 */
public final class TemporaryStatBonusRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private TemporaryStatBonusRegression() {
	}

	public static void main(String[] args) throws IOException {
		bonusesAreDerivedAndExtensible();
		effectBonusesNeverMutatePermanentStats();
		agilityScalesNeoForgeSwimSpeed();
		twoAsOneRequiresTheCompletePairAndAffectsCombatStrength();
		weaponPassivesUsePermanentStatsAndStackCorrectly();
		combatManagersUseEffectiveStats();
		intelligenceConsumersUseTheEffectiveStat();
		systemPanelItemizesTemporarySources();
		legacyActiveEffectsAreMigratedOnce();
		weaponPassiveCopyIsDescriptiveAndNonNumeric();
	}

	private static void bonusesAreDerivedAndExtensible() throws IOException {
		String source = read("util", "TemporaryStatBonusManager.java");
		expectTrue(source.contains("public interface BonusProvider")
						&& source.contains("registerProvider(ResourceLocation")
						&& source.contains("public record BonusSource"),
				"Temporary stats need an extensible, itemized provider API");
		expectTrue(source.contains("getEffect(SololevelingModMobEffects.HASTE_BUFF)")
						&& source.contains("getEffect(SololevelingModMobEffects.PHYSICAL_BUFF)"),
				"Effect bonuses must be derived from synchronized active-effect state");
		expectTrue(source.contains("HASTE_BUFF_AGILITY_BONUS = 30.0D")
						&& source.contains("PHYSICAL_BUFF_STRENGTH_BONUS = 30.0D")
						&& !source.contains("getAmplifier() + 1.0D"),
				"Derived effects must preserve the original fixed +30 bonus at every amplifier");
		expectTrue(source.contains("case STRENGTH -> variables.Strength")
						&& source.contains("case AGILITY -> variables.Speed"),
				"The permanent capability must remain the synchronized base-stat source");
	}

	private static void effectBonusesNeverMutatePermanentStats() throws IOException {
		for (String file : new String[] {
				"HasteBuffEffectStartedappliedProcedure.java",
				"HasteBuffEffectExpiresProcedure.java",
				"PhysicalBuffEffectStartedappliedProcedure.java",
				"PhysicalBuffEffectExpiresProcedure.java",
				"BuffResetProcedure.java" }) {
			String source = read("procedures", file);
			expectFalse(source.contains("capability.Speed =")
							|| source.contains("capability.Strength ="),
					file + " must not write temporary bonuses into permanent stats");
		}

		String speed = read("procedures", "SpeedUpdateProcedure.java");
		String strength = read("procedures", "StrengthUpdateProcedure.java");
		String dualWield = read("procedures", "DualWieldProcedure.java");
		String dualWieldTick = read("procedures", "DualWieldingDamageProcedure.java");
		expectTrue(speed.contains("TemporaryStatBonusManager.effectiveAgility(entity)"),
				"Haste must still affect movement, step height, and agility fall protection");
		expectTrue(strength.contains("TemporaryStatBonusManager.effectiveStrength(entity)"),
				"Physical and equipment Strength must still affect physical attack damage");
		expectFalse(strength.contains("% 20"),
				"Attack conversion must drop a removed equipment bonus on the next END tick");
		expectFalse(speed.contains("% 5"),
				"Movement conversion must drop an expired Haste bonus on the next END tick");
		expectTrue(count(dualWield, "TemporaryStatBonusManager.effectiveStrength(entity)") == 3
						&& dualWieldTick.contains("TemporaryStatBonusManager.effectiveStrength(entity)"),
				"Both Demon King's Dagger dual-wield damage paths must consume effective Strength");

		for (String file : new String[] {
				"BasicAttackSlashProcedure.java",
				"CrossStrikeProcedure.java",
				"ConsecutiveSlashesOnEffectActiveTickProcedure.java",
				"IceDashDamageProcedure.java",
				"KamishWrathEntitySwingsItemProcedure.java",
				"KatanaStierRightclickedProcedure.java",
				"QuickSlashesProcedure.java",
				"RushAttackProcedureProcedure.java",
				"SlashFurryBroadProcedure.java",
				"SnowScreenOnTickProcedure.java",
				"UpforceSlashProcedure.java" }) {
			String source = read("procedures", file);
			expectTrue(source.contains("TemporaryStatBonusManager.effectiveStrength(entity)"),
					file + " must preserve temporary Strength scaling");
			expectFalse(source.contains(")).Strength") || source.contains("-> cap.Strength"),
					file + " must not bypass effective Strength in a combat formula");
		}
		String tripleJump = read("procedures", "TripleJumpOnKeyPressedProcedure.java");
		expectTrue(tripleJump.contains("TemporaryStatBonusManager.effectiveAgility(entity) >= 31.0D")
						&& !tripleJump.contains(")).Speed >="),
				"Temporary Agility must satisfy Triple Jump's mobility threshold");

		for (String effect : new String[] {
				"HasteBuffMobEffect.java",
				"PhysicalBuffMobEffect.java" }) {
			String source = read("potion", effect);
			expectFalse(source.contains(".addAttributeModifier("),
					effect + " must not independently apply a second attribute bonus");
		}
	}

	private static void agilityScalesNeoForgeSwimSpeed() throws IOException {
		String speed = read("procedures", "SpeedUpdateProcedure.java");
		expectTrue(speed.contains("NeoForgeMod.SWIM_SPEED")
						&& speed.contains(
								"AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL")
						&& speed.contains(
								"agilitySwimSpeedMultiplier("),
				"Effective Agility must scale NeoForge swim speed without replacing other modifiers");
		expectTrue(speed.contains("MAX_SWIM_SPEED_MULTIPLIER = 4.0D")
						&& speed.contains(
								"Math.min(100.0D, speedPercent)"),
				"Swim scaling must respect the movement-speed setting and remain bounded");
	}

	private static void twoAsOneRequiresTheCompletePairAndAffectsCombatStrength() throws IOException {
		String manager = read("util", "TemporaryStatBonusManager.java");
		expectTrue(manager.contains("living.getMainHandItem().is(SololevelingModItems.DEMON_KINGS_DAGGER.get())")
						&& manager.contains("living.getOffhandItem().is(SololevelingModItems.DEMON_KINGS_DAGGER.get())"),
				"Two as One must require Demon King's Daggers in both hands");
		expectTrue(manager.contains("TWO_AS_ONE_FLAT_BONUS = 20.0D")
						&& manager.contains("TWO_AS_ONE_PERCENT_BONUS = 0.20D")
						&& manager.contains("scaledBonus(baseValue, TWO_AS_ONE_FLAT_BONUS, TWO_AS_ONE_PERCENT_BONUS)"),
				"Two as One must grant its flat-plus-scaling bonus from permanent Strength");
		expectFalse(manager.contains("sink.add(TWO_AS_ONE_SOURCE, Component.literal(\"Two as One set effect\"), baseValue)"),
				"Two as One must no longer double all permanent Strength");
	}

	private static void weaponPassivesUsePermanentStatsAndStackCorrectly() throws IOException {
		String manager = read("util", "TemporaryStatBonusManager.java");
		expectTrue(manager.contains("MANA_SENSITIVITY_STRENGTH_FLAT_BONUS = 10.0D")
						&& manager.contains("MANA_SENSITIVITY_STRENGTH_PERCENT_BONUS = 0.10D")
						&& manager.contains("MANA_SENSITIVITY_INTELLIGENCE_FLAT_BONUS = 10.0D")
						&& manager.contains("MANA_SENSITIVITY_INTELLIGENCE_PERCENT_BONUS = 0.10D")
						&& manager.contains("kamishFangs * manaSensitivityBonus(baseValue, permanentIntelligence)")
						&& manager.contains("entity, Stat.INTELLIGENCE")
						&& manager.contains("living.getMainHandItem().getItem()")
						&& manager.contains("living.getOffhandItem().getItem()")
						&& manager.contains("SololevelingModItems.KAMISH_WRATH.get()")
						&& manager.contains("SololevelingModItems.KAMISH_WRATH_2.get()"),
				"Each held Kamish fang must independently combine permanent Strength and Intelligence");
		int strengthBranch = manager.indexOf("if (stat == Stat.STRENGTH)");
		int manaSensitivity = manager.indexOf("sink.add(MANA_SENSITIVITY_SOURCE");
		int intelligenceBranch = manager.indexOf("if (stat != Stat.INTELLIGENCE)", strengthBranch);
		expectTrue(strengthBranch >= 0 && manaSensitivity > strengthBranch
						&& manaSensitivity < intelligenceBranch,
				"Mana Sensitivity must contribute to Strength rather than Intelligence");
		expectTrue(manager.contains("DEMONIC_ATTUNEMENT_FLAT_BONUS = 10.0D")
						&& manager.contains("DEMONIC_ATTUNEMENT_PERCENT_BONUS = 0.10D")
						&& manager.contains("AVARICIOUS_INSIGHT_FLAT_BONUS = 10.0D")
						&& manager.contains("AVARICIOUS_INSIGHT_PERCENT_BONUS = 0.10D")
						&& manager.contains("TEMPEST_AUTHORITY_FLAT_BONUS = 10.0D")
						&& manager.contains("TEMPEST_AUTHORITY_PERCENT_BONUS = 0.10D"),
				"The longsword, orb, and grimoire must use the smaller Intelligence tier");
		for (String item : new String[] {
				"DEMON_KINGS_LONG_SWORD",
				"ORB_OF_AVARICE",
				"STORM_GRIAMORE" }) {
			expectTrue(manager.contains("isHeld(living, SololevelingModItems." + item + ".get())"),
					item + " must grant its passive from either hand");
		}
		expectTrue(manager.contains("Math.floor(Math.max(0.0D, baseValue) * percentBonus)"),
				"Percentage additions must be based on the permanent stat and remain non-recursive");
	}

	private static void combatManagersUseEffectiveStats() throws IOException {
		assertEffectiveConsumer("AssassinSkillManager.java", "effectiveAgility(player)", 1);
		assertEffectiveConsumer("DaggerThrowManager.java", "effectiveAgility(player)", 2);
		assertEffectiveConsumer("BeastMonarchManager.java", "effectiveStrength(player)", 5);
		assertEffectiveConsumer("FrostMonarchManager.java", "effectiveStrength(player)", 6);
		assertEffectiveConsumer("GoliathCombatManager.java", "effectiveStrength(player)", 3);
		assertEffectiveConsumer("LiuManifestationManager.java", "effectiveStrength(player)", 1);
		assertEffectiveConsumer("LiuZhigangCombatManager.java", "effectiveStrength(player)", 1);
		assertEffectiveConsumer("TankerSkillManager.java", "effectiveStrength(player)", 1);

		String manaScaling = read("util", "VesselManaScaling.java");
		expectTrue(manaScaling.contains(".Strength"),
				"Temporary Strength must not raise permanent-progression mana pressure");
	}

	private static void intelligenceConsumersUseTheEffectiveStat() throws IOException {
		for (String file : new String[] {
				"MageCombatHelper.java",
				"WhiteFlameMonarchManager.java",
				"FrostMonarchManager.java",
				"RulersAuthorityManager.java",
				"RangerCombatManager.java",
				"PlayerVitalSync.java" }) {
			String source = read("util", file);
			expectTrue(source.contains("TemporaryStatBonusManager.effectiveIntelligence("),
					file + " must consume derived Intelligence");
			expectFalse(source.contains(".Intelligence"),
					file + " contains an unclassified direct Intelligence bypass");
		}
		// ManaRules is the single owner of the maximum-mana formula, so files may
		// satisfy the derived-Intelligence contract by delegating to it. The
		// guarantee stays intact because ManaRules is itself asserted below.
		String manaRules = read("util", "ManaRules.java");
		expectTrue(manaRules.contains("TemporaryStatBonusManager.effectiveIntelligence("),
				"ManaRules.java must consume derived Intelligence");
		expectFalse(manaRules.contains(".Intelligence"),
				"ManaRules.java contains an unclassified direct Intelligence bypass");

		for (String file : new String[] {
				"IntelligenceUpdateProcedure.java",
				"ManaRegenProcedure.java",
				"DemonKingsLongSwordLivingEntityIsHitWithToolProcedure.java",
				"FireReleaseBeamProcedure.java",
				"IceBallOnEntityTickUpdateProcedure.java",
				"StormBreatheTickProcedure.java" }) {
			String source = read("procedures", file);
			expectTrue(source.contains("TemporaryStatBonusManager.effectiveIntelligence(")
							|| source.contains("ManaRules.maximumMana("),
					file + " must consume derived Intelligence");
			expectFalse(source.contains(".Intelligence"),
					file + " contains an unclassified direct Intelligence bypass");
		}
		String sword = read("item", "DemonKingsLongSwordItem.java");
		expectTrue(sword.contains("TemporaryStatBonusManager.effectiveIntelligence(sourceentity)")
						&& !sword.contains(".Intelligence"),
				"Storm of the Flames must scale from effective Intelligence");
	}

	private static void assertEffectiveConsumer(String file, String token, int minimum) throws IOException {
		String source = read("util", file);
		expectTrue(count(source, token) >= minimum,
				file + " lost temporary-stat scaling in a live combat formula");
		expectFalse(source.contains(".Strength") || source.contains(".Speed"),
				file + " contains an unclassified direct temporary-stat bypass");
	}

	private static void systemPanelItemizesTemporarySources() throws IOException {
		String source = read("client", "gui", "system", "SystemPanelScreen.java");
		expectTrue(source.contains("Component.literal(\" (+\"")
						&& source.contains("ChatFormatting.GREEN"),
				"The System panel must render a green parenthesized bonus");
		expectTrue(source.contains("bonusTooltip")
						&& source.contains("\" from \"")
						&& source.contains("TemporaryStatBonusManager.sources"),
				"Hovering the bonus must itemize each contributing source");
	}

	private static void legacyActiveEffectsAreMigratedOnce() throws IOException {
		String source = read("util", "TemporaryStatBonusMigration.java");
		String resetPolicy = read("util", "PlayerResetKeyPolicy.java");
		expectTrue(source.contains("PlayerLoggedInEvent")
						&& source.contains("MIGRATION_RECEIPT")
						&& source.contains("variables.Speed - TemporaryStatBonusManager.HASTE_BUFF_AGILITY_BONUS")
						&& source.contains("variables.Strength - TemporaryStatBonusManager.PHYSICAL_BUFF_STRENGTH_BONUS"),
				"One-time login migration must remove old baked-in active-effect additions");
		int capabilityBlock = source.indexOf("ifPresent(variables -> {");
		int receiptInsideCapability = source.indexOf(
				"player.getPersistentData().putBoolean(MIGRATION_RECEIPT, true);", capabilityBlock);
		int capabilityBlockEnd = source.indexOf("\n\t\t});", capabilityBlock);
		expectTrue(capabilityBlock >= 0
						&& receiptInsideCapability > capabilityBlock
						&& receiptInsideCapability < capabilityBlockEnd,
				"The migration receipt must only be written after the player capability is available");
		expectTrue(source.contains("PlayerEvent.Clone")
						&& source.contains("event.getOriginal().getPersistentData().getBoolean(MIGRATION_RECEIPT)")
						&& source.contains("event.getEntity().getPersistentData().putBoolean(MIGRATION_RECEIPT, true)"),
				"Death cloning must carry the migration receipt to the replacement player");
		expectTrue(resetPolicy.contains("TemporaryStatBonusMigration.MIGRATION_RECEIPT.equals(key)"),
				"Character reset must preserve the completed migration receipt");
	}

	private static void weaponPassiveCopyIsDescriptiveAndNonNumeric() throws IOException {
		String dagger = read("item", "DemonKingsDaggerItem.java");
		String kamish = read("item", "KamishWrathItem.java");
		String kamishTwin = read("item", "KamishWrath2Item.java");
		String longsword = read("item", "DemonKingsLongSwordItem.java");
		String orb = read("item", "OrbOfAvariceItem.java");
		String grimoire = read("item", "StormGriamoreItem.java");
		String profile = read("client", "gui", "WeaponTooltipProfiles.java");
		expectFalse(dagger.contains("ATTACK WIL APPLY"),
				"The reported 'WIL' typo must not return");
		expectTrue(dagger.contains("TWO AS ONE DRAWS ON THE WIELDER'S PERMANENT STRENGTH")
						&& profile.contains("Two as One")
						&& profile.contains("permanent Strength"),
				"Both legacy and custom dagger tooltips must explain the working passive");
		expectTrue(kamish.contains("PASSIVE \\\"MANA SENSITIVITY\\\"")
						&& kamishTwin.contains("PASSIVE \\\"MANA SENSITIVITY\\\"")
						&& profile.contains("Mana Sensitivity")
						&& profile.contains("Each held fang combines permanent Strength and Intelligence"),
				"Both Kamish tooltips must explain the per-fang combined-power Strength scaling");
		expectTrue(longsword.contains("PASSIVE \\\"DEMONIC ATTUNEMENT\\\"")
						&& orb.contains("avaricious insight draws on permanent Intelligence")
						&& grimoire.contains("PASSIVE \\\"TEMPEST AUTHORITY\\\"")
						&& profile.contains("Demonic Attunement")
						&& profile.contains("Avaricious Insight")
						&& profile.contains("Tempest Authority"),
				"Every Intelligence relic must explain its held passive");
		for (String passiveLine : new String[] {
				lineContaining(dagger, "TWO AS ONE DRAWS"),
				lineContaining(kamish, "PASSIVE \\\"MANA SENSITIVITY\\\""),
				lineContaining(kamishTwin, "PASSIVE \\\"MANA SENSITIVITY\\\""),
				lineContaining(longsword, "PASSIVE \\\"DEMONIC ATTUNEMENT\\\""),
				lineContaining(orb, "avaricious insight draws"),
				lineContaining(grimoire, "PASSIVE \\\"TEMPEST AUTHORITY\\\"") }) {
			expectFalse(passiveLine.contains("+20") || passiveLine.contains("20%")
							|| passiveLine.contains("+10") || passiveLine.contains("10%"),
					"Item passive descriptions must explain scaling without exposing balance numbers");
		}
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN_SOURCE;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static int count(String source, String token) {
		int result = 0;
		for (int position = 0; (position = source.indexOf(token, position)) >= 0; position += token.length())
			result++;
		return result;
	}

	private static String lineContaining(String source, String token) {
		return source.lines().filter(line -> line.contains(token)).findFirst().orElse("");
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
