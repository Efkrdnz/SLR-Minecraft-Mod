package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cost, authority, synchronization, and GUI contracts for army healing. */
public final class ShadowHealingRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");

	private ShadowHealingRegression() {
	}

	public static void main(String[] args) throws IOException {
		manaCostTracksActualMissingHealth();
		healingIsServerAuthoritativeAndOwnershipSafe();
		menuSynchronizesBothLiveQuotes();
		manageSectionOffersBothActionsAndTooltips();
	}

	private static void manaCostTracksActualMissingHealth() {
		expectDouble(4.0D,
				ShadowHealingRules.HEALTH_PER_MANA,
				"Healing exchange rate changed unexpectedly");
		expectEquals(0, ShadowHealingRules.manaCost(0.0D),
				"Full-health armies must cost no mana");
		expectEquals(1, ShadowHealingRules.manaCost(0.01D),
				"Any real healing must cost at least one mana");
		expectEquals(1, ShadowHealingRules.manaCost(4.0D),
				"Four restored health should cost one mana");
		expectEquals(2, ShadowHealingRules.manaCost(4.01D),
				"Healing quotes must round upward, never undercharge");
		expectEquals(100, ShadowHealingRules.manaCost(400.0D),
				"Large army healing must scale linearly");
		expectEquals(0, ShadowHealingRules.manaCost(Double.NaN),
				"Malformed health totals must fail closed");
	}

	private static void healingIsServerAuthoritativeAndOwnershipSafe()
			throws IOException {
		String manager = readMain("util", "ShadowMonarchManager.java");
		expectTrue(manager.contains(
					"VesselProgressionManager.isShadowMonarch(owner)")
				&& manager.contains("isOwnedShadow(living, owner)")
				&& manager.contains("isCurrentSummonedInstance(owner, living)")
				&& manager.contains("hasSummonMana(owner, quotedCost)")
				&& manager.contains("consumeSummonMana(owner, quotedCost)")
				&& manager.contains("living.setHealth(living.getMaxHealth())"),
				"Server healing must validate vessel, ownership, roster instance, mana, and health");
		expectTrue(manager.contains("isHealingBossType(target.type())")
				&& manager.contains("isBoss(type) || \"iron\".equals(type)"),
				"Boss-only healing must match the GUI's named boss roster, including Iron");
		expectTrue(manager.contains("SoundEvents.BEACON_POWER_SELECT")
				&& manager.contains("SHADOW_REVIVE")
				&& manager.contains("SOUL_FIRE_FLAME"),
				"Successful army healing needs readable audiovisual feedback");

		String packet = readMain("network",
				"ShadowSummonGUIButtonMessage.java");
		expectTrue(packet.contains("HEAL_BOSS_SHADOWS_BUTTON_ID = 102")
				&& packet.contains("HEAL_ALL_SHADOWS_BUTTON_ID = 103")
				&& packet.contains(
						"entity.containerMenu instanceof ShadowSummonGUIMenu")
				&& packet.contains("summonMenu.x != x")
				&& packet.contains("ShadowMonarchManager.healSummonedShadows"),
				"Forged healing packets must fail unless the matching summon menu is open");
	}

	private static void menuSynchronizesBothLiveQuotes() throws IOException {
		String menu = readMain("world", "inventory",
				"ShadowSummonGUIMenu.java");
		expectTrue(menu.contains("HEAL_BOSS_COST_LOW")
				&& menu.contains("HEAL_BOSS_COST_HIGH")
				&& menu.contains("HEAL_ALL_COST_LOW")
				&& menu.contains("HEAL_ALL_COST_HIGH")
				&& menu.contains("healingQuoteForData()")
				&& menu.contains("public int bossHealingManaCost()")
				&& menu.contains("public int allHealingManaCost()"),
				"Both dynamic healing costs must travel through synchronized menu data");
	}

	private static void manageSectionOffersBothActionsAndTooltips()
			throws IOException {
		String screen = readMain("client", "gui",
				"ShadowSummonGUIScreen.java");
		expectTrue(screen.contains("Component.literal(\"Heal Bosses\")")
				&& screen.contains("Component.literal(\"Heal All\")")
				&& screen.contains("Tooltip.create")
				&& screen.contains("bossHealingManaCost()")
				&& screen.contains("allHealingManaCost()")
				&& screen.contains("1 MP per 4 health"),
				"Manage needs two live-cost healing buttons with explicit tooltips");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectDouble(double expected, double actual,
			String message) {
		if (Math.abs(expected - actual) > 0.0001D)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
