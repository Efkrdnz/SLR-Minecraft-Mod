package net.solocraft.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only data holder for class-passive display values received from the
 * server via ClassPassiveMessage. The overlay reads from here, and disconnect
 * cleanup prevents one world or server from leaking values into the next.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
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

    private ClassPassiveClientState() {}

    /** Called by ClassPassiveMessage.handler on the client main thread. */
    public static void update(int type, double value) {
        switch (type) {
            case 0 -> assassinTempo     = (int) value;
            case 1 -> fighterPower      = value;
            case 2 -> tankWallStacks    = (int) value;
            case 3 -> healerResonance   = (int) value;
            case 4 -> rangerFocus       = value;
        }
    }

    public static void clear() {
        assassinTempo = 0;
        fighterPower = 0.0;
        tankWallStacks = 0;
        healerResonance = 0;
        rangerFocus = 0.0;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
