package net.solocraft.util;

/**
 * Single dependency-free entry point for all pure Tanker manager regressions.
 */
public final class TankerSkillManagerRegression {
	private TankerSkillManagerRegression() {
	}

	public static void main(String[] args) {
		TankerSkillManagerProgressionRegression.main(args);
		TankerSkillManagerCombatMathRegression.main(args);
		TankerSkillManagerBoundsAndCleanupRegression.main(args);
	}
}
