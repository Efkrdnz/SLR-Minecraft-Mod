package net.solocraft.util;

import net.solocraft.util.TankerSkillManager.CombatCategory;
import net.solocraft.util.TankerSkillManager.ProtectionFundingResult;
import net.solocraft.util.TankerSkillManager.ReinforcementPhase;
import net.solocraft.util.TankerSkillManager.StrainReliefResult;
import net.solocraft.util.TankerSkillManager.WillpowerHitResult;

/**
 * Dependency-free characterization checks for the centralized Tanker balance
 * formulas. All inputs are pure values so failures identify manager math rather
	 * than NeoForge event or entity setup.
 */
public final class TankerSkillManagerCombatMathRegression {
	private static final double EPSILON = 1.0E-9D;

	private TankerSkillManagerCombatMathRegression() {
	}

	public static void main(String[] args) {
		hybridManaCostsHonorFloorsPercentagesAndCeiling();
		categoryTablesAndMitigationCapsAreExact();
		reinforcementPhasesAreExclusiveAndStable();
		willpowerConservesDamageWithOneNamedRelief();
		willpowerRatchetsCapsWithoutErasingDebt();
		protectionMarkFundsOnlyActualPrevention();
	}

	private static void hybridManaCostsHonorFloorsPercentagesAndCeiling() {
		String[] skills = {
				TankerSkillManager.TAUNT,
				TankerSkillManager.SHIELD_BASH,
				TankerSkillManager.TANK_LEAP,
				TankerSkillManager.REINFORCEMENT,
				TankerSkillManager.WILLPOWER,
				TankerSkillManager.PROTECTION_MARK
		};
		int[] percentageSideAtTenThousandMana = {150, 250, 400, 600, 900, 1200};
		int[] fractionalCeilAtTenThousandAndOneMana = {151, 251, 401, 601, 901, 1201};
		int[] flatFloors = {
				TankerSkillManager.TAUNT_FLAT_COST,
				TankerSkillManager.SHIELD_BASH_FLAT_COST,
				TankerSkillManager.TANK_LEAP_FLAT_COST,
				TankerSkillManager.REINFORCEMENT_FLAT_COST,
				TankerSkillManager.WILLPOWER_FLAT_COST,
				TankerSkillManager.PROTECTION_MARK_FLAT_COST
		};
		double[] percentages = {
				TankerSkillManager.TAUNT_PERCENT_COST,
				TankerSkillManager.SHIELD_BASH_PERCENT_COST,
				TankerSkillManager.TANK_LEAP_PERCENT_COST,
				TankerSkillManager.REINFORCEMENT_PERCENT_COST,
				TankerSkillManager.WILLPOWER_PERCENT_COST,
				TankerSkillManager.PROTECTION_MARK_PERCENT_COST
		};

		for (int i = 0; i < skills.length; i++) {
			expectInt(flatFloors[i], TankerSkillManager.beaconCost(500.0D, flatFloors[i], percentages[i]),
					skills[i] + " clamps maximum mana to at least 1000");
			expectInt(flatFloors[i], TankerSkillManager.beaconCost(1000.0D, flatFloors[i], percentages[i]),
					skills[i] + " flat-floor side");
			expectInt(percentageSideAtTenThousandMana[i],
					TankerSkillManager.beaconCost(10000.0D, flatFloors[i], percentages[i]),
					skills[i] + " percentage side");
			expectInt(fractionalCeilAtTenThousandAndOneMana[i],
					TankerSkillManager.beaconCost(10001.0D, flatFloors[i], percentages[i]),
					skills[i] + " rounds once with Math.ceil");
		}

		expectInt(180, TankerSkillManager.beaconCost(7200.0D,
						TankerSkillManager.SHIELD_BASH_FLAT_COST,
						TankerSkillManager.SHIELD_BASH_PERCENT_COST),
				"Shield Bash exact floor/percentage crossover");
		expectInt(260, TankerSkillManager.beaconCost(6500.0D,
						TankerSkillManager.TANK_LEAP_FLAT_COST,
						TankerSkillManager.TANK_LEAP_PERCENT_COST),
				"Tank Leap exact floor/percentage crossover");
		expectInt(900, TankerSkillManager.beaconCost(7500.0D,
						TankerSkillManager.PROTECTION_MARK_FLAT_COST,
						TankerSkillManager.PROTECTION_MARK_PERCENT_COST),
				"Protection Mark exact floor/percentage crossover");
	}

	private static void categoryTablesAndMitigationCapsAreExact() {
		expectDouble(0.0D, TankerSkillManager.ironWallReduction(-1, CombatCategory.NORMAL),
				"negative Iron Wall stacks clamp to zero");
		expectDouble(0.02D, TankerSkillManager.ironWallReduction(1, CombatCategory.NORMAL),
				"normal PvE Iron Wall per stack");
		expectDouble(0.20D, TankerSkillManager.ironWallReduction(10, CombatCategory.NORMAL),
				"normal PvE Iron Wall cap");
		expectDouble(0.20D, TankerSkillManager.ironWallReduction(99, CombatCategory.BOSS),
				"boss Iron Wall cap");
		expectDouble(0.10D, TankerSkillManager.ironWallReduction(10, CombatCategory.PVP),
				"PvP Iron Wall cap");

		expectMarkTable(CombatCategory.NORMAL, 0.12D, 0.18D);
		expectMarkTable(CombatCategory.BOSS, 0.10D, 0.15D);
		expectMarkTable(CombatCategory.PVP, 0.08D, 0.10D);

		expectDouble(0.65D, TankerSkillManager.customReductionCap(CombatCategory.NORMAL),
				"normal PvE custom reduction cap");
		expectDouble(0.55D, TankerSkillManager.customReductionCap(CombatCategory.BOSS),
				"boss custom reduction cap");
		expectDouble(0.45D, TankerSkillManager.customReductionCap(CombatCategory.PVP),
				"PvP custom reduction cap");

		expectDouble(35.0D, TankerSkillManager.retainedDamage(
				100.0D, CombatCategory.NORMAL, 10, 0.18D, ReinforcementPhase.PERFECT),
				"normal PvE layers are bounded by the 65% reduction cap");
		expectDouble(45.0D, TankerSkillManager.retainedDamage(
				100.0D, CombatCategory.BOSS, 10, 0.15D, ReinforcementPhase.PERFECT),
				"boss layers are bounded by the 55% reduction cap");
		expectDouble(55.0D, TankerSkillManager.retainedDamage(
				100.0D, CombatCategory.PVP, 10, 0.10D, ReinforcementPhase.PERFECT),
				"PvP layers are bounded by the 45% reduction cap");
		expectDouble(100.0D, TankerSkillManager.retainedDamage(
				100.0D, CombatCategory.NORMAL, 0, 0.0D, ReinforcementPhase.NONE),
				"no custom layers leave damage unchanged");
	}

	private static void reinforcementPhasesAreExclusiveAndStable() {
		expectInt(12, TankerSkillManager.PERFECT_BRACE_DURATION, "perfect brace duration");
		expectInt(80, TankerSkillManager.REINFORCED_STANCE_DURATION, "reinforced stance duration");

		expectReinforcementTable(CombatCategory.NORMAL, 0.60D, 0.25D, 0.20D, 0.50D);
		expectReinforcementTable(CombatCategory.BOSS, 0.50D, 0.20D, 0.20D, 0.50D);
		expectReinforcementTable(CombatCategory.PVP, 0.35D, 0.15D, 0.50D, 0.70D);
	}

	private static void willpowerConservesDamageWithOneNamedRelief() {
		WillpowerHitResult firstHit = TankerSkillManager.calculateWillpowerHit(
				20.0D, 0.0D, Double.POSITIVE_INFINITY, 100.0D, CombatCategory.NORMAL);
		expectDouble(10.0D, firstHit.immediateDamage(), "normal PvE immediate half");
		expectDouble(10.0D, firstHit.strainAdded(), "normal PvE delayed half");
		expectDouble(10.0D, firstHit.resultingStrain(), "first hit Strain");
		expectDouble(40.0D, firstHit.activeCap(), "initial normal PvE Strain cap");
		expectFalse(firstHit.endsWillpower(), "a sub-cap hit keeps Willpower active");

		StrainReliefResult relief = TankerSkillManager.calculateShieldBashRelief(
				firstHit.resultingStrain(), 100.0D, false);
		expectDouble(8.0D, relief.removedStrain(), "Shield Bash removes at most 8% max health");
		expectDouble(2.0D, relief.remainingStrain(), "relief leaves the unpaid remainder");
		expectTrue(relief.reliefUsed(), "positive relief consumes the activation budget");

		WillpowerHitResult secondHit = TankerSkillManager.calculateWillpowerHit(
				10.0D, relief.remainingStrain(), firstHit.activeCap(), 100.0D, CombatCategory.BOSS);
		expectDouble(6.0D, secondHit.immediateDamage(), "boss hit immediate share");
		expectDouble(4.0D, secondHit.strainAdded(), "boss hit delayed share");
		expectDouble(6.0D, secondHit.resultingStrain(), "post-relief mixed-category Strain");
		expectDouble(35.0D, secondHit.activeCap(), "boss damage ratchets the cap to 35%");

		double settlement = sum(TankerSkillManager.settlementPulses(secondHit.resultingStrain()));
		double retained = 20.0D + 10.0D;
		double accounted = firstHit.immediateDamage() + secondHit.immediateDamage()
				+ settlement + relief.removedStrain();
		expectDouble(retained, accounted,
				"retained damage equals immediate plus settlement plus named relief");

		StrainReliefResult repeated = TankerSkillManager.calculateShieldBashRelief(
				secondHit.resultingStrain(), 100.0D, relief.reliefUsed());
		expectDouble(0.0D, repeated.removedStrain(), "a second Bash cannot forgive Strain");
		expectDouble(secondHit.resultingStrain(), repeated.remainingStrain(),
				"a second Bash leaves debt unchanged");

		StrainReliefResult lowDebt = TankerSkillManager.calculateShieldBashRelief(
				3.0D, 100.0D, false);
		expectDouble(3.0D, lowDebt.removedStrain(), "relief cannot exceed current Strain");
		expectTrue(lowDebt.reliefUsed(), "partial use still consumes the one activation budget");
		StrainReliefResult noBanking = TankerSkillManager.calculateShieldBashRelief(
				10.0D, 100.0D, lowDebt.reliefUsed());
		expectDouble(0.0D, noBanking.removedStrain(), "unused relief is not banked");

		StrainReliefResult zeroDebt = TankerSkillManager.calculateShieldBashRelief(
				0.0D, 100.0D, false);
		expectFalse(zeroDebt.reliefUsed(), "a Bash at zero Strain does not consume relief");

		double[] pulses = TankerSkillManager.settlementPulses(10.1D);
		expectInt(4, pulses.length, "settlement pulse count");
		expectDouble(10.1D, sum(pulses), "the fourth pulse carries the floating-point remainder");
	}

	private static void willpowerRatchetsCapsWithoutErasingDebt() {
		WillpowerHitResult loweredBelowDebt = TankerSkillManager.calculateWillpowerHit(
				12.0D, 30.0D, 40.0D, 100.0D, CombatCategory.PVP);
		expectDouble(25.0D, loweredBelowDebt.activeCap(), "PvP ratchets cap to 25%");
		expectDouble(12.0D, loweredBelowDebt.immediateDamage(),
				"a hit is not delayed when existing debt already meets the lower cap");
		expectDouble(0.0D, loweredBelowDebt.strainAdded(), "no new debt above a lowered cap");
		expectDouble(30.0D, loweredBelowDebt.resultingStrain(), "existing debt is never erased");
		expectTrue(loweredBelowDebt.endsWillpower(), "lowered cap ends the active window");

		WillpowerHitResult partialCapacity = TankerSkillManager.calculateWillpowerHit(
				20.0D, 20.0D, 40.0D, 100.0D, CombatCategory.PVP);
		expectDouble(15.0D, partialCapacity.immediateDamage(),
				"only five damage can be delayed into the remaining PvP capacity");
		expectDouble(5.0D, partialCapacity.strainAdded(), "partial delay fills the cap");
		expectDouble(25.0D, partialCapacity.resultingStrain(), "Strain ends exactly at cap");
		expectTrue(partialCapacity.endsWillpower(), "filling the cap ends Willpower");

		WillpowerHitResult cannotRatchetUp = TankerSkillManager.calculateWillpowerHit(
				2.0D, 5.0D, 25.0D, 100.0D, CombatCategory.NORMAL);
		expectDouble(25.0D, cannotRatchetUp.activeCap(), "later normal damage cannot raise a ratcheted cap");
	}

	private static void protectionMarkFundsOnlyActualPrevention() {
		expectDouble(150.0D, TankerSkillManager.protectionMarkInitialIntegrity(200.0D),
				"Protection Mark starts with 75% owner max health");
		expectDouble(1.0D, TankerSkillManager.protectionMarkDrainMultiplier(CombatCategory.NORMAL),
				"normal PvE integrity drain");
		expectDouble(1.5D, TankerSkillManager.protectionMarkDrainMultiplier(CombatCategory.BOSS),
				"boss integrity drain");
		expectDouble(1.0D, TankerSkillManager.protectionMarkDrainMultiplier(CombatCategory.PVP),
				"PvP integrity drain");

		expectDouble(12.0D, TankerSkillManager.protectionMarkRequestedPrevention(
				100.0D, CombatCategory.NORMAL, 0, 0.12D, ReinforcementPhase.NONE),
				"an uncapped owner Mark requests only the damage it prevents");
		expectDouble(0.0D, TankerSkillManager.protectionMarkRequestedPrevention(
				100.0D, CombatCategory.NORMAL, 10, 0.18D, ReinforcementPhase.PERFECT),
				"a Mark requests no integrity when the category cap already fixes retained damage");

		ProtectionFundingResult normal = TankerSkillManager.calculateProtectionFunding(
				10.0D, 75.0D, CombatCategory.NORMAL);
		expectFunding(normal, 10.0D, 10.0D, 65.0D, false, "fully funded normal hit");

		ProtectionFundingResult boss = TankerSkillManager.calculateProtectionFunding(
				20.0D, 40.0D, CombatCategory.BOSS);
		expectFunding(boss, 20.0D, 30.0D, 10.0D, false, "fully funded boss hit");

		ProtectionFundingResult partialBoss = TankerSkillManager.calculateProtectionFunding(
				10.0D, 9.0D, CombatCategory.BOSS);
		expectFunding(partialBoss, 6.0D, 9.0D, 0.0D, true, "partially funded final boss hit");

		ProtectionFundingResult partialPvp = TankerSkillManager.calculateProtectionFunding(
				10.0D, 4.0D, CombatCategory.PVP);
		expectFunding(partialPvp, 4.0D, 4.0D, 0.0D, true, "partially funded final PvP hit");
	}

	private static void expectMarkTable(CombatCategory category, double owner, double ally) {
		expectDouble(owner, TankerSkillManager.markReduction(true, category),
				category + " owner Protection Mark");
		expectDouble(ally, TankerSkillManager.markReduction(false, category),
				category + " ally Protection Mark");
	}

	private static void expectReinforcementTable(CombatCategory category,
			double perfectReduction, double stanceReduction,
			double perfectKnockback, double stanceKnockback) {
		expectDouble(0.0D,
				TankerSkillManager.reinforcementReduction(ReinforcementPhase.NONE, category),
				category + " no-phase reduction");
		expectDouble(perfectReduction,
				TankerSkillManager.reinforcementReduction(ReinforcementPhase.PERFECT, category),
				category + " perfect-brace reduction");
		expectDouble(stanceReduction,
				TankerSkillManager.reinforcementReduction(ReinforcementPhase.STANCE, category),
				category + " reinforced-stance reduction");
		expectDouble(1.0D,
				TankerSkillManager.knockbackMultiplier(ReinforcementPhase.NONE, category),
				category + " no-phase knockback");
		expectDouble(perfectKnockback,
				TankerSkillManager.knockbackMultiplier(ReinforcementPhase.PERFECT, category),
				category + " perfect-brace knockback");
		expectDouble(stanceKnockback,
				TankerSkillManager.knockbackMultiplier(ReinforcementPhase.STANCE, category),
				category + " reinforced-stance knockback");
	}

	private static void expectFunding(ProtectionFundingResult actual,
			double prevented, double spent, double remaining, boolean breaks, String context) {
		expectDouble(prevented, actual.preventedDamage(), context + " prevention");
		expectDouble(spent, actual.integritySpent(), context + " integrity spent");
		expectDouble(remaining, actual.remainingIntegrity(), context + " integrity remaining");
		if (breaks)
			expectTrue(actual.breaksField(), context + " breaks field");
		else
			expectFalse(actual.breaksField(), context + " keeps field");
	}

	private static double sum(double[] values) {
		double result = 0.0D;
		for (double value : values) {
			if (value < -EPSILON)
				throw new AssertionError("settlement pulses must not be negative: " + value);
			result += value;
		}
		return result;
	}

	private static void expectInt(int expected, int actual, String context) {
		if (expected != actual)
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}

	private static void expectDouble(double expected, double actual, String context) {
		if (!Double.isFinite(actual) || Math.abs(expected - actual) > EPSILON)
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}

	private static void expectTrue(boolean condition, String context) {
		if (!condition)
			throw new AssertionError(context);
	}

	private static void expectFalse(boolean condition, String context) {
		expectTrue(!condition, context);
	}
}
