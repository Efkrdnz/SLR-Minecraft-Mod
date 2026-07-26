package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
 * Ranger – Focus / Deadeye
 *   Eligible arrow hits build Focus, with bonuses for distant and marked targets.
 *   A full gauge empowers the next eligible arrow, or fuels evolved Hyper Focus.
 *   Partial Focus is preserved while aiming, decays gradually while idle or
 *   sprinting, and loses a chunk when a nearby attacker hits the Ranger.
 */
@Mod.EventBusSubscriber
public final class ClassPassiveManager {

    // ── PersistentData keys ───────────────────────────────────────────────────
    private static final String F_POWER   = "sl_f_power";  // double: 0-100 fighter gauge
    private static final String T_STACKS  = "sl_t_stacks"; // int:  tanker wall stacks
    private static final String T_TIMER   = "sl_t_timer";  // long: ms of last hit received
    private static final String H_STACKS  = "sl_h_stacks"; // int:  healer resonance stacks
    private static final String R_FOCUS   = "sl_r_focus";  // double: 0-100 ranger focus
    private static final String R_LAST_DECAY = "sl_r_last_decay";

    // ── Timings ───────────────────────────────────────────────────────────────
    private static final long   TANK_DECAY_MS      = 10_000L;

    // ── Caps ──────────────────────────────────────────────────────────────────
    private static final int    TANK_WALL_MAX      = 10;
    private static final int    HEALER_RES_MAX     =  5;

    private ClassPassiveManager() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && playerClass(player) == 6)
            sync(player, 4, getRangerFocus(player));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && playerClass(player) == 6)
            sync(player, 4, getRangerFocus(player));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LivingHurtEvent
    // ═════════════════════════════════════════════════════════════════════════

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // ── Attacker passives (Assassin / Fighter) ────────────────────────────
        Entity src = event.getSource().getEntity();
        if (src instanceof ServerPlayer attacker) {
            int cls = playerClass(attacker);
            if (cls == 3) { // Fighter
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
    // PlayerTickEvent — decay timers and Ranger focus decay
    // ═════════════════════════════════════════════════════════════════════════

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        switch (playerClass(sp)) {
            case 4 -> tickTank(sp);
            case 6 -> tickRanger(sp);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Per-class logic
    // ═════════════════════════════════════════════════════════════════════════

    // ── Assassin ─────────────────────────────────────────────────────────────

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
        CompoundTag data = p.getPersistentData();
        double focus = data.getDouble(R_FOCUS);
        if (focus <= 0.0D || focus >= 100.0D)
            return;
        long now = p.level().getGameTime();
        if (now - data.getLong(R_LAST_DECAY) < 10L)
            return;
        data.putLong(R_LAST_DECAY, now);
        // Normal movement is allowed. Sprinting or lowering the bow only
        // decays Focus gradually instead of deleting the entire setup.
        double decay = p.isSprinting() ? 2.0D : (p.isUsingItem() ? 0.0D : 0.5D);
        if (decay > 0.0D)
            addRangerFocus(p, -decay);
    }

    /** Returns true and empties the gauge when the Ranger has 100 Focus. */
    public static boolean consumeRangerFocus(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.getDouble(R_FOCUS) < 100.0) return false;
        d.putDouble(R_FOCUS, 0.0);
        sync(p, 4, 0.0);
        return true;
    }

    public static double getRangerFocus(ServerPlayer p) {
        return p == null ? 0.0D : Mth.clamp(p.getPersistentData().getDouble(R_FOCUS), 0.0D, 100.0D);
    }

    public static void syncRangerFocus(ServerPlayer p) {
        if (p != null && playerClass(p) == 6)
            sync(p, 4, getRangerFocus(p));
    }

    public static void addRangerFocus(ServerPlayer p, double amount) {
        if (p == null || playerClass(p) != 6 || Math.abs(amount) < 0.0001D)
            return;
        CompoundTag data = p.getPersistentData();
        double oldFocus = Mth.clamp(data.getDouble(R_FOCUS), 0.0D, 100.0D);
        double newFocus = Mth.clamp(oldFocus + amount, 0.0D, 100.0D);
        if (Math.abs(newFocus - oldFocus) < 0.0001D)
            return;
        data.putDouble(R_FOCUS, newFocus);
        if ((int) (newFocus / 2.0D) != (int) (oldFocus / 2.0D)
                || newFocus == 0.0D || newFocus == 100.0D)
            sync(p, 4, newFocus);
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
