package net.solocraft.util;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-authoritative cooldown storage with a clock-independent client snapshot.
 * Server values remain absolute game-time expiries in player persistent data.
 */
@EventBusSubscriber
public final class CooldownManager {
    private static final String PREFIX = "cd_";
    private static final String FULL_DURATION_PREFIX = "slr_cd_full_";
    private static final String SNAPSHOT_V2 = "v2@";
    private static final Map<Entity, ClientSnapshotClock> CLIENT_SNAPSHOT_CLOCKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private CooldownManager() {
    }

    /** Starts or replaces a cooldown for non-Creative players. */
    public static void set(Entity entity, String key, int durationTicks) {
        setInternal(entity, key, durationTicks, false);
    }

    /** Starts a full-duration Survival cooldown; Creative still bypasses it. */
    public static void setFullDuration(Entity entity, String key, int durationTicks) {
        setInternal(entity, key, durationTicks, true);
    }

    private static void setInternal(Entity entity, String key, int durationTicks,
            boolean fullDuration) {
        if (entity == null || entity.level().isClientSide())
            return;
        if (isCreativePlayer(entity)) {
            clearStoredCooldown(entity, key);
            return;
        }
        long expiry = entity.level().getGameTime() + Math.max(0, durationTicks);
        entity.getPersistentData().putLong(PREFIX + key, expiry);
        if (fullDuration)
            entity.getPersistentData().putBoolean(FULL_DURATION_PREFIX + key, true);
        else
            entity.getPersistentData().remove(FULL_DURATION_PREFIX + key);
        pushSnapshot(entity);
    }

    public static void clear(Entity entity, String key) {
        if (entity == null || entity.level().isClientSide())
            return;
        entity.getPersistentData().remove(PREFIX + key);
        entity.getPersistentData().remove(FULL_DURATION_PREFIX + key);
        pushSnapshot(entity);
    }

    /** Removes persisted state that is longer than this ability can legitimately create. */
    public static void discardIfRemainingExceeds(Entity entity, String key, int maximumTicks) {
        if (entity == null || entity.level().isClientSide())
            return;
        if (isCreativePlayer(entity)) {
            clearStoredCooldown(entity, key);
            return;
        }
        long now = entity.level().getGameTime();
        long expiry = entity.getPersistentData().getLong(PREFIX + key);
        if (expiry - now > maximumTicks)
            clear(entity, key);
    }

    public static void clearAll(Entity entity) {
        if (entity == null || entity.level().isClientSide())
            return;
        entity.getPersistentData().getAllKeys().stream()
                .filter(key -> key.startsWith(PREFIX) || key.startsWith(FULL_DURATION_PREFIX))
                .toList()
                .forEach(entity.getPersistentData()::remove);
        pushSnapshot(entity);
    }

    public static boolean isOnCooldown(Entity entity, String key) {
        if (entity == null)
            return false;
        trimCreativeCooldown(entity, key);
        return getRemainingTicks(entity, key) > 0;
    }

    public static int getRemainingTicks(Entity entity, String key) {
        if (entity == null)
            return 0;
        if (isCreativePlayer(entity)) {
            trimCreativeCooldown(entity, key);
            return 0;
        }
        if (entity.level().isClientSide())
            return getClientRemainingTicks(entity, key);
        long expiry = entity.getPersistentData().getLong(PREFIX + key);
        int remaining = (int) Math.max(0, expiry - entity.level().getGameTime());
        if (remaining == 0)
            entity.getPersistentData().remove(FULL_DURATION_PREFIX + key);
        return remaining;
    }

    public static int getRemainingSeconds(Entity entity, String key) {
        int ticks = getRemainingTicks(entity, key);
        return ticks == 0 ? 0 : (int) Math.ceil(ticks / 20.0D);
    }

    private static void trimCreativeCooldown(Entity entity, String key) {
        if (entity == null || entity.level().isClientSide()
                || !isCreativePlayer(entity))
            return;
        clearStoredCooldown(entity, key);
    }

    private static void clearStoredCooldown(Entity entity, String key) {
        boolean changed = entity.getPersistentData().contains(PREFIX + key)
                || entity.getPersistentData().contains(FULL_DURATION_PREFIX + key);
        entity.getPersistentData().remove(PREFIX + key);
        entity.getPersistentData().remove(FULL_DURATION_PREFIX + key);
        if (changed)
            pushSnapshot(entity);
    }

    private static boolean isCreativePlayer(Entity entity) {
        return entity instanceof Player player && player.isCreative();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        pushSnapshot(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        pushSnapshot(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        pushSnapshot(event.getEntity());
    }

    /**
     * Entering Creative discards every persisted Solo Leveling cooldown. The
     * three abilities that use Minecraft's native item cooldown overlay are
     * cleared on both logical sides as well, including cooldowns inherited
     * from Survival before the game-mode change.
     */
    @SubscribeEvent
    public static void onCreativePlayerTick(PlayerTickEvent.Post event) {
        if (false || !event.getEntity().isCreative())
            return;
        event.getEntity().getCooldowns().removeCooldown(
                SololevelingModItems.DEMON_KINGS_LONG_SWORD.get());
        event.getEntity().getCooldowns().removeCooldown(
                SololevelingModItems.KATANA_STIER.get());
        event.getEntity().getCooldowns().removeCooldown(
                SololevelingModItems.MANA_GUN.get());
        if (!event.getEntity().level().isClientSide() && hasStoredCooldowns(event.getEntity()))
            clearAll(event.getEntity());
    }

    private static boolean hasStoredCooldowns(Entity entity) {
        return entity.getPersistentData().getAllKeys().stream()
                .anyMatch(key -> key.startsWith(PREFIX)
                        || key.startsWith(FULL_DURATION_PREFIX));
    }

    private static void pushSnapshot(Entity entity) {
        if (entity == null || entity.level().isClientSide() || !(entity instanceof Player player))
            return;
        player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.cooldownData = buildSnapshot(entity);
            capability.syncPlayerVariables(player);
        });
    }

    /** V2 stores remaining ticks so client/server world-clock offsets cannot distort the HUD. */
    private static String buildSnapshot(Entity entity) {
        long now = entity.level().getGameTime();
        StringBuilder snapshot = new StringBuilder(SNAPSHOT_V2).append(now);
        if (isCreativePlayer(entity))
            return snapshot.toString();
        for (String nbtKey : entity.getPersistentData().getAllKeys()) {
            if (!nbtKey.startsWith(PREFIX))
                continue;
            long expiry = entity.getPersistentData().getLong(nbtKey);
            if (expiry <= now)
                continue;
            snapshot.append(';')
                    .append(nbtKey, PREFIX.length(), nbtKey.length())
                    .append(':')
                    .append(expiry - now);
        }
        return snapshot.toString();
    }

    private static int getClientRemainingTicks(Entity entity, String key) {
        String snapshot = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .map(capability -> capability.cooldownData)
                .orElse("");
        if (snapshot == null || snapshot.isEmpty()) {
            CLIENT_SNAPSHOT_CLOCKS.remove(entity);
            return 0;
        }

        // Old saves are accepted until the login refresh replaces their snapshot.
        if (!snapshot.startsWith(SNAPSHOT_V2)) {
            long expiry = parseSnapshotValue(snapshot, key);
            return (int) Math.max(0, expiry - entity.level().getGameTime());
        }

        ClientSnapshotClock clock = CLIENT_SNAPSHOT_CLOCKS.get(entity);
        if (clock == null || !clock.snapshot.equals(snapshot) || entity.tickCount < clock.receivedAtTick) {
            clock = new ClientSnapshotClock(snapshot, entity.tickCount);
            CLIENT_SNAPSHOT_CLOCKS.put(entity, clock);
        }
        long initialRemaining = clock.remainingByKey.getOrDefault(key, 0L);
        long elapsed = Math.max(0, entity.tickCount - clock.receivedAtTick);
        return (int) Math.max(0, initialRemaining - elapsed);
    }

    private static Map<String, Long> parseV2Snapshot(String snapshot) {
        Map<String, Long> values = new HashMap<>();
        for (String entry : snapshot.split(";")) {
            int colon = entry.indexOf(':');
            if (colon < 0)
                continue;
            try {
                values.put(entry.substring(0, colon), Long.parseLong(entry.substring(colon + 1)));
            } catch (NumberFormatException ignored) {
                // Ignore malformed entries without breaking every cooldown on the HUD.
            }
        }
        return values;
    }

    private static long parseSnapshotValue(String snapshot, String key) {
        if (snapshot == null || snapshot.isEmpty())
            return 0;
        for (String entry : snapshot.split(";")) {
            int colon = entry.indexOf(':');
            if (colon < 0)
                continue;
            if (entry.regionMatches(0, key, 0, colon) && colon == key.length()) {
                try {
                    return Long.parseLong(entry.substring(colon + 1));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static final class ClientSnapshotClock {
        private final String snapshot;
        private final int receivedAtTick;
        private final Map<String, Long> remainingByKey;

        private ClientSnapshotClock(String snapshot, int receivedAtTick) {
            this.snapshot = snapshot;
            this.receivedAtTick = receivedAtTick;
            this.remainingByKey = parseV2Snapshot(snapshot);
        }
    }
}
