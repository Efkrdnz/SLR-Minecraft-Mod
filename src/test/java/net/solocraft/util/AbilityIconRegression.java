package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps the combat HUD's ability icons honest.
 *
 * <p>An unmapped ability silently falls through to the placeholder and a mistyped
 * path silently renders as the missing-texture checkerboard, so neither failure
 * announces itself in game. Both are cheap to detect from the sources.
 */
public final class AbilityIconRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");
	private static final Path SCREENS = Path.of("src", "main", "resources", "assets",
			"sololeveling", "textures", "screens");
	private static final Pattern CONSTANT = Pattern.compile(
			"public static final String\\s+([A-Z0-9_]+)\\s*=\\s*\"([^\"]+)\"");
	private static final Pattern CASE = Pattern.compile(
			"\\s*case (.+?) -> (.+);\\s*");
	private static final Pattern TEXTURE = Pattern.compile("screens/([a-z0-9_]+)\\.png");
	private static final Pattern DESCRIPTION = Pattern.compile("put\\(\"([^\"]+)\"");

	/**
	 * Abilities still awaiting art. Shrink this as icons land -- an entry that has
	 * gained an icon fails just as loudly as an ability that has lost one, so the
	 * list cannot quietly rot.
	 */
	/**
	 * Abilities with no HUD icon yet.
	 *
	 * <p>The vessel block joined this list when vessel abilities were finally
	 * given descriptions: they were always drawn on the skill bar without an
	 * icon, but until they were registered as described abilities this check
	 * could not see them. They are a real art gap, not a test artefact.
	 */
	private static final Set<String> AWAITING_ART = new TreeSet<>(List.of(
			"Colossus Charge",
			"Gigantification",
			"Heavy Blow",
			"Iron Body",
			"Mountain Breaker",
			"Seismic Grapple",
			"Shadow Feint",
			"Silent Domain",
			"Zero Presence",
			"Absolute Zero",
			"Assassin Stance",
			"Breath of Destruction",
			"Capture",
			"Claw-Rift Passage",
			"Collapse",
			"Destruction Claw",
			"Doppelganger",
			"Dragon Sword Manifestation",
			"Extinction",
			"Feral Reconstitution",
			"Flash Freeze",
			"Frost Counter",
			"Frost Monarch Spiritualization",
			"Frozen Architecture",
			"Frozen Path",
			"Golden Dragon Dance",
			"Grand Marshal Authority",
			"Heavenly Counter",
			"Hell's Army",
			"Hellstorm Dominion",
			"Ice Ball",
			"Ice Chunk",
			"King's Maul",
			"Lightning Breath",
			"Monarch Manifestation",
			"Monarch's Descent",
			"Pale Causeway",
			"Power Smash",
			"Predator's Presence",
			"Radiru Blood Spear",
			"Rubble Jaw",
			"Snow Screen",
			"Sovereign Roar",
			"Sovereign Sword Domain",
			"Spatial Execution",
			"Spiritualization",
			"Stillness Decree",
			"White Fang Sovereign",
			"White Flame Spiritualization",
			"Whiteout Procession",
			"Winter Remembers"));

	private AbilityIconRegression() {
	}

	public static void main(String[] args) throws IOException {
		Map<String, String> icons = mappedIcons();
		everyMappedIconFileExists(icons);
		onlyKnownAbilitiesFallBackToThePlaceholder(icons);
		System.out.println("Ability icon regression checks passed ("
				+ icons.size() + " mapped, " + AWAITING_ART.size() + " awaiting art).");
	}

	private static void everyMappedIconFileExists(Map<String, String> icons) {
		List<String> broken = new ArrayList<>();
		for (Map.Entry<String, String> entry : icons.entrySet()) {
			// The fire-mage cases build their path at runtime from a stem plus an
			// optional orb suffix, so they are covered by their own art instead.
			if (entry.getValue().isEmpty())
				continue;
			if (!Files.exists(SCREENS.resolve(entry.getValue() + ".png")))
				broken.add(entry.getKey() + " -> " + entry.getValue() + ".png");
		}
		expect(broken.isEmpty(),
				"These abilities point at an icon that does not exist: " + broken);
	}

	private static void onlyKnownAbilitiesFallBackToThePlaceholder(
			Map<String, String> icons) throws IOException {
		String registry = read("util", "SkillDescriptionRegistry.java");
		Set<String> unmapped = new TreeSet<>();
		Matcher matcher = DESCRIPTION.matcher(registry);
		while (matcher.find()) {
			String ability = matcher.group(1);
			if (!icons.containsKey(ability))
				unmapped.add(ability);
		}
		expect(unmapped.equals(AWAITING_ART),
				"Abilities without a HUD icon changed. Now missing: " + unmapped
						+ ", expected: " + AWAITING_ART
						+ ". Add the case to DisplayOverlay#getSkillTexture, then update"
						+ " AWAITING_ART.");
	}

	/** Ability name to icon stem, resolving the manager constants the switch uses. */
	private static Map<String, String> mappedIcons() throws IOException {
		Map<String, String> constants = constantValues();
		String overlay = read("client", "screens", "DisplayOverlay.java");
		int start = overlay.indexOf("getSkillTexture(String skillName");
		int end = overlay.indexOf("private static ResourceLocation fireMageTexture");
		expect(start >= 0 && end > start, "DisplayOverlay must expose getSkillTexture");

		Map<String, String> icons = new HashMap<>();
		for (String line : overlay.substring(start, end).split("\n")) {
			Matcher entry = CASE.matcher(line);
			if (!entry.matches())
				continue;
			String right = entry.group(2);
			if (right.contains("icon_template"))
				continue;
			Matcher texture = TEXTURE.matcher(right);
			String stem = texture.find() ? texture.group(1) : "";
			for (String label : entry.group(1).split(",")) {
				String key = label.trim();
				icons.put(key.startsWith("\"")
						? key.substring(1, key.length() - 1)
						: constants.getOrDefault(key, key), stem);
			}
		}
		return icons;
	}

	private static Map<String, String> constantValues() throws IOException {
		Map<String, String> constants = new HashMap<>();
		try (Stream<Path> files = Files.walk(MAIN)) {
			for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				String simpleName = path.getFileName().toString().replace(".java", "");
				Matcher matcher = CONSTANT.matcher(Files.readString(path));
				while (matcher.find())
					constants.put(simpleName + "." + matcher.group(1), matcher.group(2));
			}
		}
		return constants;
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
