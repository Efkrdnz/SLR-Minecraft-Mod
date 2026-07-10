package net.solocraft.util;

/** The three outcome tiers of a mage Quick-Time Event cast. */
public enum QTEResult {
    /** Needle outside the good zone, or timed out. */
    MISS,
    /** Needle inside the outer gold arc. */
    GOOD,
    /** Needle inside the inner cyan arc (centre of the good zone). */
    PERFECT
}
