package net.solocraft.client.gui;

public final class UrgentQuestClientState {
	private static boolean active;
	private static String title = "";
	private static String objective = "";
	private static String kind = "";
	private static int progress;
	private static int target;
	private static int remainingSeconds = -1;
	private static long receivedAtMillis;

	private UrgentQuestClientState() {
	}

	public static void update(boolean isActive, String questTitle, String questObjective, String questKind, int questProgress, int questTarget, int secondsRemaining) {
		active = isActive;
		title = questTitle == null ? "" : questTitle;
		objective = questObjective == null ? "" : questObjective;
		kind = questKind == null ? "" : questKind;
		progress = Math.max(0, questProgress);
		target = Math.max(0, questTarget);
		remainingSeconds = secondsRemaining;
		receivedAtMillis = System.currentTimeMillis();
	}

	public static boolean isActive() {
		return active;
	}

	public static String title() {
		return title;
	}

	public static String objective() {
		return objective;
	}

	public static String kind() {
		return kind;
	}

	public static int progress() {
		return progress;
	}

	public static int target() {
		return target;
	}

	public static int remainingSeconds() {
		if (!active || remainingSeconds < 0)
			return remainingSeconds;
		long elapsed = Math.max(0L, (System.currentTimeMillis() - receivedAtMillis) / 1000L);
		return Math.max(0, remainingSeconds - (int) elapsed);
	}
}
