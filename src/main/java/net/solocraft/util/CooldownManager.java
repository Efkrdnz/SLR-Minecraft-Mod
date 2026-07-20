package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
@Mod.EventBusSubscriber
public final class CooldownManager {
    private static final String PREFIX = "cd_";
    private static final String SNAPSHOT_V2 = "v2@";
    private static final Map<Entity, ClientSnapshotClock> CLIENT_SNAPSHOT_CLOCKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private CooldownManager() {
    }

    /** Starts or replaces a cooldown. Duration is measured in ticks. */
    public static void set(Entity entity, String key, int durationTicks) {
        if (entity == null || entity.level().isClientSide())
            return;
        if (isCreativePlayer(entity))
            durationTicks = Math.min(durationTicks, 10);
        long expiry = entity.level().getGameTime() + Math.max(0, durationTicks);
        entity.getPersistentData().putLong(PREFIX + key, expiry);
        pushSnapshot(entity);
    }

    public static void clear(Entity entity, String key) {
        if (entity == null || entity.level().isClientSide())
            return;
        entity.getPersistentData().remove(PREFIX + key);
        pushSnapshot(entity);
    }

    /** Removes persisted state that is longer than this ability can legitimately create. */
    public static void discardIfRemainingExceeds(Entity entity, String key, int maximumTicks) {
        if (entity == null || entity.level().isClientSide())
            return;
        long now = entity.level().getGameTime();
        long expiry = entity.getPersistentData().getLong(PREFIX + key);
        if (expiry - now > maximumTicks)
            clear(entity, key);
    }

    public static void clearAll(Entity entity) {
        if (entity == null || entity.level().isClientSide())
            return;
        entity.getPersistentData().getAllKeys().stream()
                .filter(key -> key.startsWith(PREFIX))
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
        if (entity.level().isClientSide())
            return getClientRemainingTicks(entity, key);
        long expiry = entity.getPersistentData().getLong(PREFIX + key);
        return (int) Math.max(0, expiry - entity.level().getGameTime());
    }

    public static int getRemainingSeconds(Entity entity, String key) {
        int ticks = getRemainingTicks(entity, key);
        return ticks == 0 ? 0 : (int) Math.ceil(ticks / 20.0D);
    }

    private static void trimCreativeCooldown(Entity entity, String key) {
        if (entity == null || entity.level().isClientSide() || !isCreativePlayer(entity))
            return;
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
