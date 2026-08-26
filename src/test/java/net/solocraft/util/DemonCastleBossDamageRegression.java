package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Regression checks for Vulcan and Baran's bounded outgoing damage. */
public final class DemonCastleBossDamageRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net",
			"solocraft");

	private DemonCastleBossDamageRegression() {
	}

	public static void main(String[] args) throws IOException {
		vulcanDamageIsNerfedButBossLike();
		baranAttackSequencesStayBounded();
		allBossDamagePathsUseTheSharedRules();
	}

	private static void vulcanDamageIsNerfedButBossLike() {
		expectNear(32.0D, DemonCastleBossDamageRules.VULCAN_MELEE,
				"Vulcan melee damage");
		expectNear(34.0D, DemonCastleBossDamageRules.VULCAN_HAMMER_SLAM,
				"Vulcan hammer slam damage");
		expectNear(22.0D, DemonCastleBossDamageRules.VULCAN_CHARGE,
				"Vulcan charge damage");
		expectNear(25.0D, DemonCastleBossDamageRules.VULCAN_MOLTEN_BURST,
				"Vulcan molten burst damage");
		expectTrue(DemonCastleBossDamageRules.VULCAN_HAMMER_SLAM
				> DemonCastleBossDamageRules.VULCAN_MELEE,
				"The telegraphed hammer slam should remain Vulcan's strongest hit");
	}

	private static void baranAttackSequencesStayBounded() {
		expectNear(16.0D, DemonCastleBossDamageRules.BARAN_MELEE,
				"Baran melee damage");
		double normalSlam = DemonCastleBossDamageRules.baranGroundSlam(false)
				+ DemonCastleBossDamageRules.baranGroundRipple(false);
		double phaseTwoSlam = DemonCastleBossDamageRules.baranGroundSlam(true)
				+ DemonCastleBossDamageRules.baranGroundRipple(true)
				+ DemonCastleBossDamageRules.BARAN_GROUND_THIRD_WAVE;
		double normalCharge = DemonCastleBossDamageRules.baranChargeCollision(false)
				+ DemonCastleBossDamageRules.baranChargeImpact(false);
		double phaseTwoCharge = DemonCastleBossDamageRules.baranChargeCollision(true)
				+ DemonCastleBossDamageRules.baranChargeImpact(true);
		double normalMagic = DemonCastleBossDamageRules.baranMagicBlast(false)
				+ DemonCastleBossDamageRules.BARAN_MAGIC_SHOCKWAVE;
		double phaseTwoMagic = DemonCastleBossDamageRules.BARAN_MAGIC_PRE_SHOT
				+ DemonCastleBossDamageRules.baranMagicBlast(true)
				+ DemonCastleBossDamageRules.BARAN_MAGIC_SHOCKWAVE;

		expectNear(25.0D, normalSlam, "Baran normal slam sequence");
		expectNear(38.0D, phaseTwoSlam, "Baran phase-two slam sequence");
		expectNear(27.0D, normalCharge, "Baran normal charge sequence");
		expectNear(37.0D, phaseTwoCharge, "Baran phase-two charge sequence");
		expectNear(26.0D, normalMagic, "Baran normal magic sequence");
		expectNear(40.0D, phaseTwoMagic, "Baran phase-two magic sequence");
		expectTrue(Math.max(phaseTwoSlam,
				Math.max(phaseTwoCharge, phaseTwoMagic)) <= 40.0D,
				"A fully connected phase-two sequence must stay at or below 40 raw damage");
	}

	private static void allBossDamagePathsUseTheSharedRules()
			throws IOException {
		String vulcan = read("entity", "VulcanEntity.java");
		String baran = read("entity", "BaranEntity.java");
		String charge = read("procedures", "BaranChargeProcedure.java");
		String slam = read("procedures", "BaranGroundSlamProcedure.java");
		String magic = read("procedures", "BaranMagicBlastProcedure.java");
		String lightning = read("procedures", "BaranLightningStormProcedure.java");

		assertContains(vulcan,
				"DemonCastleBossDamageRules.VULCAN_MELEE",
				"DemonCastleBossDamageRules.VULCAN_HAMMER_SLAM",
				"DemonCastleBossDamageRules.VULCAN_CHARGE",
				"DemonCastleBossDamageRules.VULCAN_MOLTEN_BURST");
		assertContains(baran, "DemonCastleBossDamageRules.BARAN_MELEE");
		assertContains(charge,
				"DemonCastleBossDamageRules.baranChargeCollision(phase2)",
				"DemonCastleBossDamageRules.baranChargeImpact(phase2)");
		assertContains(slam,
				"DemonCastleBossDamageRules.baranGroundSlam(phase2)",
				"DemonCastleBossDamageRules.baranGroundRipple(phase2)",
				"DemonCastleBossDamageRules.BARAN_GROUND_THIRD_WAVE");
		assertContains(magic,
				"DemonCastleBossDamageRules.BARAN_MAGIC_PRE_SHOT",
				"DemonCastleBossDamageRules.baranMagicBlast(phase2)",
				"DemonCastleBossDamageRules.BARAN_MAGIC_SHOCKWAVE");
		assertContains(lightning,
				"DemonCastleBossDamageRules.BARAN_LIGHTNING_DIRECT");
	}

	private static String read(String directory, String file) throws IOException {
		return Files.readString(MAIN.resolve(directory).resolve(file));
	}

	private static void assertContains(String source, String... snippets) {
		for (String snippet : snippets)
			expectTrue(source.contains(snippet), "Missing damage wiring: " + snippet);
	}

	private static void expectNear(double expected, double actual, String label) {
		if (Math.abs(expected - actual) > 0.0001D)
			throw new AssertionError(label + ": expected " + expected
					+ " but was " + actual);
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}
}
