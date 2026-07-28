package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free regressions for derived temporary stats and the Demon King's
 * Dagger set passive.
 */
public final class TemporaryStatBonusRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private TemporaryStatBonusRegression() {
	}

	public static void main(String[] args) throws IOException {
		bonusesAreDerivedAndExtensible();
		effectBonusesNeverMutatePermanentStats();
		twoAsOneRequiresTheCompletePairAndAffectsCombatStrength();
		combatManagersUseEffectiveStats();
		systemPanelItemizesTemporarySources();
		legacyActiveEffectsAreMigratedOnce();
		daggerCopyIsCorrect();
	}

	private static void bonusesAreDerivedAndExtensible() throws IOException {
		String source = read("util", "TemporaryStatBonusManager.java");
		expectTrue(source.contains("public interface BonusProvider")
						&& source.contains("registerProvider(ResourceLocation")
						&& source.contains("public record BonusSource"),
				"Temporary stats need an extensible, itemized provider API");
		expectTrue(source.contains("getEffect(SololevelingModMobEffects.HASTE_BUFF.get())")
						&& source.contains("getEffect(SololevelingModMobEffects.PHYSICAL_BUFF.get())"),
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

	private static void twoAsOneRequiresTheCompletePairAndAffectsCombatStrength() throws IOException {
		String manager = read("util", "TemporaryStatBonusManager.java");
		expectTrue(manager.contains("living.getMainHandItem().is(SololevelingModItems.DEMON_KINGS_DAGGER.get())")
						&& manager.contains("living.getOffhandItem().is(SololevelingModItems.DEMON_KINGS_DAGGER.get())"),
				"Two as One must require Demon King's Daggers in both hands");
		expectTrue(manager.contains("sink.add(TWO_AS_ONE_SOURCE")
						&& manager.contains("baseValue"),
				"Two as One must grant one additional copy of permanent Strength");
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

	private static void daggerCopyIsCorrect() throws IOException {
		String item = read("item", "DemonKingsDaggerItem.java");
		String profile = read("client", "gui", "WeaponTooltipProfiles.java");
		expectFalse(item.contains("ATTACK WIL APPLY"),
				"The reported 'WIL' typo must not return");
		expectTrue(item.contains("TWO AS ONE WILL GRANT BONUS STRENGTH")
						&& item.contains("PERMANENT STRENGTH")
						&& profile.contains("Two as One")
						&& profile.contains("permanent Strength"),
				"Both legacy and custom dagger tooltips must explain the working passive");
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN_SOURCE;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static int count(String source, String token) {
		int result = 0;
		for (int position = 0; (position = source.indexOf(token, position)) >= 0; position += token.length())
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
