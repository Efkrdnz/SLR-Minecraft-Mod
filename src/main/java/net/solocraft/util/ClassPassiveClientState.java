package net.solocraft.util;

/**
 * Lightweight data holder for class-passive display values received from the
 * server via ClassPassiveMessage.  No Minecraft imports — safe on both logical
 * sides; the overlay reads from here, the manager writes via the packet.
 */
public final class ClassPassiveClientState {

    /** Assassin shadow-combo tier, 0–10. */
    public static volatile int    assassinComboTier = 0;

    /** Fighter battle-power meter, 0.0–100.0. */
    public static volatile double fighterPower      = 0.0;

    /** Tanker iron-wall stacks, 0–10. */
    public static volatile int    tankWallStacks    = 0;

    /** Healer resonance stacks, 0–5. */
    public static volatile int    healerResonance   = 0;

    /** Ranger focus charge, 0.0–100.0. */
    public static volatile double rangerFocus       = 0.0;

    private ClassPassiveClientState() {}

    /** Called by ClassPassiveMessage.handler on the client main thread. */
    public static void update(int type, double value) {
        switch (type) {
            case 0 -> assassinComboTier = (int) value;
            case 1 -> fighterPower      = value;
            case 2 -> tankWallStacks    = (int) value;
            case 3 -> healerResonance   = (int) value;
            case 4 -> rangerFocus       = value;
        }
    }
}
