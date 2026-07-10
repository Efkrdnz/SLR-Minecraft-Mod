package net.solocraft.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only singleton that tracks the live QTE animation state and the
 * last result flash.  Driven entirely by wall-clock time so it stays in
 * sync with the pressedMs value the server receives on key release.
 */
@OnlyIn(Dist.CLIENT)
public final class MageQTEState {

    public static final MageQTEState INSTANCE = new MageQTEState();

    // ── Live cast state ───────────────────────────────────────────────────────
    private boolean active    = false;
    private long    startTime = 0L;   // System.currentTimeMillis() when cast started
    private float   zoneStart = 0f;   // good-zone start angle in degrees

    // ── Post-cast result flash ────────────────────────────────────────────────
    private QTEResult lastResult        = null;
    private long      resultDisplayUntil = 0L;

    private MageQTEState() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void startQTE(float zoneStart) {
        this.active    = true;
        this.startTime = System.currentTimeMillis();
        this.zoneStart = zoneStart;
        this.lastResult = null;
    }

    /** End the animation (call on key release, before showing the result). */
    public void endQTE() {
        this.active = false;
    }

    /** Cancel without showing a result (e.g. forced timeout). */
    public void cancelQTE() {
        this.active     = false;
        this.lastResult = null;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isActive() { return active; }

    public boolean hasTimedOut() {
        return active && (System.currentTimeMillis() - startTime) > MageQTEHelper.TIMEOUT_MS;
    }

    /**
     * Current needle angle (0 = 12 o'clock, clockwise), computed from
     * elapsed wall-clock time to match what the server calculates from pressedMs.
     */
    public float getCurrentRotation() {
        if (!active) return 0f;
        long elapsed = System.currentTimeMillis() - startTime;
        return (elapsed / 1000f * MageQTEHelper.ROTATION_SPEED) % 360f;
    }

    // ── Zone geometry (derived from zoneStart + helper constants) ─────────────

    public float getGoodZoneStart()    { return zoneStart; }
    public float getGoodZoneEnd()      { return (zoneStart + MageQTEHelper.GOOD_ZONE_SIZE)    % 360f; }
    public float getPerfectZoneStart() { return MageQTEHelper.perfectZoneStart(zoneStart); }
    public float getPerfectZoneEnd()   { return (getPerfectZoneStart() + MageQTEHelper.PERFECT_ZONE_SIZE) % 360f; }

    // ── Result flash ──────────────────────────────────────────────────────────

    /** Called on key release to store the result for on-screen display. */
    public void showResult(QTEResult result) {
        this.lastResult         = result;
        this.resultDisplayUntil = System.currentTimeMillis() + 1_500;
    }

    public boolean      isShowingResult() { return lastResult != null && System.currentTimeMillis() < resultDisplayUntil; }
    public QTEResult    getLastResult()   { return lastResult; }

    /**
     * Alpha (0–1) for the result flash, fading out over the display window.
     */
    public float getResultAlpha() {
        if (!isShowingResult()) return 0f;
        long remaining = resultDisplayUntil - System.currentTimeMillis();
        return Math.min(1f, remaining / 500f); // full for first 1 s, fade last 0.5 s
    }
}
