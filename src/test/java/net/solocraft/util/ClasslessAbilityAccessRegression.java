package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Source contracts for classless learned-ability access. */
public final class ClasslessAbilityAccessRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net",
			"solocraft");

	private ClasslessAbilityAccessRegression() {
	}

	public static void main(String[] args) throws IOException {
		rangerRunestonesAreClassless();
		tankerSkillsUseLearnedOwnershipRatherThanClass();
		vesselSkillsAreReconciledBeforeCasting();
		shadowRunestonesRequireShadowMonarch();
		mageRunestonesRemainUniversal();
		vesselAbilitiesRequireCurrentVessel();
	}

	private static void rangerRunestonesAreClassless() throws IOException {
		String source = read("util", "RangerCombatManager.java");
		String learner = section(source, "public static void learnFromRunestone",
				"public static void learnManaQuiverFromRunestone");
		expectFalse(learner.contains("isRanger(player)") || learner.contains("ranger_only"),
				"Ranger runestones must not reject another class");
		expectTrue(learner.contains("grantSkill(player, skill)"),
				"Ranger runestones must still use token-safe learned ownership");
	}

	private static void tankerSkillsUseLearnedOwnershipRatherThanClass()
			throws IOException {
		String source = read("util", "TankerSkillManager.java");
		String activate = section(source, "public static boolean activateSkill",
				"public static void reconcileTanker");
		expectFalse(activate.contains("Only Tankers can use this skill")
						|| activate.contains("!isTanker(player)"),
				"Tanker casts must require the learned skill rather than class four");
		expectTrue(source.contains("for (String skill : SKILLS)")
						&& source.contains("TankerProgressionRules.hasSkill(vars.Plist, skill)"),
				"Tanker runtime must stay enabled for a player with learned Tanker skills");
	}

	private static void vesselSkillsAreReconciledBeforeCasting()
			throws IOException {
		String source = read("util", "JobSkillManager.java");
		String cast = section(source, "public static boolean cast",
				"public static boolean release");
		expectTrue(cast.contains("parseSkillList(vars.Plist).contains(skill)"),
				"Vessel skill casting must use the reconciled skill list");
		expectTrue(cast.contains("syncJobSkills(entity);"),
				"Vessel skills must be reconciled against the current vessel before casting");
		expectTrue(source.contains("Vessel abilities belong to the current vessel")
						&& source.contains("if (ALL_JOB_SKILLS.contains(cleaned))\n\t\t\treturn !granted.contains(cleaned);"),
				"Changing vessels must remove unavailable vessel skills and equipped slots");
	}

	private static void shadowRunestonesRequireShadowMonarch() throws IOException {
		String exchange = read("procedures", "RunestoneShadowExchangeRightclickedProcedure.java");
		String manifestation = read("procedures", "RunestoneShadowBodyManifestationProcedure.java");
		expectTrue(exchange.contains("if (!VesselProgressionManager.isShadowMonarch(entity))")
						&& manifestation.contains("if (!VesselProgressionManager.isShadowMonarch(entity))"),
				"Shadow runestones must require the Shadow Monarch vessel");
	}

	private static void mageRunestonesRemainUniversal() throws IOException {
		String source = read("util", "MageSpellProgression.java");
		expectTrue(source.contains("Runestones are universal unlock items"),
				"Mage runestones must retain their existing classless ownership marker");
	}

	private static void vesselAbilitiesRequireCurrentVessel()
			throws IOException {
		for (String manager : new String[] {
				"GoliathCombatManager.java", "LiuZhigangCombatManager.java",
				"BeastMonarchManager.java", "AntaresCombatManager.java",
				"FrostMonarchManager.java", "WhiteFlameMonarchManager.java" }) {
			expectFalse(read("util", manager).contains("JobSkillManager.hasLearnedSkill"),
					manager + " must not treat a learned skill as vessel ownership");
		}
		String sung = read("util", "SungIlHwanCombatManager.java");
		expectTrue(!sung.contains("JobSkillManager.hasLearnedSkill")
						&& sung.contains("DeveloperModeManager.isEnabled(entity)")
						&& sung.contains("(int) vars.JOB == 7"),
				"Sung abilities must require the developer-gated Sung vessel");
		String progression = read("util", "VesselProgressionManager.java");
		expectFalse(progression.contains("JobSkillManager.hasLearnedSkill"),
				"Shadow Monarch ownership must come from the current vessel only");
		String skills = read("util", "JobSkillManager.java");
		expectTrue(skills.contains("return !granted.contains(cleaned);"),
				"A vessel change must remove unavailable vessel skills from the skill list");
	}

	/**
	 * Reads a source file with line endings normalized to {@code \n}.
	 *
	 * <p>The repository has mixed CRLF and LF files, and an editor that
	 * normalizes one of them would otherwise fail assertions that embed a
	 * literal newline — without the asserted code having changed at all.</p>
	 */
	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static String section(String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: " + startToken);
		return source.substring(start, end);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
