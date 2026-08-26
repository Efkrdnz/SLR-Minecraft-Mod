package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Checks that legacy cheat items stay registered but out of the curated tab. */
public final class CreativeTabCurationRegression {
	private static final Path JAVA = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final List<String> HIDDEN_LEGACY_ITEMS = List.of(
			"LEVEL_ITEM",
			"COIN_ITEM",
			"COIN_ITEM_100",
			"SHADOW_MONARCH",
			"TEST_PARTICLES",
			"GIVE_BERU",
			"GIVE_IGRIS",
			"GRAND_MAGE",
			"GG",
			"ROTATION_DEVICE",
			"GIVE_KAMISH",
			"JOB_CHANGE_DEBUG",
			"GIVE_TUSK",
			"DKC_TRAVEL");

	private CreativeTabCurationRegression() {
	}

	public static void main(String[] args) throws IOException {
		String tabs = Files.readString(JAVA.resolve("init")
				.resolve("SololevelingModTabs.java"));
		String cheatTab = section(tabs,
				"SOLO_LEVELING_CHEAT_ITEMS = REGISTRY.register",
				"public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DUNGEON_BLOCKS");
		String items = Files.readString(JAVA.resolve("init")
				.resolve("SololevelingModItems.java"));

		for (String item : HIDDEN_LEGACY_ITEMS) {
			expectFalse(cheatTab.contains(
							"tabData.accept(SololevelingModItems." + item + ".get())"),
					item + " was restored to the curated Cheat Items tab");
			expectTrue(items.contains("DeferredHolder<Item, Item> " + item
							+ " = REGISTRY.register"),
					item + " must remain registered for old-save compatibility");
		}

		for (String active : List.of(
				"CLASS_CHOOSER", "MAGIC_READER", "DKC_LEVEL_ITEM",
				"ENTRY_PERMIT", "ASSASIN_STARTERPACK",
				"TANKER_MASTERY_ITEM", "HOLY_WATER_OF_LIFE")) {
			expectTrue(cheatTab.contains(
							"tabData.accept(SololevelingModItems." + active + ".get())"),
					"Active utility/reward item was accidentally removed: " + active);
		}
	}

	private static String section(String source, String startToken,
			String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate Cheat Items tab source");
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
