package net.solocraft.util;

/**
 * The ambient cold of the Monarch of Frost's domain.
 *
 * <p>Sillad's arena froze the terrain but never the people standing on it: cold
 * only existed as an on-hit frostbite stack, so a player who dodged well fought
 * the Monarch of Frost without ever being cold. This is the pressure that fills
 * that gap -- standing in the domain at all is dangerous, and staying close to
 * him is worse.
 *
 * <p>The design goal is a hazard the player can <em>read and manage</em> rather
 * than a second health bar. Chill climbs faster near Sillad and on his ice, and
 * warmth pushes it back, so the arena becomes a space to negotiate instead of a
 * timer to outlast. Everything here is per-second so the caller can pick its own
 * tick interval without the balance shifting underneath it.
 */
public final class FrostChillRules {
	public static final double MAX_CHILL = 100.0D;

	/** Stage thresholds. Below the first, the player is merely cold. */
	public static final double CHILLED_AT = 25.0D;
	public static final double NUMB_AT = 50.0D;
	public static final double FROSTBOUND_AT = 75.0D;

	/** Base accumulation per second inside the domain, by boss phase. */
	private static final double[] PHASE_RATE = {0.0D, 2.5D, 4.0D, 6.0D};

	/** Chill lost per second outside the domain. */
	public static final double THAW_PER_SECOND = 5.0D;

	/** Distance at or inside which the Monarch's presence is at its coldest. */
	public static final double CORE_RADIUS = 6.0D;
	/** Distance beyond which proximity adds nothing. */
	public static final double FRINGE_RADIUS = 24.0D;
	private static final double CORE_MULTIPLIER = 2.0D;

	/** Extra per second while standing on the domain's own ice. */
	public static final double STANDING_ON_ICE = 1.5D;

	/**
	 * Per second removed by one nearby heat source, and by burning.
	 *
	 * <p>Deliberately bounded below the phase-three core rate (6.0 x 2.0 + 1.5 =
	 * 13.5). Even a player who is on fire and standing in a ring of campfires has
	 * to give ground to survive next to the Monarch -- warmth buys you the fringe
	 * of the arena, never his feet. Raising these past that budget makes a
	 * bonfire a hard counter to the whole encounter.
	 */
	public static final double WARMTH_PER_SOURCE = 2.5D;
	public static final double BURNING_WARMTH = 4.0D;
	public static final double WARMTH_RADIUS = 4.0D;

	/**
	 * Reaching the top does not lock the player there. The glacial break hands
	 * off to the existing frostbite freeze and drops the meter back to the start
	 * of Frostbound, so maximum chill is a recurring crisis to fight out of
	 * rather than a state you never escape.
	 */
	public static final double BREAK_RESET_TO = FROSTBOUND_AT;

	/**
	 * Ticks after a glacial break during which chill cannot climb.
	 *
	 * <p>Without this the meter simply refills: at the phase-three core the rate
	 * is 13.5/s, so resetting to 75 puts the player back at 100 in under two
	 * seconds and they are frozen solid every two seconds for the rest of the
	 * fight. That is not a hazard, it is a stunlock, and it would fall hardest on
	 * exactly the melee classes that have to stand there. Mirrors the frostbite
	 * system's own freeze immunity so the two cannot chain into each other.
	 */
	public static final int GLACIAL_GRACE_TICKS = 120;

	public enum Stage {
		CLEAR("Clear"),
		CHILLED("Chilled"),
		NUMB("Numb"),
		FROSTBOUND("Frostbound"),
		GLACIAL("Glacial");

		private final String displayName;

		Stage(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}
	}

	private FrostChillRules() {
	}

	public static double clamp(double chill) {
		if (!Double.isFinite(chill))
			return 0.0D;
		return Math.max(0.0D, Math.min(MAX_CHILL, chill));
	}

	public static Stage stageFor(double chill) {
		double value = clamp(chill);
		if (value >= MAX_CHILL)
			return Stage.GLACIAL;
		if (value >= FROSTBOUND_AT)
			return Stage.FROSTBOUND;
		if (value >= NUMB_AT)
			return Stage.NUMB;
		if (value >= CHILLED_AT)
			return Stage.CHILLED;
		return Stage.CLEAR;
	}

	/** Amplifier for the mirrored effect. CLEAR has no effect at all. */
	public static int amplifierFor(Stage stage) {
		return stage == null ? -1 : stage.ordinal() - 1;
	}

	public static Stage stageForAmplifier(int amplifier) {
		int ordinal = amplifier + 1;
		Stage[] stages = Stage.values();
		if (ordinal < 0)
			return Stage.CLEAR;
		return ordinal >= stages.length ? Stage.GLACIAL : stages[ordinal];
	}

	/** How close to freezing solid the player is, as 0..1. Drives the overlay. */
    public static double intensity(double chill) {
		return clamp(chill) / MAX_CHILL;
	}

	/**
	 * Proximity multiplier on the base rate: coldest within {@link #CORE_RADIUS},
	 * tapering to no bonus at {@link #FRINGE_RADIUS}.
	 */
	public static double proximityMultiplier(double distanceToMonarch) {
		if (!Double.isFinite(distanceToMonarch) || distanceToMonarch <= CORE_RADIUS)
			return CORE_MULTIPLIER;
		if (distanceToMonarch >= FRINGE_RADIUS)
			return 1.0D;
		double span = FRINGE_RADIUS - CORE_RADIUS;
		double fraction = (distanceToMonarch - CORE_RADIUS) / span;
		return CORE_MULTIPLIER - fraction * (CORE_MULTIPLIER - 1.0D);
	}

	public static double baseRateForPhase(int phase) {
		int index = Math.max(1, Math.min(PHASE_RATE.length - 1, phase));
		return PHASE_RATE[index];
	}

	/**
	 * Net chill change per second. Positive warms nothing -- a negative result
	 * means the player is winning against the cold.
	 */
	public static double deltaPerSecond(int phase, double distanceToMonarch,
			boolean insideDomain, boolean standingOnIce, int warmthSources,
			boolean burning) {
		double warmth = Math.max(0, warmthSources) * WARMTH_PER_SOURCE
				+ (burning ? BURNING_WARMTH : 0.0D);
		if (!insideDomain)
			return -THAW_PER_SECOND - warmth;
		double gain = baseRateForPhase(phase) * proximityMultiplier(distanceToMonarch)
				+ (standingOnIce ? STANDING_ON_ICE : 0.0D);
		return gain - warmth;
	}

	/** Applies a per-second delta over an arbitrary tick interval. */
	public static double advance(double chill, double deltaPerSecond, int ticks) {
		return clamp(chill + deltaPerSecond * (Math.max(0, ticks) / 20.0D));
	}
}
