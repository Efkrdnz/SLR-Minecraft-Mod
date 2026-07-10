package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side passive logic for all six player classes.
 *
 * Class IDs (cap.Classes):
 *   1 = Assassin   2 = Mage   3 = Fighter
 *   4 = Tanker     5 = Healer  6 = Ranger
 *
 * Assassin – Shadow Combo
 *   Every melee hit increments a raw counter; tier = counter / 5 (cap 10).
 *   Each tier adds +5% bonus damage to subsequent hits.
 *   Combo decays after 8 s without a new hit.
 *
 * Fighter – Battle Gauge
 *   Each hit fills a power bar by dmg×4 points (0→100).
 *   At 100 the gauge resets and the player receives Speed II + Strength II
 *   for 6 s (Berserker burst).
 *
 * Tanker – Iron Wall
 *   Each hit RECEIVED adds 1 iron-wall stack (cap 10).
 *   Each stack reduces incoming damage by 3 % (cap 30 %).
 *   Stacks decay 10 s after the last hit received.
 *
 * Healer – Resonance
 *   Using Heal Beam or Blessing Mark adds 1 resonance stack (cap 5).
 *   At 5 stacks an AoE burst heals nearby players and resets.
 *   Stacks are permanent — they never decay between casts.
 *
 * Ranger – Focus
 *   Standing still for 1 s starts building focus (0→100 over 2.5 s).
 *   Moving resets focus to 0.
 *   At 100 %, consuming focus (consumeRangerFocus) grants Strength II for 3 s.
 */
@Mod.EventBusSubscriber
public final class ClassPassiveManager {

    // ── PersistentData keys ───────────────────────────────────────────────────
    private static final String A_HITS    = "sl_a_hits";   // int:  assassin raw hit count
    private static final String A_TIMER   = "sl_a_timer";  // long: ms of last hit
    private static final String F_POWER   = "sl_f_power";  // double: 0-100 fighter gauge
    private static final String T_STACKS  = "sl_t_stacks"; // int:  tanker wall stacks
    private static final String T_TIMER   = "sl_t_timer";  // long: ms of last hit received
    private static final String H_STACKS  = "sl_h_stacks"; // int:  healer resonance stacks
    private static final String R_FOCUS   = "sl_r_focus";  // double: 0-100 ranger focus
    private static final String R_LAST_X  = "sl_r_lx";    // double: last tick X
    private static final String R_LAST_Z  = "sl_r_lz";    // double: last tick Z
    private static final String R_STILL   = "sl_r_still"; // long: ms player has been still

    // ── Timings ───────────────────────────────────────────────────────────────
    private static final long   ASSASSIN_DECAY_MS  =  8_000L;
    private static final long   TANK_DECAY_MS      = 10_000L;
    /** ms of stillness before focus starts charging (after the 1 s warm-up). */
    private static final long   RANGER_WARMUP_MS   =  1_000L;
    /** ms to charge from 0 → 100 % after the warm-up. */
    private static final long   RANGER_CHARGE_MS   =  2_500L;

    // ── Caps ──────────────────────────────────────────────────────────────────
    private static final int    ASSASSIN_TIER_MAX  = 10;
    private static final int    TANK_WALL_MAX      = 10;
    private static final int    HEALER_RES_MAX     =  5;

    private ClassPassiveManager() {}

    // ═════════════════════════════════════════════════════════════════════════
    // LivingHurtEvent
    // ═════════════════════════════════════════════════════════════════════════

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // ── Attacker passives (Assassin / Fighter) ────────────────────────────
        Entity src = event.getSource().getEntity();
        if (src instanceof ServerPlayer attacker) {
            int cls = playerClass(attacker);
            if (cls == 1) { // Assassin
                int tier = processAssassinHit(attacker);
                if (tier > 0)
                    event.setAmount(event.getAmount() * (1f + tier * 0.05f));
            } else if (cls == 3) { // Fighter
                processFighterHit(attacker, event.getAmount());
            }
        }

        // ── Defender passive (Tanker) ─────────────────────────────────────────
        if (event.getEntity() instanceof ServerPlayer victim) {
            if (playerClass(victim) == 4) { // Tanker
                int stacks = processTankHit(victim);
                if (stacks > 0)
                    event.setAmount(event.getAmount() * (1f - Math.min(stacks * 0.03f, 0.30f)));
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PlayerTickEvent — decay timers and Ranger focus charging
    // ═════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        switch (playerClass(sp)) {
            case 1 -> tickAssassin(sp);
            case 4 -> tickTank(sp);
            case 6 -> tickRanger(sp);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Per-class logic
    // ═════════════════════════════════════════════════════════════════════════

    // ── Assassin ─────────────────────────────────────────────────────────────

    /** Called when the Assassin lands a hit. Returns current tier (0-10). */
    private static int processAssassinHit(ServerPlayer p) {
        CompoundTag d   = p.getPersistentData();
        long now        = System.currentTimeMillis();

        if (now - d.getLong(A_TIMER) > ASSASSIN_DECAY_MS)
            d.putInt(A_HITS, 0); // decay happened before this hit

        int hits = Math.min(d.getInt(A_HITS) + 1, ASSASSIN_TIER_MAX * 5);
        d.putInt(A_HITS, hits);
        d.putLong(A_TIMER, now);

        int tier = Math.min(hits / 5, ASSASSIN_TIER_MAX);
        sync(p, 0, tier);
        return tier;
    }

    private static void tickAssassin(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.getInt(A_HITS) == 0) return;
        if (System.currentTimeMillis() - d.getLong(A_TIMER) > ASSASSIN_DECAY_MS) {
            d.putInt(A_HITS, 0);
            sync(p, 0, 0);
        }
    }

    // ── Fighter ───────────────────────────────────────────────────────────────

    private static void processFighterHit(ServerPlayer p, float dmg) {
        CompoundTag d   = p.getPersistentData();
        double power    = Math.min(d.getDouble(F_POWER) + dmg * 4.0, 100.0);
        d.putDouble(F_POWER, power);
        sync(p, 1, power);

        if (power >= 100.0) {
            // Berserker burst — reset gauge and grant effects
            d.putDouble(F_POWER, 0.0);
            sync(p, 1, 0.0);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   120, 1, false, true));
        }
    }

    // ── Tanker ────────────────────────────────────────────────────────────────

    /** Called when the Tanker takes a hit. Returns new stack count. */
    private static int processTankHit(ServerPlayer p) {
        CompoundTag d  = p.getPersistentData();
        int stacks     = Math.min(d.getInt(T_STACKS) + 1, TANK_WALL_MAX);
        d.putInt(T_STACKS, stacks);
        d.putLong(T_TIMER, System.currentTimeMillis());
        sync(p, 2, stacks);
        return stacks;
    }

    private static void tickTank(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.getInt(T_STACKS) == 0) return;
        if (System.currentTimeMillis() - d.getLong(T_TIMER) > TANK_DECAY_MS) {
            d.putInt(T_STACKS, 0);
            sync(p, 2, 0);
        }
    }

    // ── Healer ────────────────────────────────────────────────────────────────

    /**
     * Called when the Healer successfully casts a healing skill
     * (Heal Beam or Blessing Mark).  Called from UseSkillOnKeyPressedProcedure.
     */
    public static void onHealerCast(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        int stacks    = Math.min(d.getInt(H_STACKS) + 1, HEALER_RES_MAX);
        d.putInt(H_STACKS, stacks);
        sync(p, 3, stacks);

        if (stacks >= HEALER_RES_MAX) {
            // Resonance burst: AoE heal, then reset
            SololevelingMod.queueServerWork(1, () -> triggerResonanceBurst(p));
            d.putInt(H_STACKS, 0);
            sync(p, 3, 0);
        }
    }

    private static void triggerResonanceBurst(ServerPlayer healer) {
        if (!healer.isAlive()) return;
        // Heal nearby allies
        healer.level().getEntitiesOfClass(Player.class,
                healer.getBoundingBox().inflate(8.0),
                ally -> ally != healer && !ally.isDeadOrDying())
            .forEach(ally -> ally.heal(6f)); // +3 hearts each
        healer.heal(4f); // healer also benefits
    }

    // ── Ranger ────────────────────────────────────────────────────────────────

    private static void tickRanger(ServerPlayer p) {
        CompoundTag d   = p.getPersistentData();
        double curX     = p.getX();
        double curZ     = p.getZ();
        double lastX    = d.getDouble(R_LAST_X);
        double lastZ    = d.getDouble(R_LAST_Z);

        boolean isStill = Math.abs(curX - lastX) < 0.05 && Math.abs(curZ - lastZ) < 0.05;
        d.putDouble(R_LAST_X, curX);
        d.putDouble(R_LAST_Z, curZ);

        if (!isStill) {
            // Reset on movement
            if (d.getDouble(R_FOCUS) > 0 || d.getLong(R_STILL) > 0) {
                d.putDouble(R_FOCUS, 0);
                d.putLong(R_STILL, 0);
                sync(p, 4, 0);
            }
            return;
        }

        // Accumulate still time (~50 ms per tick at 20 TPS)
        long stillMs = Math.min(d.getLong(R_STILL) + 50L, RANGER_WARMUP_MS + RANGER_CHARGE_MS + 500L);
        d.putLong(R_STILL, stillMs);

        // Don't start charging until warm-up passed
        if (stillMs < RANGER_WARMUP_MS) return;

        double oldFocus = d.getDouble(R_FOCUS);
        if (oldFocus >= 100.0) return; // already max, nothing to do

        // Charge rate: 100 % over RANGER_CHARGE_MS
        double inc      = (50.0 / RANGER_CHARGE_MS) * 100.0;
        double newFocus = Math.min(oldFocus + inc, 100.0);
        d.putDouble(R_FOCUS, newFocus);

        // Sync on significant change (every 5 %) or at 100 %
        if ((int)(newFocus / 5) > (int)(oldFocus / 5) || newFocus >= 100.0)
            sync(p, 4, newFocus);
    }

    /**
     * Called from UseSkillOnKeyPressedProcedure when a Ranger uses a ranged
     * skill and focus is at 100 %.  Returns true if focus was consumed.
     */
    public static boolean consumeRangerFocus(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.getDouble(R_FOCUS) < 100.0) return false;
        d.putDouble(R_FOCUS, 0.0);
        d.putLong(R_STILL, 0);
        sync(p, 4, 0.0);
        // Grant a short burst of Strength II
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, true));
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private static int playerClass(ServerPlayer p) {
        return (int) p.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                      .orElse(new SololevelingModVariables.PlayerVariables()).Classes;
    }

    private static void sync(ServerPlayer p, int type, double value) {
        SololevelingMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> p),
                new ClassPassiveMessage(type, value));
    }
}
