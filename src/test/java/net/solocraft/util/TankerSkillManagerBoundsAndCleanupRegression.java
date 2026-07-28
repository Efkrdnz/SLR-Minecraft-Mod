package net.solocraft.util;

import net.solocraft.util.TankerSkillManager.CleanupAction;
import net.solocraft.util.TankerSkillManager.CleanupReason;
import net.solocraft.util.TankerSkillManager.TargetOrder;
import net.solocraft.util.TankerSkillManager.TransientState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Dependency-free checks for hard state bounds, deterministic target ordering,
 * and cleanup decisions that must not depend on wall-clock time or map iteration.
 */
public final class TankerSkillManagerBoundsAndCleanupRegression {
	private TankerSkillManagerBoundsAndCleanupRegression() {
	}

	public static void main(String[] args) {
		hardBoundsMatchTheServerContract();
		targetOrderingIsBoundedAndDeterministic();
		ordinaryTransientStateAlwaysClears();
		unpaidWillpowerDebtUsesExplicitCleanupActions();
	}

	private static void hardBoundsMatchTheServerContract() {
		expectInt(16, TankerSkillManager.TAUNT_TARGET_CAP, "Taunt stored target limit");
		expectInt(16, TankerSkillManager.LEAP_TARGET_CAP, "Tank Leap hit-ledger limit");
		expectInt(1, TankerSkillManager.SHIELD_BASH_TARGET_CAP, "Shield Bash target limit");
		expectInt(8, TankerSkillManager.MARK_BENEFICIARY_CAP,
				"Protection Mark owner-plus-seven limit");
		expectInt(1, TankerSkillManager.MAX_PROTECTION_MARKS_PER_OWNER,
				"Protection Mark owner-state limit");
		expectInt(1, TankerSkillManager.MAX_REINFORCEMENT_PHASES,
				"Reinforcement phase-state limit");
		expectInt(1, TankerSkillManager.MAX_WILLPOWER_STATES,
				"Willpower state limit");

		expectInt(12, TankerSkillManager.LEAP_DEADLINE, "Tank Leap deadline");
		expectInt(120, TankerSkillManager.TAUNT_MOB_DURATION, "Taunt duration");
		expectInt(10, TankerSkillManager.TAUNT_MAINTENANCE_INTERVAL, "Taunt maintenance cadence");
		expectInt(160, TankerSkillManager.WILLPOWER_DURATION, "Willpower duration");
		expectInt(240, TankerSkillManager.MARK_DURATION, "Protection Mark duration");
		expectInt(10, TankerSkillManager.MARK_MEMBERSHIP_INTERVAL,
				"Protection Mark membership cadence");
		expectInt(4, TankerSkillManager.WILLPOWER_PULSES,
				"Willpower settlement pulse count");
		expectInt(10, TankerSkillManager.WILLPOWER_PULSE_INTERVAL,
				"Willpower settlement cadence");
	}

	private static void targetOrderingIsBoundedAndDeterministic() {
		UUID first = uuid(1);
		UUID second = uuid(2);
		UUID third = uuid(3);
		UUID fourth = uuid(4);
		List<TargetOrder> candidates = List.of(
				new TargetOrder(fourth, 2.0D, 1.0D),
				new TargetOrder(third, 1.0D, 3.0D),
				new TargetOrder(second, 1.0D, 2.0D),
				new TargetOrder(first, 1.0D, 2.0D));

		List<UUID> expected = List.of(first, second, third);
		expectList(expected, TankerSkillManager.boundedTargetIds(candidates, 3),
				"targets sort by primary order, squared distance, then UUID");

		ArrayList<TargetOrder> reversed = new ArrayList<>(candidates);
		Collections.reverse(reversed);
		expectList(expected, TankerSkillManager.boundedTargetIds(reversed, 3),
				"input or map iteration order cannot affect selection");
		expectList(List.of(), TankerSkillManager.boundedTargetIds(candidates, 0),
				"a zero bound selects no targets");

		ArrayList<TargetOrder> twenty = new ArrayList<>();
		for (int index = 20; index >= 1; index--)
			twenty.add(new TargetOrder(uuid(index), index, index));
		List<UUID> selected = TankerSkillManager.boundedTargetIds(
				twenty, TankerSkillManager.TAUNT_TARGET_CAP);
		expectInt(16, selected.size(), "a twenty-target query stores at most sixteen");
		expectEquals(uuid(1), selected.get(0), "nearest target survives truncation");
		expectEquals(uuid(16), selected.get(15), "sixteenth target is the deterministic cutoff");
	}

	private static void ordinaryTransientStateAlwaysClears() {
		for (TransientState state : TransientState.values()) {
			if (state == TransientState.WILLPOWER)
				continue;
			for (CleanupReason reason : CleanupReason.values()) {
				expectEquals(CleanupAction.CLEAR,
						TankerSkillManager.cleanupAction(state, reason, 25.0D),
						state + " must clear on " + reason);
			}
		}
	}

	private static void unpaidWillpowerDebtUsesExplicitCleanupActions() {
		for (CleanupReason reason : CleanupReason.values()) {
			expectEquals(CleanupAction.CLEAR,
					TankerSkillManager.cleanupAction(TransientState.WILLPOWER, reason, 0.0D),
					"Willpower without debt clears on " + reason);
		}

		expectEquals(CleanupAction.CLEAR,
				TankerSkillManager.cleanupAction(
						TransientState.WILLPOWER, CleanupReason.DEATH, 25.0D),
				"death clears already-paid/dead-player Strain state");
		expectEquals(CleanupAction.START_STRAIN_SETTLEMENT,
				TankerSkillManager.cleanupAction(
						TransientState.WILLPOWER, CleanupReason.DIMENSION_CHANGE, 25.0D),
				"dimension change begins settlement");
		expectEquals(CleanupAction.START_STRAIN_SETTLEMENT,
				TankerSkillManager.cleanupAction(
						TransientState.WILLPOWER, CleanupReason.CLASS_CHANGE, 25.0D),
				"class change begins settlement");
		expectEquals(CleanupAction.PERSIST_STRAIN_SETTLEMENT,
				TankerSkillManager.cleanupAction(
						TransientState.WILLPOWER, CleanupReason.LOGOUT, 25.0D),
				"logout persists exact debt for login settlement");
		expectEquals(CleanupAction.PERSIST_STRAIN_SETTLEMENT,
				TankerSkillManager.cleanupAction(
						TransientState.WILLPOWER, CleanupReason.SERVER_STOP, 25.0D),
				"server stop persists exact debt before in-memory maps clear");
	}

	private static UUID uuid(long value) {
		return new UUID(0L, value);
	}

	private static void expectList(List<UUID> expected, List<UUID> actual, String context) {
		if (!expected.equals(actual))
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}

	private static void expectEquals(Object expected, Object actual, String context) {
		if (expected == null ? actual != null : !expected.equals(actual))
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}

	private static void expectInt(int expected, int actual, String context) {
		if (expected != actual)
			throw new AssertionError(context + ": expected " + expected + " but got " + actual);
	}
}
