package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.MageQTEHelper;
import net.solocraft.util.MageQTEState;
import net.solocraft.util.MageSpellProgression;
import net.solocraft.util.QTEResult;
import net.solocraft.util.JobSkillManager;
import net.solocraft.util.FireMageSpellManager;
import net.solocraft.util.BarrierMageSpellManager;
import net.solocraft.util.ArcaneMageSpellManager;
import net.solocraft.util.CurseMageSpellManager;
import net.solocraft.util.StormMageSpellManager;
import net.solocraft.util.OrbOfAvariceManager;
import net.solocraft.util.AssassinSkillManager;
import net.solocraft.util.TemporaryStatBonusManager;

import net.neoforged.api.distmarker.Dist;
import net.solocraft.network.compat.DistExecutor;

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
            _entity.removeEffect(SololevelingModMobEffects.CONSECUTIVE_SLASHES);
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
        if (AssassinSkillManager.isReworkedSkill(power))
            return;

        // ── Mage QTE key release ──────────────────────────────────────────────
        if (MageQTEHelper.MAGE_SKILLS.contains(power)) {
			boolean authorized = MageSpellProgression.canCastLearnedSkill(entity, power);

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
				if (authorized && qteStarted) {
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
        double   mult      = MageQTEHelper.getManaCostMultiplier(result,
                TemporaryStatBonusManager.effectiveIntelligence(entity));
        double   effectiveMult = mult * OrbOfAvariceManager.manaCostMultiplier(entity);

        boolean cast = false;

        if (FireMageSpellManager.isQteSkill(power)) {
            cast = FireMageSpellManager.cast(entity, power, result);
        } else if (BarrierMageSpellManager.isQteSkill(power)) {
            cast = BarrierMageSpellManager.cast(entity, power, result);
		} else if (ArcaneMageSpellManager.isQteSkill(power)) {
			cast = ArcaneMageSpellManager.cast(entity, power, result);
		} else if (StormMageSpellManager.isQteSkill(power)) {
			cast = StormMageSpellManager.cast(entity, power, result);
		} else if (CurseMageSpellManager.isQteSkill(power)) {
			cast = CurseMageSpellManager.cast(entity, power, result);
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

}
