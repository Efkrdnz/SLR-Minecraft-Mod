package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.KaiselinEntity;
import net.solocraft.entity.ShadowSoulEntity;
import net.solocraft.entity.VulcanEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Handles DKC floor failure when the player dies mid-floor.
 *
 * Normal floors (2–9, 11–19)  — kill count and spawn state reset; all
 *   demons/knights belonging to the player are removed so the floor
 *   starts fresh when they return.
 *
 * Floor 10 (Vulcan)           — same as normal floors; if Vulcan has
 *   already spawned it is also discarded so the 50-kill trigger fires
 *   again next attempt.
 *
 * Boss floors (1 = Cerberus, 20 = Baran) — the boss is healed back to
 *   full health and the floor-spawned flag is cleared so the intro
 *   message shows again on re-entry.
 */
@EventBusSubscriber
public class DKCPlayerDeathProcedure {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!DkcFloorRegistry.isDkc(player.level())) return;

        int floor = DKCFloorDetectorProcedure.getCurrentFloor(player);
        if (floor <= 0) return;
		double cleared = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_cleared;
		if (cleared >= floor) return; // A cleared floor must never respawn or pay twice.

        resetFloor((ServerLevel) player.level(), player, floor);
		if (floor == DkcRadiruManager.FLOOR)
			DkcRadiruManager.resetFailedEncounter((ServerLevel) player.level(), player);

        player.sendSystemMessage(Component.literal(
                "§c§lFloor " + floor + " Failed! §7Your progress on this floor has been reset."));
    }

    // ── Called externally if needed (e.g. debug command) ──────────────────────

    public static void resetFloor(ServerLevel level, Player player, int floor) {
        CompoundTag data  = player.getPersistentData();
        String      uuid  = player.getStringUUID();

        // Search area — large enough to cover a full 200-block floor around the
        // player's death position.
        AABB area = DkcFloorBuilder.combatBounds((ServerPlayer) player, floor);

        // ── Remove demons belonging to this player on this floor ──────────────
        for (DemonEntity d : level.getEntitiesOfClass(DemonEntity.class, area)) {
            if (floor == (int) d.getPersistentData().getDouble("dkc_floor_number")
                    && uuid.equals(d.getPersistentData().getString("dkc_spawned_by")))
                d.discard();
        }
        for (DemonKnightEntity k : level.getEntitiesOfClass(DemonKnightEntity.class, area)) {
            if (floor == (int) k.getPersistentData().getDouble("dkc_floor_number")
                    && uuid.equals(k.getPersistentData().getString("dkc_spawned_by")))
                k.discard();
        }
		for (ShadowSoulEntity soul : level.getEntitiesOfClass(ShadowSoulEntity.class, area)) {
			if (floor == (int) soul.getPersistentData().getDouble("dkc_floor_number")
					&& uuid.equals(soul.getPersistentData().getString("dkc_spawned_by")))
				soul.discard();
		}

        // ── Boss-specific handling ────────────────────────────────────────────
        switch (floor) {
            case 1 -> {
                // Cerberus is pre-placed — just heal it back to full
                for (CerberusEntity boss : level.getEntitiesOfClass(CerberusEntity.class, area)) {
					if (uuid.equals(boss.getPersistentData().getString("dkc_spawned_by")))
						boss.discard();
                }
            }
            case 10 -> {
                // Vulcan is dynamically spawned — remove it so the 50-kill
                // trigger fires again on the next attempt
                for (VulcanEntity boss : level.getEntitiesOfClass(VulcanEntity.class, area)) {
                    if (uuid.equals(boss.getPersistentData().getString("dkc_spawned_by")))
                        boss.discard();
                }
            }
            case 20 -> {
                // Baran is pre-placed — heal it back to full
                for (BaranEntity boss : level.getEntitiesOfClass(BaranEntity.class, area)) {
					if (uuid.equals(boss.getPersistentData().getString("dkc_spawned_by")))
						boss.discard();
                }
                for (KaiselinEntity boss : level.getEntitiesOfClass(KaiselinEntity.class, area)) {
                    if (uuid.equals(boss.getPersistentData().getString("dkc_spawned_by")))
						boss.discard();
                }
            }
        }
		DKCDemonSpawnerProcedure.invalidateAttempt(player, floor);

        // ── Clear all floor state so it restarts cleanly on re-entry ─────────
        String prefix = "dkc_floor_" + floor;
        data.remove(prefix + "_spawned");
        data.remove(prefix + "_initial_spawned");
        data.remove(prefix + "_spawning");
        data.remove(prefix + "_complete");
        data.remove(prefix + "_killed");
        data.remove(prefix + "_required");
        data.remove(prefix + "_demon_count");
        data.remove(prefix + "_knight_count");
		data.remove(prefix + "_miniboss_spawned");
		data.remove(prefix + "_spawn_retry_after");
        data.remove(prefix + "_kaiselin_defeated");
        data.remove(prefix + "_kaiselin_spawned");
		data.remove(prefix + "_baran_defeated");
		data.remove(KaiselinEntity.DKC_SOUL_SPAWNED);
        data.remove(prefix + "_enter_time");
        data.remove(prefix + "_boss_defeated");
    }
}
