package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pins the hunter-class registry against the numbering it describes, and against
 * everything downstream that has to agree with it.
 *
 * <p>The registry does not replace {@code PlayerVariables.Classes}; it describes
 * it. That only holds while the two agree, and while nothing keeps a private copy
 * of the class list on the side. The HUD used to hold exactly such a copy; it now
 * resolves through the registry, and this keeps it that way.
 *
 * <p>Source-text based to match the other regressions here, none of which load
 * Minecraft classes.
 */
public final class HunterClassRegistryRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");
	private static final Path LANG = Path.of("src", "main", "resources", "assets",
			"sololeveling", "lang", "en_us.json");

	/** Matches HunterClass.of(id("fighter"), 3, "Fighter") across line breaks. */
	private static final Pattern REGISTRY_ENTRY = Pattern.compile(
			"HunterClass\\.of\\(id\\(\"([a-z0-9_]+)\"\\)\\s*,\\s*([A-Za-z0-9_.]+)\\s*,\\s*\"([^\"]+)\"\\)");

	/** A hardcoded class-name table: case 3 -> "Fighter". */
	private static final Pattern NAME_TABLE_ENTRY = Pattern.compile(
			"case\\s+\\d+\\s*->\\s*\"[A-Z][A-Za-z ]*\"");

	private HunterClassRegistryRegression() {
	}

	public static void main(String[] args) throws IOException {
		Map<Integer, ClassEntry> registry = readRegistry();

		registryCoversTheOriginalNumbering(registry);
		identifiersAreUsableAsResourceLocations(registry);
		builtInsAreTranslatable(registry);
		theHudResolvesThroughTheRegistry();
		persistenceCoversEveryPath();
		assignWritesBothRepresentations();
		mirrorCannotImpersonateABuiltIn(registry);

		System.out.println("Hunter class registry regression checks passed ("
				+ registry.size() + " classes).");
	}

	private static void registryCoversTheOriginalNumbering(Map<Integer, ClassEntry> registry) {
		// 0 is the unawakened placeholder; 1-6 are the shipped classes.
		for (int legacyId = 0; legacyId <= 6; legacyId++)
			expect(registry.containsKey(legacyId),
					"The registry must declare a class for legacy Classes value " + legacyId);
		expect(registry.size() == 7,
				"Expected exactly 7 built-in classes, found " + registry.size() + ": " + registry);
	}

	private static void identifiersAreUsableAsResourceLocations(Map<Integer, ClassEntry> registry) {
		for (ClassEntry entry : registry.values()) {
			// A path ResourceLocation would reject only fails at class-load, which
			// on a dedicated server means a crash rather than one bad entry.
			expect(entry.path().equals(entry.path().toLowerCase(Locale.ROOT)),
					"Hunter class path must be lowercase: " + entry.path());
			expect(entry.path().matches("[a-z0-9_]+"),
					"Hunter class path must be a valid resource path: " + entry.path());
		}
	}

	/**
	 * Display names fall back to the registry's built-in string, so a missing key
	 * degrades quietly rather than showing a raw translation key. That is right
	 * for a class contributed by an addon, and wrong for one we ship: shipping
	 * without the key means it can never be translated.
	 */
	private static void builtInsAreTranslatable(Map<Integer, ClassEntry> registry) throws IOException {
		String lang = Files.readString(LANG).replace("\r\n", "\n");
		for (ClassEntry entry : registry.values()) {
			String expected = "\"hunterclass.sololeveling." + entry.path() + "\": \""
					+ entry.name() + "\"";
			expect(lang.contains(expected),
					"en_us.json is missing or disagrees with the registry for \"" + entry.path()
							+ "\"; expected " + expected);
		}
	}

	/**
	 * The HUD kept its own number-to-name switch, which is how the two could drift.
	 * It resolves from the player now, which also means a class registered without
	 * a legacy number displays correctly instead of reading as unawakened.
	 */
	private static void theHudResolvesThroughTheRegistry() throws IOException {
		String source = read("client", "screens", "MPOverlayOverlay.java");
		expect(source.contains("HunterClassRegistry.of("),
				"MPOverlayOverlay must resolve the player's class through HunterClassRegistry");

		Matcher table = NAME_TABLE_ENTRY.matcher(source);
		expect(!table.find(),
				"MPOverlayOverlay has a hardcoded class-name table again ("
						+ (table.reset().find() ? table.group() : "") + "); resolve through the registry instead");
	}

	/**
	 * A field declared but missing from one persistence path is lost silently --
	 * on reconnect, reload, or a dimension change, for some players some of the
	 * time. Far cheaper to pin here than to reproduce from a bug report.
	 */
	private static void persistenceCoversEveryPath() throws IOException {
		String source = read("network", "SololevelingModVariables.java");
		String[] required = {
				"public String hunterClassId = \"\";",
				"clone.hunterClassId = original.hunterClassId;",
				"nbt.putString(\"hunterClassId\", hunterClassId);",
				"hunterClassId = nbt.getString(\"hunterClassId\");",
				"variables.hunterClassId = message.data.hunterClassId;" };
		for (String line : required)
			expect(source.contains(line),
					"PlayerVariables is missing a hunterClassId persistence path: " + line);
	}

	private static void assignWritesBothRepresentations() throws IOException {
		String source = read("api", "hunter", "HunterClassRegistry.java");
		int start = source.indexOf("public static boolean assign(");
		expect(start >= 0, "HunterClassRegistry must expose assign(Entity, HunterClass)");
		String body = source.substring(start);
		expect(body.contains("capability.hunterClassId"),
				"assign must write the class identifier");
		expect(body.contains("capability.Classes"),
				"assign must write the legacy Classes mirror alongside the identifier");
		expect(body.contains("syncPlayerVariables"),
				"assign must sync, or the client shows the old class until relog");
	}

	private static void mirrorCannotImpersonateABuiltIn(Map<Integer, ClassEntry> registry)
			throws IOException {
		String source = read("api", "hunter", "HunterClassRegistry.java");
		Matcher matcher = Pattern.compile("CUSTOM_CLASS_LEGACY_MIRROR = ([0-9]+)").matcher(source);
		expect(matcher.find(), "HunterClassRegistry must declare CUSTOM_CLASS_LEGACY_MIRROR");
		int mirror = Integer.parseInt(matcher.group(1));
		expect(mirror != 0,
				"The custom-class mirror must not be 0, which legacy checks read as unawakened");
		expect(!registry.containsKey(mirror),
				"The custom-class mirror " + mirror + " collides with built-in class "
						+ registry.get(mirror));
	}

	private static Map<Integer, ClassEntry> readRegistry() throws IOException {
		String source = read("api", "hunter", "HunterClassRegistry.java");
		Matcher matcher = REGISTRY_ENTRY.matcher(source);
		Map<Integer, ClassEntry> classes = new LinkedHashMap<>();
		while (matcher.find()) {
			int legacyId = parseLegacyId(matcher.group(2));
			expect(!classes.containsKey(legacyId),
					"Legacy Classes value " + legacyId + " is declared twice in the registry");
			if (legacyId >= 0)
				classes.put(legacyId, new ClassEntry(matcher.group(1), matcher.group(3)));
		}
		expect(!classes.isEmpty(), "No hunter classes were found in the registry source");
		return classes;
	}

	private static int parseLegacyId(String token) {
		if (token.endsWith("UNAWAKENED_LEGACY_ID"))
			return 0;
		if (token.endsWith("NO_LEGACY_ID"))
			return -1;
		try {
			return Integer.parseInt(token);
		} catch (NumberFormatException exception) {
			throw new AssertionError("Legacy id must be a literal or a named constant, found " + token);
		}
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

	/** One class as declared in the registry source. */
	private record ClassEntry(String path, String name) {
		@Override
		public String toString() {
			return path + " (" + name + ")";
		}
	}
}
