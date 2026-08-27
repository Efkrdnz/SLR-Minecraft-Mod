package net.solocraft.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards the ambient cold of Sillad's domain.
 *
 * <p>The fight already froze the terrain; what it never did was make the player
 * cold unless the boss landed a hit. These checks are about the pressure curve
 * and its counterplay -- that standing in the domain is genuinely dangerous,
 * that closing on the Monarch is worse, and that warmth is a real answer rather
 * than a decorative one.
 */
public final class FrostChillRegression {
	private static final Path CHILL_MANAGER = Path.of("src", "main", "java", "net",
			"solocraft", "util", "SilladChillManager.java");
	private static final Path OVERLAY = Path.of("src", "main", "java", "net",
			"solocraft", "client", "screens", "FrozenOverlayOverlay.java");

	private FrostChillRegression() {
	}

	public static void main(String[] args) throws Exception {
		standingStillInTheDomainFreezesYou();
		closingOnTheMonarchIsColder();
		warmthIsRealCounterplay();
		leavingThawsYou();
		stagesAndAmplifiersRoundTrip();
		maximumChillIsNotAPermanentLock();
		freezingSolidIsNotAStunlock();
		coldIsClearedWhenTheEncounterEnds();
		overlayScalesInsteadOfBlinking();
		System.out.println("FrostChillRegression passed");
	}

	/** The core promise: doing nothing in the arena kills you. */
	private static void standingStillInTheDomainFreezesYou() {
		double chill = 0.0D;
		double rate = FrostChillRules.deltaPerSecond(1, 20.0D, true, false, 0, false);
		if (rate <= 0.0D)
			throw new AssertionError("phase one at range does not chill at all");
		for (int second = 0; second < 60 && chill < FrostChillRules.MAX_CHILL; second++)
			chill = FrostChillRules.advance(chill, rate, 20);
		if (FrostChillRules.stageFor(chill).ordinal()
				< FrostChillRules.Stage.NUMB.ordinal())
			throw new AssertionError("a minute of standing in the domain only "
					+ "reached " + FrostChillRules.stageFor(chill));
	}

	private static void closingOnTheMonarchIsColder() {
		double near = FrostChillRules.deltaPerSecond(2, 3.0D, true, false, 0, false);
		double mid = FrostChillRules.deltaPerSecond(2, 15.0D, true, false, 0, false);
		double far = FrostChillRules.deltaPerSecond(2, 30.0D, true, false, 0, false);
		if (!(near > mid && mid > far))
			throw new AssertionError("proximity does not make the cold worse: "
					+ near + " / " + mid + " / " + far);
		// Later phases must always be worse than earlier ones at the same spot.
		for (int phase = 2; phase <= 3; phase++) {
			if (FrostChillRules.baseRateForPhase(phase)
					<= FrostChillRules.baseRateForPhase(phase - 1))
				throw new AssertionError("phase " + phase + " is not colder than "
						+ (phase - 1));
		}
	}

	/**
	 * Warmth has to be able to actually win, or it is decoration. A player at the
	 * fringe with a fire going should be losing chill, not merely gaining it more
	 * slowly.
	 */
	private static void warmthIsRealCounterplay() {
		double unaided = FrostChillRules.deltaPerSecond(1, 20.0D, true, false, 0, false);
		double warmed = FrostChillRules.deltaPerSecond(1, 20.0D, true, false, 2, false);
		if (warmed >= unaided)
			throw new AssertionError("warmth did not reduce the rate");
		if (warmed >= 0.0D)
			throw new AssertionError("two heat sources at the fringe still cannot "
					+ "hold the cold back; warmth is decorative");
		// It must not trivialise standing on top of him in phase three.
		double atTheCore = FrostChillRules.deltaPerSecond(3, 0.0D, true, true, 3, true);
		if (atTheCore < 0.0D)
			throw new AssertionError("a bonfire makes the Monarch's core safe");
	}

	private static void leavingThawsYou() {
		double outside = FrostChillRules.deltaPerSecond(3, 40.0D, false, false, 0, false);
		if (outside >= 0.0D)
			throw new AssertionError("leaving the domain does not thaw");
		double chill = FrostChillRules.MAX_CHILL;
		for (int second = 0; second < 60 && chill > 0.0D; second++)
			chill = FrostChillRules.advance(chill, outside, 20);
		if (chill > 0.0D)
			throw new AssertionError("a full minute outside still left chill at "
					+ chill);
	}

	private static void stagesAndAmplifiersRoundTrip() {
		for (FrostChillRules.Stage stage : FrostChillRules.Stage.values()) {
			int amplifier = FrostChillRules.amplifierFor(stage);
			if (stage == FrostChillRules.Stage.CLEAR) {
				if (amplifier >= 0)
					throw new AssertionError("CLEAR must not carry an effect");
				continue;
			}
			if (FrostChillRules.stageForAmplifier(amplifier) != stage)
				throw new AssertionError("amplifier round trip lost " + stage);
		}
		if (FrostChillRules.stageFor(-50.0D) != FrostChillRules.Stage.CLEAR
				|| FrostChillRules.stageFor(Double.NaN) != FrostChillRules.Stage.CLEAR)
			throw new AssertionError("garbage chill values must read as CLEAR");
		if (FrostChillRules.stageFor(FrostChillRules.MAX_CHILL)
				!= FrostChillRules.Stage.GLACIAL)
			throw new AssertionError("a full meter is not GLACIAL");
		double previous = -1.0D;
		for (double chill = 0.0D; chill <= FrostChillRules.MAX_CHILL; chill += 1.0D) {
			double intensity = FrostChillRules.intensity(chill);
			if (intensity < previous || intensity < 0.0D || intensity > 1.0D)
				throw new AssertionError("intensity is not a rising 0..1 ramp");
			previous = intensity;
		}
	}

	/** Freezing solid is a crisis to escape, not a state you sit in forever. */
	private static void maximumChillIsNotAPermanentLock() {
		if (FrostChillRules.BREAK_RESET_TO >= FrostChillRules.MAX_CHILL)
			throw new AssertionError("breaking at maximum chill leaves the player "
					+ "at maximum chill, so they refreeze immediately");
		if (FrostChillRules.stageFor(FrostChillRules.BREAK_RESET_TO)
				== FrostChillRules.Stage.GLACIAL)
			throw new AssertionError("the post-break reset is still GLACIAL");
	}

	/**
	 * Resetting to Frostbound is not enough on its own.
	 *
	 * <p>At the phase-three core the rate is 13.5/s, so a reset to 75 refills to
	 * 100 in under two seconds -- a melee player would freeze solid roughly every
	 * two seconds for the whole fight. The grace window is what turns that back
	 * into a hazard instead of a stunlock.
	 */
	private static void freezingSolidIsNotAStunlock() throws Exception {
		double worstRate = FrostChillRules.deltaPerSecond(3, 0.0D, true, true, 0, false);
		double secondsToRefreeze =
				(FrostChillRules.MAX_CHILL - FrostChillRules.BREAK_RESET_TO) / worstRate;
		if (secondsToRefreeze > 4.0D)
			throw new AssertionError("the refreeze is no longer fast enough for this "
					+ "guard to be meaningful (" + secondsToRefreeze + "s)");
		if (FrostChillRules.GLACIAL_GRACE_TICKS / 20.0D <= secondsToRefreeze)
			throw new AssertionError("the grace window (" + FrostChillRules.GLACIAL_GRACE_TICKS
					+ "t) is shorter than the " + secondsToRefreeze + "s refreeze, so "
					+ "the player is still stunlocked at the Monarch's core");
		String manager = Files.readString(CHILL_MANAGER);
		if (!manager.contains("GLACIAL_GRACE_TICKS"))
			throw new AssertionError("the manager never applies the grace window");
		// The grace must still thaw, or it is a free pass to stand in the domain.
		if (!manager.contains("-FrostChillRules.THAW_PER_SECOND"))
			throw new AssertionError("chill does not thaw during the grace window");
	}

	private static void coldIsClearedWhenTheEncounterEnds() throws Exception {
		String source = Files.readString(CHILL_MANAGER);
		if (!source.contains("public static void clear("))
			throw new AssertionError("no way to clear the cold exists");
		String combat = Files.readString(Path.of("src", "main", "java", "net",
				"solocraft", "util", "SilladBossCombatManager.java"));
		if (!combat.contains("SilladChillManager.clear("))
			throw new AssertionError("the encounter teardown never clears chill, so "
					+ "the Monarch's winter follows the party out of the arena");
		if (!combat.contains("SilladChillManager.tick("))
			throw new AssertionError("the chill manager is never ticked");
	}

	/**
	 * The overlay must fade with the cold. Its old all-or-nothing behaviour is
	 * exactly why ambient chill would be invisible until it had already won.
	 */
	private static void overlayScalesInsteadOfBlinking() throws Exception {
		String source = Files.readString(OVERLAY);
		if (!source.contains("opacity"))
			throw new AssertionError("the frost overlay is not opacity-driven");
		if (source.contains("setShaderColor(1, 1, 1, 1)")
				&& !source.contains("setShaderColor(1, 1, 1, opacity)"))
			throw new AssertionError("the frost pane still draws at fixed full alpha");
	}
}
