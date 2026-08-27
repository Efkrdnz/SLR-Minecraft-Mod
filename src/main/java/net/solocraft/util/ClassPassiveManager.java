package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;

/**
 * Server-side passive logic for all six player classes.
 *
 * Class IDs (cap.Classes):
 *   1 = Assassin   2 = Mage   3 = Fighter
 *   4 = Tanker     5 = Healer  6 = Ranger
 *
 * Assassin – Tempo
 *   Owned by AssassinSkillManager, not this class. Varied dagger actions build
 *   up to five Tempo, which qualifying finishers then consume.
 *
 * Fighter – Battle Gauge
 *   Each hit fills a power bar by dmg×4 points (0→100).
 *   At 100 the gauge resets and the player receives Speed II + Strength II
 *   for 6 s (Berserker burst).
 *
 * Tanker – Iron Wall
 *   Delegated to TankerSkillManager so mitigation and stack generation share
 *   the single authoritative Tanker damage path.
 *
 * Healer – Resonance
 *   Using Heal Beam or Blessing Mark adds 1 resonance stack (cap 5).
 *   At 5 stacks a burst heals the healer plus up to five genuinely allied
 *   players, ordered by missing health.
 *   Stacks decay one at a time after 10 s without an effective heal.
 *
 * Ranger – Focus / Deadeye
 *   Eligible arrow hits build Focus, with bonuses for distant and marked targets.
 *   A full gauge empowers the next eligible arrow, or fuels evolved Hyper Focus.
 *   Partial Focus is preserved while aiming, decays gradually while idle or
 *   sprinting, and loses a chunk when a nearby attacker hits the Ranger.
 */
@EventBusSubscriber
public final class ClassPassiveManager {

    // ── PersistentData keys ───────────────────────────────────────────────────
    private static final String F_POWER   = "sl_f_power";  // double: 0-100 fighter gauge
    private static final String F_BURST_UNTIL = "sl_f_burst_until";
    private static final String H_STACKS  = "sl_h_stacks"; // int:  healer resonance stacks
    private static final String H_LAST_GAIN = "sl_h_last_gain";
    private static final String R_FOCUS   = "sl_r_focus";  // double: 0-100 ranger focus
    private static final String R_LAST_DECAY = "sl_r_last_decay";

    // ── Timings ───────────────────────────────────────────────────────────────

    // ── Caps ──────────────────────────────────────────────────────────────────
    private static final int    HEALER_RES_MAX     =  5;
    /** Beneficiaries of one Resonance burst, matching the shared ally cap. */
    private static final int    HEALER_BURST_ALLY_CAP = 5;
    private static final double HEALER_BURST_RADIUS   = 8.0D;
    /** Ticks without an effective heal before one Resonance stack is lost. */
    private static final long   HEALER_RES_DECAY_TICKS = 200L;

    private ClassPassiveManager() {}

    /** Clears one player's passive gauges, delayed burst, and owned Fighter buffs. */
    public static void resetPlayerState(ServerPlayer player) {
        if (player == null)
            return;
        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        long burstUntil = data.getLong(F_BURST_UNTIL);
        if (burstUntil >= now) {
            int expectedRemaining = (int) Math.max(0L, burstUntil - now);
            removeOwnedFighterBuff(player, MobEffects.MOVEMENT_SPEED, expectedRemaining);
            removeOwnedFighterBuff(player, MobEffects.DAMAGE_BOOST, expectedRemaining);
        }
        data.remove(F_POWER);
        data.remove(F_BURST_UNTIL);
        data.remove(H_STACKS);
        data.remove(H_LAST_GAIN);
        data.remove(R_FOCUS);
        data.remove(R_LAST_DECAY);
        sync(player, 1, 0.0D);
        sync(player, 3, 0.0D);
        sync(player, 4, 0.0D);
    }

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
    // LivingIncomingDamageEvent
    // ═════════════════════════════════════════════════════════════════════════

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        // ── Attacker passives (Assassin / Fighter) ────────────────────────────
        Entity src = event.getSource().getEntity();
        if (src instanceof ServerPlayer attacker) {
            int cls = playerClass(attacker);
            if (cls == 3) { // Fighter
                processFighterHit(attacker, event.getAmount());
            }
        }

    }

    // ═════════════════════════════════════════════════════════════════════════
    // PlayerTickEvent — decay timers and Ranger focus decay
    // ═════════════════════════════════════════════════════════════════════════

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        tickOwnedFighterBurst(sp);
        if (playerClass(sp) == 5)
            tickHealer(sp);
        if (playerClass(sp) == 6)
            tickRanger(sp);
    }

    /**
     * Resonance stacks used to be permanent, so a Healer could bank a burst
     * indefinitely and fire it the instant a fight started. They now decay one
     * stack at a time after a quiet period, which keeps the passive tied to
     * sustained healing without erasing an entire setup on a single lull.
     */
    private static void tickHealer(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int stacks = data.getInt(H_STACKS);
        if (stacks <= 0)
            return;
        long now = player.level().getGameTime();
        long lastGain = data.getLong(H_LAST_GAIN);
        if (lastGain <= 0L) {
            data.putLong(H_LAST_GAIN, now);
            return;
        }
        if (now - lastGain < HEALER_RES_DECAY_TICKS)
            return;
        data.putInt(H_STACKS, stacks - 1);
        data.putLong(H_LAST_GAIN, now);
        sync(player, 3, stacks - 1);
    }

    private static void tickOwnedFighterBurst(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long burstUntil = data.getLong(F_BURST_UNTIL);
        if (burstUntil <= 0L)
            return;
        long now = player.level().getGameTime();
        if (burstUntil < now) {
            data.remove(F_BURST_UNTIL);
            return;
        }
        if (playerClass(player) == 3)
            return;
        int expectedRemaining = (int) Math.max(0L, burstUntil - now);
        removeOwnedFighterBuff(player, MobEffects.MOVEMENT_SPEED, expectedRemaining);
        removeOwnedFighterBuff(player, MobEffects.DAMAGE_BOOST, expectedRemaining);
        data.remove(F_BURST_UNTIL);
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
            d.putLong(F_BURST_UNTIL, p.level().getGameTime() + 120L);
            sync(p, 1, 0.0);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   120, 1, false, true));
        }
    }

    // ── Healer ────────────────────────────────────────────────────────────────

    /**
     * Called when the Healer successfully casts a healing skill
     * (Heal Beam or Blessing Mark).  Called from UseSkillOnKeyPressedProcedure.
     */
    public static void onHealerCast(ServerPlayer p) {
        if (p == null || playerClass(p) != 5)
            return;
        CompoundTag d = p.getPersistentData();
        int stacks    = Math.min(d.getInt(H_STACKS) + 1, HEALER_RES_MAX);
        d.putInt(H_STACKS, stacks);
        d.putLong(H_LAST_GAIN, p.level().getGameTime());
        sync(p, 3, stacks);

        if (stacks >= HEALER_RES_MAX) {
            // Resonance burst: AoE heal, then reset
            SololevelingMod.queueServerWork(1, () -> triggerResonanceBurst(p));
            d.putInt(H_STACKS, 0);
            sync(p, 3, 0);
        }
    }

    /**
     * Heals the healer plus a capped number of genuinely allied players.
     *
     * <p>This used to heal every {@link Player} within range with no party or
     * hostility filter, so in PvP a Healer restored the health of the people
     * attacking them. Membership now goes through the shared
     * {@link MageCombatHelper#areAllied} check, which treats a blank party
     * string as "no party" instead of matching every other unaffiliated
     * player. Recipients are ordered by missing health so the burst reaches
     * whoever actually needs it before the cap is spent.</p>
     */
    private static void triggerResonanceBurst(ServerPlayer healer) {
        if (!healer.isAlive() || playerClass(healer) != 5) return;

        healer.level().getEntitiesOfClass(Player.class,
                healer.getBoundingBox().inflate(HEALER_BURST_RADIUS),
                ally -> ally != healer && ally.isAlive() && !ally.isDeadOrDying()
                        && !ally.isSpectator()
                        && ally.getHealth() < ally.getMaxHealth()
                        && MageCombatHelper.areAllied(healer, ally))
            .stream()
            .sorted(Comparator.comparingDouble(
                    ally -> ally.getHealth() - ally.getMaxHealth()))
            .limit(HEALER_BURST_ALLY_CAP)
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

	private static void removeOwnedFighterBuff(ServerPlayer player,
			Holder<MobEffect> effect, int expectedRemaining) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null && active.getAmplifier() == 1
                && active.getDuration() <= expectedRemaining + 2)
            player.removeEffect(effect);
    }

    private static void sync(ServerPlayer p, int type, double value) {
        SololevelingMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> p),
                new ClassPassiveMessage(type, value));
    }
}
