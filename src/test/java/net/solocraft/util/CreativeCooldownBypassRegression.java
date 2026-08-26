package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-contract coverage for the global Creative cooldown bypass. */
public final class CreativeCooldownBypassRegression {
	private static final Path ROOT = Path.of("");
	private static final Path MAIN = ROOT.resolve(Path.of("src", "main",
			"java", "net", "solocraft"));

	private CreativeCooldownBypassRegression() {
	}

	public static void main(String[] args) throws IOException {
		versionMetadataMatchesProjectVersion();
		centralCooldownsNeverStartOrDisplayInCreative();
		creativeModeDiscardsInheritedCooldownState();
		nativeItemCooldownsAlsoBypassCreative();
		styleResourceGatesBypassCreative();
	}

	/**
	 * Creative already skips mana and cooldowns. The style resources that gate
	 * transformations must skip too, or testing a beast form in creative means
	 * grinding a meter first.
	 */
	private static void styleResourceGatesBypassCreative() throws IOException {
		String fighter = readMain("util", "FighterSkillManager.java");
		String transformation = section(fighter,
				"private static boolean castTransformation",
				"private static void endTransformation");
		assertContains(transformation,
				"boolean free = ManaRules.isFree(player)",
				"!free && state.feral < required",
				"if (!free)");

		String juggernaut = readMain("util", "JuggernautSkillManager.java");
		String gigantification = section(juggernaut,
				"private static boolean castGigantification",
				"private static void endGigantification");
		assertContains(gigantification,
				"boolean free = ManaRules.isFree(player)",
				"!free && state.poise < GIGANT_POISE_COST",
				"if (!free)");

		String breaker = section(juggernaut,
				"private static boolean castMountainBreaker",
				"// ── Mass and Poise passive");
		assertContains(breaker, "boolean free = ManaRules.isFree(player)",
				"!free && state.poise >= BREAKER_POISE_DISCOUNT");
	}

	private static void versionMetadataMatchesProjectVersion()
			throws IOException {
		String gradle = Files.readString(ROOT.resolve("build.gradle"));
		String properties = Files.readString(ROOT.resolve("gradle.properties"));
		String mods = Files.readString(ROOT.resolve(Path.of("src", "main",
				"resources", "META-INF", "neoforge.mods.toml")));
		expectTrue(gradle.contains("version = project.mod_version")
					&& gradle.contains("releaseJarName = \"SLR${version}-neoforge-${minecraft_version}.jar\""),
				"Gradle must derive the NeoForge release artifact name from project metadata");

		// Pinning the literal version meant every release had to edit this test,
		// which trains people to bump it without reading it. The contract that
		// actually matters is that packaged metadata cannot drift from the build.
		Matcher declared = Pattern.compile("^mod_version=(.+)$", Pattern.MULTILINE)
				.matcher(properties);
		expectTrue(declared.find(), "gradle.properties must declare mod_version");
		String version = declared.group(1).trim();
		expectTrue(mods.contains("version=\"" + version + "\""),
				"Packaged NeoForge metadata must report version " + version);
	}

	private static void centralCooldownsNeverStartOrDisplayInCreative()
			throws IOException {
		String manager = readMain("util", "CooldownManager.java");
		String setInternal = section(manager,
				"private static void setInternal", "public static void clear(");
		String remaining = section(manager,
				"public static int getRemainingTicks", "public static int getRemainingSeconds");
		String snapshot = section(manager,
				"private static String buildSnapshot", "private static int getClientRemainingTicks");

		assertContains(setInternal,
				"if (isCreativePlayer(entity))",
				"clearStoredCooldown(entity, key)",
				"return;");
		expectFalse(setInternal.contains("Math.min(durationTicks, 10)"),
				"Creative must not retain the former half-second cooldown");
		assertContains(remaining,
				"if (isCreativePlayer(entity))",
				"trimCreativeCooldown(entity, key)",
				"return 0;");
		assertContains(snapshot,
				"if (isCreativePlayer(entity))",
				"return snapshot.toString()");
		expectTrue(manager.contains(
					"public static void setFullDuration(Entity entity, String key, int durationTicks)")
					&& count(manager, "setInternal(entity, key, durationTicks") == 2,
				"Ordinary and full-duration setters must share the same Creative bypass");
	}

	private static void creativeModeDiscardsInheritedCooldownState()
			throws IOException {
		String manager = readMain("util", "CooldownManager.java");
		String clearStored = section(manager,
				"private static void clearStoredCooldown", "private static boolean isCreativePlayer");
		String tick = section(manager,
				"public static void onCreativePlayerTick", "private static boolean hasStoredCooldowns");
		assertContains(clearStored,
				"remove(PREFIX + key)",
				"remove(FULL_DURATION_PREFIX + key)",
				"pushSnapshot(entity)");
		assertContains(tick,
				"false",
				"!event.getEntity().isCreative()",
				"!event.getEntity().level().isClientSide()",
				"hasStoredCooldowns(event.getEntity())",
				"clearAll(event.getEntity())");
	}

	private static void nativeItemCooldownsAlsoBypassCreative()
			throws IOException {
		String sword = readMain("item", "DemonKingsLongSwordItem.java");
		String generatedSword = readMain("procedures",
				"DemonKingsLongSwordLivingEntityIsHitWithToolProcedure.java");
		String katana = readMain("procedures",
				"KatanaStierRightclickedProcedure.java");
		String gun = readMain("procedures", "ManaGunRightclickedProcedure.java");
		String manager = readMain("util", "CooldownManager.java");

		assertContains(sword,
				"player && !player.isCreative()",
				"addCooldown(itemstack.getItem(), 60)");
		assertContains(generatedSword,
				"_player && !_player.isCreative()",
				"addCooldown(itemstack.getItem(), 60)");
		assertContains(katana,
				"_player && !_player.isCreative()",
				"addCooldown(itemstack.getItem(), 100)");
		assertContains(gun,
				"_player && !_player.isCreative()",
				"addCooldown(itemstack.getItem(), 20)");
		assertContains(manager,
				"SololevelingModItems.DEMON_KINGS_LONG_SWORD.get()",
				"SololevelingModItems.KATANA_STIER.get()",
				"SololevelingModItems.MANA_GUN.get()");

		try (var files = Files.walk(MAIN)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java"))
					.toList()) {
				String source = Files.readString(file);
				expectFalse(source.contains("isCreative() ? 10"),
						"A legacy ten-tick Creative cooldown remains in " + file);
			}
		}
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static String section(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, Math.max(0, from + start.length()));
		if (from < 0 || to <= from)
			throw new AssertionError("Could not locate source section: "
					+ start + " -> " + end);
		return source.substring(from, to);
	}

	private static void assertContains(String source, String... tokens) {
		for (String token : tokens)
			expectTrue(source.contains(token), "Missing source contract: " + token);
	}

	private static int count(String source, String token) {
		int matches = 0;
		for (int at = 0; (at = source.indexOf(token, at)) >= 0;
				at += token.length())
			matches++;
		return matches;
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
