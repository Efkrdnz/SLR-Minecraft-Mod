package net.solocraft.util;

import net.minecraft.world.entity.Entity;

import java.util.Random;
import java.util.Set;

/**
 * Shared (both sides) constants and pure functions for the Mage QTE system.
 *
 * Key design:
 *  - Ring rotates at ROTATION_SPEED degrees per wall-clock second.
 *  - The server receives pressedMs (wall-clock ms held) and computes the same
 *    angle the client animation showed, so no extra sync packet is needed.
 *  - Zone start is deterministic from UUID + game tick (/ 5 for stability across
 *    a 250 ms latency window), so both sides agree without networking.
 */
public final class MageQTEHelper {

    private MageQTEHelper() {}

    // ── QTE parameters ────────────────────────────────────────────────────────

    /** Full ring rotation speed in degrees per second. */
    public static final float ROTATION_SPEED   = 360f; // one full revolution per second

    /** Outer (gold) success arc width in degrees. */
    public static final float GOOD_ZONE_SIZE   = 40f;

    /** Inner (cyan) perfect arc width in degrees, centred inside the good zone. */
    public static final float PERFECT_ZONE_SIZE = 14f;

    /** Wall-clock timeout in milliseconds — QTE fails after this. */
    public static final int   TIMEOUT_MS        = 2000;

    // ── Which powers trigger QTE instead of instant cast ─────────────────────

    public static final Set<String> MAGE_SKILLS = Set.of(
            FireMageSpellManager.IGNITION_ORB,
            FireMageSpellManager.INFERNO_LANCE,
            FireMageSpellManager.FLASHFIRE,
            FireMageSpellManager.CREMATION,
            FireMageSpellManager.FURNACE_DOMINION,
            FireMageSpellManager.HEAVENFALL,
            BarrierMageSpellManager.SEALING_PRISM,
            BarrierMageSpellManager.RESONANT_COLLAPSE,
            BarrierMageSpellManager.ABSOLUTE_BASTION,
			ArcaneMageSpellManager.ASTRAL_ARSENAL,
			ArcaneMageSpellManager.DIMENSIONAL_REND,
			ArcaneMageSpellManager.CONVERGENCE,
            "Water Slash",
            "Curse Sphere",
            "Curse Smoke",
            "Curse Chains",
            "Magic Missiles"
    );

    // ── Pure functions ────────────────────────────────────────────────────────

    /**
     * Deterministic zone-start angle (degrees, 0 = 12 o'clock, clockwise).
     * Dividing game time by 5 gives a 250 ms stability window that covers
     * normal latency without making the zone predictable across casts.
     */
    public static float computeZoneStart(Entity entity) {
        long tick = entity.level().getGameTime() / 5;
        long u1   = entity.getUUID().getMostSignificantBits();
        long u2   = entity.getUUID().getLeastSignificantBits();
        long seed = u1 ^ (u2 * 31L) ^ (tick * 17L);
        return 90f + new Random(seed).nextFloat() * 270f;
    }

    /**
     * Evaluate which QTE tier the player hit.
     *
     * @param zoneStart the good-zone start angle from {@link #computeZoneStart}
     * @param pressedMs wall-clock milliseconds the key was held
     */
    public static QTEResult computeResult(float zoneStart, int pressedMs) {
        if (pressedMs > TIMEOUT_MS) return QTEResult.MISS;

        float rotation     = (pressedMs / 1000f * ROTATION_SPEED) % 360f;
        float goodEnd      = (zoneStart  + GOOD_ZONE_SIZE)                    % 360f;
        float perfectStart = (zoneStart  + (GOOD_ZONE_SIZE - PERFECT_ZONE_SIZE) / 2f) % 360f;
        float perfectEnd   = (perfectStart + PERFECT_ZONE_SIZE)               % 360f;

        if (isInArc(rotation, perfectStart, perfectEnd)) return QTEResult.PERFECT;
        if (isInArc(rotation, zoneStart,    goodEnd))    return QTEResult.GOOD;
        return QTEResult.MISS;
    }

    /**
     * Mana-cost multiplier for a QTE result.
     * QTE is a precision reward, not a second Intelligence scaling curve.
     * Keeping these fixed makes spell costs predictable at every output stage.
     */
    public static double getManaCostMultiplier(QTEResult result, double intelligence) {
        return switch (result) {
            case PERFECT -> 0.70;
            case GOOD    -> 0.85;
            case MISS    -> 1.00;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True if {@code angle} falls inside the arc [start, end] (handles wrap-around). */
    public static boolean isInArc(float angle, float start, float end) {
        if (end < start) return angle >= start || angle <= end; // wraps around 360
        return angle >= start && angle <= end;
    }

    /** Derive the perfect-zone start angle from a good-zone start angle. */
    public static float perfectZoneStart(float goodZoneStart) {
        return (goodZoneStart + (GOOD_ZONE_SIZE - PERFECT_ZONE_SIZE) / 2f) % 360f;
    }
}
