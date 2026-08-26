package net.solocraft.api;

import java.util.Locale;

import net.minecraft.world.entity.Entity;

import net.solocraft.util.CooldownManager;

/**
 * Per-ability cooldowns, shared with the mod's own skills.
 *
 * <p>Using this rather than a private timer means an addon's cooldowns clear on
 * death, respawn, dimension change, and in Creative exactly like everything
 * else, and survive relog the same way.
 *
 * <p>Keys are namespaced on the caller's behalf, because they share one space
 * with the mod's own keys -- an addon that picked "dash" could otherwise cancel
 * a built-in ability's cooldown, or have its own cancelled.
 */
public final class AbilityCooldowns {
	private AbilityCooldowns() {
	}

	/** @param owner your mod id @param ability your ability id */
	public static void set(Entity entity, String owner, String ability, int durationTicks) {
		CooldownManager.set(entity, key(owner, ability), durationTicks);
	}

	public static boolean isOnCooldown(Entity entity, String owner, String ability) {
		return CooldownManager.isOnCooldown(entity, key(owner, ability));
	}

	public static int remainingTicks(Entity entity, String owner, String ability) {
		return CooldownManager.getRemainingTicks(entity, key(owner, ability));
	}

	public static int remainingSeconds(Entity entity, String owner, String ability) {
		return CooldownManager.getRemainingSeconds(entity, key(owner, ability));
	}

	public static void clear(Entity entity, String owner, String ability) {
		CooldownManager.clear(entity, key(owner, ability));
	}

	private static String key(String owner, String ability) {
		return sanitise(owner) + ":" + sanitise(ability);
	}

	private static String sanitise(String value) {
		if (value == null || value.isBlank())
			return "unknown";
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
	}
}
