package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level regression for Minecraft 1.21.1's physical-ammo gate in
 * BowItem.releaseUsing.
 */
public final class RangerManaQuiverReleaseRegression {
	private static final Path RANGER = Path.of(
			"src", "main", "java", "net", "solocraft", "util",
			"RangerCombatManager.java");

	private RangerManaQuiverReleaseRegression() {
	}

	public static void main(String[] args) throws IOException {
		String source = Files.readString(RANGER);
		manaDrawCanStartWithoutPhysicalArrows(source);
		manaReleaseDoesNotDependOnArrowLooseEvent(source);
		serverReleaseRetainsTheCompleteShotPipeline(source);
	}

	private static void manaDrawCanStartWithoutPhysicalArrows(String source) {
		String nock = method(source, "public static void onArrowNock",
				"public static void onBowUseStopped");
		expectTrue(nock.contains("player.startUsingItem(event.getHand())")
					&& nock.contains("InteractionResultHolder.consume(event.getBow())"),
				"Mana Quiver must begin a bow draw even when vanilla found no projectile");
	}

	private static void manaReleaseDoesNotDependOnArrowLooseEvent(String source) {
		expectTrue(source.contains(
				"onBowUseStopped(LivingEntityUseItemEvent.Stop event)"),
				"Mana Quiver must use the ammo-independent use-item stop hook");
		expectTrue(source.contains(
				"@SubscribeEvent(priority = EventPriority.HIGHEST)"),
				"Mana Quiver must intercept release before the bow processes ammunition");
		String stop = method(source, "public static void onBowUseStopped",
				"private static void releaseManaArrow");
		expectTrue(stop.contains("event.setCanceled(true)")
					&& stop.contains("bow.getUseDuration(serverPlayer) - event.getDuration()")
					&& stop.contains("releaseManaArrow(serverPlayer, bow, charge)"),
				"The stop hook must cancel vanilla release, preserve charge, and fire server-side");
		expectFalse(source.contains(
				"import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;"),
				"The 1.21.1 Mana Quiver must not rely on ArrowLooseEvent's physical-ammo gate");
	}

	private static void serverReleaseRetainsTheCompleteShotPipeline(String source) {
		String release = method(source, "private static void releaseManaArrow",
				"@SubscribeEvent(priority = EventPriority.LOWEST)");
		expectTrue(release.contains("fireFivefoldArrow(serverPlayer, bow, charge)"),
				"Fivefold releases must keep using their specialized path");
		expectTrue(release.contains("chargeStage(serverPlayer, charge)")
					&& release.contains("spendMana(serverPlayer, cost")
					&& release.contains("spawnManaArrow(serverPlayer, bow, stage, damage")
					&& release.contains("damageBow(serverPlayer, bow)"),
				"Ordinary releases must retain charging, mana cost, spawning, and durability");
	}

	private static String method(String source, String startToken,
			String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start + startToken.length());
		if (start < 0 || end < 0 || end <= start)
			throw new AssertionError("Could not locate source section: " + startToken);
		return source.substring(start, end);
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
