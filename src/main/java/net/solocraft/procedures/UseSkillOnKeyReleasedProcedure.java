package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.MageQTEHelper;
import net.solocraft.util.MageQTEState;
import net.solocraft.util.QTEResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class UseSkillOnKeyReleasedProcedure {

    public static void execute(LevelAccessor world, Entity entity, int pressedMs) {
        if (entity == null)
            return;

        // Always clean up per-tick effects regardless of which skill was held
        if (entity instanceof LivingEntity _entity)
            _entity.removeEffect(SololevelingModMobEffects.CONSECUTIVE_SLASHES.get());
        {
            double _setval = 0;
            entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                capability.firecharge = _setval;
                capability.syncPlayerVariables(entity);
            });
        }

        String power = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                .orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower;

        // ── Mage QTE key release ──────────────────────────────────────────────
        if (MageQTEHelper.MAGE_SKILLS.contains(power)) {

            // Client: end the ring animation and show the result flash immediately
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                MageQTEState state = MageQTEState.INSTANCE;
                float zoneStart = state.isActive() ? state.getGoodZoneStart() : MageQTEHelper.computeZoneStart(entity);
                state.endQTE();
                QTEResult clientResult = MageQTEHelper.computeResult(zoneStart, pressedMs);
                state.showResult(clientResult);
            });

            // Server: compute result, apply mana discount, cast the spell
            if (world instanceof Level _lvl && !_lvl.isClientSide()) {
                entity.getPersistentData().putBoolean("mage_casting", false);
                float zoneStart = entity.getPersistentData().contains("mage_qte_zone_start")
                        ? entity.getPersistentData().getFloat("mage_qte_zone_start")
                        : MageQTEHelper.computeZoneStart(entity);
                castMageSpellWithQTE(world, entity, power, pressedMs, zoneStart);
                entity.getPersistentData().remove("mage_qte_zone_start");
            }
            return; // skip Critical Attack / Mutilation handlers below
        }

        // ── Non-mage key release ──────────────────────────────────────────────
        if (power.equals("Critical Attack")) {
            if (entity.getPersistentData().getBoolean("Critical_Attack_Targetting")) {
                CriticalAttackUseProcedure.execute(world, entity);
                entity.getPersistentData().putBoolean("Critical_Attack_Targetting", false);
                entity.getPersistentData().putString("CriticalAttackTarget", "");
            }
        }
        if (power.equals("Mutilation")) {
            if (entity.getPersistentData().getBoolean("Mutilation_Targetting")) {
                MutilationUseProcedure.execute(world, entity);
                entity.getPersistentData().putBoolean("Mutilation_Targetting", false);
                entity.getPersistentData().putString("MutilationTarget", "");
            }
        }
        entity.getPersistentData().putBoolean("Critical_Attack_Targetting", false);
        entity.getPersistentData().putBoolean("Mutilation_Targetting", false);
        entity.getPersistentData().putString("CriticalAttackTarget", "");
    }

    // ── Mage QTE spell dispatch (server-side only) ────────────────────────────

    /**
     * Computes the QTE result from pressedMs, applies the mana-cost multiplier,
     * then casts the appropriate mage spell if mana and cooldown requirements are met.
     */
    private static void castMageSpellWithQTE(LevelAccessor world, Entity entity,
                                              String power, int pressedMs, float zoneStart) {
        var cap = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new SololevelingModVariables.PlayerVariables());

        QTEResult result   = MageQTEHelper.computeResult(zoneStart, pressedMs);
        double   mult      = MageQTEHelper.getManaCostMultiplier(result, cap.Intelligence);

        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        boolean cast = false;

        switch (power) {
            case "Fireball" -> {
                int cost = (int) Math.ceil(300 * mult);
                if (cap.MP < cost)          { mpMsg(entity); return; }
                if (isCd(entity, "Fireball")) { cdMsg(entity); return; }
                deductMP(entity, cost);
                FireballProcedure.execute(entity); // sets cooldown "Fireball" 140 internally
                cast = true;
            }
            case "Fire Rain" -> {
                int cost = (int) Math.ceil(1000 * mult);
                if (cap.MP < cost)            { mpMsg(entity); return; }
                if (isCd(entity, "Fire Rain")) { cdMsg(entity); return; }
                deductMP(entity, cost);
                FireArrowCastProcedure.execute(world, entity);
                cast = true;
            }
            case "Heavy Flame" -> {
                int cost = (int) Math.ceil(550 * mult);
                if (cap.MP < cost)               { mpMsg(entity); return; }
                if (isCd(entity, "Heavy Flame"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Heavy Flame", 80);
                HeavyFlameCastProcedure.execute(entity);
                cast = true;
            }
            case "Flame Tornado" -> {
                int cost = (int) Math.ceil(500 * mult);
                if (cap.MP < cost)                { mpMsg(entity); return; }
                if (isCd(entity, "Flame Tornado")) { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Flame Tornado", 50);
                FireTornadoShootProcedure.execute(world, entity);
                cast = true;
            }
            case "Flame Vortex" -> {
                int cost = (int) Math.ceil(500 * mult);
                if (cap.MP < cost)               { mpMsg(entity); return; }
                if (isCd(entity, "Flame Vortex")) { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Flame Vortex", 50);
                FlameVortexShootProcedure.execute(entity);
                cast = true;
            }
            case "Water Slash" -> {
                int cost = (int) Math.ceil(600 * mult);
                if (cap.MP < cost)               { mpMsg(entity); return; }
                if (isCd(entity, "Water Slash"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "mana_refresh", 60);
                WaterBulletProcedure.execute(world, entity);
                cast = true;
            }
            case "Curse Sphere" -> {
                // Dynamic mana cost scales with Intelligence
                int cost = (int) Math.ceil((600 + cap.Intelligence * 4) * mult);
                if (cap.MP < cost) { mpMsg(entity); return; }
                // Curse Sphere has no per-skill cooldown, only mana_refresh
                deductMP(entity, cost);
                CooldownManager.set(entity, "mana_refresh", 60);
                AirVacuumsProcedure.execute(world, entity);
                cast = true;
            }
            case "Curse Smoke" -> {
                int cost = (int) Math.ceil(600 * mult);
                if (cap.MP < cost)               { mpMsg(entity); return; }
                if (isCd(entity, "Curse Smoke"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Curse Smoke", 150);
                CurseSmokeCastProcedure.execute(world, ex, ey, ez, entity);
                cast = true;
            }
            case "Curse Chains" -> {
                int cost = (int) Math.ceil(300 * mult);
                if (cap.MP < cost)                { mpMsg(entity); return; }
                if (isCd(entity, "Curse Chains"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Curse Chains", 150);
                CursedChainsCastProcedure.execute(world, entity);
                cast = true;
            }
            case "Magic Missiles" -> {
                int cost = (int) Math.ceil(500 * mult);
                if (cap.MP < cost)                  { mpMsg(entity); return; }
                if (isCd(entity, "Magic Missiles"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Magic Missiles", 150);
                MagicMissilesShootProcedure.execute(world, entity);
                cast = true;
            }
        }

        // QTE result feedback — only shown when the spell actually cast
        if (cast && entity instanceof Player _player && !_player.level().isClientSide()) {
            String msg = switch (result) {
                case PERFECT -> "§bPERFECT! §7(×" + String.format("%.0f", mult * 100) + "% mana)";
                case GOOD    -> "§eGOOD! §7(×75% mana)";
                case MISS    -> "§cMISS! §7(full mana cost)";
            };
            _player.displayClientMessage(Component.literal(msg), true);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void deductMP(Entity entity, double amount) {
        entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(cap -> {
            cap.MP -= amount;
            cap.syncPlayerVariables(entity);
        });
    }

    private static boolean isCd(Entity entity, String key) {
        return CooldownManager.isOnCooldown(entity, key);
    }

    private static void mpMsg(Entity entity) {
        if (entity instanceof Player p && !p.level().isClientSide())
            p.displayClientMessage(Component.literal("Not enough MP!"), true);
    }

    private static void cdMsg(Entity entity) {
        if (entity instanceof Player p && !p.level().isClientSide())
            p.displayClientMessage(Component.literal("Ability on cooldown!"), true);
    }
}
