package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Guards the 1.21 durability bridge used by every legacy generated weapon.
 *
 * <p>The previous version of this file only asserted that a particular line of
 * source text existed, so it stayed green while every zero-use legendary weapon
 * broke on its first hit. These checks exercise the modelled break condition
 * instead, and verify that no weapon can quietly opt out of the bridge.
 */
public final class WeaponDurabilityRegression {
	private static final Path ITEMS = Path.of(
			"src", "main", "java", "net", "solocraft", "item");
	private static final Pattern ZERO_USES = Pattern.compile(
			"public int getUses\\(\\)\\s*\\{\\s*return\\s+(-?\\d+)\\s*;");

	private WeaponDurabilityRegression() {
	}

	public static void main(String[] args) throws Exception {
		zeroUseWeaponsSurviveCombat();
		normalWeaponsKeepTheirDurability();
		theUnbreakableFlagIsWhatPreventsTheOneHitBreak();
		everyZeroUseWeaponRoutesThroughTheBridge();
		theBridgeUsesTheFlagRatherThanAnOverwrittenDurability();
		System.out.println("Weapon durability regression checks passed.");
	}

	/** A zero-use legendary tier must never break, matching 1.20 behaviour. */
	private static void zeroUseWeaponsSurviveCombat() {
		boolean unbreakable = LegacyWeaponDurabilityRules.requiresUnbreakable(0);
		expect(unbreakable, "A zero-use legacy tier must be marked unbreakable");
		expect(!LegacyWeaponDurabilityRules.isDamageable(unbreakable),
				"An unbreakable legendary weapon must not be damageable");
		expect(LegacyWeaponDurabilityRules.breaksOnHit(0, unbreakable) == Integer.MAX_VALUE,
				"A zero-use legendary weapon must never break");
	}

	private static void normalWeaponsKeepTheirDurability() {
		expect(!LegacyWeaponDurabilityRules.requiresUnbreakable(6000),
				"A positive-use tier must keep ordinary durability");
		expect(LegacyWeaponDurabilityRules.effectiveMaxDamage(6000) == 6000,
				"TieredItem must carry the tier's own max damage through");
		expect(LegacyWeaponDurabilityRules.breaksOnHit(6000, false) == 6000,
				"A 6000-use weapon must survive 5999 hits");
		expect(LegacyWeaponDurabilityRules.breaksOnHit(80, false) == 80,
				"An 80-use weapon must survive 79 hits");
	}

	/**
	 * The precise defect: TieredItem writes max damage 0 for a zero-use tier
	 * after the subclass's properties are built, so writing a durability there is
	 * discarded and the first hit takes damage to 1 >= 0. Only the flag survives.
	 */
	private static void theUnbreakableFlagIsWhatPreventsTheOneHitBreak() {
		expect(LegacyWeaponDurabilityRules.effectiveMaxDamage(0) == 0,
				"TieredItem overwrites any earlier durability with the tier's zero");
		expect(LegacyWeaponDurabilityRules.breaksOnHit(0, false) == 1,
				"Without the unbreakable flag a zero-use weapon breaks on the first hit");
	}

	/** No weapon may declare a zero-use tier without inheriting the bridge. */
	private static void everyZeroUseWeaponRoutesThroughTheBridge() throws IOException {
		List<String> unprotected = new ArrayList<>();
		try (Stream<Path> files = Files.list(ITEMS)) {
			for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				String source = Files.readString(path).replace("\r\n", "\n");
				Matcher matcher = ZERO_USES.matcher(source);
				boolean zeroUse = false;
				while (matcher.find()) {
					if (Integer.parseInt(matcher.group(1)) <= 0)
						zeroUse = true;
				}
				if (zeroUse && !source.contains("extends LegacySwordItem"))
					unprotected.add(path.getFileName().toString());
			}
		}
		expect(unprotected.isEmpty(),
				"Zero-use weapons must extend LegacySwordItem or they break on the first hit: "
						+ unprotected);
	}

	private static void theBridgeUsesTheFlagRatherThanAnOverwrittenDurability()
			throws IOException {
		String bridge = Files.readString(ITEMS.resolve("LegacySwordItem.java"))
				.replace("\r\n", "\n");
		expect(bridge.contains(
						"LegacyWeaponDurabilityRules.requiresUnbreakable(tier.getUses())")
						&& bridge.contains("DataComponents.UNBREAKABLE"),
				"The bridge must mark zero-use tiers unbreakable through the shared rules");
		expect(!bridge.contains("properties.durability("),
				"Writing a durability here is silently overwritten by TieredItem");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new IllegalStateException(message);
	}
}
