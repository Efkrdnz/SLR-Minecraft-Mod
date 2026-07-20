package net.solocraft.util;

public final class AwakeningStatCurves {
	private static final double[] MAGE_INTELLIGENCE = {0, 3, 13, 30, 53, 83, 120};
	private static final double[] HEALER_INTELLIGENCE = {0, 3, 11, 25, 44, 69, 100};
	private static final double[] RANGER_INTELLIGENCE = {0, 2, 9, 20, 36, 56, 80};

	private AwakeningStatCurves() {
	}

	public static double intelligenceBonus(int classId, double hunterRank) {
		int rank = Math.max(1, Math.min(6, (int) Math.round(hunterRank)));
		return switch (classId) {
			case 2 -> MAGE_INTELLIGENCE[rank];
			case 5 -> HEALER_INTELLIGENCE[rank];
			case 6 -> RANGER_INTELLIGENCE[rank];
			default -> 0;
		};
	}
}
