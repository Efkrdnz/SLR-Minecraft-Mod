package net.solocraft.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Client-only data holder for class-passive display values received from the
 * server via ClassPassiveMessage. The overlay reads from here, and disconnect
 * cleanup prevents one world or server from leaking values into the next.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClassPassiveClientState {

    /** Assassin shadow-combo tier, 0–10. */
    public static volatile int    assassinTempo     = 0;

    /** Fighter battle-power meter, 0.0–100.0. */
    public static volatile double fighterPower      = 0.0;

    /** Tanker iron-wall stacks, 0–10. */
    public static volatile int    tankWallStacks    = 0;

    /** Healer resonance stacks, 0–5. */
    public static volatile int    healerResonance   = 0;

    /** Ranger focus charge, 0.0–100.0. */
    public static volatile double rangerFocus       = 0.0;

    /** Veil meter, 0.0–100.0. */
    public static volatile double assassinVeil      = 0.0;

    /**
     * True once the server has sent a Veil update. The server only syncs Veil
     * to players who own a concealment ability, so this is the class-agnostic
     * signal that the meter should be drawn.
     */
    public static volatile boolean assassinVeilAvailable = false;

    /** Sword Tempo, 0–5. */
    public static volatile int    fighterTempo      = 0;
    public static volatile boolean fighterTempoAvailable = false;

    /** Feral meter, 0.0–100.0. */
    public static volatile double fighterFeral      = 0.0;
    public static volatile boolean fighterFeralAvailable = false;

    /** Juggernaut Poise meter, 0.0–100.0. */
    public static volatile double tankerPoise       = 0.0;
    public static volatile boolean tankerPoiseAvailable = false;

    /**
     * Sentinel meaning "this resource is no longer available, hide its bar".
     * Sent when a player resets, changes class, or loses the abilities that
     * feed a resource; without it an ability-driven bar would latch on for the
     * rest of the session.
     */
    public static final double UNAVAILABLE = -1.0D;

    private ClassPassiveClientState() {}

    /** Called by ClassPassiveMessage.handler on the client main thread. */
    public static void update(int type, double value) {
        boolean available = value >= 0.0D;
        double resolved = available ? value : 0.0D;
        switch (type) {
            case 0 -> assassinTempo     = (int) resolved;
            case 1 -> fighterPower      = resolved;
            case 2 -> tankWallStacks    = (int) resolved;
            case 3 -> healerResonance   = (int) resolved;
            case 4 -> rangerFocus       = resolved;
            case 5 -> {
                assassinVeil = resolved;
                assassinVeilAvailable = available;
            }
            case 6 -> {
                fighterTempo = (int) resolved;
                fighterTempoAvailable = available;
            }
            case 7 -> {
                fighterFeral = resolved;
                fighterFeralAvailable = available;
            }
            case 8 -> {
                tankerPoise = resolved;
                tankerPoiseAvailable = available;
            }
        }
    }

    public static void clear() {
        assassinTempo = 0;
        fighterPower = 0.0;
        tankWallStacks = 0;
        healerResonance = 0;
        rangerFocus = 0.0;
        assassinVeil = 0.0;
        assassinVeilAvailable = false;
        fighterTempo = 0;
        fighterTempoAvailable = false;
        fighterFeral = 0.0;
        fighterFeralAvailable = false;
        tankerPoise = 0.0;
        tankerPoiseAvailable = false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
