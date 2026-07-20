package net.solocraft.util;

public final class BeastHuntClientState {
	private static volatile boolean active;
	private static volatile String quarryName = "";
	private static volatile int hunt;
	private static volatile int openings;
	private static volatile int combo;
	private static volatile boolean stance;

	private BeastHuntClientState() {
	}

	public static void update(boolean isActive, String name, int huntValue, int openingCount,
			int comboStep, boolean inStance) {
		active = isActive;
		quarryName = name == null ? "" : name;
		hunt = Math.max(0, Math.min(100, huntValue));
		openings = Math.max(0, Math.min(2, openingCount));
		combo = Math.max(0, Math.min(4, comboStep));
		stance = inStance;
	}

	public static boolean isActive() {
		return active;
	}

	public static String quarryName() {
		return quarryName;
	}

	public static int hunt() {
		return hunt;
	}

	public static int openings() {
		return openings;
	}

	public static int combo() {
		return combo;
	}

	public static boolean isStance() {
		return stance;
	}

	public static void clear() {
		update(false, "", 0, 0, 0, false);
	}
}
