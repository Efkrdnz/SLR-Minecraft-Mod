package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Centralized cooldown system. Replaces the 52-effect MobEffect hack.
 *
 * Cooldowns are stored as game-time expiry ticks in player PersistentData
 * under the key "cd_<abilityKey>". The server is authoritative; the client
 * gets a compact snapshot string ("key1:expiry1;key2:expiry2") via the
 * PlayerVariables capability sync so the HUD can display remaining time.
 *
 * Ability key conventions (use consistently everywhere):
 *   - For PselectedPower abilities:  use the exact PselectedPower string
 *     e.g.  "Stealth", "Fireball", "Slash Dash", "Murderious Intent"
 *   - For weapon/other abilities:    use a short lowercase snake_case key
 *     e.g.  "dagger_rush", "counter", "mana_refresh", "job_1"
 */
public final class CooldownManager {

    private static final String PREFIX = "cd_";

    private CooldownManager() {}

    // ── Server-side write ─────────────────────────────────────────────────────

    /**
     * Starts a cooldown for {@code durationTicks} ticks.
     * Call this server-side only. Syncs the snapshot to the client automatically.
     */
    public static void set(Entity entity, String key, int durationTicks) {
        if (entity == null) return;
        if (isCreativePlayer(entity)) durationTicks = Math.min(durationTicks, 10);
        long expiry = entity.level().getGameTime() + durationTicks;
        entity.getPersistentData().putLong(PREFIX + key, expiry);
        pushSnapshot(entity);
    }

    /** Clears a specific cooldown immediately. */
    public static void clear(Entity entity, String key) {
        if (entity == null) return;
        entity.getPersistentData().remove(PREFIX + key);
        pushSnapshot(entity);
    }

    /** Clears every cooldown at once (e.g. on full reset / death if desired). */
    public static void clearAll(Entity entity) {
        if (entity == null) return;
        entity.getPersistentData().getAllKeys().stream()
                .filter(k -> k.startsWith(PREFIX))
                .toList()
                .forEach(entity.getPersistentData()::remove);
        if (!entity.level().isClientSide() && entity instanceof Player player) {
            player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
                cap.cooldownData = "";
                cap.syncPlayerVariables(player);
            });
        }
    }

    // ── Both-side read ────────────────────────────────────────────────────────

    /** Returns {@code true} if the cooldown has not yet expired. Works on both sides. */
    public static boolean isOnCooldown(Entity entity, String key) {
        if (entity == null) return false;
        trimCreativeCooldown(entity, key);
        return expiryFor(entity, key) > entity.level().getGameTime();
    }

    /** Remaining ticks (0 if ready). Works on both sides. */
    public static int getRemainingTicks(Entity entity, String key) {
        if (entity == null) return 0;
        return (int) Math.max(0, expiryFor(entity, key) - entity.level().getGameTime());
    }

    /** Remaining seconds, rounded up (0 if ready). Works on both sides. */
    public static int getRemainingSeconds(Entity entity, String key) {
        int ticks = getRemainingTicks(entity, key);
        return ticks == 0 ? 0 : (int) Math.ceil(ticks / 20.0);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Returns the raw expiry game-time for {@code key}, or 0 if none. */
    private static long expiryFor(Entity entity, String key) {
        if (!entity.level().isClientSide()) {
            // Server: read directly from PersistentData
            return entity.getPersistentData().getLong(PREFIX + key);
        } else {
            // Client: read from synced capability snapshot
            String snapshot = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                    .map(cap -> cap.cooldownData)
                    .orElse("");
            return parseExpiry(snapshot, key);
        }
    }

    private static void trimCreativeCooldown(Entity entity, String key) {
        if (entity == null || entity.level().isClientSide() || !isCreativePlayer(entity)) return;
        long now = entity.level().getGameTime();
        long expiry = entity.getPersistentData().getLong(PREFIX + key);
        if (expiry > now + 10) {
            entity.getPersistentData().putLong(PREFIX + key, now + 10);
            pushSnapshot(entity);
        }
    }

    private static boolean isCreativePlayer(Entity entity) {
        return entity instanceof Player player && player.isCreative();
    }

    /**
     * Rebuilds the compact snapshot string from all active PersistentData cooldowns
     * and pushes it to the client via the existing PlayerVariables sync.
     */
    private static void pushSnapshot(Entity entity) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return;
        player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
            cap.cooldownData = buildSnapshot(entity);
            cap.syncPlayerVariables(player);
        });
    }

    /** "key1:expiry1;key2:expiry2" — only active (not yet expired) entries. */
    private static String buildSnapshot(Entity entity) {
        long now = entity.level().getGameTime();
        StringBuilder sb = new StringBuilder();
        for (String nbtKey : entity.getPersistentData().getAllKeys()) {
            if (!nbtKey.startsWith(PREFIX)) continue;
            long expiry = entity.getPersistentData().getLong(nbtKey);
            if (expiry <= now) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(nbtKey, PREFIX.length(), nbtKey.length())
              .append(':')
              .append(expiry);
        }
        return sb.toString();
    }

    /** Parses expiry for {@code key} out of the snapshot string, or returns 0. */
    private static long parseExpiry(String snapshot, String key) {
        if (snapshot == null || snapshot.isEmpty()) return 0;
        for (String entry : snapshot.split(";")) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            if (entry.regionMatches(0, key, 0, colon) && colon == key.length()) {
                try {
                    return Long.parseLong(entry.substring(colon + 1));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }
}
