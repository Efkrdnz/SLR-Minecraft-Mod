package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.MageQTEHelper;
import net.solocraft.util.MageQTEState;
import net.solocraft.util.QTEResult;
import net.solocraft.util.JobSkillManager;
import net.solocraft.util.FireMageSpellManager;
import net.solocraft.util.BarrierMageSpellManager;
import net.solocraft.util.ArcaneMageSpellManager;
import net.solocraft.util.OrbOfAvariceManager;

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

        if (JobSkillManager.release(entity, power, pressedMs))
            return;

        // ── Mage QTE key release ──────────────────────────────────────────────
        if (MageQTEHelper.MAGE_SKILLS.contains(power)) {

            // Client: end the ring animation and show the result flash immediately
            if (world instanceof Level clientLevel && clientLevel.isClientSide()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    MageQTEState state = MageQTEState.INSTANCE;
					if (state.isActive()) {
						float zoneStart = state.getGoodZoneStart();
						state.endQTE();
						QTEResult clientResult = MageQTEHelper.computeResult(zoneStart, pressedMs);
						state.showResult(clientResult);
					}
                });
            }

            // Server: compute result, apply mana discount, cast the spell
            if (world instanceof Level _lvl && !_lvl.isClientSide()) {
				boolean qteStarted = entity.getPersistentData().getBoolean("mage_casting");
                entity.getPersistentData().putBoolean("mage_casting", false);
				if (qteStarted) {
					float zoneStart = entity.getPersistentData().contains("mage_qte_zone_start")
							? entity.getPersistentData().getFloat("mage_qte_zone_start")
							: MageQTEHelper.computeZoneStart(entity);
					castMageSpellWithQTE(world, entity, power, pressedMs, zoneStart);
				}
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
        double   effectiveMult = mult * OrbOfAvariceManager.manaCostMultiplier(entity);

        double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
        boolean cast = false;

        if (FireMageSpellManager.isQteSkill(power)) {
            cast = FireMageSpellManager.cast(entity, power, result);
        } else if (BarrierMageSpellManager.isQteSkill(power)) {
            cast = BarrierMageSpellManager.cast(entity, power, result);
		} else if (ArcaneMageSpellManager.isQteSkill(power)) {
			cast = ArcaneMageSpellManager.cast(entity, power, result);
        } else switch (power) {
            case "Water Slash" -> {
                int cost = (int) Math.ceil(600 * effectiveMult);
                if (cap.MP < cost)               { mpMsg(entity); return; }
                if (isCd(entity, "Water Slash"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "mana_refresh", 60);
                WaterBulletProcedure.execute(world, entity);
                cast = true;
            }
            case "Curse Sphere" -> {
                // Dynamic mana cost scales with Intelligence
                int cost = (int) Math.ceil((600 + cap.Intelligence * 4) * effectiveMult);
                if (cap.MP < cost) { mpMsg(entity); return; }
                if (isCd(entity, "Curse Sphere")) { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "mana_refresh", 60);
                AirVacuumsProcedure.execute(world, entity);
                cast = true;
            }
            case "Curse Smoke" -> {
                int cost = (int) Math.ceil(600 * effectiveMult);
                if (cap.MP < cost)               { mpMsg(entity); return; }
                if (isCd(entity, "Curse Smoke"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Curse Smoke", 150);
                CurseSmokeCastProcedure.execute(world, ex, ey, ez, entity);
                cast = true;
            }
            case "Curse Chains" -> {
                int cost = (int) Math.ceil(300 * effectiveMult);
                if (cap.MP < cost)                { mpMsg(entity); return; }
                if (isCd(entity, "Curse Chains"))  { cdMsg(entity); return; }
                deductMP(entity, cost);
                CooldownManager.set(entity, "Curse Chains", 150);
                CursedChainsCastProcedure.execute(world, entity);
                cast = true;
            }
            case "Magic Missiles" -> {
                int cost = (int) Math.ceil(500 * effectiveMult);
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
            String manaPercent = String.format("%.0f", effectiveMult * 100);
            String msg = switch (result) {
                case PERFECT -> "§bPERFECT! §7(×" + manaPercent + "% mana)";
                case GOOD    -> "§eGOOD! §7(×" + manaPercent + "% mana)";
                case MISS    -> "§cMISS! §7(×" + manaPercent + "% mana)";
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
