package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-contract coverage for Sung Il-Hwan's ruler-vessel skill presentation.
 */
public final class SungIlHwanSkillUiRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path ICONS = Path.of(
			"src", "main", "resources", "assets", "sololeveling",
			"textures", "screens");

	private SungIlHwanSkillUiRegression() {
	}

	public static void main(String[] args) throws IOException {
		allRulerSkillsUseOneGoldPalette();
		equipmentOrderMatchesEstablishedRulerConvention();
		unlockChronologyRemainsExplicit();
		rulerIconsAreReused();
		centralUiPathsConsumeTheSharedPalette();
	}

	private static void allRulerSkillsUseOneGoldPalette() throws IOException {
		String manager = readMain("util", "JobSkillManager.java");
		expectTrue(manager.contains(
						"public static final int RULER_SKILL_COLOR = 0xFFD34E;"),
				"Ruler skills must expose one warm-gold UI palette");
		expectTrue(manager.contains(
						"|| isLiuSkill(skill) || isSungSkill(skill))")
						&& manager.contains("return RULER_SKILL_COLOR;"),
				"Thomas, Liu, and Sung skills must share the ruler gold");
		expectTrue(manager.contains(
						"if (isSungSkill(skill))\n\t\t\treturn sungTooltip(entity, skill);")
						&& manager.contains(
								"Component.literal(skill).withStyle(\n\t\t\t\tChatFormatting.GOLD, ChatFormatting.BOLD)")
						&& manager.contains(
								"description.copy().withStyle(ChatFormatting.GRAY)"),
				"Sung tooltips must use the same gold-title presentation as other ruler vessels");
	}

	private static void equipmentOrderMatchesEstablishedRulerConvention()
			throws IOException {
		String helper = readMain("util", "SkillListHelper.java");
		String order = section(helper,
				"private static final List<String> JOB_SKILL_ORDER",
				"private SkillListHelper()");

		assertOrdered(order,
				"JobSkillManager.THOMAS_CAPTURE",
				"JobSkillManager.THOMAS_POWER_SMASH",
				"JobSkillManager.THOMAS_COLLAPSE",
				"JobSkillManager.THOMAS_MANIFESTATION");
		assertOrdered(order,
				"JobSkillManager.LIU_HEAVENLY_COUNTER",
				"JobSkillManager.LIU_GOLDEN_DRAGON_DANCE",
				"JobSkillManager.LIU_SOVEREIGN_SWORD_DOMAIN",
				"JobSkillManager.LIU_MANIFESTATION");
		assertOrdered(order,
				"JobSkillManager.SUNG_PREDATORS_PRESENCE",
				"JobSkillManager.SUNG_ASSASSIN_STANCE",
				"JobSkillManager.SUNG_SPATIAL_EXECUTION",
				"JobSkillManager.SUNG_SPIRITUALIZATION");
	}

	private static void unlockChronologyRemainsExplicit() throws IOException {
		String progression = readMain("util",
				"VesselProgressionManager.java");
		String sung = section(progression, "case 7 -> {", "case 9 -> {");
		assertOrdered(sung,
				"skills.add(JobSkillManager.SUNG_SPIRITUALIZATION)",
				"if (level >= 55)",
				"skills.add(JobSkillManager.SUNG_PREDATORS_PRESENCE)",
				"if (level >= 70)",
				"skills.add(JobSkillManager.SUNG_ASSASSIN_STANCE)",
				"if (level >= 90)",
				"skills.add(JobSkillManager.SUNG_SPATIAL_EXECUTION)");
	}

	private static void rulerIconsAreReused() throws IOException {
		String overlay = readMain("client", "screens", "DisplayOverlay.java");
		assertContains(overlay,
				"case JobSkillManager.SUNG_PREDATORS_PRESENCE -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_goliath_1.png\")",
				"case JobSkillManager.SUNG_ASSASSIN_STANCE -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_goliath_2.png\")",
				"case JobSkillManager.SUNG_SPATIAL_EXECUTION -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_goliath_3.png\")",
				"case JobSkillManager.SUNG_SPIRITUALIZATION -> ResourceLocation.parse(\"sololeveling:textures/screens/icon_spiritualize_goliath.png\")");
		for (String icon : new String[] {
				"icon_goliath_1.png",
				"icon_goliath_2.png",
				"icon_goliath_3.png",
				"icon_spiritualize_goliath.png" })
			expectTrue(Files.isRegularFile(ICONS.resolve(icon)),
					"Reused ruler icon is missing: " + icon);
	}

	private static void centralUiPathsConsumeTheSharedPalette()
			throws IOException {
		String helper = readMain("util", "SkillListHelper.java");
		String shadow = readMain("util", "ShadowMonarchManager.java");
		String equipped = readMain("procedures",
				"ReturnAbilitySlotColorProcedure.java");
		String hotbar = readMain("procedures",
				"SkillTextColorProcedure.java");

		expectTrue(helper.contains(
						"return ShadowMonarchManager.skillColor(entity, raw);")
						&& shadow.contains(
								"return JobSkillManager.skillColor(skill);")
						&& equipped.contains(
								"return ShadowMonarchManager.skillColor(entity, value);")
						&& hotbar.contains(
								"ShadowMonarchManager.skillColor(entity, selectedPower)"),
				"Unlocked list, equipped list, and hotbar must all consume the central ruler palette");
	}

	/**
	 * Line endings are normalized because the repository mixes CRLF and LF, and
	 * these assertions embed literal newlines. An editor normalizing a file
	 * would otherwise fail them without the asserted code changing.
	 */
	private static String readMain(String... path) throws IOException {
		return Files.readString(MAIN.resolve(Path.of("", path))).replace("\r\n", "\n");
	}

	private static String section(String source, String start, String end) {
		int from = source.indexOf(start);
		expectTrue(from >= 0, "Missing section start: " + start);
		int to = source.indexOf(end, from + start.length());
		expectTrue(to >= 0, "Missing section end: " + end);
		return source.substring(from, to);
	}

	private static void assertOrdered(String source, String... needles) {
		int cursor = -1;
		for (String needle : needles) {
			int next = source.indexOf(needle, cursor + 1);
			expectTrue(next > cursor, "Missing or out-of-order token: " + needle);
			cursor = next;
		}
	}

	private static void assertContains(String source, String... needles) {
		for (String needle : needles)
			expectTrue(source.contains(needle), "Missing token: " + needle);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
